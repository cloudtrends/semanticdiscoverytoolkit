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


import org.sd.cio.MessageHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract unit of work implementation to keep track of counts.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractUnitOfWork implements UnitOfWork {

  private AtomicReference<WorkStatus> workStatus;
  private String reason;

  /**
   * Get this unit of work's main contents.
   * <p>
   * The main contents distinguish this unit of work from another or match
   * it to another. The main contents should not rely on status or reason.
   */
  protected abstract Object getMainContents();


  protected AbstractUnitOfWork() {
    this.workStatus = new AtomicReference<WorkStatus>(WorkStatus.INITIALIZED);
    this.reason = null;
  }

  /**
   * Get this unit's work status.
   */
  public WorkStatus getWorkStatus() {
    return workStatus.get();
  }

  /**
   * Set this unit's work status.
   */
  public void setWorkStatus(WorkStatus workStatus) {
    this.workStatus.set(workStatus);
  }

  /**
   * Set this unit's work status to the update value iff is current
   * status is as expected.
   *
   * @return true if the status was as expected and is now updated.
   */
  public boolean compareAndSetWorkStatus(WorkStatus expect, WorkStatus update) {
    return workStatus.compareAndSet(expect, update);
  }

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
  public void recordFailure(String reason) {
    if (reason == null) {
      setWorkStatus(WorkStatus.FAILED);
    }
    else {
      setWorkStatus(WorkStatus.ERROR);
      this.reason = reason;
    }
  }

  /**
   * Get the reason for this unit of work's error or null if there has been
   * no error.
   */
  public String getReason() {
    return reason;
  }

  /**
   * Get the number of bytes required to serialize this unit of work.
   */
  public int getSerializedSize() {
    return
      MessageHelper.numOverheadBytes(workStatus.get().name()) +
      MessageHelper.numOverheadBytes(reason);
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    MessageHelper.writeString(dataOutput, workStatus.get().name());
    MessageHelper.writeString(dataOutput, reason);
  }

  /**
   * Read this message's contents from the dataInput stream that was written by
   * this.write(dataOutput).
   * <p>
   * NOTE: this requires all implementing classes to have a default constructor
   *       with no args.
   *
   * @param dataInput  the data output to write to.
   */
  public void read(DataInput dataInput) throws IOException {
    final String workStatusName = MessageHelper.readString(dataInput);
    this.workStatus = new AtomicReference<WorkStatus>(Enum.valueOf(WorkStatus.class, workStatusName));
    this.reason = MessageHelper.readString(dataInput);
  }

  public String toString() {
    final StringBuilder result =  new StringBuilder();

    result.append(workStatus);
    if (reason != null) result.append('\n').append(reason);

    return result.toString();
  }

  public boolean equals(Object o) {
    // NOTE: this cannot depend on status.
    boolean result = (this == o);

    if (!result) {
      final AbstractUnitOfWork other = (AbstractUnitOfWork)o;
      final Object myMainContents = this.getMainContents();
      final Object otherMainContents = other.getMainContents();

      result = myMainContents == otherMainContents;
      if (!result && myMainContents != null) {
        result = myMainContents.equals(otherMainContents);
      }
    }

    return result;
  }

  public int hashCode() {
    // NOTE: this cannot depend on status.
    final Object myMainContents = getMainContents();
    return myMainContents == null ? 0 : myMainContents.hashCode();
  }
}
