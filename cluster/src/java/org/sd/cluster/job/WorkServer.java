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

import org.sd.cluster.config.BooleanResponse;
import org.sd.cluster.config.ClusterException;
import org.sd.cluster.config.ClusterRunner;
import org.sd.cluster.config.Config;
import org.sd.cluster.config.Console;
import org.sd.cluster.config.StringResponse;
import org.sd.cluster.io.Context;
import org.sd.cluster.io.Response;
import org.sd.io.FileUtil;
import org.sd.io.Publishable;
import org.sd.util.MathUtil;
import org.sd.util.PropertiesParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A job for serving work to other jobs across a cluster.
 * <p>
 * @author Spence Koehler
 */
public abstract class WorkServer extends AbstractJob {

  public static final String WORK_IS_DONE_RESPONSE = "*** DONE ***";
  public static final String WORK_IS_WAITING_RESPONSE = "*** WAITING ***";

  private transient BufferedWriter outputLog;
  private transient Date startDate;
  private transient AtomicLong numSentUnits;

  public WorkServer() {
    super();
  }

  public WorkServer(Properties properties) {
    super(properties);
  }

  public String getDescription() {
    return getName() + "-" + getJobId();
  }

  protected final String getName() {
    final String[] namePieces = this.getClass().getName().split("\\.");
    return namePieces[namePieces.length - 1];
  }

  protected final BufferedWriter getOutputLog() {
    return outputLog;
  }

  protected final Date getStartDate() {
    return startDate;
  }

  protected final long incrementNumSentUnits() {
    return numSentUnits.incrementAndGet();
  }

  // create the next name of the form <number>-<filename>
  protected final File getNextNewOutputFile(File outputFile) {
    final File latestBackupFile = getLatestExistingOutputFile(outputFile);
    int nextInt = 1;
    if (latestBackupFile != null && latestBackupFile != outputFile) {
      nextInt = getBackupNumber(latestBackupFile) + 1;
    }
    final String newName = Integer.toString(nextInt) + "-" + outputFile.getName();
    final File outputDir = outputFile.getParentFile();

    return new File(outputDir, newName);
  }

  protected final File getLatestExistingOutputFile(File outputFile) {
    final File[] outputFiles = findBackupFiles(outputFile);
    File result = getLatestBackup(outputFiles);

    if (result == null && outputFile.exists()) {
      result = outputFile;
    }

    return result;
  }

  /**
   * Find files of the form <number>-<outputFileName>, not including the outputFileName alone.
   */
  protected final File[] findBackupFiles(File outputFile) {
    final File outputDir = outputFile.getParentFile();
    final String outName = outputFile.getName();

    final File[] outputFiles = outputDir.listFiles(new FileFilter() {
        public boolean accept(File file) {
          // find files whose names end with outputFile's name
          final String fileName = file.getName();
          return (fileName.endsWith(outName) && !outName.equals(fileName) && fileName.indexOf('-') > 0);
        }
      });

    return outputFiles;
  }

  protected final File getLatestBackup(File[] backupFiles) {
    int maxInt = -1;
    File result = null;

    // find the file with the greatest prefix value ("prefix-name")
    for (File file : backupFiles) {
      final int curInt = getBackupNumber(file);

      if (curInt > maxInt) {
        maxInt = curInt;
        result = file;
      }
    }

    return result;
  }

  // <backupNumber>-<name>
  protected final int getBackupNumber(File file) {
    final String fileName = file.getName();
    final int dashPos = fileName.indexOf('-');  // assume we always get a properly formatted backup file
    return dashPos >= 0 ? Integer.parseInt(fileName.substring(0, dashPos)) : -1;
  }

  public Response operate(Publishable request) {
    if (getStatus() != JobStatus.RUNNING) return null;

    final String requestString = request.toString(); //todo: don't assume its toString is the work.
    final String responseString = doOperate(requestString);

    // doOperate returns 'null' when work is finished. proper response is boolean:false
    return (responseString == null) ? new BooleanResponse(getClusterContext(), false) : new StringResponse(getClusterContext(), responseString);
  }
  
  /**
   * Given that this work server is running, generate a response string for
   * the request string representing a unit of work.
   */
  protected abstract String doOperate(String requestString);

