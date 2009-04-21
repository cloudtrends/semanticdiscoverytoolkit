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
package org.sd.cluster.job.work;

import org.sd.cluster.config.ClusterContext;
import org.sd.io.FileUtil;
import org.sd.io.Publishable;
import org.sd.util.MathUtil;
import org.sd.util.StatsAccumulator;
import org.sd.util.thread.BlockingThreadPool;
import org.sd.util.thread.HookedRunnable;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread pool for handling work units in multiple threads.
 * 
 * @author Spence Koehler
 */
public class WorkPool {
  
  /**
   * Millis to wait between checking for available slot.
   * <p>
   * Note that this will be a limiting factor in how quickly any
   * data can be processed by workers!
   */
  public static final int WAIT_INTERVAL = 10;


  private WorkClient workClient;
  private Worker jobWorker;
  private WorkQueue queue;
  private QueueDesignator queueDesignator;
  private String poolId;
  private final BlockingThreadPool pool;
  private int maxWaitSeconds;
  private LogWrapper failedWorkLog;
  private final AtomicInteger submittedCount = new AtomicInteger(0);

  public WorkPool(ClusterContext clusterContext, String jobIdString, String dataDirName,
                  WorkClient workClient, Worker jobWorker, WorkQueue queue,
                  QueueDesignator queueDesignator, final String poolId, int numThreads,
                  int maxPoolQueueSize, int maxErrors, int maxWaitSeconds) {
    this.workClient = workClient;
    this.jobWorker = jobWorker;
    this.queue = queue;
    this.queueDesignator = queueDesignator;
    this.poolId = poolId;
    this.pool = new BlockingThreadPool(poolId, numThreads, maxPoolQueueSize, maxErrors);

    // maximum number of seconds to wait to add work to the pool before killing work.
    this.maxWaitSeconds = maxWaitSeconds;

    // open failed work log
    this.failedWorkLog = new LogWrapper(clusterContext, jobIdString, dataDirName, "failedWork.log", false);
  }

  public String getDescription() { 
    return "WorkPool-" + poolId;
  }

  public boolean isUp() {
    return pool.isUp();
  }

  public BlockingThreadPool getPool() {
    return pool;
  }

