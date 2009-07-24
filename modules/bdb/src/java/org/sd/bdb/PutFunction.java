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
import com.sleepycat.je.Transaction;

import java.util.Date;

/**
 * A transaction function for putting data into the database.
 * <p>
 * @author Spence Koehler
 */
public class PutFunction implements TransactionFunction {

  private DatabaseEntry key;
  private DatabaseEntry value;

  /**
   * Package protected.
   * <p>
   * This class works tightly in conjuction with DbHandle and DbInfo where
   * value may or may not be wrapped with timestamp info.
   */
  public PutFunction(DatabaseEntry key, DatabaseEntry value) {
    this.key = key;
    this.value = value;
  }

  /**
   * Peform a function in the database
   */
  public boolean performFunction(DbInfo dbInfo, Transaction txn) throws DatabaseException {
    dbInfo.getDatabase().put(txn, key, value);
    return true;
  }

  /**
   * Log failure due to a database exception.
   * <p>
   * If de is null, then log overall transaction failure.
   */
  public void logFailure(DbInfo dbInfo, DatabaseException de) {
    System.err.println(new Date() + ": bdb." + dbInfo.getDbName() + ".put(" + dbInfo.getDbValue(value).toString() + ")  FAILED!");
    if (de != null) {
      de.printStackTrace(System.err);
    }
  }
}
