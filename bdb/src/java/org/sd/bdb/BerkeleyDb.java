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


import org.sd.io.FileUtil;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wrapper around a BerkeleyDB.
 * <p>
 * @author Spence Koehler
 */
public class BerkeleyDb {

  private static final Map<String, BerkeleyDb> roLoc2db = new HashMap<String, BerkeleyDb>();  // read-only
  private static final Map<String, BerkeleyDb> rwLoc2db = new HashMap<String, BerkeleyDb>();  // read-write

  /**
   * Create and/or get the BerkeleyDb instance at the given location.
   */
  public static final BerkeleyDb getInstance(File envLocation, boolean readOnly) {
    return getInstance(envLocation, readOnly, false);
  }

  /**
   * Create and/or get the BerkeleyDb instance at the given location.
   */
  public static final BerkeleyDb getInstance(File envLocation, boolean readOnly, boolean forceTransactional) {
    final Map<String, BerkeleyDb> loc2db = readOnly ? roLoc2db : rwLoc2db;

    final String key = envLocation.getAbsolutePath();
    BerkeleyDb result = loc2db.get(key);
    if (result == null) {
      // remove persistent lock if present.
      if (!readOnly) {
        final File lock = new File(envLocation, "je.lck");
        if (lock.exists()) lock.delete();
      }

      result = new BerkeleyDb(envLocation, readOnly, forceTransactional);
      loc2db.put(key, result);
    }

    return result;
  }


  private File envLocation;
  private boolean readOnly;
  private boolean forceTransactional;  // i.e. for use in multiple threads

  private Environment _environment;
  private Map<String, DbHandle> _name2dbHandle;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  private static final Object envOpenMutex = new Object();
  private static final Object dbOpenMutex = new Object();

  private BerkeleyDb(File envLocation, boolean readOnly, boolean forceTransactional) {
    this.envLocation = envLocation;
    this.readOnly = readOnly;
    this.forceTransactional = forceTransactional;

    this._environment = null;
    this._name2dbHandle = null;
  }

  /**
   * Get the names of databases under this environment.
   */
  public final String[] getDatabaseNames() {

    // open environment if necessary
    if (_environment == null) openEnvironment();

    List dbNames = null;

    try {
      dbNames = _environment.getDatabaseNames();
    }
    catch (DatabaseException e) {
      throw new IllegalStateException(e);
    }

    final String[] result = new String[dbNames.size()];

    int index = 0;
    for (Iterator it = dbNames.iterator(); it.hasNext(); ) {
      result[index++] = (String)it.next();
    }

    return result;
  }

  /**
   * Determine whether the given database already exists for the environment.
   */
  public final boolean hasDatabaseName(String dbName) {

    // open environment if necessary
    if (_environment == null) openEnvironment();

    List dbNames = null;

    try {
      dbNames = _environment.getDatabaseNames();
    }
    catch (DatabaseException e) {
      throw new IllegalStateException(e);
    }

    return dbNames.contains(dbName);
  }

  /**
   * Get the environment location for this instance.
   */
  public final File getEnvLocation() {
    return envLocation;
  }

  /**
   * Delete this database.
   */
  public final void delete() {
    FileUtil.deleteDir(envLocation);
  }

  /**
   * Get (opening if needed) a handle to a database (by name) within this
   * instance's environment.
   * <p>
   * If the types of keys (string -vs- long) are to be auto-detected, then
   * by convention, dbNames for databases with long keys should end in "_id";
   * otherwise, the default assumption of the key type will be 'string'.
   * <p>
   * If timestamped is null, then if the database exists timestamped will be
   * inferred by the existence of a timestamped database; otherwise, the new
   * database will be opened WITHOUT a timestamped database (by default).
   */
  public final synchronized DbHandle get(String dbName, Boolean timestamped, String dbMarkerId) {
    DbHandle result = null;

    // open environment if necessary
    if (_environment == null) openEnvironment();

    // set default dbName if necessary
    if (dbName == null) dbName = envLocation.getName();

    // open database if necessary
    if (_name2dbHandle == null || _name2dbHandle.get(dbName) == null) {
      result = openDatabase(dbName, timestamped, dbMarkerId);
    }
    // otherwise, retrieve existing database
    else {
      result = _name2dbHandle.get(dbName);
    }

    return result;
  }

