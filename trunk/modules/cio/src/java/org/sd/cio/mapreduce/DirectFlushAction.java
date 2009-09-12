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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.sd.util.NameGenerator;

/**
 * A flush strategy that directly writes each added record. This strategy is
 * best used with reducer actions.
 * <p>
 * @author Spence Koehler
 */
public class DirectFlushAction<K, V, A> extends FlushAction<K, V, A> {

  private int maxOutRecordCount;  // num records to write before rolling files
  private final AtomicInteger numWritten = new AtomicInteger(0);

  public DirectFlushAction(File flushDir, NameGenerator ngen, int maxOutRecordCount,
                           RecordFileStrategy<K, V> recordFileStrategy) {
    super(flushDir, ngen, recordFileStrategy);
    this.maxOutRecordCount = maxOutRecordCount;
  }

  /**
   * Do any initialization necessary for beginning adds for the given file.
   */
  protected boolean initializeNextFile(File toFile) throws IOException {
    bounceWriter(toFile);
    return true;
  }

  /**
   * Determine whether it is time to flush data out to a file.
   * <p>
   * Note that this is invoked after each doAdd call in a thread-safe way such
   * that doAdd cannot be called by another thread until after shouldFlush has
   * been called.
   */
  protected boolean shouldFlush(MapperPair<K, V, A> pair) {
    try {
      recordFileStrategy.writeRecord(pair.key, pair.value);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return (numWritten.incrementAndGet() == maxOutRecordCount);
  }

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
  protected boolean doFlush(File flushFile) throws IOException {
    recordFileStrategy.close();
    numWritten.set(0);
    return true;
  }

  /**
   * Finalize latest output file.
   */
  protected void finalizeOutput() throws IOException {
    recordFileStrategy.close();
  }

  private final void bounceWriter(File toFile) throws IOException {
    recordFileStrategy.bounce(toFile);
    numWritten.set(0);
  }
}
