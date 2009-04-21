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
package org.sd.crawl;


import org.sd.bdb.BerkeleyDb;
import org.sd.bdb.DbHandle;
import org.sd.bdb.DbValue;
import org.sd.cio.FilenameGenerator;
import org.sd.cio.FilenameGenerator.CreateFlag;
import org.sd.cio.NumericFilenameGenerator;
import org.sd.io.FileUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstraction for a cache map.
 * <p>
 * @author Spence Koehler
 */
public class CacheMap {

  public static final int MAX_FILES_PER_DIR = 1024;

  public static final String DB_MAP_DIR = ".dbmap";
  public static final String DB_MAP_NAME = "CacheMap";

  private File cacheDir;
  private int maxFilesPerDir;
  private File mapDir;
  private DbHandle dbHandle;

  //
  // Generate dirs under cacheDir named as numbers left padded to 5 digits with zeros, starting with 1.
  // Within each dir, create files with names as numbers left padded to 7 digits with zeros.
  // Restart counting filenames within each subdirectory from 1.
  // Allow a manual "roll" to force creating a new dir.
  //

  private final NumericFilenameGenerator dirgen = new NumericFilenameGenerator("", 1, 5);
  private final NumericFilenameGenerator filegen = new NumericFilenameGenerator(".crawl.gz", 1, 7);

  private FilenameGenerator dirNameGenerator;   // generate dir names in cache dir.
  private FilenameGenerator fileNameGenerator;  // generate files in current dir.
  private File curSubdir;

  private final Object NAME_MUTEX = new Object();

  public CacheMap(File cacheDir) {
    this(cacheDir, MAX_FILES_PER_DIR);
  }

  public CacheMap(File cacheDir, int maxFilesPerDir) {
    this.cacheDir = cacheDir;
    this.maxFilesPerDir = maxFilesPerDir;
    this.mapDir = new File(cacheDir, DB_MAP_DIR);
    final BerkeleyDb db = BerkeleyDb.getInstance(mapDir, false, true);
    this.dbHandle = db.get(DB_MAP_NAME, false);

    initDirs();
  }

  /**
   * Roll over to the next subdir.
   * 
   * @return the name of the new subdir.
   */
  public final synchronized String rollSubdir() {
    final String subdir = dirNameGenerator.getNextLocalName(CreateFlag.DIR);
    this.curSubdir = new File(cacheDir, subdir);
    this.fileNameGenerator = new FilenameGenerator(curSubdir, filegen, maxFilesPerDir);

    return subdir;
  }

  private final void initDirs() {
    this.dirNameGenerator = new FilenameGenerator(cacheDir, dirgen, 0);
    this.fileNameGenerator = null;

    this.curSubdir = dirNameGenerator.getLastFile();

    String subdir = null;
    if (curSubdir == null) { // first time in this cacheDir
      subdir = dirNameGenerator.getNextLocalName(CreateFlag.DIR);
    }
    else { // see whether the last subdir is full
      this.fileNameGenerator = new FilenameGenerator(curSubdir, filegen, maxFilesPerDir);
      if (fileNameGenerator.getNextLocalName(null) == null) {
        // need to increment dir.
        subdir = dirNameGenerator.getNextLocalName(CreateFlag.DIR);
        this.fileNameGenerator = null;
      }
    }

    if (subdir != null) {
      this.curSubdir = new File(cacheDir, subdir);
    }

    if (this.fileNameGenerator == null) {
      this.fileNameGenerator = new FilenameGenerator(curSubdir, filegen, maxFilesPerDir);
    }
  }

  /**
   * Find the entry for the clean url.
   * <p>
   * NOTE: A clean url is that which is returned by URL.toString.
   */
  public CacheEntry findEntry(String cleanUrl) {
    return lookupEntry(cleanUrl);
  }

  /**
   * Add or update the entry 
   * <p>
   * @return the added or updated entry.
   */
  public CacheEntry addEntry(CrawledPage crawledPage) {
    CacheEntry entry = lookupEntry(crawledPage.getUrl());

    if (entry == null) {
      final String[] localName = getNextLocalName();
      entry = new CacheEntry(crawledPage, localName[0], localName[1]);
    }
    else {
      if (!entry.getMetaData().update(crawledPage)) {
        entry = null;
      }
    }
    
    // add or update the entry
    if (entry != null) {
      synchronized (dbHandle) {
        dbHandle.put(crawledPage.getUrl(), new DbValue(entry));
      }
    }

    return entry;
  }

  /**
   * (Over)Write the crawled page's meta data if there is a difference.
   */
  public boolean writeMetaData(CrawledPage crawledPage) {
    boolean result = false;

    CacheEntry entry = lookupEntry(crawledPage.getUrl());

    if (entry != null && entry.getMetaData().update(crawledPage)) {
      synchronized (dbHandle) {
        dbHandle.put(crawledPage.getUrl(), new DbValue(entry));
      }
      result = true;
    }

    return result;
  }

  /**
   * Get the next local dir and name.
   */
  private final String[] getNextLocalName() {

    String nextFilename = fileNameGenerator.getNextLocalName(CreateFlag.FILE);

    synchronized (NAME_MUTEX) {
      while (nextFilename == null) {
        // need to increment
        rollSubdir();
        nextFilename = fileNameGenerator.getNextLocalName(CreateFlag.FILE);
      }
    }

    return new String[]{curSubdir.getName(), nextFilename};
  }

  /**
   * Delete the entry for the given url.
   */
  public void deleteEntry(String cleanUrl) {
    synchronized (dbHandle) {
      dbHandle.delete(cleanUrl);
    }
  }

  /**
   * Close the backing db.
   */
  public void close() {
    synchronized (dbHandle) {
      dbHandle.getBerkeleyDb().close();
    }
  }

  private final CacheEntry lookupEntry(String cleanUrl) {
    CacheEntry result = null;

    final DbValue dbValue = dbHandle.get(cleanUrl);
    if (dbValue != null) {
      result = (CacheEntry)dbValue.getPublishable();
    }

    return result;
  }
}
