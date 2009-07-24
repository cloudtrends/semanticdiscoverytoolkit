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
import java.util.LinkedList;

/**
 * A basic work factory that allows units to be added dynamically.
 * <p>
 * @author Spence Koehler
 */
public class BasicWorkFactory extends AbstractWorkFactory {
  
  private LinkedList<UnitOfWork> workUnits;
  private int numCheckedOut;

  public BasicWorkFactory() {
    this.workUnits = new LinkedList<UnitOfWork>();
    this.numCheckedOut = 0;
  }

  public void addWork(String string) {
    workUnits.add(new StringUnitOfWork(string));
  }

  public void addWork(UnitOfWork workUnit) {
    workUnits.add(workUnit);
  }

  /**
   * Get the next unit of work in a thread-safe way.
   *
   * @return the next unit of work or 'done' if there are no more.
   */
  protected UnitOfWork doGetNext() throws IOException {
    UnitOfWork result = null;

    if (workUnits.size() > 0) {
      result = workUnits.removeFirst();
      ++numCheckedOut;
    }
    else {
      result = DONE_UNIT_OF_WORK;
    }

    return result;
  }

  /**
   * Release the unit of work in a thread-safe way.
   * <p>
   * Behavior is undefined if a unit of work is re-released.
   */
  public void release(UnitOfWork unitOfWork) throws IOException {
    --numCheckedOut;
  }

  /**
   * Explicitly close the resources used by the work factory.
   */
  public void close() throws IOException {
    //nothing to do.
  }

  /**
   * Report whether all units retrieved have been released or
   * accounted for.
   */
  public boolean isComplete() {
    return workUnits.size() == 0 && numCheckedOut == 0 && super.isComplete();
  }

  /**
   * Get an estimate for the remaining work to do.
   */
  public long getRemainingEstimate() {
    return super.getRemainingEstimate() + workUnits.size() + numCheckedOut;
  }
}
