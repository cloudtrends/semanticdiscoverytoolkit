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


import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Interface for requesting work from a work job.
 * <p>
 * @author Spence Koehler
 */
public interface WorkClient {

  /**
   * Get the next work response.
   */
  public WorkResponse getWork(WorkRequest workRequest, WorkQueue queue, AtomicBoolean pause);

  /**
   * Get the name of this client's server.
   */
  public String getServer();

  /**
   * Set the work job for the work client.
   * <p>
   * This is for the client to monitor the work job's status so that it
   * can stop serving up work when appropriate.
   */
  public void setWorkJob(WorkJob workJob);

  /**
   * Perform client initializations.
   */
  public boolean initialize();

  /**
   * Close this work client.
   */
  public void close();
}
