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
import org.sd.cluster.config.ClusterContext;
import org.sd.cluster.config.ClusterDefinition;
import org.sd.cluster.config.Config;
import org.sd.cluster.config.Console;
import org.sd.cluster.config.StringResponse;
import org.sd.cluster.io.Context;
import org.sd.cluster.io.Message;
import org.sd.io.Publishable;
import org.sd.util.PropertiesParser;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A job is a type of message handled by a cluster node.
 * <p>
 * When handled, a job registers with a manager to allow following progress.
 *
 * @author Spence Koehler
 */
public abstract class AbstractJob implements Job {
  
  protected static final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

  private int numThreads;
  private String jobId;
  private String groupName;  // identifies participating nodes through clusterDef; null == single node
  private String singleNodeId;
  private GlobalJobId globalJobId;
  private boolean beginImmediately;

  private transient AtomicReference<JobStatus> jobStatus = new AtomicReference<JobStatus>(null);
  private transient boolean error = false;
  private transient boolean responded = false;
  private transient ClusterContext context = null;

  protected AbstractJob() {
    this.numThreads = 1;
    this.jobId = "Unidentified";
    this.groupName = null;  // null => single node working on job
    this.singleNodeId = null;
    this.globalJobId = null;
    this.beginImmediately = false;

    setStatus(JobStatus.INITIALIZING);
  }

  protected AbstractJob(int numThreads, String jobId, String groupName, boolean beginImmediately) {
    this();
    this.numThreads = numThreads;
    this.singleNodeId = null;
    this.jobId = jobId;
    this.groupName = groupName;
    this.globalJobId = null;
    this.beginImmediately = beginImmediately;
  }

  protected AbstractJob(Properties properties) {
    this.numThreads = PropertiesParser.getInt(properties, "numThreads", "1");
    this.jobId = properties.getProperty("jobId");
    this.groupName = properties.getProperty("groupName");
    this.beginImmediately = PropertiesParser.getBoolean(properties, "beginImmediately", "true");
    this.singleNodeId = null;
    this.globalJobId = null;

    if (jobId == null) {
      throw new IllegalArgumentException("Missing required property 'jobId'!");
    }
    if (groupName == null) {
      throw new IllegalArgumentException("Missing required property 'groupName'!");
    }
  }

  public final JobStatus getStatus() {
    return jobStatus.get();
  }

  public final void setStatus(JobStatus jobStatus) {
		if (jobStatus == JobStatus.INTERRUPTED && this.jobStatus.get() != JobStatus.RUNNING) {
			// Interruptions only take hold if we're running.
			return;
		}

    this.jobStatus.set(jobStatus);

    if (jobStatus == JobStatus.FINISHED) {
      doneHandlingHook();
    }
  }

  /**
   * Hook called when we're done handling the job (i.e. when the job
   * status is set to FINISHED)
   */
  protected void doneHandlingHook() {
  }

  public int getNumThreads() {
    return numThreads;
  }

  public String getJobId() {
    return jobId;
  }

  public void setGlobalJobId(GlobalJobId globalJobId) {
    this.globalJobId = globalJobId;
  }

  public GlobalJobId getGlobalJobId() {
    return globalJobId;
  }

  public void setProperties(Map<String, String> properties) {
    properties.put("numThreads", Integer.toString(numThreads));
  }

  public String getGroupName() {
    return groupName;
  }

  public void setSingleNodeId(String nodeId) {
    this.singleNodeId = nodeId;
  }

  public String getSingleNodeId() {
    return singleNodeId;
  }

  /**
   * Get this message's response to be returned by the server to the client.
   * <p>
   * NOTE: this response is returned synchronously from a server to the client
   *       after receiving a message. The message as received on the server
   *       is handled in its own thread later.
   */
  public Message getResponse(Context context) {
    final ClusterContext clusterContext = (ClusterContext)context;
    final Config config = clusterContext.getConfig();
    final String nodeName = config.getNodeName();

    final LocalJobId localJobId = (globalJobId == null) ? null : globalJobId.getLocalJobId(nodeName);
    final Integer lid = (localJobId != null) ? localJobId.getId() : null;

    final String idstr = jobId + "(" + localJobId + ") - " + getDescription() +
      "' at " + new Date().toString() + ")";

    StringResponse result = null;

    if (localJobId == null) {
      this.error = true;
      final String message = "Can't start Job: '" + idstr +
        "'! (globalJobId=" + globalJobId +
        ", localJobId=" + localJobId + ")";
      result = new StringResponse(context, message);
      System.out.println(message + ". nodeName=" + nodeName + " port=" + config.getServerPort());
    }
    else {
      result = new StringResponse(context, "Started Job: '" + idstr);
    }

    responded = true;
    return result;
  }

