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

import org.sd.cluster.io.Context;

import java.io.BufferedWriter;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A job for serving work to other jobs across a cluster.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractBatchWorkServer extends WorkServer {

  protected boolean onlyOwn;     // flag to only send a node its own data.
                                 // intended for re-using non-crawl-server caches, not for processing data on crawl server.
  protected boolean acceptHelp;  // when onlyOwn && acceptHelp, work will be parceled to non-owning machines when their work is done.
  private boolean restart;       // flag to enable reconciliation of already-done domains
  private boolean evenLimit;     // flag to limit work distribution to evenly encompass all requesting nodes

  private transient PathBatch pathBatch;
  private transient long totalWorkCount;
  private transient Map<String, Long> dest2count;

  protected AbstractBatchWorkServer() {
    super();
  }

  protected AbstractBatchWorkServer(Properties properties) {
    super(properties);

    this.onlyOwn = "true".equals(properties.getProperty("onlyOwn"));
    this.acceptHelp = "true".equals(properties.getProperty("acceptHelp"));
    this.restart = "true".equals(properties.getProperty("restart"));
    this.evenLimit = "true".equals(properties.getProperty("limit"));

    if (onlyOwn) {
      System.out.println("***NOTE: WorkServer will only distribute work to owners! [re-use cache mode].");

      if (acceptHelp) {
        System.out.println("\tBut WorkServer will allow machines that are done to help finish work.");
      }
      else {
        System.out.println("\tAnd WorkServer will not allow machines that are done to help finish work.");
      }
    }
    if (restart) {
      System.out.println("***NOTE: WorkServer will restart where it left off!");
    }
    else {
      System.out.println("***NOTE: WorkServer is starting from the begining!");
    }
    if (evenLimit) {
      System.out.println("***NOTE: WorkServer will distribute work evenly!");
    }
  }

  public boolean getOnlyOwn() {
    return onlyOwn;
  }

  public boolean getAcceptHelp() {
    return acceptHelp;
  }

  public boolean getRestart() {
    return restart;
  }

  public boolean getEvenLimit() {
    return evenLimit;
  }

  protected abstract PathBatch getPathBatch();
  
  protected final PathBatch loadPathBatch() {
    PathBatch result = getPathBatch();

    if (result != null) {
      final String outputFilename = buildOutputFilename();
      File outputFile = new File(outputFilename);

      // uniquely rename any existing output file so it won't get munged.
      if (outputFile.exists()) {
        final File newFile = getNextNewOutputFile(outputFile);
        outputFile.renameTo(newFile);

        System.out.println(new Date() + ": AbstractBatchWorkServer.loadPathBatch *** NOTE: Moved old output file to '" + newFile + "'! localJobId=" + getLocalJobId());
        outputFile = newFile;
      }
      else {
        outputFile = null;
      }

      if (restart) {
        final File existingFile = outputFile == null ? getLatestExistingOutputFile(outputFile) : outputFile;

        if (existingFile != null) {
          // if restart, remove already done domains from the batch using latest output file.
          try {
            System.out.println(new Date() + ": AbstractBatchWorkServer.loadPathBatch *** NOTE: Removing finished work from batch using '" + existingFile + "'! localJobId=" + getLocalJobId());
            final int numRemoved = result.removeFinishedWork(existingFile);
            System.out.println(new Date() + ": AbstractBatchWorkServer.loadPathBatch *** NOTE: Removed " + numRemoved + " units from '" + existingFile + "'! localJobId=" + getLocalJobId());
          }
          catch (IOException e) {
            System.err.println(new Date() + ": AbstractBatchWorkServer.loadPathBatch *** ERROR: Unable to remove finished work from '" + existingFile + "'! localJobId=" + getLocalJobId());
            throw new IllegalStateException(e);
          }

          // Name back to default if not already.
          if (existingFile != outputFile) {
            existingFile.renameTo(outputFile);
          }
        }
        else {
          System.err.println(new Date() + ": AbstractBatchWorkServer.loadPathBatch *** ERROR: Unable to find prior work for 'restart'! Starting from beginning. (" + outputFilename + ")");
          throw new IllegalStateException("Can't restart!");
        }
      }
    }

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
    dataOutput.writeBoolean(onlyOwn);
    dataOutput.writeBoolean(acceptHelp);
    dataOutput.writeBoolean(restart);
    dataOutput.writeBoolean(evenLimit);
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
    this.onlyOwn = dataInput.readBoolean();
    this.acceptHelp = dataInput.readBoolean();
    this.restart = dataInput.readBoolean();
    this.evenLimit = dataInput.readBoolean();
  }
  
  // get|requestingJobId|requestingNodeId  --> workString or WORK_IS_DONE_RESPONSE
  // add|workUnit  -->  "true"/null
  // report --> reportString
  // <other> --> responseString
  protected String doOperate(String requestString) {
    String result = null;

    final String[] pieces = requestString.split("\\s*\\|\\s*");
    pieces[0] = pieces[0].toLowerCase();

    if ("get".equals(pieces[0])) {
      // check for limit in sending to pieces[2]
      if (evenLimit) {
        final Long curCount = dest2count.get(pieces[2]);
        final int numDests = dest2count.size();
        if (curCount != null && numDests > 0) {
          final long curLimit = (totalWorkCount / numDests);
          if (curCount > curLimit) {
            return null;  // we've reached the limit for this destination
          }
        }
      }

      // get work string from pieces[1] work factory
      try {
        result = getNextWorkString(pieces[1], pieces[2]);

        if (result != null) {
          if (!WORK_IS_DONE_RESPONSE.equals(result)) {
            incrementNumSentUnits();

            final Long curCount = dest2count.get(pieces[2]);
            dest2count.put(pieces[2], curCount == null ? 1L : curCount + 1);
          }
          // log it
          //System.err.println(new Date() + ": Sending work '" + result + "' for job '" + pieces[1] + "' to node '" + pieces[2] + "'!");
        }
      }
      catch (IOException e) {
        System.err.println(new Date() + ": Error getting work for job '" + pieces[1] + "' node '" + pieces[2] + "'!");
        e.printStackTrace(System.err);
      }
    }
    else if ("add".equals(pieces[0])) {
      // add work string from pieces[1] to the path batch
      pathBatch.addPath(pieces[1]);
      
      // log it
      System.out.println(new Date() + ": Added work '" + pieces[1] + "' to " + getDescription() + " pathBatch.");
      
      result = "true";
    }
    else if ("report".equals(pieces[0])) {
      // generate a report string
      result = generateReport();
    }
    else {
      result = doOtherOperate(pieces);
    }

    return result;
  }
  
  /**
   * Method to override for handling other commands that add, get, and report (in pieces[0]).
   * <p>
   * Default behavior is to return a null response.
   *
   * @return the response string for the designated other operation.
   */
  protected String doOtherOperate(String[] pieces) {
    return null;
  }

  protected final String getNextWorkString(String requestingJobId, String requestingNodeId) throws IOException {
    final String result = doGetNextWorkString(requestingJobId, requestingNodeId);

    if (result != null && !WORK_IS_DONE_RESPONSE.equals(result)) {
      // record that this work is parceled out  workString|timeStamp(long)|requestingNode|timeStamp(String)
      final StringBuilder line = new StringBuilder();

      final long curTime = System.currentTimeMillis();
      line.append(result).append('|').append(curTime).append('|').append(requestingNodeId).append('|').append(new Date(curTime).toString());

      // log it
      final BufferedWriter outputLog = getOutputLog();
      synchronized (outputLog) {
        outputLog.write(line.toString());
        outputLog.newLine();
        outputLog.flush();
      }
    }

    return result;
  }
  
  protected final String doGetNextWorkString(String requestingJobId, String requestingNodeId) throws IOException {
    String result = null;

    if (pathBatch != null) {
      if (pathBatch.isComplete()) {
        result = WORK_IS_DONE_RESPONSE;

        System.out.println(new Date() + ": " + getDescription() + ": (" + requestingJobId + ") Work is Done! (request from '" + requestingNodeId + "')");
      }
      else {
        final String machine = BatchUtil.getMachineName(requestingNodeId);
        result = pathBatch.getNext(machine);
      }
    }

    return result;
  }

  
  private final String generateReport() {
    final StringBuilder report = new StringBuilder();

    report.append(getDescription()).append(" isComplete=").append(pathBatch.isComplete());
    if (!pathBatch.isComplete()) {
      report.
        append(" numMachinePaths=").append(pathBatch.getNumMachinePaths()).
        append(" numRemaining=").append(pathBatch.getRemainingEstimate());
    }

    return report.toString();
  }

  /**
   * Called by the handle method only if 'canHandle' returns true before
   * registering this job with the job manager. If this hook returns false,
   * this job will NOT be registered with the job manager and the postHandleHook
   * will not be called.
   */
  protected boolean doPreHandleHook(Context context) {
    this.pathBatch = loadPathBatch();
    this.totalWorkCount = pathBatch != null ? pathBatch.getRemainingEstimate() : 0;
    this.dest2count = new HashMap<String, Long>();

    return true;
  }  

  public synchronized void shutdown(boolean now) {  // called when cluster is being shutdown
    super.shutdown(now);

    boolean finished = pathBatch.isComplete();

    try {
      pathBatch.close();
    }
    catch (IOException e) {
      System.err.println(new Date() + ": Problem closing pathBatch '" + getDescription() + "'!");
      e.printStackTrace(System.err);
    }

    if (!finished) setStatus(JobStatus.INTERRUPTED);
  }
  
  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append(super.toString());

    if (getStartDate() != null) {
      result.append("  ").append(generateReport());
    }

    return result.toString();
  }

  public String dumpDetails() {
    final StringBuilder result = new StringBuilder();

    result.append(super.dumpDetails());
    result.append(pathBatch.dumpDetails());

    return result.toString();
  }
}
