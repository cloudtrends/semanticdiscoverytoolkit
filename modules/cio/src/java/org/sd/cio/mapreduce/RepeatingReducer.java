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

import org.sd.io.DirectorySelector;
import org.sd.util.MathUtil;

/**
 * A reducer that repeats execution until done.
 * <p>
 * @author Spence Koehler
 */
public abstract class RepeatingReducer<K, V, A, R> extends Reducer<K, V, A, R> {

  /**
   * Given a file (or directory), determine its action key.
   * 
   * @return the file's (or directory's) action key or null if not applicable.
   */
  protected abstract A getActionKey(File file);


  private File prevOutputFile;
  private FlushActionFactory<K, V, A> priorFlushActionFactory;

  public RepeatingReducer(MapReduceBase<K, V, A, R> priorPhase) {
    super();
    this.prevOutputFile = null;
    this.priorFlushActionFactory = priorPhase.getFlushActionFactory();
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

  public void reset(int nextChainNumber) {
    this.priorFlushActionFactory = getFlushActionFactory();  // will be reset by next run
    super.reset(nextChainNumber);
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

  /** Override current output dir with a chain-based directory under the root. */
  protected File getCurrentOutputFile() {
    return new File(getRootOutputFile(), "phase-" + MathUtil.integerString(getChainNum(), 2, '0'));
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

  /**
   * Apply directory selection criterea to the given directory.
   */
  protected DirectorySelector.Action selectDirectory(File dir) {
    return DirectorySelector.Action.DESCEND;
  }

  /**
   * Apply file selection criterea to the given file.
   */
  protected boolean selectFile(File file) {
    boolean result = false;

    final A actionKey = getActionKey(file);
    if (actionKey != null) {
      FlushAction flushAction = priorFlushActionFactory.getFlushAction(actionKey);
      if (flushAction == null) {
        // force build
        flushAction = priorFlushActionFactory.getFlushAction(new MapperPair<K, V, A>(null, null) {
            public A getActionKey() { return actionKey; }
          });
      }
      result = flushAction.getNameGenerator().isValidName(file.getName());
    }

    return result;
  }
}
