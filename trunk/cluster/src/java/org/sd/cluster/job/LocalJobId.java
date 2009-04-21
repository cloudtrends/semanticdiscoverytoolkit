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
import org.sd.util.StringUtil;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An identifier for a job in reference to a single node.
 * <p>
 * Note that the job may be distributed across multiple nodes, each of
 * which will have a unique local job id.
 *
 * @author Spence Koehler
 */
public class LocalJobId implements Publishable {
  
  private String nodeName;  // nodeName-jvmNum of node running job
                            //  can also be groupName and/or
                            //  comma-delimited list of node/groupNames.
  private int id;           // local id of the job on that node
  private String jobDescription;

  public LocalJobId() {
  }

  public LocalJobId(String nodeName, int id) {
    this.nodeName = nodeName;
    this.id = id;
  }

  public LocalJobId(String jobDescription, String groupName) {
    this.nodeName = groupName;
    this.jobDescription = jobDescription;
  }

  /**
   * Construct from an instance of the form "id.nodeName-jvmNum" or
   * "jobDescription.groupName"
   */
  public LocalJobId(String jobIdString) {
    final String[] pieces = jobIdString.split("\\.");

    if (StringUtil.isDigits(pieces[0])) {
      // have form "id.nodeName-jvmNum"
      this.id = "".equals(pieces[0]) ? 0 : Integer.parseInt(pieces[0]);
      this.nodeName = pieces[1];
    }
    else {
      // have form "jobDescription.groupName"
      this.jobDescription = pieces[0];
      this.nodeName = pieces[1];  // groupName
    }
  }

  /**
   * Get the node (or group) name(s).
   */
  public String getNodeName() {
    return nodeName;
  }

  /**
   * Get the local job id number (if it exists).
   */
  public int getId() {
    return id;
  }

  /**
   * Set the local job id number.
   */
  public void setId(int id) {
    this.id = id;
  }

  /**
   * Get the job description (if it exists).
   */
  public String getJobDescription() {
    return jobDescription;
  }

  public boolean equals(Object o) {
    boolean result = (this == o);

    if (!result) {
      final LocalJobId other = (LocalJobId)o;
      result = this.nodeName.equals(other.nodeName) &&
        ((jobDescription == null) ? (this.id == other.id) : this.jobDescription.equals(other.jobDescription));

    }

    return result;
  }

  public int hashCode() {
    int result = nodeName.hashCode();

    if (jobDescription == null) {
      result = 17 * id + result;
    }
    else {
      result = 17 * jobDescription.hashCode() + result;
    }

    return result;
  }

  public String toString() {
    String result = null;

    if (jobDescription == null) {
      result = id + "." + nodeName;
    }
    else {
      result = jobDescription + "." + nodeName;
    }

    return result;
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    MessageHelper.writeString(dataOutput, nodeName);
    dataOutput.writeInt(id);
    MessageHelper.writeString(dataOutput, jobDescription);
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
    this.nodeName = MessageHelper.readString(dataInput);
    this.id = dataInput.readInt();
    this.jobDescription = MessageHelper.readString(dataInput);
  }
}
