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
package org.sd.cluster.job.work;


import org.sd.cluster.config.ClusterContext;
import org.sd.cluster.config.Config;
import org.sd.io.FileUtil;
import org.sd.util.MathUtil;
import org.sd.util.PropertiesParser;
import org.sd.util.RollingStore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for managing logging units of work and tracking rates, etc.
 * <p>
 * @author Spence Koehler
 */
public class LogWrapper extends RollingStore<String> {

  private File logDir;           // directory containing log file(s)
  private String logNamePrefix;  // X for X\d+Y
  private String logNameSuffix;  // Y for X\d+Y
  private Pattern namePattern;
  private List<File> logFiles;

  private String[] lastLogLines;
  private String logName;
  private long startTime;
  private Long lastSuccessTime;
  private Long lastFailTime;
  private Long endTime;
  private AtomicLong succeeded;
  private AtomicLong total;

  private final AtomicBoolean closed = new AtomicBoolean(false);

  public LogWrapper(ClusterContext clusterContext, String jobIdString, String dataDirName, String logName, boolean storeLastLines) {
    this(clusterContext.getConfig(), jobIdString, dataDirName, logName, storeLastLines);
  }

  public LogWrapper(ClusterContext clusterContext, String jobIdString, String dataDirParent, String dataDirName, String logName, boolean storeLastLines) {
    this(clusterContext.getConfig(), jobIdString, dataDirParent, dataDirName, logName, storeLastLines);
  }

  public LogWrapper(Config config, String jobIdString, String dataDirName, String logName, boolean storeLastLines) {
    this(config, jobIdString, null, dataDirName, logName, storeLastLines);
  }

  public LogWrapper(Config config, String jobIdString, String dataDirParent, String dataDirName, String logName, boolean storeLastLines) {
    super(null, true, false, true);

    final File firstLogFile = getFirstLogFile(config, jobIdString, dataDirParent, dataDirName, logName);
    this.logDir = firstLogFile.getParentFile();
    this.logName = logName;

//    System.out.println(new Date() + ": LogWrapper.init(" + firstLogFile + ")");

    final int logLen = logName.length();

    int lastDotPos = logLen;
    if (logName.endsWith(".gz")) {
      lastDotPos = logName.lastIndexOf('.', logLen - 4);
      if (lastDotPos < 0) lastDotPos = logLen - 3;
    }
    else {
      lastDotPos = logName.lastIndexOf('.');
      if (lastDotPos < 0) lastDotPos = logLen;
    }

    this.logNamePrefix = lastDotPos >= 0 ? logName.substring(0, lastDotPos) : logName;
    this.logNameSuffix = lastDotPos < 0 || lastDotPos >= logLen ? "" : logName.substring(lastDotPos);
    this.namePattern = Pattern.compile("^" + logNamePrefix + "-(\\d+)" + logNameSuffix + "(.*)$");
    this.logFiles = getLogFiles(logDir, logNamePrefix, logNameSuffix, namePattern);
    this.lastLogLines = storeLastLines ? retrieveLastLogLines(logFiles) : null;

    this.succeeded = new AtomicLong(0L);
    this.total = new AtomicLong(0L);

    initialize(null);
  }

  /**
   * Get the next available store file as the last element of the list (and all
   * existing store files as prior elements in the list).
   */
  protected List<File> nextAvailableFile() {
    File result = null;

    final File lastLogFile = getLastLogFile();
    int nextNum = 0;

    if (lastLogFile != null) {
      nextNum = getLogNumber(lastLogFile, namePattern) + 1;
    }

    result = new File(logDir, logNamePrefix + "-" + nextNum + logNameSuffix);
    logFiles.add(result);

    return logFiles;
  }

  /**
   * Get the root location of this store's elements
   */
  public File getStoreRoot() {
    return logDir;
  }

  /**
   * Build an instance of a store at the given location.
   */
  protected Store<String> buildStore(File storeFile) {
    return new LogStore(storeFile);
  }

  /**
   * Hooked called while (after) opening.
   */
  protected void afterOpenHook(boolean firstTime, List<File> nextAvailable, File justOpenedStore) {
    // Here we'll reset all the stats and get rates for each log instead of cumulative across logs.
    // If instead we do want cumulative stats, then initialize startTime elsewhere and do nothing here.

    // start/reset the clock now.
    this.startTime = System.currentTimeMillis();

    // reset the totals.
    this.total.set(0L);
    this.succeeded.set(0L);
  }


