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


import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DeadlockException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.Transaction;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Container for database info.
 * <p>
 * @author Spence Koehler
 */
public class DbInfo {

  public static final int MAX_DEADLOCK_RETRIES = 10;

  private static final TupleBinding<Long> LONG_TUPLE_BINDING = TupleBinding.getPrimitiveBinding(Long.class);

  private BerkeleyDb bdb;  // backpointer.
  private Environment environment;
  private String dbName;
  private Database database;
  private SecondaryDatabase timestampedDb;

  private final AtomicBoolean open = new AtomicBoolean(true);
  private final AtomicInteger errorCount = new AtomicInteger(0);
  private transient Boolean _hasStringKeys;

  public DbInfo(BerkeleyDb bdb, Environment environment, String dbName, Database database, SecondaryDatabase timestampedDb) {
    this.bdb = bdb;
    this.environment = environment;
    this.dbName = dbName;
    this.database = database;
    this.timestampedDb = timestampedDb;
    this._hasStringKeys = null;
  }

  /**
   * Get this info's berkeley db backpointer.
   */
  public BerkeleyDb getBerkeleyDb() {
    return bdb;
  }

  /**
   * Close the database, not the environment.
   */
  public final void close() throws DatabaseException {
    if (open.compareAndSet(true, false)) {
      if (timestampedDb != null) timestampedDb.close();
      if (database != null) {
        database.close();
      }
    }
  }

  /**
   * Get the environment.
   */
  public final Environment getEnvironment() {
    return environment;
  }

  /**
   * Get the database name.
   */
  public final String getDbName() {
    return dbName;
  }

  /**
   * Get the database.
   */
  public final Database getDatabase() {
    return database;
  }

  /**
   * Get the timestamped db, possibly null.
   */
  public final SecondaryDatabase getTimestampedDb() {
    return timestampedDb;
  }

  /**
   * Report whether this instance has a timestamped database.
   */
  public final boolean hasTimestampedDb() {
    return timestampedDb != null;
  }

  /**
   * Report whether the db has string keys. The alternative is long keys.
   * <p>
   * If not set on the instance, the value will be computed based on the
   * absence of "_id" at the end of the dbName.
   */
  public final boolean hasStringKeys() {
    if (_hasStringKeys == null) {
      _hasStringKeys = !dbName.toLowerCase().endsWith("_id");
    }
    return _hasStringKeys;
  }

  /**
   * Record on this instance (transiently) that the keys should be assumed to
   * be longs.
   */
  public final void setHasLongKeys() {
    setHasLongKeys(false);
  }

  /**
   * Record on this instance (transiently) whether the keys should be assumed
   * to be longs.
   */
  public final void setHasLongKeys(boolean hasLongKeys) {
    this._hasStringKeys = !hasLongKeys;
  }

  /**
   * Wrapper to translate a (string) key into a DatabaseEntry.
   */
  public final DatabaseEntry getKeyEntry(String string) {
    return getDatabaseEntry(string);
  }

  /**
   * Wrapper to translate a (long) key into a DatabaseEntry.
   */
  public final DatabaseEntry getKeyEntry(long key) {
    final DatabaseEntry result = new DatabaseEntry();
    LONG_TUPLE_BINDING.objectToEntry(key, result);
    return result;
  }

  /**
   * Wrapper to translate a value into a DatabaseEntry.
   * <p>
   * Note that the stored value will be a timestamped value if this instance
   * is configured with a timestamped db; otherwise, it will be the string
   * itself.
   */
  public final DatabaseEntry getValueEntry(DbValue dbValue) {
    return dbValue.getValueEntry(timestampedDb != null);
  }

  /**
   * Wrapper to translate a value into a DatabaseEntry that preserves the oldValue's
   * timestamp if applicable.
   * <p>
   * Note that the stored value will be a timestamped value if this instance
   * is configured with a timestamped db; otherwise, it will be the string
   * itself.
   */
  public final DatabaseEntry getValueEntry(DbValue newValue, Long oldTimestamp) {
    DatabaseEntry result = null;

    if (oldTimestamp == null || timestampedDb == null) {
      result = newValue.getValueEntry(timestampedDb != null);
    }
    else {
      result = TimestampedValue.getDbEntry(oldTimestamp, newValue);
    }

    return result;
  }

  /**
   * Wrapper to extract the key string from the database entry.
   * <p>
   * Note that this assumes that string keys are consistently being used
   * with this instance.
   */
  public final String getKeyString(DatabaseEntry keyEntry) {
    return getString(keyEntry);
  }

  /**
   * Wrapper to extract the db value from the database entry.
   * <p>
   * The returned value will be a TimestampedValue if this instance is
   * configured to maintain the secondary timestamped db; otherwise, it
   * will be a DbValue with a timestamp of 0 (undefined).
   */
  public final DbValue getDbValue(DatabaseEntry valueEntry) {
    DbValue result = null;

    if (timestampedDb != null) {
      result = TimestampedValue.getTsRecord(valueEntry);
    }
    else {
      result = new DbValue(valueEntry.getData(), 0L);
    }

    return result;
  }

  /**
   * Wrapper to extract the key (as a long) from the database entry.
   * <p>
   * Note that this assumes that long keys are consistently being used
   * with this instance.
   */
  public final long getKeyLong(DatabaseEntry keyEntry) {
    final Long result = LONG_TUPLE_BINDING.entryToObject(keyEntry);
    return result;
  }

  /**
   * Helper method to turn a string into a database entry.
   */
  private final DatabaseEntry getDatabaseEntry(String string) {
    return new DatabaseEntry(DbUtil.getBytes(string));
  }

  /**
   * Helper method to get a string from a database entry.
   */
  private final String getString(DatabaseEntry dbEntry) {
    return DbUtil.getString(dbEntry.getData());
  }

  /**
   * Perform the function under a transaction in this instance's environment.
   */
  public boolean performTransaction(TransactionFunction txFn) {
    boolean result = false;
    Transaction txn = null;

    for (int retry = 0; retry < MAX_DEADLOCK_RETRIES; ++retry) {
      try {
        txn = environment.beginTransaction(null, null);
        if (txFn.performFunction(this, txn)) {
          txn.commit();
          result = true;
        }
        else {
          txn.abort();
        }
        break;
      }
      catch (DeadlockException deadlockException) {
        try {
          // Abort the transaction and retry
          txn.abort();
        }
        catch (DatabaseException ddException) {
          // Log failure.
          txFn.logFailure(this, ddException);
          errorCount.incrementAndGet();
          break;
        }
      }
      catch (DatabaseException databaseException) {
        try {
          // Abort the transaction
          if (txn != null) txn.abort();

          // log failure
          txFn.logFailure(this, databaseException);
          errorCount.incrementAndGet();
        }
        catch (DatabaseException ddException) {
          // Log failure.
          txFn.logFailure(this, ddException);
          errorCount.incrementAndGet();
        }
        break;
      }
    }

    if (!result) {
      // log failure
      txFn.logFailure(this, null);
      errorCount.incrementAndGet();
    }

    return result;
  }

  /**
   * Get the number of errors that have occurred.
   */
  public final int getErrorCount() {
    return errorCount.get();
  }
}
