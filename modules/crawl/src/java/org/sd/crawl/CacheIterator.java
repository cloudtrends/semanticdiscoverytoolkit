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


import java.io.File;
import java.util.Iterator;
import org.sd.bdb.BerkeleyDb;
import org.sd.bdb.DbHandle;
import org.sd.bdb.DbIterator;
import org.sd.bdb.DbTraversal;
import org.sd.bdb.StringKeyValuePair;

/**
 * An iterator over a crawl cache.
 * <p>
 * @author Spence Koehler
 */
public class CacheIterator implements Iterator<CacheEntry> {
  
  private File cacheDir;
  private File mapDir;
  private DbHandle dbHandle;

  private DbIterator<StringKeyValuePair> iter;
  private CacheEntry lastCacheEntry;
  private CacheEntry nextCacheEntry;

  public CacheIterator(File cacheDir) {
    this.cacheDir = cacheDir;
    this.mapDir = new File(cacheDir, CacheMap.DB_MAP_DIR);
    final BerkeleyDb db = BerkeleyDb.getInstance(mapDir, false, true);
    this.dbHandle = db.get(CacheMap.DB_MAP_NAME, false);

    this.iter = dbHandle.iterator(DbTraversal.KEY_ORDER);
    this.lastCacheEntry = null;
    this.nextCacheEntry = getNextCacheEntry();
  }

  public boolean hasNext() {
    return nextCacheEntry != null;
  }

  public CacheEntry next() {
    this.lastCacheEntry = nextCacheEntry;
    this.nextCacheEntry = getNextCacheEntry();

    return lastCacheEntry;
  }

  public void remove() {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  /**
   * Close resources associated with this iterator.
   */
  public void close() {
    iter.close();
    dbHandle.close();
  }

  /**
   * Get the crawled file associated with the last CacheEntry returned by this
   * iterator.
   */
  public File getCrawledFile() {
    File result = null;

    if (lastCacheEntry != null) {
      result = lastCacheEntry.getFile(cacheDir);
    }

    return result;
  }

  /**
   * Get the cacheDir on which this iterator is based.
   */
  public File getCacheDir() {
    return cacheDir;
  }

  private CacheEntry getNextCacheEntry() {
    CacheEntry result = null;

    while (iter.hasNext()) {
      final StringKeyValuePair kvPair = iter.next();
      if (kvPair == null) continue;

      result = (CacheEntry)kvPair.getPublishable();
      break;
    }

    return result;
  }
}