  /**
   * Get the last few log lines present before this log was opened.
   */
  public String[] getLastLogLines() {
    return lastLogLines;
  }

  /**
   * Build the output filename for this work server's log.
   * <p>
   * Extenders may override. Default is to use the work server's class name
   * and jobId through the config's output data path.
   */
  public static final String buildOutputFilename(ClusterContext clusterContext, String jobIdString, String dataDirName, String logName) {
    return buildOutputFilename(clusterContext.getConfig(), jobIdString, dataDirName, logName);
  }

  public static final String buildOutputFilename(ClusterContext clusterContext, String jobIdString, String dataDirParent, String dataDirName, String logName) {
    return buildOutputFilename(clusterContext.getConfig(), jobIdString, dataDirParent, dataDirName, logName);
  }
  
  /**
   * Build the output filename for this work server's log.
   * <p>
   * Extenders may override. Default is to use the work server's class name
   * and jobId through the config's output data path.
   */
  public static final String buildOutputFilename(Config config, String jobIdString, String dataDirName, String logName) {
    return config.getOutputDataPath(jobIdString, dataDirName) + "/" + logName;
  }
  
  public static final String buildOutputFilename(Config config, String jobIdString, String dataDirParent, String dataDirName, String logName) {
    return config.getOutputDataPath(jobIdString, dataDirParent, dataDirName, config.getNodeName()) + "/" + logName;
  }

  /**
   * Get the first file.
   */
  private final File getFirstLogFile(Config config, String jobIdString, String dataDirParent, String dataDirName, String logName) {
    String outputFilename = null;

    if (dataDirParent == null){
      outputFilename = buildOutputFilename(config, jobIdString, dataDirName, logName);
    }
    else {
      outputFilename = buildOutputFilename(config, jobIdString, dataDirParent, dataDirName, logName);
    }
      
    return new File(outputFilename);
  }

  /**
   * Get the log number for the file (if it has one) or -1.
   */
  private final int getLogNumber(final File logFile, final Pattern p) {
    int result = -1;

    final Matcher m = p.matcher(logFile.getName());
    if (m.matches()) {
      result = Integer.parseInt(m.group(1));
    }

    return result;
  }

