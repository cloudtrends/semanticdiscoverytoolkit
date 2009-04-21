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

import java.io.IOException;
import java.util.Iterator;

/**
 * Key order iterator over the database.
 * <p>
 * @author Spence Koehler
 */
public class KeyOrderIterator extends BaseDbIterator<StringKeyValuePair> {

  private static final StringKeyValuePair EMPTY = new StringKeyValuePair(null, (String)null);


  private Cursor cursor;
  private DatabaseEntry key;
  private DatabaseEntry value;

  public KeyOrderIterator(DbInfo dbInfo, boolean forward, String dbMarkerId) {
    super(dbInfo, forward, dbMarkerId);
  }

  protected final Cursor getCursor() {
    return cursor;
  }

  protected final StringKeyValuePair doInitFirst(DbInfo dbInfo, boolean forward) throws DatabaseException {
    this.cursor = getPrimaryCursor(dbInfo);

    this.key = new DatabaseEntry();
    this.value = new DatabaseEntry();

    StringKeyValuePair result = null;
    // seek to marker position if the marker is not null
    if(this.marker != null){
      String lastKey = this.marker.getLastStringKey();
      if(lastKey != null){
        // roll to marker adds to queue
        OperationStatus opStatus = rollToMarker(lastKey);
        if(opStatus == OperationStatus.SUCCESS) return null;
      }
    }

    if(result == null){
//      System.out.println("Iterator cursor initialized.");

      final OperationStatus opStatus = forward ?
        cursor.getFirst(key, value, LockMode.READ_UNCOMMITTED) :
        cursor.getLast(key, value, LockMode.READ_UNCOMMITTED);

      result = getNextStringPair(dbInfo, key, value, opStatus);
    }    

    return result;
  }

  protected final StringKeyValuePair doGetNext(DbInfo dbInfo, boolean forward) throws DatabaseException {
    StringKeyValuePair result = null;

    if (this.cursor != null) {
      final OperationStatus opStatus = forward ?
        cursor.getNext(key, value, LockMode.READ_UNCOMMITTED) :
        cursor.getPrev(key, value, LockMode.READ_UNCOMMITTED);

      if (opStatus == OperationStatus.SUCCESS) {
        result = getNextStringPair(dbInfo, key, value, opStatus);
      }
    }

    return result;
  }

  protected final boolean doWriteKeyToMarker(StringKeyValuePair kvPair){
    if (kvPair != null){
      try {
        return this.marker.writeNextKey(kvPair.getKey());
      }
      catch (IOException ioe){
        System.err.println("Unable to write key '" + kvPair.getKey() + "' to marker!: " + ioe.getMessage());
        return false;
      }
    }
    else {
      return false;
    }
  }

  /** Return a value that represents the empty marker. */
  protected final StringKeyValuePair getEmptyMarker() {
    return EMPTY;
  }

  /** Test whether the element is the empty marker. */
  protected final boolean isEmptyMarker(StringKeyValuePair marker) {
    return marker == EMPTY;
  }
}
