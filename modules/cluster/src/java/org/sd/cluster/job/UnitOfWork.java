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


import org.sd.io.Publishable;

/**
 * Marker interface to encapsulate a unit of work to perform.
 * <p>
 * @author Spence Koehler
 */
public interface UnitOfWork extends Publishable {
  
  /**
   * Get this unit's work status.
   */
  public WorkStatus getWorkStatus();

  /**
   * Set this unit's work status.
   */
  public void setWorkStatus(WorkStatus workStatus);

  /**
   * Set this unit's work status to the update value iff is current
   * status is as expected.
   *
   * @return true if the status was as expected and is now updated.
   */
  public boolean compareAndSetWorkStatus(WorkStatus expect, WorkStatus update);

  /**
   * Record the failure of this unit of work.
   * <p>
   * If the reason is null, assume that the unit of work failed because
   * of a "false" return value while being processed in a job and set the
   * work status to FAILED; otherwise, set the work status to ERROR and
   * record the non-null reason.
   *
   * @param reason  either null for 'normal' failure or a stack trace
   *                signalling error;
   */
  public void recordFailure(String reason);

  /**
   * Get the reason for this unit of work's error or null if there has been
   * no error.
   */
  public String getReason();

  /**
   * Get the number of bytes required to serialize this unit of work.
   */
  public int getSerializedSize();

//todo: need descriptive toString to describe current unit of work.
//todo: need to encapsulate or have access to partitioning of output generated through this unit of work?

}