  private final List<File> getLogFiles(File logDir, String logNamePrefix, String logNameSuffix, final Pattern p) {
    final List<File> result = new ArrayList<File>();

    if (!logDir.exists()) {
      logDir.mkdirs();
    }
    else {
      final File[] files = logDir.listFiles(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return p.matcher(name).matches();
          }
        });
      Arrays.sort(files, new Comparator<File>() {
          public int compare(File f1, File f2) {
            final int i1 = getLogNumber(f1, p);
            final int i2 = getLogNumber(f2, p);
            return i1 - i2;
          }
          public boolean equals(Object o) {
            return this == o;
          }
        });
      for (File file : files) result.add(file);
    }

    return result;
  }
  
  /**
   * Get the last existing log file, or null if none exist.
   */
  private final File getLastLogFile() {
    File result = null;

    if (this.logFiles.size() > 0) {
      result = logFiles.get(logFiles.size() - 1);
    }

    return result;
  }

  private final String[] retrieveLastLogLines(List<File> logFiles) {
    String[] result = null;

    for (int i = logFiles.size() - 1; i >= 0; --i) {
      final File lastLogFile = logFiles.get(i);
      if (lastLogFile != null) {
        result = FileUtil.getLastLines(lastLogFile);
        if (result != null && result.length > 0) break;
      }
    }

    return result;
  }

  public void addStat(boolean success) {
    this.total.incrementAndGet();
    if (success) {
      this.succeeded.incrementAndGet();
      this.lastSuccessTime = System.currentTimeMillis();
    }
    else {
      this.lastFailTime = System.currentTimeMillis();
    }
  }

  public void writeLine(String line, boolean addTimeStamp, boolean success) {
    if (addTimeStamp) {
      line = new Date() + ":" + line;
    }

    try {
      addElement(line);
    }
    catch (IOException e) {
      System.err.println(new Date() + ": LogWrapper(" + getLastLogFile() + ") : WARNING : Trouble writing to log!: " + line);
      e.printStackTrace(System.err);
    }

    addStat(success);
  }

  public String getRatesString() {
    final StringBuilder result = new StringBuilder();

    final long curTime = (endTime == null) ? System.currentTimeMillis() : endTime;
    final long delta = curTime - startTime;  // millis
    final long count = total.get();
    final long correct = succeeded.get();
    final double rate = ((double)count * 1000) / ((double)delta);          // units/sec
    final double successRate = ((double)correct * 100) / ((double)count);  // percent

    // logName: N in T, R units/sec, S(P%) success.
    result.
      append(logName).append(": ").
      append(total.get()).append(" in ").
      append(MathUtil.timeString(delta, false)).append(", ").
      append(MathUtil.doubleString(rate, 3)).append(" u/s, ").
      append(correct).append("(").
      append(MathUtil.doubleString(successRate, 3)).append("%) success. ");

    if (endTime != null) {
      result.append("DONE");
    }
    else {
      result.append("ACTIVE");

      if (lastSuccessTime != null || lastFailTime != null) {
        result.append(" <");
        if (lastSuccessTime != null) {
          result.append(MathUtil.timeString(curTime - lastSuccessTime, false));
        }
        result.append(',');
        if (lastFailTime != null) {
          result.append(MathUtil.timeString(curTime - lastFailTime, false));
        }
        result.append('>');
      }
    }

    return result.toString();
  }

  public void close() {
    super.close();
    this.endTime = System.currentTimeMillis();
  }


  public static final class LogStore implements Store<String> {
    private File outputFile;
    private BufferedWriter outputLog;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    public LogStore(File outputFile) {
      this.outputFile = outputFile;
    }

    public void open() throws IOException {
//      System.out.println(new Date() + ": LogWrapper.LogStore.open(" + outputFile + ")");
      this.outputLog = FileUtil.getWriter(outputFile, true);  // always append
    }

    public File getDirPath() {
      return outputFile;
    }

    public void close(boolean closingInThread) throws IOException {
      if (closed.compareAndSet(false, true)) {
        if (outputLog != null) {
          outputLog.flush();
          outputLog.close();
        }

        // compress the log
        if (!outputFile.getName().endsWith(".gz") && outputFile.length() > 0) {
          final File compressedFile = new File(outputFile.getParentFile(), outputFile.getName() + ".gz");
          if (!compressedFile.exists()) {

            System.out.println(new Date() + ": LogWrapper compressing closed store '" + outputFile + "' as '" + compressedFile + "'.");

            boolean done = false;
            try {
              final BufferedWriter writer = FileUtil.getWriter(compressedFile);
              final BufferedReader reader = FileUtil.getReader(outputFile);
              String line = null;
              while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
              }
              reader.close();
              writer.close();
              done = true;
            }
            catch (IOException e) {
              System.err.println(new Date() + ": LogWrapper.compress(" + outputFile + "," + compressedFile + ") : ERROR : Trouble compressing!");
              e.printStackTrace(System.err);
            }

            if (done) outputFile.delete();
          }
        }
      }
    }

    public boolean isClosed() {
      return closed.get();
    }

    public boolean addElement(String line) throws IOException {
      this.outputLog.write(line);
      outputLog.newLine();
      outputLog.flush();
      return false;
    }

    public boolean shouldResume() {
      // always start a new log on restarts, unless this log is empty
      return outputFile.length() == 0;
    }

    public boolean shouldRoll() {
      // never roll without being told to do so.
      return false;
    }
  }


  public static final void main(String[] args) throws IOException {
    // properties
    //
    // jvmNum -- (optional, default=0) the jvmNum of the log to access
    // jobId -- (required) the jobId
    // dataDirName -- (required) the dataDirName
    // logName -- (required) the log name (i.e. "server.log")
    //
    final PropertiesParser propertiesParser = new PropertiesParser(args);
    final Properties properties = propertiesParser.getProperties();

    final int jvmNum = Integer.parseInt(properties.getProperty("jvmNum", "0"));
    final String jobId = properties.getProperty("jobId");
    final String dataDirName = properties.getProperty("dataDirName");
    final String logName = properties.getProperty("logName");

    final Config config = Config.makeTestConfig(jvmNum);
    final LogWrapper logWrapper = new LogWrapper(config, jobId, dataDirName, logName, true);
    final String[] lastLogLines = logWrapper.getLastLogLines();

    System.out.println("Got lastLogLines=" + lastLogLines);
    for (String line : lastLogLines) {
      System.out.println(line);
    }
  }
}
