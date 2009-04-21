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
 * A transaction function for popping timestamped data from the database.
 * <p>
 * NOTE: Instances of this class are not thread safe. One instance is required
 *       per transaction so that the return value can be properly captured.
 *
 * @author Spence Koehler
 */
class PopFunction implements TransactionFunction {

  private boolean keyIsString;  // otherwise, keyIsLong
  private boolean getEarliest;  // otherwise, getLatest
  private boolean peekOnly;     // otherwise, doPop
  private boolean usePrimary;   // otherwise, useSecondary (timestamped)

  private StringKeyValuePair stringResult;
  private LongKeyValuePair longResult;

  PopFunction(boolean keyIsString, boolean getEarliest, boolean peekOnly, boolean usePrimary) {
    this.keyIsString = keyIsString;
    this.getEarliest = getEarliest;
    this.peekOnly = peekOnly;
    this.usePrimary = usePrimary;
    this.stringResult = null;
    this.longResult = null;
  }

  /**
   * Get the string key based result of the pop.
   * <p>
   * Note that if this instance is configured for a long, an exception will be thrown.
   */
  public StringKeyValuePair getStringKeyValuePair() {
    if (!keyIsString) throw new IllegalStateException("Attempt to get 'string' keyValuePair when expecting to hold 'long'!");
    return stringResult;
  }

  /**
   * Get the long key based result of the pop.
   * <p>
   * Note that if this instance is configured for a string, an exception will be thrown.
   */
  public LongKeyValuePair getLongKeyValuePair() {
    if (keyIsString) throw new IllegalStateException("Attempt to get 'long' keyValuePair when expecting to hold 'string'!");
    return longResult;
  }

  /**
   * Peform a function in the database
   */
  public boolean performFunction(DbInfo dbInfo, Transaction txn) throws DatabaseException {
    boolean result = false;

    if (usePrimary) {
      result = performPrimaryFunction(dbInfo, txn);
    }
    else {
      result = performSecondaryFunction(dbInfo, txn);
    }

    return result;
  }

  private final boolean performPrimaryFunction(DbInfo dbInfo, Transaction txn) throws DatabaseException {
    boolean result = false;

    final DatabaseEntry key = new DatabaseEntry();
    final DatabaseEntry value = new DatabaseEntry();

    Cursor cursor = null;

    try {
      cursor = dbInfo.getDatabase().openCursor(txn, null);
      OperationStatus opStatus = null;

      LockMode lockMode = LockMode.RMW;
      if (peekOnly) lockMode = LockMode.READ_UNCOMMITTED;

      if (getEarliest) {
        opStatus = cursor.getFirst(key, value, lockMode);
      }
      else {
        opStatus = cursor.getLast(key, value, lockMode);
      }

      if (opStatus == OperationStatus.SUCCESS) {
        if (peekOnly || cursor.delete() == OperationStatus.SUCCESS) {
          final DbValue dbValue = dbInfo.getDbValue(value);
          if (keyIsString) {
            this.stringResult = new StringKeyValuePair(dbInfo.getKeyString(key), dbValue.getValueBytes());
          }
          else {
            this.longResult = new LongKeyValuePair(dbInfo.getKeyLong(key), dbValue.getValueBytes());
          }
          result = true;
        }
      }
    }
    finally {
      if (cursor != null) {
        cursor.close();
      }
    }

    return result;
  }

  private final boolean performSecondaryFunction(DbInfo dbInfo, Transaction txn) throws DatabaseException {
    boolean result = false;

    if (!dbInfo.hasTimestampedDb()) throw new IllegalStateException(
      "Can't pop secondary w/out timestamped db! (" + dbInfo.getDbName() + ")");

    final DatabaseEntry timestamp = new DatabaseEntry();
    final DatabaseEntry key = new DatabaseEntry();
    final DatabaseEntry value = new DatabaseEntry();

    SecondaryCursor secCursor = null;

    try {
      secCursor = dbInfo.getTimestampedDb().openSecondaryCursor(txn, null);
      OperationStatus opStatus = null;

      LockMode lockMode = LockMode.RMW;
      if (peekOnly) lockMode = LockMode.READ_UNCOMMITTED;

      if (getEarliest) {
        opStatus = secCursor.getFirst(timestamp, key, value, lockMode);
      }
      else {
        opStatus = secCursor.getLast(timestamp, key, value, lockMode);
      }

      if (opStatus == OperationStatus.SUCCESS) {
        if (peekOnly || secCursor.delete() == OperationStatus.SUCCESS) {
          final DbValue dbValue = dbInfo.getDbValue(value);
          if (keyIsString) {
            this.stringResult = new StringKeyValuePair(dbInfo.getKeyString(key), dbValue.getValueBytes(), dbInfo.getKeyLong(timestamp));
          }
          else {
            this.longResult = new LongKeyValuePair(dbInfo.getKeyLong(key), dbValue.getValueBytes(), dbInfo.getKeyLong(timestamp));
          }
          result = true;
        }
      }
    }
    finally {
      if (secCursor != null) {
        secCursor.close();
      }
    }

    return result;
  }

  /**
   * Log failure due to a database exception.
   * <p>
   * If de is null, then log overall transaction failure.
   */
  public void logFailure(DbInfo dbInfo, DatabaseException de) {
    if (de != null) {
      System.err.println(new Date() + ": bdb." + dbInfo.getDbName() + ".pop()  FAILED!");
      de.printStackTrace(System.err);
    }
  }
}
