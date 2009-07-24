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


import org.sd.io.Publishable;
import org.sd.io.FileUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Map class backed by a Berkeley DB.
 * <p>
 * @author Spence Koehler
 */
public class DbMap {

  public static final String COMPLETED_MARKER = "completed.marker";

  public static interface Loader {
    /**
     * Load the dbMap with data.
     * <p>
     * @return true if successfully loaded; otherwise, false.
     */
    public boolean load(DbMap dbMap);
  }

  public static interface ResumableLoader extends Loader {
    /**
     * Spin through the input file until an entry not already in the
     * map is found, then resume loading the file.
     */
    public boolean resumeLoad(DbMap dbMap);
  }


  /**
   * Create and load (if necessary) a DbMap instance from the given file
   * and loader into the default db dir.
   * <p>
   * Note that the file is used as a location and naming reference for the
   * backing db and the loader must use its own reference to the backing file.
   *
   * @return the loaded map or null if unable to load.
   */
  public static final DbMap createInstance(File backingFile, Loader loader) {
    return createInstance(backingFile.getName(), getDbDir(backingFile.getParentFile(), backingFile.getName()), loader);
  }

  /**
   * Create and load (if necessary) a DbMap instance from the given file
   * and loader into the default db dir.
   * <p>
   * Note that the file is used as a location and naming reference for the
   * backing db and the loader must use its own reference to the backing file.
   *
   * @return the loaded map or null if unable to load.
   */
  public static final DbMap createInstance(File backingFile, ResumableLoader loader) {
    return createInstance(backingFile.getName(), getDbDir(backingFile.getParentFile(), backingFile.getName()), loader);
  }

  /**
   * Create and load (if necessary) a DbMap instance from the given file
   * and loader into the default db dir.
   * <p>
   * Note that the file is used as a location and naming reference for the
   * backing db and the loader must use its own reference to the backing file.
   *
   * @return the loaded map or null if unable to load.
   */
  public static final DbMap createInstance(File fileDir, String filename, Loader loader) {
    return createInstance(filename, getDbDir(fileDir, filename), loader);
  }

  /**
   * Create and load (if necessary) a DbMap instance at the given dbDir
   * with the given name using the loader.
   */
  public static final DbMap createInstance(String dbName, File dbDir, Loader loader) {
    final DbMap result = new DbMap(dbDir, dbName);
    boolean loaded = false;
    try {
      loaded = result.loadAllOrNothing(loader);
    }
    finally {
      if (!loaded) result.close();
    }
    return loaded ? result : null;
  }

  public static final DbMap createInstance(String dbName, File dbDir, ResumableLoader loader) {
    final DbMap result = new DbMap(dbDir, dbName);
    boolean loaded = false;
    try {
      loaded = result.loadComplete(loader);
    }
    finally {
      if (!loaded) result.close();
    }
    return loaded ? result : null;
  }

  /**
   * Get the default backing dbDir for a file.
   * <p>
   * NOTE: The default dbDir is fileDir/filename.bdb
   */
  public static final File getDbDir(File fileDir, String filename) {
    return new File(fileDir, filename + ".bdb");
  }

  /**
   * Mark the dbLocation as a completed map.
   * <p>
   * Note that this will open backing DB's in read-only mode so that they can
   * be shared, but they can no longer be added to.
   */
  public static final void markAsCompleted(File dbLocation) {
    final File completedFile = new File(dbLocation, COMPLETED_MARKER);
    if (!completedFile.exists()) {
      try {
        FileUtil.touch(completedFile);
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }


  private File dbLocation;
  private DbHandle dbHandle;
  private boolean exists;
  private File completedFile;
  private boolean completed;

  /**
   * Open the backing DB on construction.
   */
  public DbMap(File dbLocation, String dbName) {
    this.exists = dbLocation.exists();
    this.completedFile = new File(dbLocation, COMPLETED_MARKER);
    this.completed = completedFile.exists();
    final BerkeleyDb db = BerkeleyDb.getInstance(dbLocation, completed, true);
    this.dbLocation = dbLocation;
    this.dbHandle = db.get(dbName, false);
  }

  /**
   * Get the number of entries in this map.
   */
  public long size() {
    return dbHandle.getNumRecords();
  }

  /**
   * Get the map's backing handle.
   */
  public DbHandle getDbHandle() {
    return dbHandle;
  }

  /**
   * Put the value for the long key.
   */
  public void put(long key, DbValue dbValue) {
    dbHandle.put(key, dbValue);
  }

  /**
   * Put the value for the String key.
   */
  public void put(String key, DbValue dbValue) {
    dbHandle.put(key, dbValue);
  }

  /**
   * Put the value for the long key.
   */
  public void put(long key, Publishable publishable) {
    dbHandle.put(key, new DbValue(publishable));
  }

  /**
   * Put the value for the String key.
   */
  public void put(String key, Publishable publishable) {
    dbHandle.put(key, new DbValue(publishable));
  }

  /**
   * Get the value for the long key.
   */
  public DbValue get(long key) {
    return dbHandle.get(key);
  }

  /**
   * Get the value for the String key.
   */
  public DbValue get(String key) {
    return dbHandle.get(key);
  }

  /**
   * Mark this map as complete, or fully loaded.
   */
  public void markAsCompleted() {
    markAsCompleted(dbLocation);
  }

  /**
   * Load all of the data through the loader.
   * <p>
   * If the bdb directory exists, then
   *   If the completed marker file exists assume the file has been completely loaded;
   *   otherwise, skip through the file and start loading where it left off.
   * Otherwise, begin loading the whole file.
   * <p>
   * When the file has been completely loaded, create the "complete marker" file
   * in the bdb directory.
   */
  public boolean loadComplete(ResumableLoader loader) {
    boolean result = true;

    if (exists) {
      if (completed) return result;

      result = loader.resumeLoad(this);
    }
    else {
      result = loader.load(this);
    }

    // create marker file to signal completion.
    if (result) {
      try {
        final BufferedWriter writer = FileUtil.getWriter(completedFile);
        writer.close();
      }
      catch (IOException e) {
        System.err.println(new Date() + ": WARNING -- Unable to create completed marker file '" +
                           completedFile + "'!");
        e.printStackTrace(System.err);
      }
    }

    return result;
  }

  /**
   * Load all of the data, or nothing. Assume that if the directory
   * exists, all has already been loaded.
   * <p>
   * If all data is not successfully loaded, delete the backing db.
   */
  public boolean loadAllOrNothing(Loader loader) {
    if (exists) {
      markAsCompleted();
      return true;
    }

    boolean result = false;

    try {
      result = loader.load(this);
      if (result) markAsCompleted();
    }
    finally {
      if (!result) {
        destroy();
      }
    }

    return result;
  }

  /**
   * Delete the backing db.
   */
  public void destroy() {
    close();
    dbHandle.getBerkeleyDb().delete();
  }

  /**
   * Close the backing db.
   */
  public void close() {
    dbHandle.getBerkeleyDb().close();
  }
}
