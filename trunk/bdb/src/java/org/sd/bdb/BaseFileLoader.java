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
package org.sd.bdb;


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * A base loader for loading a file line-by-line.
 * <p>
 * @author Spence Koehler
 */
public abstract class BaseFileLoader implements DbMap.ResumableLoader {

  /**
   * Determine whether the line has been loaded into the map.
   */
  protected abstract boolean lineHasBeenLoaded(DbMap dbMap, String line, int lineNum);

  /**
   * Load the line from the file.
   */
  protected abstract boolean doLoadLine(DbMap dbMap, String line, int lineNum);


  private Integer verboseCount;
  private File file;
  private int numLoadedLines;

  protected BaseFileLoader(Integer verboseCount, File file) {
    this.verboseCount = verboseCount;
    this.file = file;
    this.numLoadedLines = 0;
  }

  /**
   * Load the file line by line, ignoring blank lines and those starting
   * with a pound (#) sign.
   * <p>
   * NOTE: IOExceptions are wrapped and rethrown as IllegalStateExceptions.
   *
   * @return true if successfully loaded.
   */
  public boolean load(DbMap dbMap) {
    boolean result = true;

    try {
      final BufferedReader reader = FileUtil.getReader(file);

      if (verboseCount != null) {
        System.err.println(new Date() + ": starting load of '" + file + "'");
      }

      // load entries
      result = doLoad(reader, dbMap);

      reader.close();

      if (verboseCount != null) {
        System.err.println(new Date() + ": loaded " + numLoadedLines + " lines. (TOTAL)");
      }
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return result;
  }

  public boolean resumeLoad(DbMap dbMap) {
    boolean result = true;

    try {
      final BufferedReader reader = FileUtil.getReader(file);

      if (verboseCount != null) {
        System.err.println(new Date() + ": starting load of '" + file + "'");
      }

      // load entries
      result = spinToStart(reader, dbMap);
      if (result) {
        result = doLoad(reader, dbMap);
      }

      reader.close();

      if (verboseCount != null) {
        System.err.println(new Date() + ": loaded " + numLoadedLines + " lines. (TOTAL)");
      }
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return result;
  }

  private final boolean spinToStart(BufferedReader reader, DbMap dbMap) throws IOException {
    boolean result = false;

    String line = null;
    while ((line = reader.readLine()) != null) {
      if (line.length() > 0 && line.charAt(0) != '#') {
        if (!lineHasBeenLoaded(dbMap, line, numLoadedLines++)) {
          result = true;
          break;
        }

        if (verboseCount != null && (numLoadedLines % verboseCount) == 0) {
          System.err.println(new Date() + ": skipped " + numLoadedLines + " lines.");
        }
      }
    }

    return result;
  }

  private final boolean doLoad(BufferedReader reader, DbMap dbMap) throws IOException {
    boolean result = false;

    String line = null;
    while ((line = reader.readLine()) != null) {
      if (line.length() > 0 && line.charAt(0) != '#') {
        result = doLoadLine(dbMap, line, numLoadedLines++);
        if (!result) break;

        if (verboseCount != null && (numLoadedLines % verboseCount) == 0) {
          System.err.println(new Date() + ": loaded " + numLoadedLines + " lines.");
        }
      }
    }

    return result;
  }

  public int getNumLoadedLines() {
    return numLoadedLines;
  }
}
