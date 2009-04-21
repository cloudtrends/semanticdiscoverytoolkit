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
import org.sd.bdb.DbValue;
import org.sd.bdb.LongKeyValuePair;
import org.sd.cluster.config.ClusterContext;
import org.sd.io.Publishable;
import org.sd.util.ReflectUtil;

import com.sleepycat.je.DatabaseException;

import java.io.File;
import java.util.Date;
import java.util.Properties;

/**
 * Queue work server implementation (based on BerkeleyDb).
 * <p>
 * @author Spence Koehler
 */
public class QueueWorkServer extends AbstractWorkServer {

  private String queueDir;
  private String queueName;
  private QueueChooser queueChooser;

  private transient BerkeleyDb bdb;
  private transient DbHandle _dbHandle;

  /**
   * Properties-based constructor.
   * <p>
   * <ul>
   * <li>serverQueueDir -- absolute or relative path to queue directory</li>
   * <li>serverQueueName -- [optional, default=basename(queueDir)] name of queue.</li>
   * <li>serverQueueChooser -- [optional, default=null] queue chooser.</li>
   * </ul>
   */
  public QueueWorkServer(Properties properties) {
    super(properties);

    this.queueDir = properties.getProperty("serverQueueDir");
    this.queueName = properties.getProperty("serverQueueName");
    final String queueChooserClass = properties.getProperty("serverQueueChooser");

    if (queueDir == null) {
      throw new IllegalStateException("Must define 'serverQueueDir'!");
    }

    if (queueChooserClass != null) {
      this.queueChooser = (QueueChooser)ReflectUtil.buildInstance(queueChooserClass, properties);
    }
    else if (queueName == null) {  // default queueName == queue directory name
      queueName = new File(queueDir).getName();
    }
  }

  /**
   * Initialize this work server.
   */
  protected boolean doInitialize(ClusterContext clusterContext, String jobIdString, String dataDirName) {
    String queuePath = null;

    if (queueDir.charAt(0) == '/') {
      // given absolute path to the queue.
      queuePath = queueDir;
    }
    else if (clusterContext != null) {
      // build path relative to cluster output directory.
      queuePath = clusterContext.getConfig().getOutputDataPath(jobIdString, dataDirName) + "/" + queueDir;
    }

    if (queuePath != null) {
      this.bdb = BerkeleyDb.getInstance(new File(queuePath), false);
      this._dbHandle = null;
    }

    return true;
  }

  /**
   * Close this work server.
   */
  public void close() {
    super.close();

    if (bdb != null) {
      bdb.close();
    }
  }

  /**
   * Add work to the end of this queue.
   */
  public void addWork(WorkRequest workRequest) {
    final DbHandle dbHandle = getDbHandle(workRequest);

    long key = workRequest.getKey();
    final Publishable work = workRequest.getWork();
    if (key == -1L) {  // if key is undefined, create a definition.
      final LongKeyValuePair lastEntry = dbHandle.peekLastLong();
      key = lastEntry == null ? 1L : lastEntry.getKey() + 1;
    }

    if (workRequest.getTimestamp() > 0) {
      dbHandle.update(key, new DbValue(work), workRequest.getTimestamp());
    }
    else {
      dbHandle.put(key, new DbValue(work));
    }
  }

  /**
   * Insert work to the front of this queue.
   */
  public void insertWork(WorkRequest workRequest) {
    final DbHandle dbHandle = getDbHandle(workRequest);

    long key = workRequest.getKey();
    final Publishable work = workRequest.getWork();
    if (key == -1L) {  // if key is undefined, create a definition.
      final LongKeyValuePair lastEntry = dbHandle.peekLastLong();
      key = lastEntry == null ? 1L : lastEntry.getKey() + 1;
    }
    dbHandle.push(key, new DbValue(work));
  }

  /**
   * Get (pop) work from the beginning of this queue.
   *
   * @return the next work from this queue, or null if the queue is empty.
   */
  public KeyedWork getWork(WorkRequest workRequest) {
    KeyedWork result = null;

    final DbHandle dbHandle = getDbHandle(workRequest);

    if (dbHandle != null) {
      final LongKeyValuePair earliest = dbHandle.popEarliestLong();
      if (earliest != null) {
        result = new KeyedWork(earliest.getKey(), earliest.getPublishable());
      }
    }

    return result;
  }

  /**
   * Get (peek) work from the beginning of this queue.
   *
   * @return the next work from this queue, or null if the queue is empty.
   */
  public KeyedWork peek(WorkRequest workRequest) {
    KeyedWork result = null;

    final DbHandle dbHandle = getDbHandle(workRequest);
    if (dbHandle.isClosed()) return null;

    final LongKeyValuePair earliest = dbHandle.peekEarliestLong();
    if (earliest != null) {
      result = new KeyedWork(earliest.getKey(), earliest.getPublishable());
    }

    return result;
  }

  /**
   * Find work in this queue.
   *
   * @return the found work from this queue, or null if not found.
   */
  public KeyedWork findWork(WorkRequest workRequest) {
    KeyedWork result = null;

    final DbHandle dbHandle = getDbHandle(workRequest);
    if (dbHandle.isClosed()) return null;

    final DbValue dbValue = dbHandle.get(workRequest.getKey());
    if (dbValue != null) {
      result = new KeyedWork(workRequest.getKey(), dbValue.getPublishable());
    }

    return result;
  }

  /**
   * Delete work from this queue.
   */
  public void deleteWork(WorkRequest workRequest) {
    final DbHandle dbHandle = getDbHandle(workRequest);

    if (!dbHandle.isClosed()) {
      dbHandle.delete(workRequest.getKey());
    }
  }

  /**
   * Get the number of elements currently contained in this queue.
   */
  public long size() {
    return size(null);
  }

  /**
   * Get the number of elements currently contained in the queue identified
   * by the WorkRequest.
   */
  public long size(WorkRequest workRequest) {
    long result = -1;

    final DbHandle dbHandle = getDbHandle(workRequest);

    if (!dbHandle.isClosed()) {
      result = dbHandle.getNumRecords();
    }

    return result;
  }

  protected final boolean waitForDataInQueue(WorkRequest workRequest, long millisToWait) {
    final DbHandle dbHandle = getDbHandle(workRequest);
    if (!dbHandle.isClosed()) {
/*
      final long startTime = System.currentTimeMillis();
      final long endTime = startTime + millisToWait;
      while (dbHandle.getNumRecords() == 0 && System.currentTimeMillis() < endTime) {
        Thread.yield();
      }
*/
      try {
        Thread.sleep(millisToWait);
      }
      catch (InterruptedException ignore){}
    }

    return (!dbHandle.isClosed() && dbHandle.getNumRecords() > 0);
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    final DbHandle dbHandle = getDbHandle(null);
    if (dbHandle != null) {
      result.append("queueSize=").append(dbHandle.getNumRecords()).append("\n   ");
    }

    result.append(super.toString());

    return result.toString();
  }

  protected final DbHandle getDbHandle(WorkRequest workRequest) {
    if (bdb == null) return null;
    if (queueChooser == null && _dbHandle != null) return _dbHandle;  // return the cached default

    String queueName = null;

    if (queueChooser != null) {
      queueName = queueChooser.getQueueName(workRequest);
    }
    if (queueName == null) queueName = this.queueName;

    final DbHandle result = bdb.get(queueName, true);

    if (queueChooser == null) _dbHandle = result;  // cache the default

    return result;
  }
}