  public final synchronized DbHandle get(String dbName, Boolean timestamped) {
    return get(dbName, timestamped, null);
  }

  /**
   * Get a list of all databases retrieved from this instance.
   *
   * @return the database names or null if none have been retrieved.
   */
  public final Set<String> listDatabaseNames() {
    return _name2dbHandle == null ? null : _name2dbHandle.keySet();
  }

  /**
   * Get all dbHandles retrieved through this instance.
   *
   * @return the dbHandles or null if none have been retrieved.
   */
  public final Collection<DbHandle> getAllDbHandles() {
    return _name2dbHandle == null ? null : _name2dbHandle.values();
  }

  /**
   * Determine whether this instance has been closed.
   */
  public boolean isClosed() {
    return closed.get();
  }

  /**
   * Close all of this instance's databases and environment.
   * <p>
   * Note that individual databases can be closed separately through their
   * dbHandle acquired when retrieved through 'get'.
   */
  public void close() {
    if (!closed.getAndSet(true)) {
      if (_name2dbHandle != null) {
        for (DbHandle dbHandle : _name2dbHandle.values()) {
          dbHandle.close();
        }
      }
      if (_environment != null) {
        closeEnvironment();
      }

      final Map<String, BerkeleyDb> loc2db = readOnly ? roLoc2db : rwLoc2db;
      loc2db.remove(envLocation.getAbsolutePath());
    }
  }

	void remove(DbHandle dbHandle) {
		if (_name2dbHandle != null) {
			_name2dbHandle.remove(dbHandle.getDbInfo().getDbName());
		}
	}

  private final void openEnvironment() {
    synchronized (envOpenMutex) {
      if (_environment == null) {
        try {
          final EnvironmentConfig envConfig = new EnvironmentConfig();
          envConfig.setReadOnly(readOnly);
          envConfig.setAllowCreate(!readOnly);
          envConfig.setTransactional(forceTransactional || !readOnly);
          envConfig.setTxnWriteNoSync(!readOnly);
          envConfig.setCacheSize(32 * 1024 * 1024);  // limit cache size to 32M

          if (/*!readOnly &&*/ !envLocation.exists()) envLocation.mkdirs();
          _environment = new Environment(envLocation, envConfig);
        }
        catch (DatabaseException e) {
          throw new IllegalStateException(e);
        }
      }
    }
  }

  private final void closeEnvironment() {
    try {
      _environment.close();
    }
    catch (DatabaseException e) {
      throw new IllegalStateException(e);
    }
  }

  private final DbHandle openDatabase(String dbName, Boolean timestamped, String dbMarkerId) {
    DbHandle result = null;

    synchronized (dbOpenMutex) {
      if (_name2dbHandle == null || _name2dbHandle.get(dbName) == null) {
        if (_name2dbHandle == null) _name2dbHandle = new HashMap<String, DbHandle>();
        try {
          final DatabaseConfig dbConfig = new DatabaseConfig();
          dbConfig.setReadOnly(readOnly);
          dbConfig.setSortedDuplicates(false);
          dbConfig.setAllowCreate(!readOnly);
          dbConfig.setTransactional(forceTransactional || !readOnly);

          final Database database = _environment.openDatabase(null, dbName, dbConfig);
          SecondaryDatabase tsDb = null;

          final String tsDbName = dbName + "_timestamped";
          if (timestamped == null) timestamped = hasDatabaseName(tsDbName);

          if (timestamped) {
            // Open secondary database
            final SecondaryConfig secConfig = new SecondaryConfig();
            secConfig.setReadOnly(readOnly);
            secConfig.setSortedDuplicates(true);  // expect duplicate secondary keys
            secConfig.setAllowCreate(!readOnly);
            secConfig.setTransactional(forceTransactional || !readOnly);
            secConfig.setKeyCreator(TimestampedValue.KEY_CREATOR);

            tsDb = _environment.openSecondaryDatabase(null, tsDbName, database, secConfig);
          }

          result = new DbHandle(new DbInfo(this, _environment, dbName, database, tsDb), dbMarkerId);
          _name2dbHandle.put(dbName, result);
        }
        catch (DatabaseException e) {
          throw new IllegalStateException(e);
        }
      }
    }

    return result;
  }
}
