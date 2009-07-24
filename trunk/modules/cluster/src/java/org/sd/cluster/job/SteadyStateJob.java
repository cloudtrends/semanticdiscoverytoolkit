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


import org.sd.cio.MessageHelper;
import org.sd.cluster.config.BooleanResponse;
import org.sd.cluster.config.ClusterContext;
import org.sd.cluster.config.Config;
import org.sd.cluster.io.Context;
import org.sd.cluster.io.Response;
import org.sd.io.Publishable;
import org.sd.util.MathUtil;
import org.sd.util.StatsAccumulator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Job implementation that waits for and queues up work to be done.
 * <p>
 * @author Spence Koehler
 */
public abstract class SteadyStateJob extends AbstractJob {

  /**
   * Get the steady state job to which this job's output is to be sent, or null
   * if this is the last stage.
   * <p>
   * NOTE: In the cluster, this method will be called only once during the
   *       postHandleHook.
   */
  public abstract SteadyStateJob getNextJob();
  
  /**
   * Process the given work. When successful and there is a next job, the request
   * will be sent to the next job after this method returns.
   * <p>
   * A typical implementation will use workFile.getAbsolutePath()
   * to access an input file for processing and will send a variation of the
   * workFile.getName() to outputDir.getJobDataFile(outputWorkString) to get the
   * output file for processing results and the JobDataFile handle to return from
   * this method.
   *
   * @return the output job data files for input to the next stage's job (ok if
   *         empty); or null to report failure.
   */
  protected abstract List<JobDataFile> doProcessing(JobDataFile workFile, JobDataDirectory outputDir, AtomicBoolean die);


  private String dataDirName;
  private PartitionFunction partitionFunction;

  private transient SimpleWorkPool workPool = null;
  private transient SteadyStateJob nextJob = null;
  private transient JobDataDirectory inputDir = null;
  private transient OutputJobDataDirectory outputDir = null;

  /**
   * Reconstruct a steady state job to be run on the current cluster node.
   */
  protected SteadyStateJob() {
    super();
  }

  /**
   * Construct a steady state job with the given parameters to be sent to
   * cluster nodes to run.
   */
  protected SteadyStateJob(int numThreads, String jobId, String groupName, String dataDirName, PartitionFunction partitionFunction) {
    super(numThreads, jobId, groupName, true);

    this.dataDirName = dataDirName;
    this.partitionFunction = partitionFunction;
  }

  public String getDataDir() {
    return dataDirName;
  }

  public PartitionFunction getPartitionFunction() {
    if (partitionFunction == null) {
      // default to a round-robin partition function.
      partitionFunction = new RoundRobinPartitionFunction(getClusterContext(), getGroupName());
    }

    return partitionFunction;
  }

  public boolean preHandleHook(Context context) {
    if (!super.preHandleHook(context)) return false;

    final ClusterContext clusterContext = getClusterContext();
    final Config config = clusterContext.getConfig();
    this.workPool = new SimpleWorkPool(getJobId(), getNumThreads(), 0);

    return true;
  }

  public void postHandleHook(Context context) {
    this.nextJob = getNextJob();

    final ClusterContext clusterContext = getClusterContext();
    final Config config = clusterContext.getConfig();
    this.inputDir = new InputJobDataDirectory(this);

    String nextJobId = null;
    String nextDataDir = null;
    if (nextJob != null) {
      nextJobId = nextJob.getJobId();
      nextDataDir = nextJob.getDataDir();
    }
    else {
      nextJobId = getJobId();  // this job.
      nextDataDir = getDataDir();
    }

    this.outputDir = new OutputJobDataDirectory(getClusterContext(), getPartitionFunction(), nextJobId, nextDataDir);
  }

  protected final boolean processWorkRequest(String workRequestString, AtomicBoolean die) {
    final JobDataFile workFile = inputDir.getJobDataFile(workRequestString);
    final List<JobDataFile> outputWorkFiles = doProcessing(workFile, outputDir, die);

    if (outputWorkFiles != null && nextJob != null) {
      for (JobDataFile outputWork : outputWorkFiles) {
        // forward the next work requests to the next job.
        outputDir.forwardWork(outputWork, nextJob, getConsole(), 5000);  // todo: parameterize timeout?
      }
    }

    return outputWorkFiles != null;
  }

  public Response operate(Publishable request) {
    boolean result = false;

    if (workPool != null) {
      final String requestString = request.toString();
      result = workPool.addWork(this, requestString);
    }

    return new BooleanResponse(getClusterContext(), result);
  }

  public void start() {  // start processing job's work.
    setStatus(JobStatus.RUNNING);
  }

  public void stop() {  // end processing job in this jvm; could still suspend? can't resume.
    setStatus(JobStatus.INTERRUPTED);
  }

  public void pause() {  // pause a job in this jvm to be resumed.
    setStatus(JobStatus.PAUSED);
  }

  public void resume() {  // resume paused job.
    start();
  }

  public void suspend() {  // suspend to disk for restoration through JobManager in another jvm
    //todo: suspend to disk for restoration through JobManager in another jvm
    pause();
  }

  public void shutdown(boolean now) {  // called when cluster is being shutdown

    System.out.println("SteadyStateJob-" + getJobId() + " shutting down! (now=" + now + ")");

    if (workPool != null) {
      workPool.shutdown(now);
    }

    setStatus(JobStatus.FINISHED);
  }

  public void setProperties(Map<String, String> properties) {
    properties.put("startDate", Long.toString(workPool.getStartDate().getTime()));
    properties.put("activeUnits", Integer.toString(workPool.getActiveUnits()));
    
    final StatsAccumulator opTimes = workPool.getOperationTimes();
    properties.put("opTimes.runTime", Long.toString((long)(opTimes.getSum() + 0.5)));
    properties.put("opTimes.n", Integer.toString(opTimes.getN()));
    properties.put("opTimes.ave", Long.toString((long)(opTimes.getMean() + 0.5)));
    properties.put("opTimes.min", Long.toString((long)(opTimes.getMin() + 0.5)));
    properties.put("opTimes.max", Long.toString((long)(opTimes.getMax() + 0.5)));
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();
    
    final Date startDate = workPool.getStartDate();
    result.append(" started:").append(dateFormat.format(startDate));

    final long upTime = new Date().getTime() - startDate.getTime();
    result.append("  upTime:").append(MathUtil.timeString(upTime, true));

    final int activeUnits = workPool.getActiveUnits();
    result.append("  active:").append(activeUnits);

    final String statsString = workPool.getStatsString();
    result.append("\n  ").append(statsString);

    return result.toString();
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);
    MessageHelper.writeString(dataOutput, dataDirName);
    MessageHelper.writePublishable(dataOutput, partitionFunction);
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
    this.dataDirName = MessageHelper.readString(dataInput);
    this.partitionFunction = (PartitionFunction)MessageHelper.readPublishable(dataInput);
  }
}
