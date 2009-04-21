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
import org.sd.io.Publishable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A request package as payload to a job command message to a work server.
 * <p>
 * @author Spence Koehler
 */
public class WorkRequest implements Publishable {
  
  public static enum RequestType {ADD_FIRST, ADD_LAST, GET, PEEK, FIND, DELETE, OTHER};


  private RequestType requestType;
  private String requestingJobId;
  private String requestingNodeId;
  private long key;  // -1L means unset, which is typical. intended to be used to identify specific work.
  private String otherRequest;
  private Publishable work;  // non-null if work is sent with the request. (i.e. for adding)
  private long timestamp;

  /**
   * Default constructor for reconstruction.
   */
  public WorkRequest() {
    this(null, null, null);
  }

  public WorkRequest(RequestType requestType, String requestingJobId, String requestingNodeId) {
    this(requestType, requestingJobId, requestingNodeId, -1L);
  }

  public WorkRequest(RequestType requestType, String requestingJobId, String requestingNodeId, long key) {
    this.requestType = requestType;
    this.requestingJobId = requestingJobId;
    this.requestingNodeId = requestingNodeId;
    this.key = key;
    this.otherRequest = null;
    this.work = null;
    this.timestamp = 0L;
  }

  public WorkRequest(long key, Publishable work) {
    this();

    this.key = key;
    this.work = work;
  }

  /**
   * Set the requested key.
   */
  public void setKey(long key) {
    this.key = key;
  }

  /**
   * Set the other request.
   */
  public void setOtherRequest(String otherRequest) {
    this.otherRequest = otherRequest;
  }

  /**
   * Set the work.
   */
  public void setWork(Publishable work) {
    this.work = work;
  }

  /**
   * Set the timestamp.
   */
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * Get the request type.
   */
  public RequestType getRequestType() {
    return requestType;
  }

  /**
   * Get the requesting job id.
   */
  public String getRequestingJobId() {
    return requestingJobId;
  }

  /**
   * Get the requesting node id.
   */
  public String getRequestingNodeId() {
    return requestingNodeId;
  }

  /**
   * Get the requested key.
   */
  public long getKey() {
    return key;
  }

  /**
   * Get the other request.
   */
  public String getOtherRequest() {
    return otherRequest;
  }

  /**
   * Get the work.
   */
  public Publishable getWork() {
    return work;
  }

  /**
   * Get the timestamp;
   */
  public long getTimestamp() {
    return timestamp;
  }

  /**
   * Get a string representation of this request.
   */
  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append("WorkRequest(reqType=").
      append(requestType).
      append(",reqNode=").
      append(requestingNodeId);

    if (work != null) {
      result.
        append(",work=").
        append(work.toString());
    }

    result.append(")");

    return result.toString();
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    MessageHelper.writeString(dataOutput, requestType.name());
    MessageHelper.writeString(dataOutput, requestingJobId);
    MessageHelper.writeString(dataOutput, requestingNodeId);
    dataOutput.writeLong(key);
    MessageHelper.writeString(dataOutput, otherRequest);
    MessageHelper.writePublishable(dataOutput, work);
    dataOutput.writeLong(timestamp);
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
    this.requestType = Enum.valueOf(RequestType.class, MessageHelper.readString(dataInput));
    this.requestingJobId = MessageHelper.readString(dataInput);
    this.requestingNodeId = MessageHelper.readString(dataInput);
    this.key = dataInput.readLong();
    this.otherRequest = MessageHelper.readString(dataInput);
    this.work = MessageHelper.readPublishable(dataInput);
    this.timestamp = dataInput.readLong();
  }
}
