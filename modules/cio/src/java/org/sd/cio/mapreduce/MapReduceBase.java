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
import java.io.IOException;

import org.sd.io.DirectorySelector;
import org.sd.io.FileSelector;
import org.sd.util.thread.Governable;
import org.sd.util.thread.UnitCounter;

/**
 * Base map and reduce process definition utility.
 * <p>
 * This class serves as a container for common functionality across both map
 * and reduce processes. Consumers should extend either the Mapper or Reducer
 * instead of this base class.
 * <p>
 * Extenders of Mapper or Reducer must implement the following methods from
 * this base class:
 * <ul>
 * <li>For specifying the input:</li>
 *   <ul>
 *     <li>getRootInputFile() -- identify the top directory containing input.</li>
 *     <li>selectDirectory() -- identify actions to take with each input directory
 *                              encountered.</li>
 *     <li>selectFile() -- identify input files to operate on.</li>
 *   </ul>
 * <li>For specifying the output:</li>
 *   <ul>
 *     <li>buildFlushActionFactory -- specify output naming and flushing.</li>
 *   </ul>
 * <li>NOTE: implementation of Mapper and Reducer abstract methods will allow
 *           specification of the operations to perform on the data including
 *           details of handling input and output formats.</li>
 * </ul>
 *
 * @author Spence Koehler
 */
public abstract class MapReduceBase<K, V, A, R> implements Governable {

//K = type of mapped key
//V = type of mapped value
//A = type of action key (for retrieving appropriate FlushAction)
//R = type of input records (i.e. String)


  /**
   * Get the root input file (directory) from which directory walking will
   * proceed.
   */
  protected abstract File getRootInputFile();

  /**
   * Get the root output file (directory) to which flushed output directories
   * and/or files will be flushed.
   * <p>
   * NOTE: It is the output from this method that will be fed as input to
   *       buildFlushActionFactory.
   */
  protected abstract File getRootOutputFile();

  /**
   * Apply directory selection criterea to the given directory.
   */
  protected abstract DirectorySelector.Action selectDirectory(File dir);

  /**
   * Apply file selection criterea to the given file.
   */
  protected abstract boolean selectFile(File file);

  /**
   * Build the flush action factory to provide for capturing processing output.
   * <p>
   * NOTE: A typical implementation of this will usually require a custom
   *       class that extends AbstractFlushActionFactory and maintains data
   *       structures for each flush action that can be communicated across
   *       methods. In particular the mapper's "operate" method will likely
   *       need access to these data structures.
   *
   * @param outDir  The root output directory under which to build flush
   *                directories and/or files. The directory that is sent into
   *                this method invocation during initialize will be the result
   *                from getRootOutputFile.
   */
  protected abstract FlushActionFactory<K, V, A> buildFlushActionFactory(File outDir);

  
  /**
   * Build the appropriate DirWalker instance for performing the map or reduce
   * task.
   */
  protected abstract DirWalker buildWalker();


  private UnitCounter uc;
  private FlushActionFactory<K, V, A> faf;
  private DirWalker walker;

  /**
   * Default constructor.
   */
  protected MapReduceBase() {
    this.uc = new UnitCounter();
  }

  /**
   * Get this instance's unit counter.
   */
  public UnitCounter getUnitCounter() {
    return uc;
  }

  /**
   * Setter to override the default unit counter.
   */
  public void setUnitCounter(UnitCounter uc) {
    this.uc = uc;
  }

  /**
   * Accessor for flush action factory instance.
   * <p>
   * NOTE: This instance is not available until during walker.run(), which
   *       is after preRunHook has been invoked.
   */
  protected final FlushActionFactory<K, V, A> getFlushActionFactory() {
    if (faf == null) initialize();
    return faf;
  }

  /**
   * Run the DirWalker.
   * <p>
   * If the preRunHook is successful, initialization will be invoked and the
   * walker will be run; after which, the flush action factory will be
   * finalized and the postRunHook will be invoked.
   * <p>
   * If an IOException occurs during the processing, it will be rethrown
   * as an IllegalStateException.
   */
  public void run() {
    if (preRunHook()) {
      initialize();
      walker.run();

      try {
        faf.finalize();
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }

      postRunHook();
    }
  }

  /**
   * Perform any pre-initialization operations.
   * <p>
   * This is intended to be overridden by implementations that require
   * additional setup before initialization and running.
   * <p>
   * @return true to continue with initialization and running or false
   *         to abort execution immediately.
   */
  protected boolean preRunHook() {
    return true;
  }

  /**
   * Perform any post-run operations.
   * <p>
   * This is intended to be overridden by implementations that require
   * additional functionality or cleanup after the walker execution is
   * complete and the flush action factory has been finalized.
   */
  protected void postRunHook() {
  }

  /**
   * Access whether verbosity should be turned on inside of the DirWalker
   * while it runs.
   * <p>
   * This is intended to be overridden by implmentations to inject the
   * verbose flag into the DirWalker execution.
   *
   * @return true for verbosity in the DirWalker or false (default).
   */
  protected boolean getVerbose() {
    return false;
  }

  /**
   * Helper method for bulding a directory selector that uses this class's
   * selectDirectory method.
   */
  protected final DirectorySelector buildDirectorySelector() {
    return new DirectorySelector() { 
      public DirectorySelector.Action select(File dir) {
        return selectDirectory(dir);
      }
    };
  }

  /**
   * Helper method for building a file selector that uses this class's
   * selectFile method.
   */
  protected final FileSelector buildFileSelector() {
    return new FileSelector() {
      public boolean select(File file) {
        return selectFile(file);
      }
    };
  }

  /**
   * Do the work of getting the current input file.
   * <p>
   * Note that this method can be overridden so that the input file can be more
   * flexibly specified when needed.
   * <p>
   * In this default implementation, the result from getRootInputFile is always
   * returned.
   */
  protected File getCurrentInputFile() {
    return getRootInputFile();
  }

  /**
   * Do the work of getting the current output file.
   * <p>
   * Note that this method can be overridden so that the output file can be more
   * flexibly specified when needed.
   * <p>
   * In this default implementation, the result from getRootOutputFile is always
   * returned.
   */
  protected File getCurrentOutputFile() {
    return getRootOutputFile();
  }

  /**
   * Perform instance initializations by building the flush action factory
   * followed by building the directory walker.
   */
  private final void initialize() {
    this.faf = buildFlushActionFactory(getCurrentOutputFile());
    this.walker = buildWalker();
  }
}
