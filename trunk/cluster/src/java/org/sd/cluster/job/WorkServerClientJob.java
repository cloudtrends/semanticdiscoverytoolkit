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
import java.util.Date;
import java.util.Properties;

/**
 * Abstract implementation of a job that will be a client to a work server.
 * <p>
 * Extenders must implement OldAbstractJob.doNextOperation.
 *
 * @author Spence Koehler
 */
public abstract class WorkServerClientJob extends OldAbstractJob {

  private LocalJobId localWorkServerId;

  /**
   * Default constructor.
   * <p>
   * NOTE: All extending classes MUST create a default constructor
   *       that calls this super!!!
   */
  protected WorkServerClientJob() {
    super();
  }

  /**
   * Construct with properties.
   * <p>
   * Properties: numThreads, jobId, groupName, beginImmediately,
   *             workServer, maxTimePerUnit
   */
  protected WorkServerClientJob(Properties properties) {
    super(properties);

    this.localWorkServerId = JobUtil.parseWorkServerId(properties.getProperty("workServer"));

    if (this.localWorkServerId == null) {
      throw new IllegalArgumentException("Missing required property 'workServer'!");
    }
  }

  /**
   * Get the work factory that will serve up work units to this job.
   */
  protected WorkFactory getWorkFactory() throws IOException {
    final Config config = getConfig();
    final String myNodeName = config.getNodeName();

    final ClientWorkFactory result = new ClientWorkFactory(localWorkServerId, getJobId(), myNodeName, getConsole());

    return result;
  }
  
  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);

    MessageHelper.writePublishable(dataOutput, localWorkServerId);
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

    this.localWorkServerId = (LocalJobId)MessageHelper.readPublishable(dataInput);
  }

  /**
   * Hook to run when the job is paused.
   */
  protected void runPauseHook() {
    System.out.println(new Date() + " " + getJobId() + " -- PAUSED");
  }

  /**
   * Hook to run after each operation is submitted to the thread pool.
   */
  protected void runRunningHook() {
    // nothing to do.
  }

  /**
   * Try to find more work to do.
   *
   * @return true if more work was found; otherwise false.
   */
  protected boolean findMoreWorkToDo() {
    // nothing to do.
    return false;
  }
}
