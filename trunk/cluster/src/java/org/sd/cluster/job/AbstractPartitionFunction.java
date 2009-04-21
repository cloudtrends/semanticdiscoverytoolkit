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
import org.sd.cluster.config.ClusterContext;
import org.sd.cluster.config.ClusterDefinition;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

/**
 * Abstract implementation of the partition function interface for cluster tasks.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractPartitionFunction implements PartitionFunction {
  
  /**
   * Get the key's partition based on this partition function's parameters.
   * <p>
   * Possible partitioning schemes:
   * <ul>
   * <li>Return key % numPartitions.</li>
   * <li>Pick a uniformly distributed random number from 0 to numPartitions.</li>
   * <li>Find and return the group node most ready for work.</li>
   * <li>Keep a rotating counter for each request.</li>
   * </ul>
   *
   * @return the partition for the given key: a number between 0 and numPartitions.
   */
  protected abstract int getPartition(long key, int numPartitions);


  private String groupName;
  private String[] groupNodeNames;

  protected AbstractPartitionFunction() {
  }

  protected AbstractPartitionFunction(ClusterContext clusterContext, String groupName) {
    this.groupName = groupName;
    this.groupNodeNames = null;

    final ClusterDefinition clusterDef = clusterContext.getClusterDefinition();
    if (groupName != null) {
      final List<String> names = clusterDef.getGroupNodeNames(groupName, true);
      if (names != null) {
        this.groupNodeNames = names.toArray(new String[names.size()]);
      }
      else {
        throw new IllegalArgumentException("Bad groupName '" + groupName + "'!");
      }
    }
    else {
      // no group ==> this node is the destination.
      this.groupNodeNames = new String[]{clusterContext.getConfig().getNodeName()};
    }
  }

  protected AbstractPartitionFunction(String groupName, String[] groupNodeNames) {
    this.groupName = groupName;
    this.groupNodeNames = groupNodeNames;
  }

  public String getGroupName() {
    return groupName;
  }

  public String[] getGroupNodeNames() {
    return groupNodeNames;
  }

  public int getNumPartitions() {
    return groupNodeNames.length;
  }

  public final String computeDestinationId(long dataKey) {

    final int numPartitions = getNumPartitions();
    final int partitionNum = getPartition(dataKey, numPartitions);

    if (partitionNum < 0 || partitionNum >= numPartitions) {
      throw new IllegalStateException("getPartition(" + dataKey + ") returned a bad partition (" +
                                      partitionNum + "). Must be between 0 (inclusive) and " +
                                      numPartitions + " (exclusive)!");
    }

    return groupNodeNames[partitionNum];
  }

  /**
   * Write thie publishable to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    MessageHelper.writeString(dataOutput, groupName);
    MessageHelper.writeStrings(dataOutput, groupNodeNames);
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
    this.groupName = MessageHelper.readString(dataInput);
    this.groupNodeNames = MessageHelper.readStrings(dataInput);
  }
}
