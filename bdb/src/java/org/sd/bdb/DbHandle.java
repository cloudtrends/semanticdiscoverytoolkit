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

import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handle on a berkeley database with transactional db read/write methods.
 * <p>
 * @author Spence Koehler
 */
public class DbHandle {

  private DbInfo dbInfo;
  private String dbMarkerId;

  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Package protected for construction from BerkeleyDb class.
   */
  DbHandle(DbInfo dbInfo, String dbMarkerId) {
    this.dbInfo = dbInfo;
    this.dbMarkerId = dbMarkerId;
  }

  /**
   * Get this handle's info.
   */
  public DbInfo getDbInfo() {
    return dbInfo;
  }

  /**
   * Get the db.
   */
  public BerkeleyDb getBerkeleyDb() {
    return dbInfo.getBerkeleyDb();
  }

  /**
   * Get the number of records in this handle's database.
   */
  public long getNumRecords() {
    long result = -1L;

    try {
      result = dbInfo.getDatabase().count();
    }
    catch (DatabaseException e) {
      // error indicated by result == -1L
    }

    return result;
  }

  /**
   * Report whether this handle's database has an associated timestamped database.
   */
  public boolean hasTimestampedDb() {
    return dbInfo.hasTimestampedDb();
  }

  /**
   * Determine whether this instance has been closed.
   */
  public boolean isClosed() {
    return closed.get();
  }

