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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Properties;

import org.sd.cio.MessageHelper;
import org.sd.cluster.config.ClusterException;
import org.sd.cluster.config.Config;
import org.sd.util.PropertiesParser;

/**
 * A job for serving work to other jobs across a cluster.
 * <p>
 * @author Spence Koehler
 */
public class DataTransferWorkServer extends AbstractBatchWorkServer {

  private String[] dataMachineDirs;
  private String[] dataMachines;
  
  public DataTransferWorkServer() {
    super();
  }

  public DataTransferWorkServer(Properties properties) {
    super(properties);
    
    dataMachines = properties.getProperty("dataMachines").split("\\s*,\\s*");
    dataMachineDirs = properties.getProperty("dataMachineDir").split("\\s*,\\s*");
    for (int i = 0; i < dataMachineDirs.length; ++i) {
      if (!dataMachineDirs[i].endsWith("/")) {
        dataMachineDirs[i] += "/";
      }
    }
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);
    MessageHelper.writeStrings(dataOutput, dataMachineDirs);
    MessageHelper.writeStrings(dataOutput, dataMachines);
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
    this.dataMachineDirs = MessageHelper.readStrings(dataInput);
    this.dataMachines = MessageHelper.readStrings(dataInput);
  }

  protected final PathBatch getPathBatch() {
    PathBatch result = new PathBatch(onlyOwn, acceptHelp);

    for (String machine : dataMachines) {
      for (int i=0; i < 256; i++) {
        for (String dataMachineDir : dataMachineDirs) {
          result.addPath(machine + ":" + dataMachineDir + (i < 16 ? "0" : "") + Integer.toHexString(i));
        }
      }
    }
    
    return result;
  }

  public static void main(String[] args) throws IOException, ClusterException {
    // start the work server on a node
    //
    //properties: [user], [defName], [machines], jobId(=dataDirName), groupName(=workServerId), dataMachines(=machineName,machineName,...), dataMachineDir(=rootDataDir1,rootDataDir2,...), [onlyOwn] [acceptHelp] [limit]
    final PropertiesParser pp = new PropertiesParser(args);
    final WorkServer workServer = new DataTransferWorkServer(pp.getProperties());
    run(pp, workServer);
  }
}
