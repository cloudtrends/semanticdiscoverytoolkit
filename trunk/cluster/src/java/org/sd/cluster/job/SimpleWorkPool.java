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
import org.sd.util.MathUtil;
import org.sd.util.StatsAccumulator;
import org.sd.util.thread.BlockingThreadPool;
import org.sd.util.thread.HookedRunnable;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple thread pool for processing data in multiple threads.
 * <p>
 * NOTE: This class was made as a more simple alternative to the WorkPool
 *       class as we begin phasing away from "OldAbstractJob" and "UnitOfWork"
 *       abstractions (batch-oriented) and toward steady-state processing.
 *
 * @author Spence Koehler
 */
public class SimpleWorkPool {
  
  private final BlockingThreadPool pool;
  private final AtomicInteger submittedCount = new AtomicInteger(0);

  public SimpleWorkPool(final String poolId, int numThreads, int maxErrors) {
    this.pool = new BlockingThreadPool(poolId, numThreads, 1, maxErrors);
  }

  public boolean isUp() {
    return pool.isUp();
  }

  public BlockingThreadPool getPool() {
    return pool;
  }

  public Date getStartDate() {
    return pool.getStartDate();
  }

  public int getActiveUnits() {
    return pool.getNumRunningThreads();
  }

  public String getStatsString() {
    final StringBuilder result = new StringBuilder();

    final StatsAccumulator opTimes = pool.getOperationTimes();
    result.append("runTime:").append(MathUtil.timeString((long)opTimes.getSum(), true));
    result.
      append("  [n=").append(opTimes.getN()).
      append(",ave=").append(MathUtil.timeString((long)(opTimes.getMean() + 0.5), false)).
      append(",min=").append(MathUtil.timeString((long)opTimes.getMin(), false)).
      append(",max=").append(MathUtil.timeString((long)opTimes.getMax(), false)).
      append(']');

    return result.toString();
  }

  public StatsAccumulator getOperationTimes() {
    return pool.getOperationTimes();
  }

  public void shutdown(boolean now) {
    if (pool.isUp()) {
      System.out.println("NOTE: shutting down work pool!");

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

  public boolean addWork(SteadyStateJob job, String workRequestString) {
    submittedCount.incrementAndGet();
    return pool.add(new WorkHandler(job, workRequestString), 600, TimeUnit.SECONDS);  //todo: parameterize
  }

  public int getNumSubmitted() {
    return submittedCount.get();
  }

  private final class WorkHandler implements HookedRunnable {
    private SteadyStateJob job;
    private String workRequestString;
    private boolean didRelease;
    private boolean failed;
    private AtomicBoolean die = new AtomicBoolean(false);

    public WorkHandler(SteadyStateJob job, String workRequestString) {
      this.job = job;
      this.workRequestString = workRequestString;
      this.didRelease = false;
      this.failed = false;
    }

    public void preRunHook() {
      System.out.println(new Date() + ": Starting work request '" + workRequestString + "'.");
    }

    public void postRunHook() {
      doRelease();
    }

    private final void doRelease() {
      if (didRelease) return;
      didRelease = true;
      
      if (!failed) {
        // log completion of the workRequestString
        System.out.println(new Date() + ": Completed work request '" + workRequestString + "'.");
      }
    }

    public void exceptionHook(Throwable t) {
      System.err.println(new Date() + ": ***ERROR: Unexpected failure while processing '" + workRequestString + "'!");
      final String stackTrace = FileUtil.getStackTrace(t);
      System.err.println(stackTrace);

      failed = true;
      doRelease();

      //todo: configure to pause job on errors if desired?
    }

    public void reclaim() {
      System.out.println(new Date() + ": Interrupting work request '" + workRequestString + "'!");
    }

    public void die() {
      die.set(true);
    }

    public void run() {
      if (!job.processWorkRequest(workRequestString, die)) {
        System.out.println(new Date() + ": Work request '" + workRequestString + "' failed!");
        failed = true;
      }
    }
  }
}
