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
package org.sd.cluster.util;


import org.sd.cluster.config.ConfigUtil;
import org.sd.io.FileUtil;
import org.sd.util.DateUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for managing cluster/log files.
 * <p>
 * @author Spence Koehler
 */
public class LogManager {
  
  private static final int MAX_MILLIS_BETWEEN_NODE_STARTS = 15000;  // 15 seconds

  private static final Pattern LOG_FILE_PATTERN = Pattern.compile("^log-(.+)-(\\d+)\\.(err|out).*$");


  private LogInfo[] logs;
  private LogInfo[] latestLogs;

  public LogManager() {
    final File logDir = new File(ConfigUtil.getClusterRootDir() + "/log");
    final File[] logFiles = logDir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          final Matcher m = LOG_FILE_PATTERN.matcher(name);
          return m.matches();
        }
      });

    this.logs = toLogInfos(logFiles);
    this.latestLogs = null;  // lazy load
  }

  protected LogManager(File[] logFiles) {
    this.logs = toLogInfos(logFiles);
    this.latestLogs = null;  // lazy load
  }

  public final LogInfo[] getLatestLogs() {
    if (latestLogs == null) {
      this.latestLogs = getLatestLogs(logs);
    }
    return latestLogs;
  }

  private static final LogInfo[] toLogInfos(File[] logFiles) {
    final LogInfo[] result = new LogInfo[logFiles.length];
    for (int i = 0; i < logFiles.length; ++i) {
      result[i] = new LogInfo(logFiles[i]);
    }
    Arrays.sort(result);
    return result;
  }

  private static final LogInfo[] getLatestLogs(LogInfo[] logs) {
    final List<LogInfo> result = new ArrayList<LogInfo>();
    final Set<Integer> seenNodeNums = new HashSet<Integer>();

    // NOTE: This isn't going to work properly if one of multiple nodes
    //       for a machine is restarted while the other node keeps going.

    LogInfo lastLog = null;
    for (LogInfo log : logs) {
      boolean keepLog = true;
      if (lastLog != null) {
        if (log.timeMillis != lastLog.timeMillis) {
          if (seenNodeNums.contains(log.nodeNum)) {
            keepLog = false;
          }
          else if ((lastLog.timeMillis - log.timeMillis) > MAX_MILLIS_BETWEEN_NODE_STARTS) {
            // this is likely a log from a prior run, probably with a config that used more nodes
            keepLog = false;
          }
        }
      }

      if (keepLog) {
        result.add(log);
        seenNodeNums.add(log.nodeNum);
        lastLog = log;
      }
      else {
        break;
      }
    }

    return result.toArray(new LogInfo[result.size()]);
  }


  public static interface LogVisitor {

    /**
     * Reset the visitor before visiting each log line.
     *
     * @return true to continue with visiting log lines; false to abort visit.
     */
    public boolean reset(LogInfo visitingLogInfo, int nextLineToVisit);

    /**
     * Visit the log line.
     *
     * @return true to continue visiting logLines; false to stop visiting.
     */
    public boolean visit(LogInfo visitingLogInfo, String logLine, int lineNum);

    /**
     * Flush the visitor after visiting all log lines.
     *
     * @return true if an actionable event occurred while visiting.
     */
    public boolean flush(LogInfo visitingLogInfo);
  }

  public static final class LogInfo implements Comparable<LogInfo> {
    public final File logFile;
    public final int nodeNum;
    public final String timeString;
    public final long timeMillis;
    public final boolean isErrLog;  // otherwise, is "out" log.

    private int nextLineToVisit;

    public LogInfo(File logFile) {
      this.logFile = logFile;

      final Matcher m = LOG_FILE_PATTERN.matcher(logFile.getName());
      if (!m.matches()) {
        throw new IllegalArgumentException("File '" + logFile + "' is not a log file!");
      }

      this.nodeNum = Integer.parseInt(m.group(2));
      this.timeString = m.group(1);
      this.timeMillis = DateUtil.parseDate(timeString);
      this.isErrLog = "err".equals(m.group(3));

      this.nextLineToVisit = 0;
    }

    public int getNextLineToVisit() {
      return nextLineToVisit;
    }

    public void setNextLineToVisit(int nextLineToVisit) {
      this.nextLineToVisit = nextLineToVisit;
    }

    /**
     * sorts in reverse timestamp order, in node-num order, with .out before .err
     */
    public int compareTo(LogInfo other) {

      int result = this.timeMillis > other.timeMillis ? -1 : this.timeMillis < other.timeMillis ? 1 : 0;

      if (result == 0) {
        result = (this.nodeNum - other.nodeNum);

        if (result == 0) {
          if (this.isErrLog != other.isErrLog) {
            result = this.isErrLog ? 1 : -1;
          }
        }
      }
      
      return result;
    }

    /**
     * Visit this log and its lines using the given visitor.
     *
     * @return true if an actionable event occurred while visiting.
     */
    public boolean visit(LogVisitor logVisitor) throws IOException {
      boolean result = false;

      if (logVisitor.reset(this, nextLineToVisit)) {
        final BufferedReader reader = FileUtil.getReader(logFile);

        int lineNum = 0;
        String line = null;

        while ((line = reader.readLine()) != null) {
          final boolean shouldVisit = (lineNum >= nextLineToVisit);

          ++lineNum;  // next line's number

          if (shouldVisit) {
            if (!logVisitor.visit(this, line, lineNum)) break;
          }
        }

        // record the lines we've visited
        nextLineToVisit = lineNum;

        reader.close();

        result = logVisitor.flush(this);
      }

      return result;
    }
  }


  public static final void main(String[] args) {
    final LogManager logManager = new LogManager();
    final LogInfo[] logs = logManager.getLatestLogs();

    for (int i = 0; i < logs.length; ++i) {
      final LogInfo log = logs[i];
      System.out.println(i + ": (n=" + log.nodeNum + ",t=" + log.timeMillis + ") " + log.logFile);
    }
  }
}
