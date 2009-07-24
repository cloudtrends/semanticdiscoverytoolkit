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
import com.sleepycat.je.SecondaryCursor;

import java.io.IOException;
import java.util.Iterator;

/**
 * Key order iterator over the database.
 * <p>
 * @author Spence Koehler
 */
public class LongTimeOrderIterator extends BaseDbIterator<LongKeyValuePair> {

  private static final LongKeyValuePair EMPTY = new LongKeyValuePair(0L, (String)null);


  private SecondaryCursor cursor;
  private DatabaseEntry key;
  private DatabaseEntry pKey;
  private DatabaseEntry value;

  public LongTimeOrderIterator(DbInfo dbInfo, boolean forward, String dbMarkerId) {
    super(dbInfo, forward, null);

    if(dbMarkerId != null) throw new UnsupportedOperationException("Cannot write to key marker on timestamped database!");
  }

  protected final Cursor getCursor() {
    return cursor;
  }

  protected final LongKeyValuePair doInitFirst(DbInfo dbInfo, boolean forward) throws DatabaseException {
    this.cursor = getSecondaryCursor(dbInfo);

    this.key = new DatabaseEntry();
    this.pKey = new DatabaseEntry();
    this.value = new DatabaseEntry();


    final OperationStatus opStatus = forward ?
      cursor.getFirst(key, pKey, value, LockMode.READ_UNCOMMITTED) :
      cursor.getLast(key, pKey, value, LockMode.READ_UNCOMMITTED);

    return getNextLongPair(dbInfo, pKey, value, opStatus);
  }

  protected final LongKeyValuePair doGetNext(DbInfo dbInfo, boolean forward) throws DatabaseException {
    LongKeyValuePair result = null;

    if (this.cursor != null) {
      final OperationStatus opStatus = forward ?
        cursor.getNext(key, pKey, value, LockMode.READ_UNCOMMITTED) :
        cursor.getPrev(key, pKey, value, LockMode.READ_UNCOMMITTED);

      if (opStatus == OperationStatus.SUCCESS) {
        result = getNextLongPair(dbInfo, pKey, value, opStatus);
      }
    }

    return result;
  }

  protected final boolean doWriteKeyToMarker(LongKeyValuePair kvPair){
    throw new UnsupportedOperationException("Cannot write to key marker on timestamped database!");
  }

  /** Return a value that represents the empty marker. */
  protected final LongKeyValuePair getEmptyMarker() {
    return EMPTY;
  }

  /** Test whether the element is the empty marker. */
  protected final boolean isEmptyMarker(LongKeyValuePair marker) {
    return marker == EMPTY;
  }
}
