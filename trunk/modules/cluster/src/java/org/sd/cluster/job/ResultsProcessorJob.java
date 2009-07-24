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
import org.sd.cluster.job.WorkServer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A job for processing the results of a mounted cache job.
 * <p>
 * @author Spence Koehler
 */
public abstract class ResultsProcessorJob extends OldAbstractJob {
  
  private String dataDirName;  // name or label for data

  protected ResultsProcessorJob() {
    super();
  }

  protected ResultsProcessorJob(String id, String dataDirName, String groupName, int numThreads) {
    super(numThreads, id, groupName, true);
    this.dataDirName = dataDirName;
  }

  protected ResultsProcessorJob(Properties properties) {
    super(properties);

    this.dataDirName = properties.getProperty("dataDirName");

    if (this.dataDirName == null) {
      throw new IllegalArgumentException("Missing required property 'dataDirName'!");
    }
  }

  protected String getDataDirName() {
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
    this.dataDirName = MessageHelper.readString(dataInput);
  }
  
  protected WorkFactory getWorkFactory() throws IOException {
    final Config config = getConfig();

    final BasicWorkFactory result = new BasicWorkFactory();
    final String resultFilename = config.getOutputDataPath(getJobId(), dataDirName);
    result.addWork(resultFilename);
    result.addWork(WorkServer.WORK_IS_DONE_RESPONSE);

    return result;
  }

  protected File getJobFile(String jobId, String jobDataDir, boolean shouldExist) {
    File result = null;

    final Config config = getConfig();
    String filename = config.getOutputDataPath(jobId, jobDataDir);
    result = new File(filename);
    if (shouldExist && !result.exists()) {
      // try looking for compressed form.
      filename += ".gz";
      result = new File(filename);
    }

    return result;
  }

  protected static final Pattern NODE_FILE_PATTERN = Pattern.compile("^(.*/jvm-)(\\d+)(/.*-)(\\d+)(\\.out.*)$");

  /**
   * Given a job file for the current node, find the same job file for each of the
   * nodes on the machine.
   * <p>
   * NOTE: this exploits the ".../jvm-N/.../nodename-N.out..." convention set up in Config.
   */
  protected File[] extrapolateToAllNodes(File jobFile) {
    final Matcher matcher = NODE_FILE_PATTERN.matcher(jobFile.getAbsolutePath());

    if (!matcher.matches()) return new File[]{jobFile};

    final List<File> result = new ArrayList<File>();

    // count up from 0 until the file doesn't exist, max-ing out at 100 -- assuming we'll not have 100 nodes on a machine.
    for (int i = 0; i < 100; ++i) {
      final String nodeNum = Integer.toString(i);
      final String filename = matcher.group(1) + nodeNum + matcher.group(3) + nodeNum + matcher.group(5);
      final File file = new File(filename);
      if (file.exists()) {
        result.add(file);
      }
      else break;
    }

    return result.toArray(new File[result.size()]);
  }

  /**
   * Hook to run when the job is paused.
   */
  protected void runPauseHook() {
    //nothing to do.
  }

  /**
   * Hook to run after each operation is submitted to the thread pool.
   */
  protected void runRunningHook() {
    //nothing to do.
  }

  /**
   * Do the next operation in a thread-safe way.
   * <p>
   * NOTES:
   * <ol>
   * <li>Do not 'synchronize' this method to make it thread-safe; rather,
   *     make sure all instance accesses from within the implementation
   *     are managed in a thread-safe manner.
   * <li>The unit of work's status will be set by the underlying infrastructure
   *     and need not be set by extending classes.
   * <li>Exceptions thrown while executing this method will end up being
   *     submitted to the unitOfWork.recordFailure method.
   * </ol>
   *
   * @return true if the unit of work was successfuly handled.
   */
  public boolean doNextOperation(UnitOfWork unitOfWork, AtomicBoolean die, AtomicBoolean pause) {
    boolean result = true;

    System.out.println(new Date() + " -- Submitting '" + unitOfWork + "' to thread pool.");

   final String workPath = ((StringUnitOfWork)unitOfWork).getString();
    try {
      File workFile = new File(workPath);
      if (!workFile.exists()) {
        workFile = new File(workPath + ".gz");
      }

      if (workFile.exists()) {
        final int count = processDataAt(workFile, die, pause);

        System.out.println(new Date() + " -- processed " + count + " items within '" + unitOfWork + "'.");
      }
      else {
        System.err.println("*** WARNING: Can't find work file for '" + workPath + "'!");
      }
    }
    catch (IOException e) {
      e.printStackTrace(System.err);
      result = false;
    }

    return result;
  }

  /**
   * Try to find more work to do.
   *
   * @return true if more work was found; otherwise false.
   */
  protected boolean findMoreWorkToDo() {
    return false;
  }

  /**
   * Process the data.
   *
   * @return the number of units or items processed within the data at workPath.
   */
  protected abstract int processDataAt(File workPath, AtomicBoolean die, AtomicBoolean pause) throws IOException;
}