  /**
   * Close this handle's database.
   * <p>
   * NOTE: This will automatically be called when the spawning BerkeleyDb instance is closed,
   *       but re-closing will have no determinental side-effects.
   */
  public final synchronized void close() {
    if (!closed.getAndSet(true)) {
      try {
        dbInfo.close();
				dbInfo.getBerkeleyDb().remove(this);
      }
      catch (DatabaseException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  /**
   * Put the value associated with the key.
   */
  public final void put(long key, DbValue value) {
    final DatabaseEntry keyEntry = dbInfo.getKeyEntry(key);
    final DatabaseEntry valueEntry = dbInfo.getValueEntry(value);

    doPut(keyEntry, valueEntry);
  }

  /**
   * Put the value associated with the key.
   */
  public final void put(String key, DbValue value) {
    final DatabaseEntry keyEntry = dbInfo.getKeyEntry(key);
    final DatabaseEntry valueEntry = dbInfo.getValueEntry(value);

    doPut(keyEntry, valueEntry);
  }

  /**
   * Put the entry under a transaction.
   */
  private final boolean doPut(DatabaseEntry key, DatabaseEntry value) {
    return dbInfo.performTransaction(new PutFunction(key, value));
  }

  /**
   * Push the value associated with the key to the 'beginning'.
   * <p>
   * NOTE: This is only meaningful when a timestamped db is being used.
   *       The 'pushed' value will be inserted as if before the first value
   *       in the database.
   */
  public final void push(long key, DbValue value) {
    final DatabaseEntry keyEntry = dbInfo.getKeyEntry(key);

    doPush(keyEntry, value);
  }

  /**
   * Put the value associated with the key.
   */
  public final void push(String key, DbValue value) {
    final DatabaseEntry keyEntry = dbInfo.getKeyEntry(key);

    doPush(keyEntry, value);
  }

  /**
   * Put the entry under a transaction.
   */
  private final boolean doPush(DatabaseEntry key, DbValue value) {
    return dbInfo.performTransaction(new PushFunction(key, value));
  }

  /**
   * Get the DbValue for the key.
   */
  public final DbValue get(long key) {
    final DatabaseEntry keyEntry = dbInfo.getKeyEntry(key);

    return doGet(keyEntry);
  }

  /**
   * Get the DbValue for the key.
   */
  public final DbValue get(String key) {
    final DatabaseEntry keyEntry = dbInfo.getKeyEntry(key);

    return doGet(keyEntry);
  }

  /**
   * Get the DbValue for the key under a transaction.
   */
  private final DbValue doGet(DatabaseEntry key) {
    DbValue result = null;
    final DatabaseEntry value = new DatabaseEntry();

    try {
      if (dbInfo.getDatabase().get(null, key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
        result = dbInfo.getDbValue(value);
      }
    }
    catch (DatabaseException de) {
      logGetFailure(de, key);
    }

    return result;
  }

  private final void logGetFailure(DatabaseException de, DatabaseEntry key) {
    System.err.println(new Date() + ": get(" + dbInfo.getKeyString(key) + ")  FAILED!");
    if (de != null) {
      de.printStackTrace(System.err);
    }
  }

  public final void delete(long key) {
    final DatabaseEntry keyEntry = dbInfo.getKeyEntry(key);

    doDelete(keyEntry);
  }

  public final void delete(String key) {
    final DatabaseEntry keyEntry = dbInfo.getKeyEntry(key);

    doDelete(keyEntry);
  }

  private final void doDelete(DatabaseEntry key) {
    try {
      dbInfo.getDatabase().delete(null, key);
    }
    catch (DatabaseException de) {
      logDeleteFailure(de, key);
    }
  }

  private final void logDeleteFailure(DatabaseException de, DatabaseEntry key) {
    System.err.println(new Date() + ": delete(" + dbInfo.getKeyString(key) + ")  FAILED!");
    if (de != null) {
      de.printStackTrace(System.err);
    }
  }

  /**
   * Update the record with the given key to have the new value.
   * <p>
   * If the record doesn't exist, then return false without adding it.
   * <p>
   * If oldTimestamp is non-null and timestamps are warranted, keep the
   * oldTimestamp with the new updated value.
   *
   * @return true if the record existed and was updated; otherwise, false.
   */
  public final boolean update(long key, DbValue newValue, Long oldTimestamp) {
    final DatabaseEntry keyEntry = dbInfo.getKeyEntry(key);
    final DatabaseEntry newValueEntry = dbInfo.getValueEntry(newValue, oldTimestamp);

    return doUpdate(keyEntry, newValueEntry);
  }

  /**
   * Update the record with the given key to have the new value.
   * <p>
   * If the record doesn't exist, then return false without adding it.
   * <p>
   * If oldTimestamp is non-null and timestamps are warranted, keep the
   * oldTimestamp with the new updated value.
   *
   * @return true if the record existed and was updated; otherwise, false.
   */
  public final boolean update(String key, DbValue newValue, Long oldTimestamp) {
    final DatabaseEntry keyEntry = dbInfo.getKeyEntry(key);
    final DatabaseEntry newValueEntry = dbInfo.getValueEntry(newValue, oldTimestamp);

    return doUpdate(keyEntry, newValueEntry);
  }

  /**
   * Update the record with the given key to have the new value under a
   * transaction.
   * <p>
   * If the record doesn't exist, then return false without adding it.
   * <p>
   * If oldValue is non-null and timestamps are warranted, keep the oldValue's
   * timestamp with the new updated value.
   *
   * @return true if the record existed and was updated; otherwise, false.
   */
  private final boolean doUpdate(DatabaseEntry key, DatabaseEntry newValue) {
    return dbInfo.performTransaction(new UpdateFunction(key, newValue));
  }

  /**
   * Pop the earliest record (assuming a string key) from the database.
   */
  public final StringKeyValuePair popEarliest() {
    StringKeyValuePair result = null;

    final PopFunction popFunction = new PopFunction(true, true, false, false);
    if (dbInfo.performTransaction(popFunction)) {
      result = popFunction.getStringKeyValuePair();
    }

    return result;
  }

  /**
   * Pop the latest record (assuming a string key) from the database.
   */
  public final StringKeyValuePair popLatest() {
    StringKeyValuePair result = null;

    final PopFunction popFunction = new PopFunction(true, false, false, false);
    if (dbInfo.performTransaction(popFunction)) {
      result = popFunction.getStringKeyValuePair();
    }

    return result;
  }

  /**
   * Pop the earliest record (assuming a long key) from the database.
   */
  public final LongKeyValuePair popEarliestLong() {
    LongKeyValuePair result = null;

    final PopFunction popFunction = new PopFunction(false, true, false, false);
    if (dbInfo.performTransaction(popFunction)) {
      result = popFunction.getLongKeyValuePair();
    }

    return result;
  }

  /**
   * Pop the latest record (assuming a long key) from the database.
   */
  public final LongKeyValuePair popLatestLong() {
    LongKeyValuePair result = null;

    final PopFunction popFunction = new PopFunction(false, false, false, false);
    if (dbInfo.performTransaction(popFunction)) {
      result = popFunction.getLongKeyValuePair();
    }

    return result;
  }

  /**
   * Peek the earliest record (assuming a string key) from the database.
   */
  public final StringKeyValuePair peekEarliest() {
    StringKeyValuePair result = null;

    final PopFunction popFunction = new PopFunction(true, true, true, false);
    if (dbInfo.performTransaction(popFunction)) {
      result = popFunction.getStringKeyValuePair();
    }

    return result;
  }

  /**
   * Peek the latest record (assuming a string key) from the database.
   */
  public final StringKeyValuePair peekLatest() {
    StringKeyValuePair result = null;

    final PopFunction popFunction = new PopFunction(true, false, true, false);
    if (dbInfo.performTransaction(popFunction)) {
      result = popFunction.getStringKeyValuePair();
    }

    return result;
  }

  /**
   * Peek the earliest record (assuming a long key) from the database.
   */
  public final LongKeyValuePair peekEarliestLong() {
    LongKeyValuePair result = null;

    final PopFunction popFunction = new PopFunction(false, true, true, false);
    if (dbInfo.performTransaction(popFunction)) {
      result = popFunction.getLongKeyValuePair();
    }

    return result;
  }

  /**
   * Peek the latest record (assuming a long key) from the database.
   */
  public final LongKeyValuePair peekLatestLong() {
    LongKeyValuePair result = null;

    final PopFunction popFunction = new PopFunction(false, false, true, false);
    if (dbInfo.performTransaction(popFunction)) {
      result = popFunction.getLongKeyValuePair();
    }

    return result;
  }

  /**
   * Pop the first (primary) record (assuming a string key) from the database.
   */
  public final StringKeyValuePair popFirst() {
    StringKeyValuePair result = null;

    final PopFunction popFunction = new PopFunction(true, true, false, true);
    if (dbInfo.performTransaction(popFunction)) {
      result = popFunction.getStringKeyValuePair();
    }

    return result;
  }

  /**
   * Pop the last (primary) record (assuming a string key) from the database.
   */
  public final StringKeyValuePair popLast() {
    StringKeyValuePair result = null;

    final PopFunction popFunction = new PopFunction(true, false, false, true);
    if (dbInfo.performTransaction(popFunction)) {
      result = popFunction.getStringKeyValuePair();
    }

    return result;
  }

  /**
   * Pop the first (primary) record (assuming a long key) from the database.
   */
  public final LongKeyValuePair popFirstLong() {
    LongKeyValuePair result = null;

    final PopFunction popFunction = new PopFunction(false, true, false, true);
    if (dbInfo.performTransaction(popFunction)) {
      result = popFunction.getLongKeyValuePair();
    }

    return result;
  }

  /**
   * Pop the last (primary) record (assuming a long key) from the database.
   */
  public final LongKeyValuePair popLastLong() {
    LongKeyValuePair result = null;

    final PopFunction popFunction = new PopFunction(false, false, false, true);
    if (dbInfo.performTransaction(popFunction)) {
      result = popFunction.getLongKeyValuePair();
    }

    return result;
  }

  /**
   * Peek the first (primary) record (assuming a string key) from the database.
   */
  public final StringKeyValuePair peekFirst() {
    StringKeyValuePair result = null;

    final PopFunction popFunction = new PopFunction(true, true, true, true);
    if (dbInfo.performTransaction(popFunction)) {
      result = popFunction.getStringKeyValuePair();
    }

    return result;
  }

  /**
   * Peek the last (primary) record (assuming a string key) from the database.
   */
  public final StringKeyValuePair peekLast() {
    StringKeyValuePair result = null;

    final PopFunction popFunction = new PopFunction(true, false, true, true);
    if (dbInfo.performTransaction(popFunction)) {
      result = popFunction.getStringKeyValuePair();
    }

    return result;
  }

  /**
   * Peek the first (primary) record (assuming a long key) from the database.
   */
  public final LongKeyValuePair peekFirstLong() {
    LongKeyValuePair result = null;

    final PopFunction popFunction = new PopFunction(false, true, true, true);
    if (dbInfo.performTransaction(popFunction)) {
      result = popFunction.getLongKeyValuePair();
    }

    return result;
  }

  /**
   * Peek the last record (assuming a long key) from the database.
   */
  public final LongKeyValuePair peekLastLong() {
    LongKeyValuePair result = null;

    final PopFunction popFunction = new PopFunction(false, false, true, true);
    if (dbInfo.performTransaction(popFunction)) {
      result = popFunction.getLongKeyValuePair();
    }

    return result;
  }

  /**
   * Load all entries from the other db handle into this handle's db,
   * replacing this db's with the other's on finding duplicates.
   *
   * @return the number of entries loaded.
   */
  public final long load(DbHandle other) {
    long result = 0L;

    for (DbIterator<RawKeyValuePair> iter = new RawDbIterator(other.getDbInfo(), true); iter.hasNext(); ) {
      final RawKeyValuePair kvPair = iter.next();
      doPut(kvPair.getKey(), kvPair.getValue());
      ++result;
    }

    return result;
  }

  /**
   * Get an iterator over the database entries, where the keys are strings.
   * <p>
   * NOTE: Remember to close the iterator in a finally block when finished!
   */
  public final DbIterator<StringKeyValuePair> iterator(DbTraversal traversalType) {
    DbIterator<StringKeyValuePair> result = null;

    switch (traversalType) {
      case KEY_ORDER :
        result = new KeyOrderIterator(dbInfo, true, dbMarkerId);
        break;
      case REVERSE_KEY_ORDER :
        result = new KeyOrderIterator(dbInfo, false, dbMarkerId);
        break;
      case TIME_ORDER :
        result = new TimeOrderIterator(dbInfo, true, dbMarkerId);
        break;
      case REVERSE_TIME_ORDER :
        result = new TimeOrderIterator(dbInfo, false, dbMarkerId);
        break;
    }

    return result;
  }

  /**
   * Get an iterator over the database entries, where the keys are longs.
   * <p>
   * NOTE: Remember to close the iterator in a finally block when finished!
   */
  public final DbIterator<LongKeyValuePair> iteratorLong(DbTraversal traversalType) {
    DbIterator<LongKeyValuePair> result = null;

    switch (traversalType) {
      case KEY_ORDER :
        result = new LongKeyOrderIterator(dbInfo, true, dbMarkerId);
        break;
      case REVERSE_KEY_ORDER :
        result = new LongKeyOrderIterator(dbInfo, false, dbMarkerId);
        break;
      case TIME_ORDER :
        result = new LongTimeOrderIterator(dbInfo, true, dbMarkerId);
        break;
      case REVERSE_TIME_ORDER :
        result = new LongTimeOrderIterator(dbInfo, false, dbMarkerId);
        break;
    }

    return result;
  }
}
