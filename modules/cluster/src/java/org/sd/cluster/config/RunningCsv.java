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
package org.sd.cluster.config;


import org.sd.io.FileUtil;
import org.sd.io.FileLock;
import org.sd.util.ExecUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** 
 * Class to manage the running.csv file.
 * <p>
 * @author Spence Koehler
 */
public final class RunningCsv {

  public static final String RUNNING_CSV_PATH = ConfigUtil.getClusterPath("running/running.csv");
  private static final String LOCK_PATH = ConfigUtil.getClusterPath("running/running-csv-lock");
  private static final int LOCK_WAIT_TIMEOUT = 1000;

  private static final RunningCsv INSTANCE = new RunningCsv();

  public static final RunningCsv getInstance() {
    while (!INSTANCE.isInitialized()) {
      try {
        Thread.sleep(100);
      }
      catch (InterruptedException e) {
        break;
      }
    }
    return INSTANCE;
  }

  private FileLock<Integer> fileLock;
  private FileLock.LockOperation<Integer> lockOperation;
  private boolean initialized;

  private RunningCsv() {
    this.initialized = false;
    this.fileLock = new FileLock<Integer>(RUNNING_CSV_PATH, LOCK_WAIT_TIMEOUT);
    this.lockOperation = new FileLock.LockOperation<Integer>() {
        public Integer operate(String filename) throws IOException {
          int result = -1;

          // load file into memory
          final Map<String, Line> lines = readFile();

          // find active lines (jvmNum -> map.entry)
          final Map<Integer, Map.Entry<String, Line>> activeLines = getActiveLines(lines);

          // process
          Line newLine = null;
          final int numActiveLines = activeLines.size();
          for (int i = 0; i < numActiveLines; ++i) {
            final Map.Entry<String, Line> entry = activeLines.get(i);
            final Line line = entry.getValue();
            if (!line.isUp()) {
              // deactivate line... it's not up anymore
              lines.put(entry.getKey(), getInactiveLine(line));

              // this'll be this jvm's line
              newLine = getReactivatedLine(line);
              result = i;
              break;
            }
          }
          if (newLine == null) newLine = getNewLine(numActiveLines);
          lines.put(newLine.toString(), newLine);
          result = newLine.jvmNum;

          // write out the modified file
          writeRunningFile(lines);

          return result;
        }
      };
    this.initialized = true;
  }

  boolean isInitialized() {
    return initialized;
  }

  /**
   * Get this jvm's jvm id.
   * <p>
   * Note that this will either find this jvm's id in the running.csv file or add
   * this jvm's assigned id to the file.
   * <p>
   * The assigned id is the first jvm id that is not currently up or assigned.
   * <p>
   * In other words, if a jvm's process is found to have died, the jvm id will
   * be reclaimed by this jvm.
   *
   * @return an integer greater than or equal to 0 or -1 if there is a problem.
   */
  public int getCurrentJvmId() {
    return fileLock.operateWhileLocked(lockOperation);
  }

  private final Line getInactiveLine(Line line) {
    return new Line(line.jvmNum, line.processId, line.startTimeStamp, new Date().toString());
  }

  private final Line getReactivatedLine(Line line) {
    return new Line(line.jvmNum, ExecUtil.getProcessId(), new Date().toString(), null);
  }

  private final Line getNewLine(int jvmNum) {
    return new Line(jvmNum, ExecUtil.getProcessId(), new Date().toString(), null);
  }

  /**
   * Assuming we already have a lock on the file, read the file.
   */
  private final Map<String, Line> readFile() throws IOException {
    final Map<String, Line> result = new LinkedHashMap<String, Line>();

    final File file = new File(RUNNING_CSV_PATH);
    if (!file.exists()) return result;

    final BufferedReader reader = FileUtil.getReader(RUNNING_CSV_PATH);
    String fileline = null;
    while ((fileline = reader.readLine()) != null) {
      Line line = null;
      if (fileline.startsWith("#@")) {
        line = new Line(fileline.substring(2));
      }
      else if (!fileline.startsWith("#")) {
        line = new Line(fileline);
      }
      result.put(fileline, line);
    }
    reader.close();

    return result;
  }

  private final void writeRunningFile(Map<String, Line> lines) throws IOException {
    final BufferedWriter writer = FileUtil.getWriter(RUNNING_CSV_PATH);
    for (Map.Entry<String, Line> entry : lines.entrySet()) {
      final Line line = entry.getValue();
      String outLine = (line == null) ? entry.getKey() : line.toString();
      writer.write(outLine);
      writer.newLine();
    }
    writer.close();
  }

  private final Map<Integer, Map.Entry<String, Line>> getActiveLines(Map<String, Line> lines) {
    final Map<Integer, Map.Entry<String, Line>> result = new HashMap<Integer, Map.Entry<String, Line>>();

    for (Map.Entry<String, Line> entry : lines.entrySet()) {
      final Line line = entry.getValue();
      if (line != null && line.isActive()) {
        result.put(line.jvmNum, entry);
      }
    }

    return result;
  }

  public final class Line {
    public final int jvmNum;
    public final int processId;
    public final int serverPort;         // port accepting client connections
    public final String startTimeStamp;
    public final String endTimeStamp;

    Line(String fileLine) {
      final String[] pieces = fileLine.split(",");
      this.jvmNum = Integer.parseInt(pieces[0]);
      this.processId = Integer.parseInt(pieces[1]);
      this.serverPort = Integer.parseInt(pieces[2]);
      this.startTimeStamp = pieces[3];
      this.endTimeStamp = (pieces.length > 4) ? pieces[4] : null;
    }

    public Line(int jvmNum, int processId, String startTimeStamp, String endTimeStamp) {
      this.jvmNum = jvmNum;
      this.processId = processId;
      this.serverPort = ConfigUtil.getServerPort(jvmNum);
      this.startTimeStamp = startTimeStamp;
      this.endTimeStamp = endTimeStamp;
    }

    public boolean isActive() {
      // line is active if it doesn't have an endTimeStamp.
      final boolean hasEndTimeStamp = (endTimeStamp != null && endTimeStamp.length() > 0);
      return !hasEndTimeStamp;
    }

    public boolean isUp() {
      return ExecUtil.isUp(processId);
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();
      final boolean isActive = isActive();

      if (!isActive) {
        result.append("#@");
      }
      result.append(jvmNum).append(',').
        append(processId).append(',').
        append(serverPort).append(',').
        append(startTimeStamp);
      if (!isActive) {
        result.append(',').append(endTimeStamp);
      }
        
      return result.toString();
    }
  }
}
