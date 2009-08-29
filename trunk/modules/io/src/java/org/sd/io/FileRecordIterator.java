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

/**
 * Base class to iterate over records of type T in a file.
 * <p>
 * @author Spence Koehler
 */
public abstract class FileRecordIterator<T> implements RecordIterator<T> {

  /** Initialize this instance for reading the file. */
  protected abstract void init(File file) throws IOException;

  /** Read the next record, returning null if there are no more. */
  protected abstract T readNextRecord() throws IOException;

  /** Close resources for this instance. */
  public abstract void close() throws IOException;


  private File file;
  private T record;

  protected FileRecordIterator(File file) throws IOException {
    this.file = file;
    init(file);
    this.record = readNextRecord();
  }

  /**
   * Peek at the next record without incrementing.
   */
  public T getRecord() {
    return record;
  }

  /**
   * Get the next record and increment.
   */
  public T next() {
    T result = this.record;

    if (result != null) {
      try {
        this.record = readNextRecord();
      }
      catch (IOException e) {
        throw new IllegalStateException("Can't read next record from '" + file + "'!", e);
      }
    }
    // else, already reached end of stream

    return result;
  }

  /** Remove not supported. */
  public void remove() {
    throw new UnsupportedOperationException("Not supported!");
  }

  /**
   * Determine whether there is a next record.
   */
  public boolean hasNext() {
    return this.record != null;
  }
}
