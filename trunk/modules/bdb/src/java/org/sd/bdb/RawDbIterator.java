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
package org.sd.bdb;


import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;

/**
 * Iterator over raw (DatabaseEntry) database content.
 * <p>
 * @author Spence Koehler
 */
public class RawDbIterator extends BaseDbIterator<RawKeyValuePair> {

  private static final RawKeyValuePair EMPTY = new RawKeyValuePair(null, null);


  private Cursor cursor;
  private DatabaseEntry key;
  private DatabaseEntry value;

  public RawDbIterator(DbInfo dbInfo, boolean forward) {
    super(dbInfo, forward, null/*marker not implement for byte key*/);
  }

  protected final Cursor getCursor() {
    return cursor;
  }

  protected final RawKeyValuePair doInitFirst(DbInfo dbInfo, boolean forward) throws DatabaseException {
    this.cursor = getPrimaryCursor(dbInfo);

    this.key = new DatabaseEntry();
    this.value = new DatabaseEntry();

    final OperationStatus opStatus = forward ?
      cursor.getFirst(key, value, LockMode.READ_UNCOMMITTED) :
      cursor.getLast(key, value, LockMode.READ_UNCOMMITTED);

    return new RawKeyValuePair(key, value);
  }

  protected final RawKeyValuePair doGetNext(DbInfo dbInfo, boolean forward) throws DatabaseException {
    RawKeyValuePair result = null;

    if (this.cursor != null) {
      final OperationStatus opStatus = forward ?
        cursor.getNext(key, value, LockMode.READ_UNCOMMITTED) :
        cursor.getPrev(key, value, LockMode.READ_UNCOMMITTED);

      if (opStatus == OperationStatus.SUCCESS) {
        result = new RawKeyValuePair(key, value);
      }
    }

    return result;
  }

  protected final boolean doWriteKeyToMarker(RawKeyValuePair kvPair){
    // todo: implement this method
    return false;
  }

  /** Return a value that represents the empty marker. */
  protected final RawKeyValuePair getEmptyMarker() {
    return EMPTY;
  }

  /** Test whether the element is the empty marker. */
  protected final boolean isEmptyMarker(RawKeyValuePair marker) {
    return marker == EMPTY;
  }
}