  /**
   * Build the output filename for this work server's log.
   * <p>
   * Extenders may override. Default is to use the work server's class name
   * and jobId through the config's output data path.
   */
  protected String buildOutputFilename() {
    final Config config = getConfig();
    return config.getOutputDataPath(getName(), getJobId());
  }
  
  private final BufferedWriter openOutputLog() {
    BufferedWriter result = null;

    final String outputFilename = buildOutputFilename();
    final File outputFile = new File(outputFilename);

    if (!outputFile.exists()) {
      outputFile.getParentFile().mkdirs();
    }

    try {
      result = FileUtil.getWriter(outputFile, true);  // always append

      System.out.println(new Date() + ": Opened outputLog at '" + outputFile.getAbsolutePath() + "'. " + getDescription());
    }
    catch (IOException e) {
      System.err.println("*** Can't open output log file at '" + outputFile.getAbsolutePath() + "'!");
    }

    return result;
  }
  
  /**
   * Called by the handle method only if 'canHandle' returns true before
   * registering this job with the job manager. If this hook returns false,
   * this job will NOT be registered with the job manager and the postHandleHook
   * will not be called.
   */
  protected final boolean preHandleHook(Context context) {
    if (!super.preHandleHook(context)) return false;

    this.startDate = new Date();
    this.numSentUnits = new AtomicLong(0L);

    // execute extender's pre-handle hook which may rely on the existence
    // of the output log for backup/restart logic.
    doPreHandleHook(context);

    // now we can open the output log.
    this.outputLog = openOutputLog();

    return true;
  }  

  /**
   * Do the preHandleHook operations for the extended class.
   */
  protected abstract boolean doPreHandleHook(Context context);

  public void start() {  // start processing job's work.
    setStatus(JobStatus.RUNNING);
  }

  public void stop() {  // end processing job in this jvm; could still suspend? can't resume.
    setStatus(JobStatus.INTERRUPTED);
  }

  public void pause() {  // pause a job in this jvm to be resumed.
    setStatus(JobStatus.PAUSED);
  }

  public void resume() {  // resume paused job.
    setStatus(JobStatus.RUNNING);
  }

  public void suspend() {  // suspend to disk for restoration through JobManager in another jvm
    setStatus(JobStatus.PAUSED);
  }

  public synchronized void shutdown(boolean now) {  // called when cluster is being shutdown
    try {
      outputLog.flush();
      outputLog.close();
    }
    catch (IOException e) {
      System.err.println("Problem closing outputLog '" + getDescription() + "'!");
        e.printStackTrace(System.err);
    }

    setStatus(JobStatus.FINISHED);
  }
  
  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append(getDescription());

    if (startDate != null) {
      result.append(' ').append(" started:").append(dateFormat.format(startDate));

      final long upTime = new Date().getTime() - startDate.getTime();
      result.append("  upTime:").append(MathUtil.timeString(upTime, true));

      result.append("\n  jobStatus=").append(getStatus()).append("  numSentUnits:").append(numSentUnits);
    }

    return result.toString();
  }

  protected static final void run(PropertiesParser pp, WorkServer workServer) throws IOException, ClusterException {
    // start the work server on a node
    //

    Console console = null;
    String serverNodeName = null;

    // use properties
    console = new ClusterRunner(true/*useActiveCluster*/, pp.getProperties()).getConsole();
    serverNodeName = pp.getProperties().getProperty("groupName");

    if (console != null) {
      console.showResponses(System.out, console.sendJob(workServer, 5000));

      LocalJobId localJobId = null;

      int retryCount = 250;
      while (retryCount > 0) {
        localJobId = console.getLocalJobId(serverNodeName, workServer, 5000);

        if (localJobId.getId() < 0) {
          --retryCount;
          System.err.println("Couldn't get localJobId. Waiting (" + retryCount + " more retries.)");

          try {
            Thread.sleep(500);
          }
          catch (InterruptedException e) {
            //do nothing.
          }
        }
        else {
          retryCount = 0;
        }
      }

      System.out.println("*** " + workServer.getName() + " localJobId = " + localJobId);

      console.shutdown();
    }
  }
}
