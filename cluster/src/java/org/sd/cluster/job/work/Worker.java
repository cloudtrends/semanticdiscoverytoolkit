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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Interface for performing work for a job.
 * <p>
 * NOTE: Implementations must be thread-safe for workers that will be run
 *       by WorkJob's with more than one thread!
 *
 * @author Spence Koehler
 */
public interface Worker {

  /**
   * Initialize this worker.
   */
  public boolean initialize(ClusterContext clusterContext, String jobIdString, String dataDirName);

  /**
   * Perform work for a job on the workUnit.
   * <p>
   * If destination is non-null and this worker generates more work, then add
   * the generated work to the destination queue.
   *
   * @return true if work was successfully completed; otherwise, false.
   */
  public boolean performWork(KeyedWork keyedWork, AtomicBoolean die, AtomicBoolean pause, QueueDesignator queueDesignator, WorkQueue destination);

  /**
   * Flush this worker.
   */
  public boolean flush(Publishable payload);

  /**
   * Close this worker.
   */
  public void close();

  /**
   * Get a status string or null.
   */
  public String getStatusString();

}
