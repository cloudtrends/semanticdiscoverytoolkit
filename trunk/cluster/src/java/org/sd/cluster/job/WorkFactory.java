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


import java.io.IOException;
import java.util.List;

/**
 * Interface for iterating over units of work.
 * <p>
 * @author Spence Koehler
 */
public interface WorkFactory {

  /**
   * Add the work unit to the front of this factory's work.
   */
  public void addToFront(UnitOfWork workUnit);

  /**
   * Add all of the work units to the front of this factory's work.
   */
  public void addAllToFront(List<UnitOfWork> workUnits);

  /**
   * Add the work unit to the back of this factory's work.
   */
  public void addToBack(UnitOfWork workUnit);

  /**
   * Get the next unit of work in a thread-safe way.
   *
   * @return the next unit of work or null if there are no more.
   */
  public UnitOfWork getNext() throws IOException;

  /**
   * Release the unit of work in a thread-safe way.
   * <p>
   * Behavior is undefined if a unit of work is re-released.
   */
  public void release(UnitOfWork unitOfWork) throws IOException;

  /**
   * Explicitly close the resources used by the work factory.
   */
  public void close() throws IOException;

  /**
   * Report whether all units retrieved have been released or
   * accounted for.
   */
  public boolean isComplete();

  /**
   * Get an estimate for the remaining work to do.
   */
  public long getRemainingEstimate();
}
