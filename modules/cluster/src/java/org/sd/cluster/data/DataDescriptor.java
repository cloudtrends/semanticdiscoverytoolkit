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
package org.sd.cluster.data;


import org.sd.cio.MessageHelper;
import org.sd.cluster.config.ClusterContext;
import org.sd.cluster.config.Clusterable;
import org.sd.cluster.config.Config;
import org.sd.io.Publishable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * An class to encapsulate the definition of a data store.
 * <p>
 * @author Spence Koehler
 */
public class DataDescriptor implements Publishable, Clusterable {
  
//  public static enum Distribution {PARTITIONED, REPLICATED, MOUNTED};
//  public static enum Format {BINARY, TEXT, FILESET};

//partitioned across nodes -vs- replicated on each node -vs- accessible via common mount

//todo: need factory creation methods that use a ClusterDefinition to get the right machines and paths
//todo: need to keep track of failed units of work by machine (notified through AbstractJob -> AbstractDataJob -> InputDataDescriptor) but it must be persisted/tracked at this level.

  private String dataId;                 // dataId or name for the data
  private Collection<String> nodes;      // nodes (machine-jvmNum) over which the data is partitioned or full data is replicated; null if accessed via common mount
  private PartitionFunction partitionFunction;  // partition function, possibly null
  private StringKeyFunction stringKeyFunction;  // string key function, possibly null
  private String overridingDataPath;

  private transient ClusterContext context;
  private transient String dataPath;  // full path to data
  private transient String myNodeId;
  private transient int myNodeNum;


//  private Distribution distribution;     // distribution of data in cluster
//  private Format format;                 // format of data (binary or text).

//private int jobProgress;  // record status of job progress so it can be resumed after a hard failure??? here? elsewhere?

  public DataDescriptor() {
    this.dataId = null;
    this.nodes = null;
    this.partitionFunction = null;
    this.stringKeyFunction = null;
    this.overridingDataPath = null;

//    this.distribution = null;
//    this.format = null;
  }

  public DataDescriptor(String dataId, Collection<String> nodes,
                        PartitionFunction partitionFunction, StringKeyFunction stringKeyFunction,
                        String overridingDataPath) {
//, Distribution distribution, String path, Format format) {
    this.dataId = dataId;
    this.nodes = nodes;
    this.partitionFunction = partitionFunction;
    this.stringKeyFunction = stringKeyFunction;
    this.overridingDataPath = overridingDataPath;

    // note: transients will be initialized when we have a config thru setClusterContext.

//    this.distribution = distribution;
//    this.format = format;
  }

  private final void initializeTransients(Config config) {
    this.dataPath = (overridingDataPath == null || overridingDataPath.length() == 0) ? buildDataPath(config) : overridingDataPath;
    this.myNodeId = config.getMachineName() + "-" + config.getJvmNum();
    this.myNodeNum = computeNodeNum(myNodeId);
  }

  private final int computeNodeNum(String myNodeId) {
    int result = -1;

    if (nodes != null) {
      for (String node : nodes) {
        ++result;
        if (myNodeId.equalsIgnoreCase(node)) break;
      }
    }

    return result;
  }

  private final String buildDataPath(Config config) {
    final StringBuilder result = new StringBuilder();

    result.append(config.getJvmRootDir()).append("data/output/").
      append(getDataId()).append('/');

    return result.toString();
  }

  protected final PartitionFunction getPartitionFunction() {
    return partitionFunction;
  }

  protected final StringKeyFunction getStringKeyFunction() {
    return stringKeyFunction;
  }

  protected final String getMyNodeId() {
    return myNodeId;
  }

  protected final int getMyNodeNum() {
    return myNodeNum;
  }

  protected final String getPartitionPath(int partition, String ext) {
    final StringBuilder result = new StringBuilder();

    // getDataPath()/sourceId.destId.{bin,txt}
    // where
    //   dataPath = $HOME/cluster/jvm-myJvmNum/data/output/dataId/
    //   sourceId = machine-myJvmNum
    //   destId = partition'sMachine-jvmNum
    result.append(dataPath).append(myNodeId).append('.').
      append(buildNodeId(partition)).append(ext);

    return result.toString();
  }

