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


import org.sd.io.FileUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * OutputDataDescriptor for writing text (utf-8) data.
 * <p>
 * @author Spence Koehler
 */
public class TextOutputDataDescriptor extends OutputDataDescriptor {
  
  private transient BufferedWriter[] writers;

  public TextOutputDataDescriptor() {
    super();
    this.writers = null;
  }

  public TextOutputDataDescriptor(String dataId, Collection<String> nodes,
                                  PartitionFunction partitionFunction, StringKeyFunction stringKeyFunction,
                                  String overridingDataPath, boolean append) {
    super(dataId, nodes, partitionFunction, stringKeyFunction, overridingDataPath, append);
    this.writers = null;
  }

  /**
   * Write the UTF-8 text including a newLine.
   */
  public void writeTextLine(String line) throws IOException {
    final long key = getStringKeyFunction().getKey(line);  // get key
    final int partition = getPartitionFunction().getPartition(key, getNumPartitions());  // get key's partition
    final BufferedWriter writer = getWriter(partition);  // get partition's filehandle
    synchronized(writer) {
      writer.write(line);  // write to filehandle
      writer.newLine();    // new line
      writer.flush();      // flush
    }
  }

  // open getNumPartitions() BufferedWriters
  public void open() throws IOException {
    final int numHandles = getNumPartitions();
    this.writers = new BufferedWriter[numHandles];

    for (int i = 0; i < writers.length; ++i) {
      writers[i] = createBufferedWriter(i);
    }

    // mkdirs
    final String dataPath = getDataPath();
    final File file = new File(dataPath);
    file.mkdirs();
  }

  private final BufferedWriter createBufferedWriter(int partition) throws IOException {
    final String path = getPartitionPath(partition, ".txt");
    return FileUtil.getWriter(path, append());
  }

  // close all BufferedWriters
  public void close() throws IOException {
    for (BufferedWriter writer : writers) {
      writer.flush();
      writer.close();
    }
  }

  /**
   * Get the writer for the given partition.
   */
  private final BufferedWriter getWriter(int partition) {
    return writers[partition];
  }
}
