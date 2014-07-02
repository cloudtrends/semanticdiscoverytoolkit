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


import org.sd.cluster.config.ClusterContext;
import org.sd.cluster.config.Config;
import org.sd.cluster.io.Context;
import org.sd.cluster.io.Response;
import org.sd.io.Publishable;
import org.sd.util.MathUtil;
import org.sd.util.StatsAccumulator;
import org.sd.util.Timer;
import org.sd.util.thread.BlockingThreadPool;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A job is a type of message handled by a cluster node.
 * <p>
 * When handled, a job registers with a manager to allow following progress.
 *
 * @author Spence Koehler
 */
public abstract class OldAbstractJob extends AbstractJob {
  
  public static int MAX_ERRORS = 100;  // automatically stop after 100 errors.

  /**
   * Get the work factory that will serve up work units to this job.
   */
  protected abstract WorkFactory getWorkFactory() throws IOException;

  /**
   * Get the total number of work units for this job instance to process.
   */
//  protected abstract int getNumWorkUnits();
//todo: remove this.

  /**
   * Hook to run when the job is paused.
   */
  protected abstract void runPauseHook();

  /**
   * Hook to run after each operation is submitted to the thread pool.
   */
  protected abstract void runRunningHook();

  /**
   * Do the next operation in a thread-safe way.
   * <p>
   * NOTES:
   * <ol>
   * <li>Do not 'synchronize' this method to make it thread-safe; rather,
   *     make sure all instance accesses from within the implementation
   *     are managed in a thread-safe manner.
   * <li>The unit of work's status will be set by the underlying infrastructure
   *     and need not be set by extending classes.
   * <li>Exceptions thrown while executing this method will end up being
   *     submitted to the unitOfWork.recordFailure method.
   * </ol>
   *
   * @return true if the unit of work was successfuly handled.
   */
  public abstract boolean doNextOperation(UnitOfWork unitOfWork, AtomicBoolean die, AtomicBoolean pause);

  /**
   * Try to find more work to do.
   *
   * @return true if more work was found; otherwise false.
   */
  protected abstract boolean findMoreWorkToDo();


  private int maxTimePerUnit;

  private transient String namePrefix;
  private transient int jobLevel;
  private transient WorkPool workPool = null;
  private transient WorkFactory workFactory = null;
  private transient AtomicBoolean pause = new AtomicBoolean(false);
  private transient Timer allDoneTimer = new Timer(600000);  // 10 minute timer
  private transient Timer incompleteWorkFactoryTimer = new Timer(600000);  // 10 minute timer

  protected OldAbstractJob() {
    super();
    this.maxTimePerUnit = 600;  // default=10 minutes
  }

  protected OldAbstractJob(int numThreads, String jobId, String groupName, boolean beginImmediately) {
    super(numThreads, jobId, groupName, beginImmediately);
    this.maxTimePerUnit = 600;  // default=10 minutes;
    this.namePrefix = null;
  }

  protected OldAbstractJob(Properties properties) {
    super(properties);
    this.namePrefix = null;

    final String mtpuString = properties.getProperty("maxTimePerUnit");
    if (mtpuString != null) {
      this.maxTimePerUnit = Integer.parseInt(mtpuString);
    }
    else {
      this.maxTimePerUnit = 600;  // default=10 minutes
    }
  }

  protected WorkPool getWorkPool() {
    return workPool;
  }

