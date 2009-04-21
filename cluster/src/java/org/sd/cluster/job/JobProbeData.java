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
import org.sd.io.Publishable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Container for probed job data.
 * <p>
 * @author Spence Koehler
 */
public class JobProbeData implements Publishable {

  private JobProbeData prev;
  private JobProbeData next;
  private GlobalJobId globalJobId;
  private String jobId;
  private JobStatus jobStatus;
  private Map<String, String> properties;

  private transient String nodeName;

  public JobProbeData() {
    this.prev = null;
    this.next = null;
    this.globalJobId = null;
    this.jobId = null;
    this.jobStatus = null;
    this.properties = null;
    this.nodeName = null;
  }

  public JobProbeData(Job job) {
    this.prev = null;
    this.next = null;

    this.globalJobId = job.getGlobalJobId();
    this.jobId = job.getJobId();
    this.jobStatus = job.getStatus();
    this.properties = new LinkedHashMap<String, String>();
    job.setProperties(properties);

    this.nodeName = null;
  }

  /**
   * Set the name of the node from which this data originated.
   * <p>
   * NOTE: currently only set on "receiving" side of messaging;
   *       used to regenerate localJobId.
   */
  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  /**
   * Get the local job id from which this data originated.
   * <p>
   * NOTE: relies on setNodeName having been called on receiving end
   *       of message.
   */
  public LocalJobId getLocalJobId() {
    LocalJobId result = null;

    if (nodeName != null && globalJobId != null) {
      return globalJobId.getLocalJobId(nodeName);
    }

    return result;
  }

  public void setNext(JobProbeData jobProbeData) {
    if (this.next != null && this.next != jobProbeData) {
      this.next.prev = null;  // disconnect old next
    }
    this.next = jobProbeData;
    if (jobProbeData != null && jobProbeData.prev != this) {
      jobProbeData.setPrev(this);  // back-connect if needed
    }
  }

  public JobProbeData getNext() {
    return next;
  }

  public void setPrev(JobProbeData jobProbeData) {
    if (this.prev != null && this.prev != jobProbeData) {
      this.prev.next = null;  // disconnect old prev
    }
    this.prev = jobProbeData;
    if (jobProbeData != null && jobProbeData.next != this) {
      jobProbeData.setNext(this);  // forward-connect if needed
    }
  }

  public JobProbeData getPrev() {
    return prev;
  }

  public GlobalJobId getGlobalJobId() {
    return globalJobId;
  }

  public String getJobId() {
    return jobId;
  }

  public JobStatus getJobStatus() {
    return jobStatus;
  }

  public Map<String, String> getPropertyMap() {
    return properties;
  }

  public String getPropertyValue(String key) {
    return properties.get(key);
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    for (JobProbeData jpd = this; jpd != null; jpd = jpd.getNext()) {
      MessageHelper.writePublishable(dataOutput, jpd.globalJobId);
      MessageHelper.writeString(dataOutput, jpd.jobId);
      MessageHelper.writeString(dataOutput, jpd.jobStatus.name());

      dataOutput.writeInt(properties.size());
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        MessageHelper.writeString(dataOutput, entry.getKey());
        MessageHelper.writeString(dataOutput, entry.getValue());
      }

      dataOutput.writeBoolean(jpd.next != null);
    }
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
    this.globalJobId = (GlobalJobId)MessageHelper.readPublishable(dataInput);
    this.jobId = MessageHelper.readString(dataInput);
    final String jobStatusName = MessageHelper.readString(dataInput);
    this.jobStatus = Enum.valueOf(JobStatus.class, jobStatusName);

    this.properties = new LinkedHashMap<String, String>();
    final int numProperties = dataInput.readInt();
    for (int i = 0; i < numProperties; ++i) {
      final String key = MessageHelper.readString(dataInput);
      final String value = MessageHelper.readString(dataInput);
      properties.put(key, value);
    }

    final boolean hasNext = dataInput.readBoolean();
    if (hasNext) {
      final JobProbeData nextData = new JobProbeData();
      nextData.read(dataInput);
      this.setNext(nextData);
    }
  }

  public final String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(jobId).append(' ').append(jobStatus).append(' ').append(properties);
    if (next != null) {
      result.append('\n');
      result.append(next.toString());
    }

    return result.toString();
  }
}