  public void shutdown(boolean now) {
    if (!pool.isShutdown()) {
      System.out.println(new Date() + ": " + getDescription() + ": NOTE: shutting down work pool!");

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

    failedWorkLog.close();
  }

  public boolean addWork(AtomicBoolean pause) {
    boolean result = true;

    if (waitForAvailableSlot(pause)) {
      final WorkResponse workResponse = getWorkResponse(pause);

      if (workResponse != null) {
        final WorkResponseStatus status = workResponse.getStatus();

        switch (status) {
        case WORK :
          result = doAddWork(workResponse.getKeyedWork(), pause);
          break;
        case DONE :
          System.out.println(new Date() + " : " + getDescription() + " : NOTE : got 'DONE' workResponse from workClient!");
          result = false;
          break;
        case WAITING :
          result = true;
          break;
        case DOWN :
          result = false;
          break;
        case ERROR :
//todo: log the error?
          result = false;
          break;
        case OK :
          result = true;
          break;
        }
      }
      else {
        System.out.println(new Date() + " : " + getDescription() + " : NOTE : couldn't get work. Quitting.");
        result = false;
      }
    }

    return result;
  }

  private final boolean waitForAvailableSlot(AtomicBoolean pause) {
    boolean result = false;

//changed from 1000 to 10 using WAIT_INTERVAL.
//    final int waitInterval = Math.min(maxWaitSeconds * 100, 1000);  // 1/10th the maxWait or 1000 millis (1 sec), whichever is less.
    for (int tryNum = 0; tryNum < 3; ++tryNum) {
      if (pause != null && pause.get()) break;
      if (pool.waitForAvailableSlot(maxWaitSeconds * 1000, WAIT_INTERVAL, pause)) {
        result = true;
        break;
      }
    }

    return result;
  }

  private final WorkResponse getWorkResponse(AtomicBoolean pause) {
    WorkResponse result = null;

    int numRetries = 3;    //default=3
    long sleepTime = 500;  //default=500ms
    long reportWaitingTime = 900000;  //default=15min

    long waitTime = 0;

    for (int tryNum = 0; numRetries <= 0 || tryNum < numRetries; ++tryNum) {
      if (pause != null && pause.get()) break;
      result = workClient.getWork(null, queue, pause);

      if (result == null || result.getStatus() == WorkResponseStatus.WAITING) {
        if (result != null && result.getStatus() == WorkResponseStatus.WAITING) {
          final KeyedWork keyedWork = result.getKeyedWork();
          if (keyedWork != null) {
            WaitingKeyedWork waitingKeyedWork = (WaitingKeyedWork)keyedWork;
            numRetries = waitingKeyedWork.getRetries();
            sleepTime = waitingKeyedWork.getSleepTime();
          }
        }

        boolean logMessage = false;
        String msg = null;
        if (result == null) {
          waitTime = 0;
          logMessage = true;
        }
        if (result != null) {
          msg = "WAITING";
          waitTime += sleepTime;

          // only log the message once every so often, including the first time through.
          logMessage = ((waitTime % reportWaitingTime) <= sleepTime);
        }

        if (logMessage) {
          System.out.println(new Date() + " : " + getDescription() + " : NOTE : got '" + msg + "' workResponse from workClient! (try=" + (tryNum + 1) + "/" + numRetries + ") sleepTime=" + sleepTime + " totalWaitingTime=" + waitTime);
        }

        if (result != null) tryNum = -1;  // wait until paused.

        try {
          Thread.sleep(sleepTime);
        }
        catch (InterruptedException e) {
          break;
        }
      }
      else {
        break;
      }
    }

    return result;
  }

  private final boolean doAddWork(KeyedWork keyedWork, AtomicBoolean pause) {
    boolean result = false;

    for (int tryNum = 0; tryNum < 3 && !result; ++tryNum) {
      if (pause != null && pause.get()) return result;

      // add work to the thread pool, blocking until able to do so or timeout is reached.
      result = addWork(keyedWork, pause);

      if (!result) {
        if (tryNum > 0) {
          System.out.println(new Date() + " : " + getDescription() + " : NOTE : timed out adding work '" + keyedWork.getWork() + "' to thread pool! Aborting a prior!");
        }

        // kill the longest running thread in the blocking thread pool and retry adding this work unit
        if (tryNum == 2) {
          pool.killLongestRunningThread();
        }

        // give the killed thread a little time to die or another a little time to finish.
        try {
          Thread.sleep(100L);
        }
        catch (InterruptedException e) {
          // ignore.
        }
      }
    }

    return result;
  }

  private final boolean addWork(KeyedWork keyedWork, AtomicBoolean pause) {
    submittedCount.incrementAndGet();
    return pool.add(new WorkHandler(jobWorker, keyedWork, pause), maxWaitSeconds, TimeUnit.SECONDS);
  }

  public void waitUntilFinished(long waitInterval) {
    Long killEntriesRunningLongerThanThis = (maxWaitSeconds <= 0) ? null : new Long(maxWaitSeconds * 1000 + 1);

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
            System.out.println(new Date() + ": " + getDescription() + ": Killing '" + runner.getWorkUnit() + "' runner! (runningTime=" + runningTime + ", limit=" + killEntriesRunningLongerThanThis + ")");
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

  public Map<Long, Publishable> getRunningWork() {
    final Map<Long, Publishable> result = new LinkedHashMap<Long, Publishable>();
    final Map<Long, HookedRunnable> runnables = pool.getRunning();
    for (Map.Entry<Long, HookedRunnable> entry : runnables.entrySet()) {
      final Long runTime = entry.getKey();
      final WorkHandler wh = (WorkHandler)entry.getValue();
      result.put(runTime, wh.getWorkUnit());
    }
    return result;
  }

  public int getNumSubmitted() {
    return submittedCount.get();
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    final long upTime = new Date().getTime() - pool.getStartDate().getTime();
    final StatsAccumulator opTimes = pool.getOperationTimes();

    result.
      append("upTime:").append(MathUtil.timeString(upTime, true)).
      append("  queued:").append(pool.getNumQueued()).
      append("  active:").append(pool.getNumRunningThreads()).
      append("\n  runTime:").append(MathUtil.timeString((long)opTimes.getSum(), true)).
      append("  [n=").append(opTimes.getN()).
      append(",ave=").append(MathUtil.timeString((long)(opTimes.getMean() + 0.5), false)).
      append(",min=").append(MathUtil.timeString((long)opTimes.getMin(), false)).
      append(",max=").append(MathUtil.timeString((long)opTimes.getMax(), false)).
      append(']').
      append(" server=").append(workClient.getServer());

    final String jobWorkerStatus = jobWorker.getStatusString();
    if (jobWorkerStatus != null) {
      result.append("\n  ").append(jobWorkerStatus);
    }

    return result.toString();
  }

  private final class WorkHandler implements HookedRunnable {
    private Worker jobWorker;
    private KeyedWork keyedWork;
    private Publishable workUnit;
    private boolean didRelease;
    private AtomicBoolean pause;
    private AtomicBoolean die = new AtomicBoolean(false);

    public WorkHandler(Worker jobWorker, KeyedWork keyedWork, AtomicBoolean pause) {
      this.jobWorker = jobWorker;
      this.keyedWork = keyedWork;
      this.workUnit = keyedWork == null ? null : keyedWork.getWork();
      this.didRelease = false;
      this.pause = pause;
    }

    public Publishable getWorkUnit() {
      return workUnit;
    }

    public void preRunHook() {
    }

    public void postRunHook() {
      if (!didRelease) doRelease();
    }

    private final void doRelease() {
      try{
        didRelease = true;
//todo: do we need to 'release' work? report in logs? report to a server?
//        job.release(unitOfWork);
      }
      catch (Exception e) {
        System.err.println(new Date() + ": " + getDescription() + ": error while releasing workUnit='" + workUnit + "'!");
        e.printStackTrace(System.err);
      }
    }

    public void exceptionHook(Throwable t) {
//      workUnit.setWorkStatus(WorkStatus.ERROR);

      System.err.println(new Date() + ": " + getDescription() + ": ***ERROR: Unexpected Failure during '" + workUnit + "'!");
      final String stackTrace = FileUtil.getStackTrace(t);
      System.err.println(stackTrace);

//      workUnit.recordFailure(stackTrace);
      if (!didRelease) doRelease();

      //todo: configure to pause on errors if desired?
    }

    public void reclaim() {
//      unitOfWork.setWorkStatus(WorkStatus.INITIALIZED);
    }

    public void die() {
      System.err.println(new Date() + ": " + getDescription() + ": WorkPool.WorkHandler: received signal to die! workUnit=" + workUnit);
      die.set(true);
    }

    public void run() {
//       if (!unitOfWork.compareAndSetWorkStatus(WorkStatus.SUBMITTED, WorkStatus.PROCESSING)) {
// //        System.err.println("WorkPool.WorkHandler: Unexpected unit of work '" + unitOfWork + "' status! (Submitted -> Processing) Ignoring.");
//         unitOfWork.setWorkStatus(WorkStatus.PROCESSING);
// //        return;
//       }
        
      if (!jobWorker.performWork(keyedWork, die, pause, queueDesignator, queue)) {
//        unitOfWork.recordFailure(null);
//        System.err.println(new Date() + ": " + getDescription() + ": WorkUnit '" + workUnit + "' failed!");
        failedWorkLog.writeLine(workUnit.toString(), true, false);
      }
      else {
//        unitOfWork.setWorkStatus(WorkStatus.COMPLETED);
      }
    }
  }
}
