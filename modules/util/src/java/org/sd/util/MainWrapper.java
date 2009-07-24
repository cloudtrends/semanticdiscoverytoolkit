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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A wrapper for main executables that operate recursively over directories.
 * <p>
 * @author Spence Koehler
 */
public abstract class MainWrapper {
  
  private long timeLimit;     // time limit in seconds, <= 0 ==> unlimited

  /**
   * Default constructor with no time limit.
   */
  protected MainWrapper() {
    this(0L);  // no time limit.
  }

  /**
   * Construct with the given timeLimit.
   *
   * @param timeLimit  A limit on the length of time to run in seconds. 0 for unlimited.
   */
  protected MainWrapper(long timeLimit) {
    this.timeLimit = timeLimit * 1000;  // convert seconds to millis
  }

  /**
   * A hook for doing something before operating.
   * <p>
   * For example, opening a log or output file.
   *
   * @return false to halt processing; true to continue.
   */
  protected boolean preRunHook() {
    return true;
  }

  /**
   * A hook for doing something after operating.
   * <p>
   * For example, closing a log or output file.
   *
   * @return false if failed; true if ok.
   */
  protected boolean postRunHook() {
    return true;
  }

  protected final void reportRate(PrintStream out, AtomicInteger numOperations, long time) {
    out.println("Did " + numOperations + " operations in " + (time / 1000) +
                " sec. rate=" + (((double)numOperations.get() / (double)time) * 1000.0) +
                " operations/sec");
  }

  protected final boolean increment(PrintStream out, AtomicInteger numOperations, long startTime) {
    final int num = numOperations.incrementAndGet();
    boolean result = true;

    if (out != null && ((num % 100) == 0)) {
      final long time = System.currentTimeMillis() - startTime;
      reportRate(out, numOperations, time);
      if (timeLimit > 0 && time > timeLimit) result = false;
    }

    return result;
  }

  // get a description of the type of run defined by the implementing class.
  protected abstract String getDescription();

  // do the type of run defined by the implementing class, calling "increment" after each operation.
  protected abstract boolean doRun(PrintStream verbose, AtomicInteger numOperations, long startTime, AtomicBoolean die) throws IOException;

  public boolean run(PrintStream verbose, AtomicBoolean die) throws IOException {
    if (verbose != null) {
      verbose.println("Running " + getDescription());
      if (timeLimit <= 0) {
        verbose.println("\tno timelimit.");
      }
      else {
        verbose.println("\ttimelimit=" + (timeLimit / 1000) + " seconds.");
      }
      verbose.println("Started at " + new Date());
    }

    if (!preRunHook()) return false;

    final AtomicInteger numOperations = new AtomicInteger(0);
    boolean finished = false;
    long time = 0L;
    
    try {
      final long startTime = System.currentTimeMillis();
      finished = doRun(verbose, numOperations, startTime, die);
      time = System.currentTimeMillis() - startTime;
    }
    finally {
      if (!postRunHook()) finished = false;
    }

    if (verbose != null) {
      verbose.print("Status: ");
      if (finished) verbose.print("FINISHED");
      else verbose.print("QUIT");
      verbose.println();

      reportRate(verbose, numOperations, time);
      verbose.println("Finished at " + new Date());
    }

    return finished;
  }
}
