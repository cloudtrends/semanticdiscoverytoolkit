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
 * OutputDataDescriptor for writing sets of files.
 * <p>
 * @author Spence Koehler
 */
public class FilesetOutputDataDescriptor extends OutputDataDescriptor {
  
  public FilesetOutputDataDescriptor() {
    super();
  }

  public FilesetOutputDataDescriptor(String dataId, Collection<String> nodes,
                                     PartitionFunction partitionFunction, StringKeyFunction stringKeyFunction,
                                     String overridingDataPath) {
    super(dataId, nodes, partitionFunction, stringKeyFunction, overridingDataPath, false);
  }

  /**
   * Get the next file writer.
   * <p>
   * NOTE: It is the responsibility of the consumer to flush and close the writer.
   *
   * @param relativeName A name for the file relative to the partition directory. If it
   *                     ends in .gz, the stream will be gzipped. The name can include
   *                     directories. For example, "00/00/domain/timestamp.xhtml.gz"
   *                     will be written to:
   *  $HOME/cluster/jvm-myJvmNum/data/output/dataId/srcId-destId.dir/00/00/domain/timestamp.xhtml.gz
   *
   * @return a buffered writer.
   */
  public final BufferedWriter getNextFileWriter(String relativeName) throws IOException {
    final long key = getStringKeyFunction().getKey(relativeName);  // get key
    final int partition = getPartitionFunction().getPartition(key, getNumPartitions());  // get key's partition
    final String fileName = ".dir/" + relativeName;
    final String path = getPartitionPath(partition, fileName);
    FileUtil.mkdirs(path);
    return FileUtil.getWriter(path);
  }

  public void open() throws IOException {
    final int numHandles = getNumPartitions();

    // mkdirs
    for (int i = 0; i < numHandles; ++i) {
      final String path = getPartitionPath(i, "/");
      final File file = new File(path);
      file.mkdirs();
    }
  }

  public void close() throws IOException {
    // nothing to do. ...consumer is expected to close files.
  }
}
