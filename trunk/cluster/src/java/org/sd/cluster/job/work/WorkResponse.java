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


import org.sd.cio.MessageHelper;
import org.sd.cluster.config.SignedResponse;
import org.sd.cluster.io.Context;
import org.sd.io.Publishable;
import org.sd.io.PublishableException;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Properties;

/**
 * A response from a work server to a request for work.
 * <p>
 * @author Spence Koehler
 */
public class WorkResponse extends SignedResponse {

  private static final Object DONE_RESPONSE_MUTEX = new Object();
  private static WorkResponse doneWorkResponse = null;
  private static final Object WAITING_RESPONSE_MUTEX = new Object();
  private static WorkResponse waitingWorkResponse = null;
  private static final Object DOWN_RESPONSE_MUTEX = new Object();
  private static WorkResponse downWorkResponse = null;
  private static final Object OK_RESPONSE_MUTEX = new Object();
  private static WorkResponse okWorkResponse = null;
  private static final Object ERROR_RESPONSE_MUTEX = new Object();
  private static WorkResponse errorWorkResponse = null;

  /**
   * Get the done response.
   */
  public static final WorkResponse getDoneResponse(Context context) {
    synchronized (DONE_RESPONSE_MUTEX) {
      if (doneWorkResponse == null) {
        doneWorkResponse = new WorkResponse(context, WorkResponseStatus.DONE, null);
      }
    }
    return doneWorkResponse;
  }

  /**
   * Get the waiting response.
   */
  public static final WorkResponse getWaitingResponse(Context context) {
    synchronized (WAITING_RESPONSE_MUTEX) {
      if (waitingWorkResponse == null) {
        waitingWorkResponse = new WorkResponse(context, WorkResponseStatus.WAITING, null);
      }
    }
    return waitingWorkResponse;
  }

  /**
   * Get the waiting response.
   */
  public static final WorkResponse getWaitingResponse(Context context, int retries, long sleepTime) {
    synchronized (WAITING_RESPONSE_MUTEX) {
      if (waitingWorkResponse == null) {
        waitingWorkResponse = new WorkResponse(context, WorkResponseStatus.WAITING, new WaitingKeyedWork(retries, sleepTime));
      }
    }
    return waitingWorkResponse;
  }

  /**
   * Get the waiting response.
   */
  public static final WorkResponse getDownResponse(Context context) {
    synchronized (DOWN_RESPONSE_MUTEX) {
      if (downWorkResponse == null) {
        downWorkResponse = new WorkResponse(context, WorkResponseStatus.DOWN, null);
      }
    }
    return downWorkResponse;
  }

  /**
   * Get the ok response.
   */
  public static final WorkResponse getOkResponse(Context context) {
    synchronized (OK_RESPONSE_MUTEX) {
      if (okWorkResponse == null) {
        okWorkResponse = new WorkResponse(context, WorkResponseStatus.OK, null);
      }
    }
    return okWorkResponse;
  }

  /**
   * Get the error response.
   */
  public static final WorkResponse getErrorResponse(Context context, Exception e, String message) {
    synchronized (ERROR_RESPONSE_MUTEX) {
      if (errorWorkResponse == null) {
        errorWorkResponse = new WorkResponse(context, WorkResponseStatus.ERROR, new KeyedWork(-1L, new PublishableException(e, message)));
      }
    }
    return errorWorkResponse;
  }

  /**
   * Get a work response.
   */
  public static final WorkResponse getInstance(Context context, Publishable work) {
    return new WorkResponse(context, WorkResponseStatus.WORK, new KeyedWork(-1L, work));
  }

  /**
   * Get a work response.
   */
  public static final WorkResponse getInstance(Context context, KeyedWork keyedWork) {
    return new WorkResponse(context, WorkResponseStatus.WORK, keyedWork);
  }


  private WorkResponseStatus status;
  private KeyedWork keyedWork;

  /**
   * Default publishable constructor for reconstruction.
   */
  public WorkResponse() {
    super();
  }

  private WorkResponse(Context context, WorkResponseStatus status, KeyedWork keyedWork) {
    super(context);
    this.status = status;
    this.keyedWork = keyedWork;
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);

    MessageHelper.writeString(dataOutput, status.name());
    MessageHelper.writePublishable(dataOutput, keyedWork);
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
    super.read(dataInput);

    this.status = Enum.valueOf(WorkResponseStatus.class, MessageHelper.readString(dataInput));
    this.keyedWork = (KeyedWork)MessageHelper.readPublishable(dataInput);
  }

  /**
   * Get this instance's status.
   */
  public final WorkResponseStatus getStatus() {
    return status;
  }

  /**
   * Get this instance's work.
   */
  public final KeyedWork getKeyedWork() {
    return keyedWork;
  }
}
