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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract implementation of the work factory interface.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractWorkFactory implements WorkFactory {
  
  protected static final StringUnitOfWork DONE_UNIT_OF_WORK = new StringUnitOfWork(WorkServer.WORK_IS_DONE_RESPONSE);
  static {
    DONE_UNIT_OF_WORK.setWorkStatus(WorkStatus.ALL_DONE);
  }

  protected static final StringUnitOfWork WAITING_UNIT_OF_WORK = new StringUnitOfWork(WorkServer.WORK_IS_WAITING_RESPONSE);
  static {
    WAITING_UNIT_OF_WORK.setWorkStatus(WorkStatus.WAITING);
  }

  /**
   * Get the next unit of work.
   */
  protected abstract UnitOfWork doGetNext() throws IOException;


  private LinkedList<UnitOfWork> addedFrontWork;
  private LinkedList<UnitOfWork> addedBackWork;
  private final AtomicBoolean sawAllDone;

  protected AbstractWorkFactory() {
    this.addedFrontWork = new LinkedList<UnitOfWork>();
    this.addedBackWork = new LinkedList<UnitOfWork>();
    this.sawAllDone = new AtomicBoolean(false);
  }

  /**
   * Add the work unit to the front of this factory's work.
   */
  public void addToFront(UnitOfWork workUnit) {
    synchronized (addedFrontWork) {
      addedFrontWork.addFirst(workUnit);
    }
  }

  /**
   * Add all of the work units to the front of this factory's work.
   */
  public void addAllToFront(List<UnitOfWork> workUnits) {
    synchronized (addedFrontWork) {
      addedFrontWork.addAll(0, workUnits);
    }
  }

  /**
   * Add the work unit to the back of this factory's work.
   */
  public void addToBack(UnitOfWork workUnit) {
    synchronized (addedBackWork) {
      addedBackWork.addLast(workUnit);
    }
  }

  /**
   * Get the next unit of work in a thread-safe way.
   *
   * @return the next unit of work or null if there are no more.
   */
  public final UnitOfWork getNext() throws IOException {
    UnitOfWork result = null;

    synchronized (addedFrontWork) {
      if (addedFrontWork.size() > 0) {
        result = addedFrontWork.removeFirst();
      }
    }

    if (result == null) {
      result = doGetNext();

      if (result == null) {
        synchronized (addedBackWork) {
          if (addedBackWork.size() > 0) {
            result = addedBackWork.removeFirst();
          }
        }
      }
    }

    if (result != null && result.getWorkStatus() == WorkStatus.ALL_DONE) {
      sawAllDone.set(true);
    }

    return result;
  }

  /**
   * Report whether all units retrieved have been released or
   * accounted for.
   */
  public boolean isComplete() {
    return addedFrontWork.size() == 0 && addedBackWork.size() == 0 && sawAllDone.get();
  }

  /**
   * Get an estimate for the remaining work to do.
   */
  public long getRemainingEstimate() {
    return addedFrontWork.size() + addedBackWork.size();
  }
}
