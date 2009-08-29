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
package org.sd.io;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility to iterate over multiple sorted file lines simultaneously, getting
 * all equal lines according to the recordComparer with each 'next'.
 * <p>
 * @author Spence Koehler
 */
public abstract class MultiFileIterator<T> implements MultiRecordIterator<T> {

  /**
   * Build a FileRecordIterator instance for the given file.
   */
  protected abstract FileRecordIterator<T> buildRecordIterator(File file) throws IOException;


  private Comparator<T> recordComparer;
  private Container container;
  private List<T> next;
  private final FileRecordIteratorComparator friComparator;

  /**
   * Iterate over the given file lines simultaneously.
   */
  public MultiFileIterator(File[] files, Comparator<T> recordComparer) {
    this.recordComparer = recordComparer;
    this.friComparator = new FileRecordIteratorComparator();  //note: must come before new Container()
    this.container = new Container(files);
    this.next = container.get();
  }

  /**
   * Close the resources associated with this instance.
   */
  public void close() throws IOException {
    container.close();
  }

  /** Determine whether there are more strings to get. */
  public boolean hasNext() {
    return next != null;
  }

  /** Get the next equal strings, or null if done. */
  public List<T> next() {
    final List<T> result = next;
    next = container.get();
    return result;
  }

  public void remove() {
    throw new UnsupportedOperationException("Not supported.");
  }

  /** Container class for reader recordIterators */
  private final class Container {
    private List<FileRecordIterator<T>> recordIterators;

    public Container(File[] files) {
      this.recordIterators = new ArrayList<FileRecordIterator<T>>();
      for (File file : files) {
        try {
          this.recordIterators.add(buildRecordIterator(file));
        }
        catch (IOException e) {
          throw new IllegalArgumentException("bad file '" + file + "'", e);
        }
      }
      Collections.sort(recordIterators, friComparator);
    }

    /**
     * Get all lines that are currently equal.
     */
    public List<T> get() {
      List<T> result = null;

      T lastRecord = null;
      for (FileRecordIterator<T> recordIterator : recordIterators) {
        if (!recordIterator.hasNext()) break;
        if (lastRecord == null) {
          do {
            if (result == null) result = new ArrayList<T>();
            final T curRecord = recordIterator.next();
            result.add(curRecord);
            lastRecord = curRecord;
          } while (recordIterator.hasNext() && recordComparer.compare(lastRecord, recordIterator.getRecord()) == 0);
        }
        else {
          boolean added = false;
          while (recordIterator.hasNext() && recordComparer.compare(lastRecord, recordIterator.getRecord()) == 0) {
            if (result == null) result = new ArrayList<T>();
            final T curRecord = recordIterator.next();
            result.add(curRecord);
            lastRecord = curRecord;
            added = true;
          }

          if (!added) break;  // farther down the record can't be equal
        }
      }

      if (result != null) Collections.sort(recordIterators, friComparator);

      return result;
    }

    public void close() throws IOException {
      for (FileRecordIterator<T> recordIterator : recordIterators) {
        recordIterator.close();
      }
    }
  }

  /** Container class for readers and lines */
  private final class FileRecordIteratorComparator implements Comparator<FileRecordIterator<T>> {

    private FileRecordIteratorComparator() {}

    /**
     * Compare this recordIterator to the other based on how records compare.
     */
    public int compare(FileRecordIterator<T> i1, FileRecordIterator<T> i2) {
      int result = 0;

      final T record1 = i1.getRecord();
      final T record2 = i2.getRecord();

      if (record1 == null) {
        if (record2 != null) result = 1;
        // else leave at 0
      }
      else if (record2 == null) {
        if (record1 != null) result = -1;
        // else leave at 0
      }
      else {
        result = recordComparer.compare(record1, record2);
      }

      return result;
    }
  }
}
