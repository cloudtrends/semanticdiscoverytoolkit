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

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A work server that serves only WAITING work.
 * <p>
 * This is typically used to allow a WorkClient with side-effects to run and
 * stay alive while attached to this work server.
 *
 * @author Spence Koehler
 */
public class DummyWorkServer extends AbstractWorkServer {
  
  private final AtomicInteger numAdds = new AtomicInteger(0);
  private final AtomicInteger numInserts = new AtomicInteger(0);
  private final AtomicInteger numGets = new AtomicInteger(0);
  private final AtomicInteger numPeeks = new AtomicInteger(0);
  private final AtomicInteger numFinds = new AtomicInteger(0);
  private final AtomicInteger numDeletes = new AtomicInteger(0);

  private KeyedWork response;

  /**
   * Properties-based constructor.
   * <p>
   * <ul>
   * <li>responseType -- (optional, default=WAITING) "WAITING" or "EMPTY" to send a WAITING or and empty response.</li>
   * </ul>
   */
  public DummyWorkServer(Properties properties) {
    super(properties);

    final String responseType = properties.getProperty("responseType", "WAITING").toUpperCase();
    this.response = "WAITING".equals(responseType) ? null : new KeyedWork();
  }

  /**
   * Initialize this work server.
   */
  protected boolean doInitialize(ClusterContext clusterContext, String jobIdString, String dataDirName) {
    //nothing to do.
    System.out.println(new Date() + ": NOTE: Initialized DUMMY workServer jobIdString=" + jobIdString + " dataDirName=" + dataDirName);
    return true;
  }

  /**
   * Add work to the end of this queue.
   */
  public void addWork(WorkRequest workRequest) {
    numAdds.incrementAndGet();
    System.out.println(new Date() + ": DummyWorkServer dropping added work '" + workRequest + "'!");
  }

  /**
   * Insert work to the front of this queue.
   */
  public void insertWork(WorkRequest workRequest) {
    numInserts.incrementAndGet();
    System.out.println(new Date() + ": DummyWorkServer dropping inserted work '" + workRequest + "'!");
  }

  /**
   * Get (pop) work from the beginning of this queue.
   *
   * @return the next work from this queue, or null if the queue is empty.
   */
  public KeyedWork getWork(WorkRequest workRequest) {
    numGets.incrementAndGet();

    // note that returning null without calling setNoMoreWork(true) causes
    // a WAITING response to be sent.
    return response;
  }

  /**
   * Get (peek) work from the beginning of this queue.
   *
   * @return the next work from this queue, or null if the queue is empty.
   */
  public KeyedWork peek(WorkRequest workRequest) {
    numPeeks.incrementAndGet();
    return null;
  }

  /**
   * Find work in this queue.
   *
   * @return the found work from this queue, or null if not found.
   */
  public KeyedWork findWork(WorkRequest workRequest) {
    numFinds.incrementAndGet();
    return null;
  }

  /**
   * Delete work from this queue.
   */
  public void deleteWork(WorkRequest workRequest) {
    numDeletes.incrementAndGet();
    System.out.println(new Date() + ": DummyWorkServer dropping delete work '" + workRequest + "'!");
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
    return (numAdds.get() + numInserts.get()) - (numGets.get() + numDeletes.get());
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append("numAdds=").append(numAdds.get()).
      append(",numInserts=").append(numInserts.get()).
      append(",numGets=").append(numGets.get()).
      append(",numPeeks=").append(numPeeks.get()).
      append(",numFinds=").append(numFinds.get()).
      append(",numDeletes=").append(numDeletes.get());

    return result.toString();
  }
}
