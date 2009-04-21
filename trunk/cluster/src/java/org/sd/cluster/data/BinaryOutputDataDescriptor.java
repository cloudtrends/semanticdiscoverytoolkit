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
import org.sd.cluster.io.Partitionable;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

/**
 * OutputDataDescriptor for writing partitionable binary data.
 * <p>
 * @author Spence Koehler
 */
public class BinaryOutputDataDescriptor extends OutputDataDescriptor {
  
  private transient DataOutputStream[] outputs;

  public BinaryOutputDataDescriptor() {
    super();
    this.outputs = null;
  }

  public BinaryOutputDataDescriptor(String dataId, Collection<String> nodes,
                                    PartitionFunction partitionFunction,
                                    String overridingDataPath, boolean append) {
    super(dataId, nodes, partitionFunction, null, overridingDataPath, append);
    this.outputs = null;
  }

  public void writePartitionable(Partitionable partitionable) throws IOException {
    final long key = partitionable.getKey();  // get key
    final int partition = getPartitionFunction().getPartition(key, getNumPartitions());  // get key's partition
    final DataOutput dataOutput = getDataOutput(partition);  // get partition's filehandle
    synchronized(dataOutput) {
      MessageHelper.writePublishable(dataOutput, partitionable);  // write to filehandle
    }
  }

  // open getNumPartitions() dataOutputs
  public void open() throws IOException {
    final int numHandles = getNumPartitions();
    this.outputs = new DataOutputStream[numHandles];

    for (int i = 0; i < outputs.length; ++i) {
      outputs[i] = createDataOutput(i);
    }

    // mkdirs
    final String dataPath = getDataPath();
    final File file = new File(dataPath);
    file.mkdirs();
  }

  private final DataOutputStream createDataOutput(int partition) throws IOException {
    final String path = getPartitionPath(partition, ".bin");
    return new DataOutputStream(new FileOutputStream(path, append()));
  }

  // close all dataOutputs
  public void close() throws IOException {
    for (DataOutputStream output : outputs) {
      output.close();
    }
  }

  /**
   * Get the data output for the given partition.
   */
  private final DataOutputStream getDataOutput(int partition) {
    return outputs[partition];
  }
}
