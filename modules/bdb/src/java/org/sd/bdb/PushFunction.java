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
import com.sleepycat.je.Transaction;

import java.util.Date;

/**
 * A transaction function for inserting a value to the 'front' of the
 * timestamped index.
 * <p>
 * @author Spence Koehler
 */
public class PushFunction implements TransactionFunction {

  private DatabaseEntry key;
  private DbValue value;

  /**
   * Package protected.
   * <p>
   * This class works tightly in conjuction with DbHandle and DbInfo where
   * value may or may not be wrapped with timestamp info. If it is not wrapped
   * with timestamp info, then 'push' does not differ from 'put' in any way.
   */
  PushFunction(DatabaseEntry key, DbValue value) {
    this.key = key;
    this.value = value;
  }

  /**
   * Peform a function in the database
   */
  public boolean performFunction(DbInfo dbInfo, Transaction txn) throws DatabaseException {
    boolean result = false;
    Cursor cursor = null;

    if (dbInfo.hasTimestampedDb()) {
      final DatabaseEntry timestamp = new DatabaseEntry();
      final DatabaseEntry firstKey = new DatabaseEntry();
      final DatabaseEntry firstValue = new DatabaseEntry();

      SecondaryCursor secCursor = null;

      try {
        secCursor = dbInfo.getTimestampedDb().openSecondaryCursor(txn, null);

        final LockMode lockMode = LockMode.READ_UNCOMMITTED;
        final OperationStatus opStatus = secCursor.getFirst(timestamp, firstKey, firstValue, lockMode);

        if (opStatus == OperationStatus.SUCCESS || opStatus == OperationStatus.NOTFOUND) {
          final long firstTimestamp = (opStatus == OperationStatus.SUCCESS) ? dbInfo.getKeyLong(timestamp) : System.currentTimeMillis();
          dbInfo.getDatabase().put(txn, key, TimestampedValue.getDbEntry(firstTimestamp - 1, value));
          result = true;
        }
      }
      finally {
        if (secCursor != null) {
          secCursor.close();
        }
      }
    }
    else {
      // no timestamp info, just do a normal put.
      dbInfo.getDatabase().put(txn, key, value.getValueEntry(false));
      result = true;
    }

    return result;
  }

  /**
   * Log failure due to a database exception.
   * <p>
   * If de is null, then log overall transaction failure.
   */
  public void logFailure(DbInfo dbInfo, DatabaseException de) {
    System.err.println(new Date() + ": bdb." + dbInfo.getDbName() + ".push(" + value.toString() + ")  FAILED!");
    if (de != null) {
      de.printStackTrace(System.err);
    }
  }
}
