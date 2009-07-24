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

/**
 * Interface for serving work from a work job.
 * <p>
 * @author Spence Koehler
 */
public interface WorkServer extends WorkQueue {

  /**
   * Process the work request.
   *
   * @return a non-null work response.
   */
  public WorkResponse processRequest(WorkRequest workRequest);

  /**
   * Initialize this work server.
   */
  public boolean initialize(ClusterContext clusterContext, String jobIdString, String dataDirName);

  /**
   * Notify this instance not to (or to) expect (wait for) more work.
   */
  public void setNoMoreWork(boolean noMoreWork);

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
  public Long shouldKeepRunning(); 

  /**
   * Get the number of units sent by this instance.
   */
  public long getNumServedUnits();

  /**
   * Close this work server.
   */
  public void close();
}
