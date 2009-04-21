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


import org.sd.io.Publishable;

/**
 * Interface for collecting and distributing work.
 * <p>
 * @author Spence Koehler
 */
public interface WorkQueue {

  /**
   * Add work to the end of this queue.
   */
  public void addWork(WorkRequest workRequest);

  /**
   * Insert work to the front of this queue.
   */
  public void insertWork(WorkRequest workRequest);

  /**
   * Get (pop) work from the beginning of this queue.
   *
   * @return the next work from this queue, or null if the queue is empty.
   */
  public KeyedWork getWork(WorkRequest workRequest);

  /**
   * Get (peek) work from the beginning of this queue.
   *
   * @return the next work from this queue, or null if the queue is empty.
   */
  public KeyedWork peek(WorkRequest workRequest);

  /**
   * Find and return (leaving in the queue) the designated work.
   *
   * @return the found work or null if it doesn't exist in the queue.
   */
  public KeyedWork findWork(WorkRequest workRequest);

  /**
   * Delete the designated work from the queue.
   */
  public void deleteWork(WorkRequest workRequest);

  /**
   * Get the number of elements currently contained in this queue.
   */
  public long size();

  /**
   * Get the number of elements currently contained in the queue identified
   * by the WorkRequest.
   */
  public long size(WorkRequest workRequest);
}
