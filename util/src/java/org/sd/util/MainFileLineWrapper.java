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
package org.sd.util;


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A main wrapper that operates on each line of a file.
 * <p>
 * @author Spence Koehler
 */
public abstract class MainFileLineWrapper extends MainWrapper {
  
  private String description;
  private String filePath;

  /**
   * Operate on the line in the file.
   *
   * @param inputLine   The line from the file to operate on.
   * @param die         Boolean for impl to monitor for kill processing signal.
   *
   * @return true if the line was successfully operated on; false if skipped.
   */
  protected abstract boolean operate(String inputLine, AtomicBoolean die);

  /**
   * Construct an instance.
   *
   * @param description  A description of this instance.
   * @param filePath     The path to the file whose lines will be operated on.
   * @param timeLimit    A limit on the length of time to run in seconds. 0 for unlimited.
   */
  protected MainFileLineWrapper(String description, String filePath, long timeLimit) {
    super(timeLimit);

    this.description = description;
    this.filePath = filePath;
  }

  protected String getDescription() {
    return description + " over lines in '" + filePath + "'...";
  }

  protected boolean doRun(PrintStream verbose, AtomicInteger numOperations, long startTime, AtomicBoolean die) throws IOException {
    if (!new File(filePath).exists()) {
      if (verbose != null) {
        verbose.println("ERROR! can't find file '" + filePath + "'!");
        return false;
      }
    }

    final BufferedReader reader = FileUtil.getReader(filePath);
    String line = null;
    boolean result = true;
    boolean increment = true;

    while (result && (line = reader.readLine()) != null) {
      try {
        increment = operate(line, die);
      }
      catch (Throwable t) {
        final PrintStream out = (verbose == null) ? System.err : verbose;
        out.println("!ERROR!: line='" + line + "'");
        t.printStackTrace(out);
        result = false;
        increment = false;
      }

      if (die != null && die.get()) return false;

      // increment stats
      if (increment) {
        result = increment(verbose, numOperations, startTime);
      }
    }
    
    return result;
  }
}
