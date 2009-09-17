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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.sd.util.NameGenerator;

/**
 * Strategy to flush generated data out to structured files.
 * <p>
 * @author Spence Koehler
 */
public abstract class FlushAction<K, V, A> {
  
  /**
   * Do the work of operating on data while filling for a flush.
   */
  public static interface AddStrategy<K, V, A> {
    public void operate(MapperPair<K, V, A> data);
  }

  /**
   * Do any initialization necessary for beginning adds for the given file.
   */
  protected abstract boolean initializeNextFile(File toFile) throws IOException;

  /**
   * Determine whether it is time to flush data out to a file.
   * <p>
   * Note that this is invoked after each operate call in a thread-safe way such
   * that operate cannot be called by another thread until after shouldFlush has
   * been called.
   */
  protected abstract boolean shouldFlush(MapperPair<K, V, A> pair);

  /**
   * Flush current data to the given file, resetting to receive more add
   * actions.
   * <p>
   * Note that this is invoked after true has been returned by shouldFlush()
   * in a thread-safe way such that doAdd cannot be called by another thread
   * until after doFlush has been called.
   *
   * @return true if flushed and should increment files; otherwise, false.
   */
  protected abstract boolean doFlush(File tofile) throws IOException;

  /**
   * Finalize latest output file.
   */
  protected abstract void finalizeOutput() throws IOException;


  protected final File flushDir;
  protected final RecordFileStrategy<K, V> recordFileStrategy;
  private final NameGenerator ngen;

  private final Object addMutex = new Object();
  private final AtomicReference<String> lastName = new AtomicReference<String>();
  private final AtomicBoolean initializedNextFile = new AtomicBoolean(false);

  /**
   * Construct an instance to flush data into the given directory with
   * generated names.
   *
   * @param flushDir  Parent directory to generated files.
   * @param ngen  Name generator
   */
  protected FlushAction(File flushDir, NameGenerator ngen, RecordFileStrategy<K, V> recordFileStrategy) {
    this.flushDir = flushDir;
    this.recordFileStrategy = recordFileStrategy;
    this.ngen = ngen;
  }

  /**
   * Get this instance's flush dir.
   */
  public File getFlushDir() {
    return flushDir;
  }

  /**
   * Get this instance's record file strategy.
   */
  public RecordFileStrategy<K, V> getRecordFileStrategy() {
    return recordFileStrategy;
  }

  /**
   * Get this instance's name generator.
   */
  public NameGenerator getNameGenerator() {
    return ngen;
  }

  /**
   * Add data, flushing afterwards if warranted.
   *
   * @return true if flushed; otherwise, false.
   */
  public final boolean add(MapperPair<K, V, A> data, AddStrategy<K, V, A> adder) throws IOException {
    boolean result = false;

    synchronized (addMutex) {
      if (initializedNextFile.compareAndSet(false, true)) {
        initializeNextFile();
      }

      if (adder != null) {
        adder.operate(data);
      }

      // check for flush
      if (shouldFlush(data)) {
        result = doflush();
      }
    }

    return result;
  }

  /**
   * Force a flush to the next file.
   *
   * @return true if flushed; otherwise, false.
   */
  public final boolean flush() throws IOException {
    boolean result = false;

    synchronized (addMutex) {
      result = doflush();
    }

    return result;
  }

  private final boolean initializeNextFile() throws IOException {
    boolean result = false;

    final String nextName = ngen.getNextName(lastName.get());

    if (nextName != null) {
      final File file = new File(flushDir, nextName);

      // call extending init
      result = initializeNextFile(file);

      // only increment names if init succeeded
      if (result) {
        lastName.set(nextName);
      }
    }

    return result;
  }

  /**
   * Generate the next filename and flush to it.
   *
   * @return true if flushed; otherwise, false.
   */
  private final boolean doflush() throws IOException {
    boolean result = false;

    // recall the next filename
    final String nextName = lastName.get();

    if (nextName != null) {
      final File file = new File(flushDir, nextName);

      // call extending flush
      result = doFlush(file);

      // only trigger initialize (and increment) if flush succeeded
      if (result) {
        initializedNextFile.set(false);
      }
    }
    else {
      finalizeOutput();
    }

    return result;
  }
}
