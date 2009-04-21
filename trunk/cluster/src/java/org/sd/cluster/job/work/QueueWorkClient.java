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


import org.sd.bdb.BerkeleyDb;
import org.sd.bdb.DbHandle;
import org.sd.bdb.LongKeyValuePair;
import org.sd.util.ReflectUtil;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Work client that requests work from a local queue.
 * <p>
 * @author Spence Koehler
 */
public class QueueWorkClient implements WorkClient {
  
  private String queueDir;
  private String queueName;
  private QueueChooser queueChooser;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  private transient BerkeleyDb bdb;
  private transient WorkJob workJob;
  private transient String serverName;

  /**
   * Construct with the given properties.
   * <p>
   * <ul>
   * <li>clientQueueDir -- absolute or relative path to queue directory</li>
   * <li>clientQueueName -- [optional, default=basename(queueDir)] name of queue.</li>
   * </ul>
   */
  public QueueWorkClient(Properties properties) {
    this.queueDir = properties.getProperty("clientQueueDir");
    this.queueName = properties.getProperty("clientQueueName");
    final String queueChooserClass = properties.getProperty("clientQueueChooser");

    if (queueDir == null) {
      throw new IllegalStateException("Must define 'clientQueueDir'!");
    }

    if (queueChooserClass != null) {
      this.queueChooser = (QueueChooser)ReflectUtil.buildInstance(queueChooserClass, properties);
    }
    else if (queueName == null) {  // default queueName == queue directory name
      queueName = new File(queueDir).getName();
    }

    workJob = null;
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

  /**
   * Perform client initializations.
   */
  public boolean initialize() {
    String queuePath = null;

    if (queueDir.charAt(0) == '/') {
      // given absolute path to the queue.
      queuePath = queueDir;
    }
    else {
      // build path relative to cluster output directory.
//todo: correlate this to QueueWorkServer.doInitialize so both use the same code!
      queuePath = workJob.getClusterContext().getConfig().getOutputDataPath(workJob.getName(), workJob.getJobId()) + "/" + queueDir;
    }

    this.bdb = BerkeleyDb.getInstance(new File(queuePath), false);

    return true;
  }

  /**
   * Close this work client.
   */
  public void close() {
    if (!closed.getAndSet(true)) {
      if (bdb != null) {
        bdb.close();
      }
    }
  }

  /**
   * Get the next work response.
   */
  public WorkResponse getWork(WorkRequest workRequest, WorkQueue queue, AtomicBoolean pause) {
    WorkResponse result = null;

    final DbHandle dbHandle = getDbHandle(workRequest);
    if (dbHandle == null) return result;

    final LongKeyValuePair earliest = dbHandle.popEarliestLong();
    if (earliest != null) {
      result = WorkResponse.getInstance(workJob.getClusterContext(), new KeyedWork(earliest.getKey(), earliest.getPublishable()));
    }

    if (result == null) {
      if (closed.get()) {
        result = WorkResponse.getDoneResponse(workJob.getClusterContext());
      }
      else {
        result = WorkResponse.getWaitingResponse(workJob.getClusterContext());
      }
    }

    return result;
  }

  /**
   * Get the name of this client's server.
   */
  public String getServer() {
    if (serverName == null) {
      serverName = queueName + "(queue)";
    }
    return serverName;
  }

  private final DbHandle getDbHandle(WorkRequest workRequest) {
    String queueName = null;

    if (queueChooser != null) {
      queueName = queueChooser.getQueueName(workRequest);
    }
    if (queueName == null) queueName = this.queueName;

    final DbHandle result = bdb.get(queueName, true);

    return result;
  }
}