  protected final String getDataPath() {
    return dataPath;
  }

  private final String buildNodeId(int partition) {
    final Collection<String> nodes = getNodes();
    final Iterator<String> iter = nodes.iterator();
    for (int i = 0; i < partition && iter.hasNext(); ++i) iter.next();
    return iter.hasNext() ? iter.next() : null;
  }

  /**
   * Set this clusterable's clusterContext.
   * <p>
   * Note that this is not set until a cluster node's node server handles
   * a message that uses this clusterable object and the message passes
   * the context along to this clusterable object.
   *
   * @param clusterContext  this object's clusterContext.
   */
  public void setClusterContext(ClusterContext clusterContext) {
    this.context = clusterContext;
    initializeTransients(context.getConfig());
  }

  /**
   * Get this clusterable's clusterContext.
   *
   * @return this object's clusterContext.
   */
  public ClusterContext getClusterContext() {
    return context;
  }

  /**
   * Get the dataId or name for the data.
   */
  public String getDataId() {
    return dataId;
  }

  /**
   * Get the nodes (machine-jvmNum) over which the data is partitioned or full data is replicated.
   * <p>
   * Note that these nodes are where the final data ends up to be used for input,
   * not necessarily the same as the nodes where it is created.
   *
   * @return nodes or null if data is accessed via a common mount point.
   */
  public Collection<String> getNodes() {
    return nodes;
  }

  /**
   * Get the number of partitions for the data.
   * <p>
   * Note that these partitions are where the final data ends up to be used for input,
   * not necessarily the same as the partitions where it is created.
   */
  public int getNumPartitions() {
    return nodes == null ? 1 : nodes.size();
  }

//   /**
//    * Get the distribution of the data in the cluster.
//    */
//   public Distribution getDistribution() {
//     return distribution;
//   }

//   /**
//    * Get the format of the data.
//    * <p>
//    * Format.BINARY data is Publishable data suitable for reading using MessageUtil.readPublishable until EOFException is caught.
//    * Format.TEXT data is UTF-8 data suitable for reading using a BufferedReader.
//    */
//   public Format getFormat() {
//     return format;
//   }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    MessageHelper.writeString(dataOutput, dataId);
    if (nodes == null) {
      dataOutput.writeInt(0);
    }
    else {
      dataOutput.writeInt(nodes.size());
      for (String node : nodes) {
        MessageHelper.writeString(dataOutput, node);
      }
    }

    MessageHelper.writeNonPublishable(dataOutput, partitionFunction);
    MessageHelper.writeNonPublishable(dataOutput, stringKeyFunction);

//    MessageHelper.writeString(dataOutput, distribution.toString());
//    MessageHelper.writeString(dataOutput, format.toString());
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
    this.dataId = MessageHelper.readString(dataInput);
    int numNodes = dataInput.readInt();
    if (numNodes > 0) {
      this.nodes = new ArrayList<String>();
      for (int i = 0; i < numNodes; ++i) {
        this.nodes.add(MessageHelper.readString(dataInput));
      }
    }

    this.partitionFunction = (PartitionFunction)MessageHelper.readNonPublishable(dataInput);
    this.stringKeyFunction = (StringKeyFunction)MessageHelper.readNonPublishable(dataInput);
    this.overridingDataPath = MessageHelper.readString(dataInput);

//     this.distribution = Enum.valueOf(Distribution.class, MessageHelper.readString(dataInput));
//     this.format = Enum.valueOf(Format.class, MessageHelper.readString(dataInput));
  }

  /**
   * Synchronize this data descriptor across the cluster.
   */
  public void synchronize() {
// in particular, we keep track of what work has been done on each node
// and let the other nodes know what we've done on this node.
  }

  public void reset() {
// reset such that no work is marked done.
  }

  void save() throws IOException {
//maybe a data manager oughtta take care of this
// ~/cluster/data/<cluster-name>/descriptor/<dataId>.def

  }

  void restore() throws IOException {
//maybe a data manager oughtta take care of this
// ~/cluster/data/<cluster-name>/descriptor/<dataId>.def

  }
}