  public final void initialize(boolean start) {
    if (startHandlingHook()) {
      setStatus(JobStatus.INITIALIZED);
      if (start) start();
    }
    else {
      setStatus(JobStatus.PAUSED);
    }
  }

  public boolean flush(Publishable payload) {
    // no-op until overridden in impls that should respond.
    return true;
  }

  /**
   * Returns true if this job has responded and did not have an error while responding.
   */
  protected final boolean canHandle() {
    return !error && responded;
  }

  protected final void setError(boolean error) {
    this.error = error;
  }

  /**
   * Called by the handle method only if 'canHandle' returns true before
   * registering this job with the job manager. If this hook returns false,
   * this job will NOT be registered with the job manager and the postHandleHook
   * will not be called.
   */
  protected boolean preHandleHook(Context context) {
    final ClusterContext clusterContext = (ClusterContext)context;
    setClusterContext(clusterContext);

    return true;
  }

  /**
   * Called by the handle method after registering with the job manager.
   */
  protected void postHandleHook(Context context) {
  }

  public final void handle(Context context) {
    if (canHandle()) {
      if (preHandleHook(context)) {
        final ClusterContext clusterContext = getClusterContext();
        final Config config = clusterContext.getConfig();

        clusterContext.getJobManager().registerJob(this, config.getNodeName(), beginImmediately);
        postHandleHook(context);
      }
    }
  }

  /**
   * Get the job manager.
   * <p>
   * NOTE: this is only available after the preHandleHook has been run!
   */
  public JobManager getJobManager() {
    return (context == null) ? null : context.getJobManager();
  }

  /**
   * Get a handle to the job manager's console.
   * <p>
   * NOTE: this is only available after the preHandleHook has been run!
   */
  public Console getConsole() {
    final JobManager jobManager = getJobManager();
    return (jobManager == null) ? null : jobManager.getConsole();
  }


  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    dataOutput.writeInt(numThreads);
    MessageHelper.writeString(dataOutput, jobId);
    MessageHelper.writeString(dataOutput, groupName);
    MessageHelper.writeString(dataOutput, singleNodeId);
    MessageHelper.writePublishable(dataOutput, globalJobId);
    dataOutput.writeBoolean(beginImmediately);
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
    this.numThreads = dataInput.readInt();
    this.jobId = MessageHelper.readString(dataInput);
    this.groupName = MessageHelper.readString(dataInput);
    this.singleNodeId = MessageHelper.readString(dataInput);
    this.globalJobId = (GlobalJobId)MessageHelper.readPublishable(dataInput);
    this.beginImmediately = dataInput.readBoolean();
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
    return true;
  }

  public void setClusterContext(ClusterContext context) {
    this.context = context;
  }

  public ClusterContext getClusterContext() {
    return context;
  }

  public String dumpDetails() {
    return new Date() + ": DETAILS : " + getJobId() + " -- " + getDescription() + " (" + getStatus() + ")\n";
  }

  /**
   * Get the local job id.
   * <p>
   * NOTE: this is only available after the preHandleHook has been run!
   */
  protected final LocalJobId getLocalJobId() {
    final Config config = getConfig();
    if (config == null) return null;
    final String nodeName = config.getNodeName();
    return (globalJobId == null) ? null : globalJobId.getLocalJobId(nodeName);
  }

  /**
   * Convenience method for accessing the current cluster definition.
   */
  protected final ClusterDefinition getClusterDefinition() {
    return context.getClusterDefinition();
  }

  /**
   * Convenience method for accessing the current cluster config.
   */
  public final Config getConfig() {
    return context == null ? null : context.getConfig();
  }

  /**
   * Get the node id (n of m) of the current jvm for this job.
   */
  protected final int myNodeId() {
    final Config config = getConfig();
    final String machineName = config.getMachineName().toLowerCase();
    final int jvmNum = config.getJvmNum();

    return getClusterDefinition().getGroupNodePosition(groupName, machineName, jvmNum);
  }

  /**
   * Get the total number of jvms for this job.
   */
  protected final int numNodes() {
    return getClusterDefinition().getNumGroupNodes(groupName);
  }

  /**
   * Get the nodes (machine-jvmNum) for this job.
   */
  protected final Collection<String> getNodeNames() {
    Collection<String> result = getClusterDefinition().getGroupNodeNames(groupName, true);

    if (result == null) {
      final Config config = getConfig();
      result = new ArrayList<String>();
      result.add(config.getMachineName().toLowerCase() + "-" + config.getJvmNum());
    }

    return result;
  }
}