  protected String getWorkString(UnitOfWork workUnit) {
    String result = ((StringUnitOfWork)workUnit).getString();

    if (result.startsWith(namePrefix)) {
      // strip "/mnt/<nodename>" when it is this node!
      result = result.substring(namePrefix.length() - 1);
    }

    return result;
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);
    dataOutput.writeInt(maxTimePerUnit);
  }

  /**
   * Read this message's contents from the dataInput stream that was written by
   * this.write(dataOutput).
   * <p>
   * NOTE: this requires all implementing classes to have a default constructor
   *       with no args.
   *
   * @param dataInput  the data output to write to.
   */
  public void read(DataInput dataInput) throws IOException {
    super.read(dataInput);
    this.maxTimePerUnit = dataInput.readInt();
  }
  
  public void shutdown(boolean now) {

System.out.println(new Date() + ": OldAbstractJob-" + getJobId() + " shutting down! (now=" + now + ")");

    if (workPool != null) {
      workPool.shutdown(now);
    }
    if (workFactory != null) {
      try {
        workFactory.close();
      }
      catch (IOException e) {
        System.err.println(new Date() + ": job '" + getJobId() + "': problem closing workFactory during shutdown!");
        e.printStackTrace(System.err);
      }
    }
  }

  public final boolean preHandleHook(Context context) {
    if (!super.preHandleHook(context)) return false;

    final ClusterContext clusterContext = getClusterContext();
    final Config config = clusterContext.getConfig();
    this.workPool = new WorkPool(getJobId(), getNumThreads(), MAX_ERRORS, maxTimePerUnit);
    try {
      this.workFactory = getWorkFactory();
    }
    catch (IOException e) {
      setError(true);
      System.err.println("Unable to initialize work factory!");
      e.printStackTrace(System.err);
      return false;
    }
    return true;
  }

  public final void release(UnitOfWork unitOfWork) throws IOException {
    if (workFactory != null) workFactory.release(unitOfWork);
  }

  /**
   * Hook called when we start handling the job.
   * <p>
   * A common operation is to initialize the totalOps.
   *
   * @return true to start handling right away; false to delay handling
   *         until manually started.
   */
  protected boolean startHandlingHook() {
    if (!super.startHandlingHook()) return false;

    final Config config = getConfig();

    // set up namePrefix
    this.namePrefix = "/mnt/" + config.getMachineName().toLowerCase() + "/";

    return true;
  }

  public void start() {
    if (getStatus() == JobStatus.FINISHED ||
        getStatus() == JobStatus.INTERRUPTED) return;

    setStatus(JobStatus.RUNNING);

    while (getStatus() == JobStatus.RUNNING && performNextOperation(pause)) {
      runRunningHook();
    }

    if (getStatus() == JobStatus.INTERRUPTED) { // got us out of the running loop
      setStatus(JobStatus.PAUSED);
    }
    else if (getStatus() == JobStatus.RUNNING /*&& workFactory.isComplete()*/) {

      System.out.println(new Date() + ": done performing operations. marking job as finished. waiting for workPool to finish. (" + workPool.getPool().getNumQueued() + ")");
      setStatus(JobStatus.FINISHED);

      // wait for workPool to empty
      workPool.waitUntilFinished(null, 5000); //todo: parameterize "killEntriesRunningLongerThanThisAfterFinished"
      System.out.println(new Date() + ": WorkPool is finished!");

      // run finished hook.
      runFinishedProcessingHook();

      // see if there is something more we can do.
      if (workFactory.isComplete() && findMoreWorkToDo()) {
        // bounce this job through the JobManager
        final LocalJobId localJobId = getLocalJobId();
        if (localJobId != null) {
          getJobManager().handleJobCommand(getClusterContext(), JobCommand.BOUNCE, localJobId, null, null);
        }
      }
    }
  }

  protected void runFinishedProcessingHook() {
    //nothing to do here.
  }

  public void stop() {
    setStatus(JobStatus.INTERRUPTED);
  }

  public void pause() {
    setStatus(JobStatus.PAUSED);
    pause.set(true);
    runPauseHook();
  }

  public void resume() {
    pause.set(false);
    start();
  }

  public void suspend() {
    //todo: suspend to disk for restoration through JobManager in another jvm
  }

  public void setProperties(Map<String, String> properties) {
    super.setProperties(properties);

    final BlockingThreadPool pool = workPool.getPool();
    properties.put("startDate", Long.toString(pool.getStartDate().getTime()));
    properties.put("activeUnits", Integer.toString(pool.getNumRunningThreads()));

    final StatsAccumulator opTimes = pool.getOperationTimes();
    properties.put("opTimes.runTime", Long.toString((long)(opTimes.getSum() + 0.5)));
    properties.put("opTimes.n", Long.toString(opTimes.getN()));
    properties.put("opTimes.ave", Long.toString((long)(opTimes.getMean() + 0.5)));
    properties.put("opTimes.min", Long.toString((long)(opTimes.getMin() + 0.5)));
    properties.put("opTimes.max", Long.toString((long)(opTimes.getMax() + 0.5)));

    properties.put("numToDo", Long.toString(workFactory.getRemainingEstimate()));
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

// %=
// startTime=
// rate= units/[ms,s,m,h]
// timeRemaining=[ms,s,m,h]
// eta=

    final BlockingThreadPool pool = (workPool == null) ? null : workPool.getPool();

    result.append(getJobId());

    if (pool != null) {
      final Date startDate = pool.getStartDate();
      result.append(' ')./*append(getStatus()).*/
        append(" started:").append(dateFormat.format(startDate));

      final long upTime = new Date().getTime() - startDate.getTime();
      result.append("  upTime:").append(MathUtil.timeString(upTime, true));

      final int numQueued = pool.getNumQueued();
      result.append("  queued:").append(numQueued);

      final int activeUnits = pool.getNumRunningThreads();
      result.append("  active:").append(activeUnits);

      final StatsAccumulator opTimes = pool.getOperationTimes();
      result.append("\n   runTime:").append(MathUtil.timeString((long)opTimes.getSum(), true));
      result.
        append("  [n=").append(opTimes.getN()).
        append(",ave=").append(MathUtil.timeString((long)(opTimes.getMean() + 0.5), false)).
        append(",min=").append(MathUtil.timeString((long)opTimes.getMin(), false)).
        append(",max=").append(MathUtil.timeString((long)opTimes.getMax(), false)).
        append(']');

      result.
        append(" todo~").append(workFactory.getRemainingEstimate());
    }

//     final Date endDate = pool.getStopDate();
//     if (endDate!= null) {
//       result.append(" ended:").append(dateFormat.format(endDate));
//     }
//     result.append(" up=").append(pool.isUp());
//     if (numStages > 1) {
//       result.append(" stage:").append(stageNum).append('/').append(numStages);
//     }

//     result.append("\n   ");
//     result.append("threads:").
//       append(pool.getNumRunningThreads()).append('/').append(pool.getNumThreads());
//     result.append(" queued:").
//       append(pool.getNumQueued()).append('/').append(pool.getMaxQueueSize());
//     result.append(" error:").
//       append(pool.getNumErrors()).append('/').append(pool.getMaximumErrors());

//     result.append("\n   ");
//     result.append("rate ").append(pool.getOperationTimes());

// //     result.append(jobId).append(',').
// //       append(getDescription()).append(',').
// //       append(getStatus()).append(',').
// //       append("stage ").append(stageNum).append(" of ").append(numStages).append(',').
// //       append("completed ").append(numCompletedOps()).append(" of ").append(totalOps()).
// //       append(" (").append(numFailed()).append(" failed, ").
// //       append(numErrors()).append(" errors)");

    return result.toString();
  }

  public Response operate(Publishable request) {
    throw new UnsupportedOperationException("Extend SteadyStateJob instead if/when this is needed!\n" +
                                            "Soon, all jobs should be converted to SteadyStateJob and this class should disappear.");
  }

  // perform the next operation, catching/logging unexpected errors so
  // only one unit of work is lost.
  private final boolean performNextOperation(AtomicBoolean pause) {
    UnitOfWork unitOfWork = null;

    // wait until work pool's queue has an opening
    final int waitInterval = Math.min(maxTimePerUnit * 100, 1000);  // 1/10th the maxWait or 1000 millis (1 sec), whichever is less.
    boolean getMoreWork = false;
    for (int tryNum = 0; tryNum < 3; ++tryNum) {
      if (pause != null && pause.get()) break;
      if (workPool.getPool().waitForAvailableSlot(maxTimePerUnit * 1000, waitInterval, pause)) {
        getMoreWork = true;
        break;
      }
    }

    if (getMoreWork) {
      for (int tryNum = 0; tryNum < 3; ++tryNum) {
        try {
          unitOfWork = workFactory.getNext();

          if (unitOfWork == null) {
            System.out.println(new Date() + ": NOTE: got 'null' unitOfWork from workFactory! (try=" + tryNum + ")");

            try {
              Thread.sleep(500);
            }
            catch (InterruptedException e) {
              break;
            }
          }
          else {
            if (unitOfWork.getWorkStatus() == WorkStatus.ALL_DONE) {
              if (allDoneTimer.reachedTimerMillis()) {  // only report once every 10 minutes
                System.out.println(allDoneTimer.getLastCheckDate() + ": NOTE: got 'all done' unit of work from workFatory!");
              }
            }
            else if (unitOfWork.getWorkStatus() == WorkStatus.WAITING) {
              // keep looping until work is available.
            }
            break;
          }
        }
        catch (IOException e) {
          System.err.println(new Date() + ": Couldn't get next unit of work!");
          e.printStackTrace(System.err);
          break;
        }
      }
    }
    else {
      System.out.println(new Date() + ": NOTE: Timed out waiting for opening in queue for more work!");
    }

    boolean result = false;
    if (unitOfWork == null || unitOfWork.getWorkStatus() == WorkStatus.ALL_DONE) {
      if (!workFactory.isComplete()) {
        if (incompleteWorkFactoryTimer.reachedTimerMillis()) {  // only report once every 10 minutes
          System.out.println(incompleteWorkFactoryTimer.getLastCheckDate() + ": NOTE: Won't close incomplete workFactory yet!");
        }
        result = true;
      }
      else {
        // no more work. time to close the workFactory.
        try {
          workFactory.close();
        }
        catch (IOException e) {
          System.err.println(new Date() + ": Error closing workFactory!");
          e.printStackTrace(System.err);
        }
      }
    }
    else if (unitOfWork.getWorkStatus() == WorkStatus.WAITING) {
      result = true;  // todo: set some status flag so that it can be seen that we're waiting for work.
    }
    else {
      int numTries = 0;
      while (!result && numTries++ < 3) {
        if (pause.get()) return false;

        result = workPool.addWork(this, unitOfWork, pause);

        if (!result) {
          if (!workFactory.isComplete()) {
            System.out.println(new Date() + ": NOTE: got 'false' from workPool.addWork, but workFactory isn't complete! Aborting a prior!");

            // kill the longest running thread in the factory's blocking thread pool and retry adding this unitOfWork
            workPool.getPool().killLongestRunningThread();

            // give the killed thread a little time to die
            try {
              Thread.sleep(100L);
            }
            catch (InterruptedException e) {
              // ignore.
            }

            result = true;
          }
          else {
            System.out.println(new Date() + ": NOTE: got 'false' from workPool.addWork, and workFactory is complete! '" + unitOfWork + "'");
            break;
          }
        }
      }
      if (!result && numTries >= 3 && !workFactory.isComplete()) {
        System.err.println(new Date() + " OldAbstractJob.performNextOperation -- couldn't get work submitted! '" + unitOfWork + "'. Skipping. (tries=" + numTries + ")");

        Map<Long, UnitOfWork> runningWork = workPool.getRunningWork();
        System.err.println("\tWorkPool.pool has runningWorkUnits:");
        for (Map.Entry<Long, UnitOfWork> entry : runningWork.entrySet()) {
          final Long runTime = entry.getKey();
          final UnitOfWork workUnit = entry.getValue();
          System.err.println("\t\t(" + MathUtil.timeString(runTime, false) + ") " + workUnit);
        }

        result = true;
      }
    }

    return result;
  }
}
