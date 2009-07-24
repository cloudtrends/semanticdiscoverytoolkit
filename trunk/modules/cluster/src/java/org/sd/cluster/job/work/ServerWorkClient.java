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


import org.sd.cluster.config.ClusterException;
import org.sd.cluster.config.Config;
import org.sd.cluster.io.Response;
import org.sd.cluster.job.JobCommand;
import org.sd.cluster.job.JobCommandMessage;
import org.sd.cluster.job.JobUtil;
import org.sd.cluster.job.LocalJobId;
import org.sd.cluster.config.Console;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Work client that requests work from a work job with a work server.
 * <p>
 * @author Spence Koehler
 */
public class ServerWorkClient implements WorkClient {

  private static final String NO_SERVER_NAME = "NONE";


  private LocalJobId workServerId;  // the work server to contact
  private int numRetries;           // number of times to retry getting work
  private int timeoutMillis;        // timeout while trying to get work from server

  private WorkJob workJob;          // this client's work job.
  private String myJobId;           // my job id
  private String myNodeName;        // my node name
  private Console console;          // for sending/receiving job operate messages

  private JobCommandMessage workMessage;
  private JobCommunicator jobCommunicator;

  /**
   * Construct with the given properties.
   * <p>
   * <ul>
   * <li>workServerId -- local job id for work server that this job is a client of (form = <lid>.<serverName>-<serverJvmNum>)</li>
   * <li>numClientToServerRetries -- [optional, default=10] number of consecutive retries.</li>
   * <li>clientToServerTimeoutMillis -- [optional, default=5000] number of milliseconds to wait for response from server.</li>
   * </ul>
   */
  public ServerWorkClient(Properties properties) {
    this.workServerId = JobUtil.parseWorkServerId(properties.getProperty("workServerId"));

    if (this.workServerId != null) {
      this.numRetries = Integer.parseInt(properties.getProperty("numClientToServerRetries", "10"));
      this.timeoutMillis = Integer.parseInt(properties.getProperty("clientToServerTimeoutMillis", "60000"));

      System.out.println(new Date() + " : ServerWorkClient workServer=" + workServerId +
                         " numClientToServerRetries=" + numRetries +
                         " clientToServerTimeoutMillis=" + timeoutMillis);
    }
    else {
      throw new IllegalStateException(new Date() + " : ServerWorkClient has no WorkServer! " +
                                      "(need workServerId, [numClientToServerRetries], [clientToServerTimeoutMillis])");
    }
  }

  /**
   * Set the work job for the work client.
   * <p>
   * This is for the client to monitor the work job's status so that it
   * can stop serving up work when appropriate.
   */
  public void setWorkJob(WorkJob workJob) {
    this.workJob = workJob;
  }

  public String getDescription() {
    return "WorkClient-" + workJob.getJobId();
  }

  /**
   * Perform client initializations.
   */
  public boolean initialize() {

    final Config config = workJob.getConfig();
    
    this.myJobId = workJob.getJobId();
    this.myNodeName = config.getNodeName();
    this.console = workJob.getConsole();

    this.workMessage = new JobCommandMessage(JobCommand.OPERATE, workServerId,
                                             new WorkRequest(WorkRequest.RequestType.GET, myJobId, myNodeName));
    this.jobCommunicator = new JobCommunicator(console, getDescription(), myNodeName, workServerId,
                                               workJob.getGroupName(), numRetries, timeoutMillis);

    return true;
  }

  /**
   * Close this work client.
   */
  public void close() {
    // nothing to do.
  }

  /**
   * Get the next work response.
   * <p>
   * Note that this client ignores the queue and contacts the server.
   */
  public WorkResponse getWork(WorkRequest workRequest, WorkQueue queue, AtomicBoolean pause) {
    WorkResponse result = null;

    // monitor workJob's status, to determine whether to fetch work.
    if (workJob.isAcceptingWork()) {
      result = requestWorkFromServer(pause);
    }

    return result;
  }
//todo: when processing work, monitor the workJob's status.

  /**
   * Get the name of this client's server.
   */
  public String getServer() {
    String serverName = NO_SERVER_NAME;

    if (jobCommunicator == null || jobCommunicator.getCurrentServer() == null) {
      serverName = "NONE";
    }
    else {
      serverName = jobCommunicator.getCurrentServer() + "(node)";
    }

    return serverName;
  }

  private final WorkResponse requestWorkFromServer(AtomicBoolean pause) {
    return jobCommunicator.getResponse(workMessage, pause);
  }
}
