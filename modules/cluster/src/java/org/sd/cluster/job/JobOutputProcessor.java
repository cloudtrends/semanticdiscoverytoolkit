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
import org.sd.cluster.io.Response;
import org.sd.io.Publishable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Properties;

/**
 * Base class for processing mounted cache job output.
 * <p>
 * @author Spence Koehler
 */
public abstract class JobOutputProcessor extends AbstractJob {

  private String jobIdString;  // job ID of job whose output we'll process
  private String dataDirName;  // name or label for data of job whose output we'll process

  public JobOutputProcessor() {
    super();
  }

  public JobOutputProcessor(String id, String dataDirName, String groupName) {
    super(1, id + "-output", groupName, true);
    this.jobIdString = id;
    this.dataDirName = dataDirName;
  }

  public JobOutputProcessor(Properties properties) {
    this(properties.getProperty("id"), properties.getProperty("dataDirName"), properties.getProperty("groupName"));

    if (jobIdString == null || dataDirName == null) {
      throw new IllegalArgumentException("Missing required property(ies) among 'id', 'dataDirName', 'groupName'!");
    }
  }

  protected final String getOriginalJobId() {
    return jobIdString;
  }

  protected final String getDataDirName() {
    return dataDirName;
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);
    MessageHelper.writeString(dataOutput, jobIdString);
    MessageHelper.writeString(dataOutput, dataDirName);
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
    this.jobIdString = MessageHelper.readString(dataInput);
    this.dataDirName = MessageHelper.readString(dataInput);
  }
  
  public String getDescription() {
    return "JobOutputProcessor jobId=" + getJobId() + " dataDir=" + dataDirName;
  }

  public Response operate(Publishable request) {
    throw new UnsupportedOperationException("Shouldn't ever need this with this job!");
  }

//   public void start() {  // start processing job's work.
//   }

  public void stop() {  // end processing job in this jvm; could still suspend? can't resume.
  }

  public void pause() {  // pause a job in this jvm to be resumed.
  }

  public void resume() {  // resume paused job.
  }

  public void suspend() {  // suspend to disk for restoration through JobManager in another jvm
  }

  public void shutdown(boolean now) {  // called when cluster is being shutdown
  }
}
