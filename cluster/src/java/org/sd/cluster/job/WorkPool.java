/*
    Copyright 2009 Semantic Discovery, Inc. (www.semanticdiscovery.com)

    This file is part of the Semantic Discovery Toolkit.

    The Semantic Discovery Toolkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Semantic Discovery Toolkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.cluster.job;


import org.sd.io.FileUtil;
import org.sd.util.thread.BlockingThreadPool;
import org.sd.util.thread.HookedRunnable;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread pool for handling units of work in multiple threads.
 * <p>
 * @author Spence Koehler
 */
public class WorkPool {
  
  private final BlockingThreadPool pool;
  private int maxWaitSeconds;
  private final AtomicInteger submittedCount = new AtomicInteger(0);

  public WorkPool(final String poolId, int numThreads, int maxErrors, int maxWaitSeconds) {
    this.pool = new BlockingThreadPool(poolId, numThreads, 1, maxErrors);

    // maximum number of seconds to wait to add work to the pool before killing work.
    this.maxWaitSeconds = maxWaitSeconds;
  }

  public boolean isUp() {
    return pool.isUp();
  }

  public BlockingThreadPool getPool() {
    return pool;
  }

  public void shutdown(boolean now) {
    if (pool.isUp()) {
      System.out.println(new Date() + ": NOTE: shutting down work pool!");

      List<HookedRunnable> runnables = null;
      if (!now) {  // do orderly shutdown
        pool.stopAcceptingWork();
        pool.waitForQueueToEmpty(5000, 50); //todo: parameterize these values.
        runnables = pool.shutdown(false);
        pool.waitUntilDone(5, TimeUnit.SECONDS);  //todo: parameterize these values.
      }
      else {
        runnables = pool.shutdown(true);
      }

      if (runnables != null) {
        for (HookedRunnable reclaimed : runnables) {
          // reclaim each unit of work
          ((WorkHandler)reclaimed).reclaim();
        }
      }
    }
  }

  public boolean addWork(OldAbstractJob job, UnitOfWork unitOfWork, AtomicBoolean pause) {
    submittedCount.incrementAndGet();
    return pool.add(new WorkHandler(job, unitOfWork, pause), maxWaitSeconds, TimeUnit.SECONDS);
  }

  public void waitUntilFinished(Long killEntriesRunningLongerThanThis, long waitInterval) {
    while (true) {

      final Map<Long, HookedRunnable> running = pool.getRunning();
      if (running.size() == 0) break;
      boolean killed = false;

      if (killEntriesRunningLongerThanThis != null) {
        for (Map.Entry<Long, HookedRunnable> entry : running.entrySet()) {
          final Long runningTime = entry.getKey();
          final WorkHandler runner = (WorkHandler)entry.getValue();
        
          if (runningTime > killEntriesRunningLongerThanThis) {
            // kill runner
            System.out.println(new Date() + ": Killing '" + runner.getUnitOfWork() + "' runner! (runningTime=" + runningTime + ", limit=" + killEntriesRunningLongerThanThis + ")");
            runner.die();
            killed = true;
          }
        }
      }

      final long timeToWait = killed ? 1000 : waitInterval;  // wait a shorter time before looping
      try {
        Thread.sleep(timeToWait);
      }
      catch (InterruptedException e) {
        break;  // stop waiting to finish.
      }
    }
  }

  public Map<Long, UnitOfWork> getRunningWork() {
    final Map<Long, UnitOfWork> result = new LinkedHashMap<Long, UnitOfWork>();
    final Map<Long, HookedRunnable> runnables = pool.getRunning();
    for (Map.Entry<Long, HookedRunnable> entry : runnables.entrySet()) {
      final Long runTime = entry.getKey();
      final WorkHandler wh = (WorkHandler)entry.getValue();
      result.put(runTime, wh.getUnitOfWork());
    }
    return result;
  }

  public int getNumSubmitted() {
    return submittedCount.get();
  }

  private final class WorkHandler implements HookedRunnable {
    private OldAbstractJob job;
    private UnitOfWork unitOfWork;
    private boolean didRelease;
    private AtomicBoolean pause;
    private AtomicBoolean die = new AtomicBoolean(false);

    public WorkHandler(OldAbstractJob job, UnitOfWork unitOfWork, AtomicBoolean pause) {
      this.job = job;
      this.unitOfWork = unitOfWork;
      this.didRelease = false;
      this.pause = pause;
      unitOfWork.setWorkStatus(WorkStatus.SUBMITTED);
    }

    public UnitOfWork getUnitOfWork() {
      return unitOfWork;
    }

    public void preRunHook() {
    }

    public void postRunHook() {
      if (!didRelease) doRelease();
    }

    private final void doRelease() {
      try{
        didRelease = true;
        job.release(unitOfWork);
      }
      catch (IOException e) {
        System.err.println("error while releasing unitOfWork='" + unitOfWork + "'!");
        e.printStackTrace(System.err);
      }
    }

    public void exceptionHook(Throwable t) {
      unitOfWork.setWorkStatus(WorkStatus.ERROR);

      System.err.println(new Date() + ": ERROR : Unexpected Failure during '" + unitOfWork + "'!");
      final String stackTrace = FileUtil.getStackTrace(t);
      System.err.println(stackTrace);

      unitOfWork.recordFailure(stackTrace);
      if (!didRelease) doRelease();

      //todo: configure to pause on errors if desired?
    }

    public void reclaim() {
      unitOfWork.setWorkStatus(WorkStatus.INITIALIZED);
    }

    public void die() {
      System.err.println("WorkPool.WorkHandler: received signal to die! workUnit=" + unitOfWork);
      die.set(true);
    }

    public void run() {
      if (!unitOfWork.compareAndSetWorkStatus(WorkStatus.SUBMITTED, WorkStatus.PROCESSING)) {
//        System.err.println("WorkPool.WorkHandler: Unexpected unit of work '" + unitOfWork + "' status! (Submitted -> Processing) Ignoring.");
        unitOfWork.setWorkStatus(WorkStatus.PROCESSING);
//        return;
      }
        
      if (!job.doNextOperation(unitOfWork, die, pause)) {
        unitOfWork.recordFailure(null);
        System.err.println("UnitOfWork '" + unitOfWork + "' failed!");
      }
      else {
        unitOfWork.setWorkStatus(WorkStatus.COMPLETED);
      }
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.
        append("workUnit=").append(unitOfWork).
        append(",didRelease=").append(didRelease).
        append(",die=").append(die);

      return result.toString();
    }
  }
}
