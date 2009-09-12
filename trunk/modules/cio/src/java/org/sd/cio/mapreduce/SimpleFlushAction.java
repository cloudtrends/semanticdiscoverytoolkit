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
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sd.util.NameGenerator;

/**
 * A flush strategy that uses a MapContainer for flushing a constant number
 * of records. This strategy is best used with mapper actions.
 * <p>
 * @author Spence Koehler
 */
public class SimpleFlushAction<K, V, A> extends FlushAction<K, V, A> {

  private int maxOutRecordCount;
  private final MapContainer<K, V> data;

  public SimpleFlushAction(File flushDir, NameGenerator ngen, int maxOutRecordCount,
                           RecordFileStrategy<K, V> recordFileStrategy,
                           MapContainer<K, V> data) {
    super(flushDir, ngen, recordFileStrategy);
    this.maxOutRecordCount = maxOutRecordCount;
    this.data = data;
  }

  /**
   * Access this instance's data.
   */
  public MapContainer<K, V> getData() {
    return data;
  }

  /**
   * Do any initialization necessary for beginning adds for the given file.
   */
  protected boolean initializeNextFile(File toFile) throws IOException {
    //NOTE: file is only opened when data is flushed
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
    return (data.getMap().size() == maxOutRecordCount);
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
    boolean result = true;

    if (recordFileStrategy.bounce(flushFile)) {
      int numWritten = 0;
      try {
        for (Map.Entry<K, V> entry : data.getMap().entrySet()) {
          if (recordFileStrategy.writeRecord(entry.getKey(), entry.getValue())) {
            ++numWritten;
          }
        }
      }
      finally {
        recordFileStrategy.close();
      }

      if (numWritten == 0) {
        result = false;  // nothing to flush, don't increment
      }

      // zero out the map for the next batch
      data.getMap().clear();
    }
    // else return true anyway so that file increments

    return result;
  }

  /**
   * Finalize latest output file.
   */
  protected void finalizeOutput() throws IOException {
    //nothing to do.
  }
}
