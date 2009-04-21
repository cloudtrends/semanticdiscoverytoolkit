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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * An identifier for a job that is distributed across multiple nodes.
 * <p>
 * The global ID consists of all local ID's in association with the local
 * nodes.
 *
 * @author Spence Koehler
 */
public class GlobalJobId implements Publishable {
  
  private Collection<LocalJobId> localJobIds;
  private int globalJobId;

  private transient Map<String, LocalJobId> nodeName2jobId;

  public GlobalJobId() {
    this.globalJobId = 0;
  }

  public void add(String nodeName, int localJobId) {
    add(new LocalJobId(nodeName, localJobId));
  }

  public void add(LocalJobId localJobId) {
    if (localJobIds == null) localJobIds = new ArrayList<LocalJobId>();
    localJobIds.add(localJobId);

    if (nodeName2jobId == null) nodeName2jobId = new HashMap<String, LocalJobId>();
    nodeName2jobId.put(localJobId.getNodeName().toLowerCase(), localJobId);

    // global job ID is the maximum of all local job IDs and should uniquely identify
    // this job globally to the cluster.
    if (globalJobId < localJobId.getId()) {
      globalJobId = localJobId.getId();
    }
  }

  public Collection<LocalJobId> getLocalJobIds() {
    return localJobIds;
  }

  public LocalJobId getLocalJobId(String nodeName) {
    return nodeName2jobId.get(nodeName.toLowerCase());
  }

  public int getGlobalJobId() {
    return globalJobId;
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    if (localJobIds == null) {
      dataOutput.writeInt(0);
    }
    else {
      dataOutput.writeInt(localJobIds.size());
      for (LocalJobId localJobId : localJobIds) {
        MessageHelper.writePublishable(dataOutput, localJobId);
      }
    }
    dataOutput.writeInt(globalJobId);
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
    final int numNodes = dataInput.readInt();
    if (numNodes > 0) {
      this.localJobIds = new ArrayList<LocalJobId>();
      for (int i = 0; i < numNodes; ++i) {
        // must go through add method to keep map up-to-date
        add((LocalJobId)MessageHelper.readPublishable(dataInput));
      }
    }
    this.globalJobId = dataInput.readInt();
  }

  public String toString() {
    return nodeName2jobId.toString();
  }
}
