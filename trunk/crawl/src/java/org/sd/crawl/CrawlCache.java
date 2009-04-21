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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstraction for caching crawled pages.
 * <p>
 * @author Spence Koehler
 */
public class CrawlCache {
  
  private static final Map<String, CrawlCache> INSTANCES = new HashMap<String, CrawlCache>();

  public static final CrawlCache getInstance(File cacheDir) {
    CrawlCache result = null;

    if (cacheDir != null) {
      final String key = cacheDir.getAbsolutePath();
      synchronized (INSTANCES) {
        result = INSTANCES.get(key);
        if (result == null) {
          result = new CrawlCache(cacheDir);
          INSTANCES.put(key, result);
        }
      }
    }

    return result;
  }

  /**
   * Close the instance associated with the given cache dir (if it is open).
   */
  public static final void closeInstance(File cacheDir) {
    if (cacheDir != null) {
      final String key = cacheDir.getAbsolutePath();
      final CrawlCache crawlCache = INSTANCES.get(key);
      if (crawlCache != null) {
        crawlCache.close();
      }
    }
  }

  private File cacheDir;
  private CacheMap cacheMap;

  private CrawlCache(File cacheDir) {
    this.cacheDir = cacheDir;
    this.cacheMap = new CacheMap(cacheDir);
  }

  /**
   * Roll over to the next subdir so that unretrievable pages will be cached
   * in a fresh subdirectory.
   *
   * @return the name of the new subdir (within cacheDir).
   */
  public String rollSubdir() {
    return cacheMap.rollSubdir();
  }

  /**
   * Retrieve the page in this cache corresponding to the given
   * url unless it is older (crawled longer ago) than the forceRefreshTime
   * (magnitude).
   * <p>
   * If the page is not cached or too old, return null without deleting
   * the page. Let the page be overwritten if the consumer chooses to
   * recrawl and re-cache the page.
   */
  public CrawledPage retrieveCachedPage(String url, long forceRefreshTime) {
    CrawledPage result = null;

    CacheEntry cacheEntry = cacheMap.findEntry(url);

    if (cacheEntry != null) {
      if (forceRefreshTime > 0) {
        if (cacheEntry.getMetaData().getAge() > forceRefreshTime) {
          cacheEntry = null;
        }
      }
    }

    if (cacheEntry != null) {
      final File cacheFile = cacheEntry.getFile(cacheDir);

      // load cachedPage from cacheFile and return.
      result = new CrawledPage(cacheFile, cacheEntry.getMetaData());
    }

    return result;
  }

  /**
   * Store the page in this cache, overwriting any existing.
   * <p>
   * If originalContent is null, write the "processed" content from the crawledPage;
   * otherwise, write the originalContent.
   */
  public void storePageInCache(CrawledPage crawledPage, String originalContent) throws IOException {
    final CacheEntry entry = cacheMap.addEntry(crawledPage);
    if (entry != null) {
      crawledPage.dumpContent(entry.getFile(cacheDir), originalContent);
    }
  }

  /**
   * Update the page's meta data in the cache map.
   *
   * @return true if the meta data was updated, which only happens when there
   *         is a difference; otherwise, false.
   */
  public boolean writeMetaData(CrawledPage crawledPage) {
    return cacheMap.writeMetaData(crawledPage);
  }

  /**
   * Close this cache.
   */
  public void close() {
    synchronized (INSTANCES) {
      INSTANCES.remove(cacheDir);
    }
    cacheMap.close();
  }
}
