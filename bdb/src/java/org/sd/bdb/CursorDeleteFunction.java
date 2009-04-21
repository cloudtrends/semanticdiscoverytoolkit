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


import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Transaction;

import java.util.Date;

/**
 * A transaction function for deleting the item 'before' the cursor.
 * <p>
 * @author Spence Koehler
 */
public class CursorDeleteFunction implements TransactionFunction {

  private Cursor cursor;
  private boolean forward;

  /**
   * Package protected.
   */
  CursorDeleteFunction(Cursor cursor, boolean forward) {
    this.cursor = cursor;
    this.forward = forward;
  }

  /**
   * Peform a function in the database
   */
  public boolean performFunction(DbInfo dbInfo, Transaction txn) throws DatabaseException {
    boolean result = false;

    try {
      final DatabaseEntry key = new DatabaseEntry();
      final DatabaseEntry value = new DatabaseEntry();

      // move cursor to the entry to delete
      OperationStatus opStatus = forward ? cursor.getPrev(key, value, LockMode.RMW) : cursor.getNext(key, value, LockMode.READ_UNCOMMITTED);
      if (opStatus == OperationStatus.SUCCESS) {

        // delete it.
        opStatus = cursor.delete();

        // put cursor back where it was
        if (forward && opStatus != OperationStatus.SUCCESS) {
          cursor.getNext(key, value, LockMode.RMW);
        }
        else if (!forward) {
          cursor.getPrev(key, value, LockMode.RMW);
        }

        result = (opStatus == OperationStatus.SUCCESS);
      }
    }
    catch (DatabaseException de) {
      throw new IllegalStateException(de);
    }

    return result;
  }

  /**
   * Log failure due to a database exception.
   * <p>
   * If de is null, then log overall transaction failure.
   */
  public void logFailure(DbInfo dbInfo, DatabaseException de) {
    System.err.println(new Date() + ": bdb." + dbInfo.getDbName() + ".cursorDelete()  FAILED!");
    if (de != null) {
      de.printStackTrace(System.err);
    }
  }
}
