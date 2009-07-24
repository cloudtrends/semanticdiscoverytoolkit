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
import org.sd.cluster.config.Config;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Properties;

/**
 * Base class for work server client jobs with output.
 * <p>
 * Manages the dataDirName and outputFile.
 *
 * @author Spence Koehler
 */
public abstract class WorkServerClientWithOutputJob extends WorkServerClientJob {

  private String dataDirName;  // name or label for data
  private boolean doCopy;      // flag to copy data if missing

  private transient String outputFile;

  /**
   * Default constructor.
   * <p>
   * NOTE: All extending classes MUST create a default constructor
   *       that calls this super!!!
   */
  public WorkServerClientWithOutputJob() {
    super();
  }

  /**
   * Construct with properties.
   * <p>
   * Properties: numThreads, jobId, groupName, beginImmediately,
   *             workServer, maxTimePerUnit,
   *             dataDirName
   */
  public WorkServerClientWithOutputJob(Properties properties) {
    super(properties);

    this.dataDirName = properties.getProperty("dataDirName");

    if (dataDirName == null) {
      throw new IllegalArgumentException("Missing required property 'dataDirName'!");
    }

    this.doCopy = "true".equals(properties.getProperty("doCopy", "true"));
  }

  /**
   * Get the dataDirName.
   */
  protected final String getDataDirName() {
    return dataDirName;
  }

  /**
   * Get the doCopy flag's value.
   */
  protected final boolean doCopy() {
    return doCopy;
  }

  /**
   * Get the output file name derived from the dataDirName.
   */
  protected final String getOutputFilename() {
    if (this.outputFile == null) {
      final Config config = getConfig();

      // set up output file
      this.outputFile = config.getOutputDataPath(getJobId(), dataDirName);
    }
    return this.outputFile;
  }
  
  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);
    MessageHelper.writeString(dataOutput, dataDirName);
    dataOutput.writeBoolean(doCopy);
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
    this.dataDirName = MessageHelper.readString(dataInput);
    this.doCopy = dataInput.readBoolean();
  }
  
}
