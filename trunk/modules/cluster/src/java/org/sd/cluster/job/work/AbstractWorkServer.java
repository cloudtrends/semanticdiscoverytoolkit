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
import org.sd.io.Publishable;
import org.sd.util.LineBuilder;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract work server implementation.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractWorkServer implements WorkServer {
  
  /**
   * Initialize this work server.
   */
  protected abstract boolean doInitialize(ClusterContext clusterContext, String jobIdString, String dataDirName);


  private final AtomicLong numServedUnits = new AtomicLong(0L);
  private LogWrapper outputLog;
  private String jobId;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicBoolean noMoreWork = new AtomicBoolean(false);
  private long keepRunningSleepTime;
  private final AtomicBoolean shouldKeepRunning = new AtomicBoolean(false);

  private transient ClusterContext _clusterContext;

  protected AbstractWorkServer(Properties properties) {
    this.jobId = properties.getProperty("jobId", "*unknown*");
    this.keepRunningSleepTime = Long.parseLong(properties.getProperty("keepRunningSleepTime", "1000"));
  }

  public String getDescription() {
    return "WorkServer-" + jobId;
  }

  /**
   * Notify this instance not to expect (wait for) more work.
   */
  public void setNoMoreWork(boolean noMoreWork) {
    this.noMoreWork.set(noMoreWork);
  }

  /**
   * Process the work request.
   *
   * @return a non-null work response.
   */
  public final WorkResponse processRequest(WorkRequest workRequest) {
    WorkResponse result = doProcessRequest(workRequest);

    if (result != null && result.getStatus() == WorkResponseStatus.WORK) {
      numServedUnits.incrementAndGet();

      // log it.
      final LineBuilder line = new LineBuilder();
      final Date curDate = new Date();
      line.append(workRequest.getRequestType().toString());

      addLogInfo(line, result);

      line.
        append(curDate.getTime()).
        append(workRequest.getRequestingNodeId()).
        append(curDate.toString());

      // log it
      outputLog.writeLine(line.toString(), false, true);
    }

    return result;
  }

  /**
   * Helper to log information about a workResponse.
   * <p>
   * The default behavior is to write the "toString" form of the Publishable
   * returned in the KeyedWork instance from getWork or the work status name
   * if the response is not of type WorkResponseStatus.WORK.
   * <p>
   * Extenders should override this for other behavior.
   */
  protected void addLogInfo(LineBuilder line, WorkResponse workResponse) {
    if (workResponse.getStatus() == WorkResponseStatus.WORK) {
      final Publishable work = workResponse.getKeyedWork().getWork();
      if (work != null) {
        line.append(work.toString());
      }
    }
    else {
      line.append(workResponse.getStatus().name());
    }
  }

  /**
   * Initialize this work server.
   */
  public final boolean initialize(ClusterContext clusterContext, String jobIdString, String dataDirName) {

    this._clusterContext = clusterContext;
    numServedUnits.set(0L);

    // open the output log so doInitialize can use its last line if it exists.
    this.outputLog = new LogWrapper(clusterContext, jobIdString, dataDirName, "server.log", true);

    // execute extender's initializations that may rely on the existence of the
    // output log for backup/restart logic.
    boolean result = doInitialize(clusterContext, jobIdString, dataDirName);

//todo: in doInitialize, AbstractBatchWorkServer.loadPathBatch would be called, which does 'restart' logic. Do that here? -vs- re-implement AbstractBatchWorkServer?

    return result;
  }

  /**
   * Close this work server.
   */
  public void close() {
    if (closed.compareAndSet(false, true) && outputLog != null) {
      outputLog.close();
    }
    shouldKeepRunning.set(false);
  }

  protected ClusterContext getClusterContext() {
    return _clusterContext;
  }

  /**
   * Process the work request.
   *
   * @return a non-null work response.
   */
  protected WorkResponse doProcessRequest(WorkRequest workRequest) {
    WorkResponse result = null;

    switch (workRequest.getRequestType()) {
      case ADD_FIRST :
        result = doAddFirst(workRequest);
        break;

      case ADD_LAST :
        result = doAddLast(workRequest);
        break;

      case GET :
        result = doGet(workRequest);
        break;

      case PEEK :
        result = doPeek(workRequest);
        break;

      case FIND :
        result = doFind(workRequest);
        break;

      case DELETE :
        deleteWork(workRequest);  // leave result null, returning a WAITING response.
        break;

      case OTHER :
        result = doOtherRequest(workRequest);
        break;
    }

    if (result == null) {
      if (closed.get()) {
        result = WorkResponse.getDoneResponse(_clusterContext);
      }
      else {
        result = WorkResponse.getWaitingResponse(_clusterContext);
      }
    }

    return result;
  }

  /**
   * Process ADD_LAST request type.
   */
  protected WorkResponse doAddLast(WorkRequest workRequest) {
    addWork(workRequest);
    return WorkResponse.getOkResponse(_clusterContext);
  }

  /**
   * Process ADD_FIRST request type.
   */
  protected WorkResponse doAddFirst(WorkRequest workRequest) {
    insertWork(workRequest);
    return WorkResponse.getOkResponse(_clusterContext);
  }

  /**
   * Process GET request type.
   */
  protected WorkResponse doGet(WorkRequest workRequest) {
    WorkResponse result = null;

    final KeyedWork keyedWork = getWork(workRequest);

    if (keyedWork != null) {
      result = WorkResponse.getInstance(_clusterContext, keyedWork);
    }
    else if (noMoreWork.get()) {
      result = WorkResponse.getDoneResponse(_clusterContext);
      close();
    }

    return result;
  }

  /**
   * Process PEEK request type.
   */
  protected WorkResponse doPeek(WorkRequest workRequest) {
    WorkResponse result = null;

    final KeyedWork keyedWork = peek(workRequest);

    if (keyedWork != null) {
      result = WorkResponse.getInstance(_clusterContext, keyedWork);
    }

    return result;
  }

  /**
   * Process FIND request type.
   */
  protected WorkResponse doFind(WorkRequest workRequest) {
    WorkResponse result = null;

    final KeyedWork keyedWork = findWork(workRequest);

    if (keyedWork != null) {
      result = WorkResponse.getInstance(_clusterContext, keyedWork);
    }
    else if (noMoreWork.get()) {
      result = WorkResponse.getDoneResponse(_clusterContext);
      close();
    }

    return result;
  }

  /**
   * Process OTHER request type.
   */
  protected WorkResponse doOtherRequest(WorkRequest workRequest) {
    return null;
  }

  /**
   * Get the output log.
   */
  protected final LogWrapper getOutputLog() {
    return outputLog;
  }

  /**
   * Roll the output log.
   */
  protected final void rollOutputLog() {
    if (outputLog != null) outputLog.roll();
  }

  /**
   * If there was already a log file when initialized, get the last line of
   * that file that exists before processing this go'round.
   */
  public String[] getLastOutputLogLines() {
    return outputLog == null ? null : outputLog.getLastLogLines();
  }

  /**
   * Report to the WorkJob whether it should keep running for this workServer.
   * <p>
   * Note: this is called by the work job in its running loop after the work
   *       pool fails to add work from the client. If there is data expected
   *       to pass through the client at a later time and/or work from the worker
   *       still expected to be completed and served through this workServer,
   *       then a non-null result should be returned.
   *
   * @return null if the WorkJob doesn't need to keep running; otherwise, return
   *         the number of milliseconds for the WorkJob to sleep before continuing
   *         it's running loop.
   */
  public Long shouldKeepRunning() {
    Long result = null;

    if (!noMoreWork.get() && shouldKeepRunning.get()) {
      result = keepRunningSleepTime;
    }

    return result;
  }

  protected void setShouldKeepRunning(boolean shouldKeepRunning) {
    this.shouldKeepRunning.set(shouldKeepRunning);
  }

  /**
   * Get the number of units sent by this instance.
   */
  public long getNumServedUnits() {
    return numServedUnits.get();
  }

  public String toString() {
    String result = null;

    if (outputLog != null) {
      result = outputLog.getRatesString();
    }
    else {
      result = "";
    }

    return result;
  }
}
