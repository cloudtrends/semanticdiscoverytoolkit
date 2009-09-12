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
package org.sd.cio.mapreduce;


import java.io.File;

/**
 * A reducer that repeats execution until done.
 * <p>
 * @author Spence Koehler
 */
public abstract class RepeatingReducer<K, V, A, R> extends Reducer<K, V, A, R> {

  private File prevOutputFile;

  public RepeatingReducer() {
    super();
    this.prevOutputFile = null;
  }

  public void run() {
    boolean repeat = false;

    do {
      repeat = false;

      if (preReduceHook()) {
        super.run();

        if (postReduceHook() && needsReduce()) {
          this.prevOutputFile = getCurrentOutputFile();
          reset(getChainNum() + 1);
          repeat = true;
        }
      }
    } while (repeat);
  }

  /** Override current input dir to be prior phase's output dir. */
  protected final File getCurrentInputFile() {
    File result = null;

    if (prevOutputFile == null) {
      // defer to the normal input file (first time through)
      result = super.getCurrentInputFile();
    }
    else {
      // the 'current' input file will be the previous output file
      result = prevOutputFile;
    }

    return result;
  }

  /**
   * Perform any tasks before running reduce for the current chain number.
   * <p>
   * This is intended to be overridden by implementations that require
   * additional setup or control over the repeated reduce loop.
   *
   * @return true to execute the next reduce phase or false to terminate.
   */
  protected boolean preReduceHook() {
    return true;
  }

  /**
   * Perform any tasks immediately after running a reduce phase (before reset).
   * <p>
   * This is intended to be overridden by implementations that require
   * additional control over the repeated reduce loop.
   *
   * @return true to reset and run another reducer phase (if warranted) or false
   *         to terminate immediately.
   */
  protected boolean postReduceHook() {
    return true;
  }
}
