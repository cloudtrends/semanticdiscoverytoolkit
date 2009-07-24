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

import org.sd.io.FileUtil;
import org.sd.util.PropertiesParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Utility to crawl (fetch) a page through a url.
 * <p>
 * @author Spence Koehler
 */
public class PageCrawler {
  
  private CrawlSettings crawlSettings;
  private LineFixer lineFixer;

  /**
   * Construct a new page crawler with default settings.
   */
  public PageCrawler() {
    this(new CrawlSettings());
  }

  /**
   * Construct a new page crawler with this given settings to use by default.
   */
  public PageCrawler(CrawlSettings crawlSettings) {
    this.crawlSettings = crawlSettings;
  }

  /**
   * Properties:
   * <ul>
   * <li>connectTimeout -- (default=4500) Connect timeout in millis.</li>
   * <li>readTimeout -- (default=4500) Read timeout in millis.</li>
   * <li>numRetries -- (default=3) Number of times to retry before giving up.</li>
   * <li>forceRefreshTime -- If this amount of time has past since a cached
   *                         item was stored in the cache, then fetch it anew.
   *                         If 0 (default), always use the cached item.</li>
   * <li>allowRedirects -- (default=true) Allow automatic redirects through the url connection.</li>
   * <li>skipContent -- If "true" (default="false"), then only fetch header info (ignoring cache).</li>
   * <li>verbose -- If "true" (default="false"), then print progress information to stdout.</li>
   * </ul>
   */
  public PageCrawler(Properties properties) {
    this(new CrawlSettings(properties));
  }

  /**
   * Get this instance's crawl settings.
   */
  public CrawlSettings getCrawlSettings() {
    return crawlSettings;
  }

  /**
   * Fetch the url's page, with no referring url or caching.
   */
  public CrawledPage fetch(String url) {
    return fetch(new UrlData(url));
  }

  /**
   * Fetch the url's page, with no caching.
   */
  public CrawledPage fetch(UrlData urlData) {
    return fetch(urlData, null);
  }

  /**
   * Fetch the url's page.
   *
   * @param urlData  UrlData of page to fetch.
   * @param overrideSettings  Settings to use overriding this crawler's settings.
   */
  public CrawledPage fetch(UrlData urlData, CrawlSettings overrideSettings) {

    CrawledPage result = null;

    if (urlData.getError() != null) {
      // build a result that indicates that the url is bad.
      result = new CrawledPage(urlData.getCleanString(), urlData.getError());
    }
    else {
      result = doFetch(urlData, overrideSettings);
    }

    return result;
  }

  /**
   * If this crawler is caching pages, update the cached version of the given
   * page with its current state.
   */
  public void updateCache(CrawledPage crawledPage, CrawlSettings overrideSettings, boolean updateMetaData, boolean updateContent) {
    final CrawlSettings theSettings = overrideSettings != null ? overrideSettings : crawlSettings;
    final File cacheDir = theSettings.getCacheDir();
    if (cacheDir != null) {
      final CrawlCache crawlCache = CrawlCache.getInstance(cacheDir);
      if (crawlCache != null) {
        if (updateContent) {
          try {
            crawlCache.storePageInCache(crawledPage, null);

            if (theSettings.verbose()) {
              System.out.println(new Date() + ": PageCrawler updated '" + crawledPage.getUrl() + "' in cache.");
            }
          }
          catch (IOException e) {
            if (theSettings.verbose()) {
              System.err.println(new Date() + ": ERROR PageCrawler can't update page in cache! (cacheDir=" + cacheDir + ", url=" + crawledPage.getUrl() + ")");
              e.printStackTrace(System.err);
            }
          }
        }
        if (updateMetaData) {
          if (crawlCache.writeMetaData(crawledPage)) {
            if (theSettings.verbose()) {
              System.out.println(new Date() + ": PageCrawler updated '" + crawledPage.getUrl() + "' metaData in cache.");
            }
          }
        }
      }
    }
  }

  public void setLineFixer(LineFixer lineFixer) {
    this.lineFixer = lineFixer;
  }

  /**
   * Do the work of fetching the URL.
   */
  private final CrawledPage doFetch(UrlData urlData, CrawlSettings overrideSettings) {
    CrawledPage result = null;
    CrawlCache crawlCache = null;

    final String urlString = urlData.getCleanString();  // normalized string for cache key

    final CrawlSettings theSettings = overrideSettings != null ? overrideSettings : crawlSettings;
    final File cacheDir = theSettings.getCacheDir();

    // try to retrieve from cache
    if (cacheDir != null && !theSettings.skipContent()) {
      crawlCache = CrawlCache.getInstance(cacheDir);
      final Long overrideForceRefreshTime = urlData.getOverrideForceRefreshTime();
      final long forceRefreshTime = (overrideForceRefreshTime != null) ? overrideForceRefreshTime : theSettings.getForceRefreshTime();
      result = crawlCache.retrieveCachedPage(urlString, forceRefreshTime);
      if (result != null) {
        if (!result.getFile().exists()) {
          System.out.println(new Date() + ": PageCrawler lost cached page for '" +
                             urlString + "'. file=" + result.getFile() +
                             " ...recrawling.");
          result = null;
        }
        else {
          if (theSettings.verbose()) {
            System.out.println(new Date() + ": PageCrawler retrieved '" + urlString + "' from cache.");
          }
          return result;
        }
      }
    }

    // prepare for writing content if warranted
    final CacheHelper cacheHelper = new CacheHelper(urlData, theSettings, crawlCache);

    // do the work of grabbing the page
    result = grabPage(urlData, theSettings, cacheHelper);

    // store in cache if needed
    cacheHelper.addMapEntry(result);  // create map entry for result

    return result;
  }

  private final CrawledPage grabPage(UrlData urlData, CrawlSettings theSettings, CacheHelper cacheHelper) {
    CrawledPage result = null;

    // check robots.txt
    final RobotsDeniedException okayToCrawl = theSettings.canCrawl(urlData, this);

    if (okayToCrawl == null) {
      // enforce crawl delay
      theSettings.enforceSiteDelay(urlData);

      // grab the page
      result = doGrabPage(urlData, theSettings, cacheHelper);
    }
    else {
      // build a result that indicates that our robot was denied access to the page.
      result = new CrawledPage(urlData.getCleanString(), okayToCrawl);
    }

    return result;
  }

  private final CrawledPage doGrabPage(UrlData urlData, CrawlSettings theSettings, CacheHelper cacheHelper) {
    CrawledPage result = null;

    HttpURLConnection conn = null;
    InputStream cis = null;
    BufferedReader pageReader = null;

    final URL url = urlData.getUrl();

    for (int tryNum = 0; result == null && tryNum <= theSettings.getNumRetries(); ++tryNum) {
      try {
        conn = (HttpURLConnection)url.openConnection();
        conn.setInstanceFollowRedirects(theSettings.allowRedirects());
        conn.setRequestProperty("User-Agent", theSettings.getUserAgent());
        conn.setConnectTimeout(theSettings.getConnectTimeout());
        conn.setReadTimeout(theSettings.getReadTimeout());

        final long startDownloadHeaderTime = System.currentTimeMillis();
        final int responseCode = conn.getResponseCode();
        final String responseMessage = conn.getResponseMessage();
        final String contentType = conn.getContentType();
        final int contentLength = conn.getContentLength();
        final long lastModified = conn.getLastModified();
        final long endDownloadHeaderTime = System.currentTimeMillis();

        // build a result that sets page header info
        result = new CrawledPage(urlData.getCleanString(), responseCode, responseMessage,
                                 contentType, contentLength, lastModified,
                                 urlData.getReferringUrl(), urlData.getReferringLinkText(),
                                 tryNum, endDownloadHeaderTime - startDownloadHeaderTime);

        if (!theSettings.skipContent()) {
          // read content
          final long startDownloadContentTime = System.currentTimeMillis();
          cis = conn.getInputStream();
          pageReader = new BufferedReader(new InputStreamReader(cis));

          final StringBuilder content = new StringBuilder();
          String line;
          while ((line = pageReader.readLine()) != null) {
            if (lineFixer != null) line = lineFixer.fixLine(line);
            content.append(line).append('\n');
          }

          final long endDownloadContentTime = System.currentTimeMillis();
          result.setContent(content.toString());
          result.setDownloadContentTime(endDownloadContentTime - startDownloadContentTime);
          cacheHelper.writeContent(result, content.toString());
        }
      }
      catch (Exception e) {
        if (result != null) result.setError(e);
        else result = new CrawledPage(urlData.getCleanString(), e);  // build an instance that identifies downloading error

//         if (verbose) {
//           System.err.println(new Date() + ": ERROR PageCrawler.fetch(" + url + ")");
//           e.printStackTrace(System.err);
//         }
      }
      finally {
        if (pageReader != null) {
          try {
            pageReader.close();
          }
          catch (IOException ioe) {
            if (result != null) result.setError(ioe);
            else result = new CrawledPage(urlData.getCleanString(), ioe);  // build an instance that identifies closing reader error

//             if (verbose) {
//               System.err.println(new Date() + ": ERROR PageCrawler.fetch(" + url + ") page reader close");
//               ioe.printStackTrace(System.err);
//             }
          }
        }

        if (cis != null) {
          try {
            cis.close();
          }
          catch (IOException ioe) {
            if (result != null) result.setError(ioe);
            else result = new CrawledPage(urlData.getCleanString(), ioe);  // build an instance that identifies closing stream error

//             if (verbose) {
//               System.err.println(new Date() + ": ERROR PageCrawler.fetch(" + url + ") connection stream close");
//               ioe.printStackTrace(System.err);
//             }
          }
        }

        if (conn != null) conn.disconnect();
      }
    }

    return result;
  }


  /**
   * Helper for running the crawler from the command line.
   */
  private static final void doMain(PageCrawler crawler, String arg) {
    final CrawledPage page = crawler.fetch(arg);

    if (page == null) {
      System.out.println(arg + " --> FAILED!");
    }
    else {
      System.out.println(arg);
      System.out.println("\tresponseCode=" + page.getResponseCode());
      System.out.println("\tresponseMessage=" + page.getResponseMessage());
      if (page.hasContent()) {
        System.out.println("\tcontent=\n" + page.getContent());
      }
      if (page.hasError()) {
        System.out.println("\terror=");
        page.getError().printStackTrace(System.out);
      }
      final List<Link> links = page.getLinks();
      if (links.size() > 0) {
        System.out.println("\tfound " + links.size() + " links:");
        int linkNum = 0;
        for (Link link : links) {
          System.out.println((linkNum++) + ": " + link);
        }
      }
      System.out.println();
    }
  }


  /**
   * Cache helper class to write content (if warranted) and add map entries.
   * <p>
   * @author Spence Koehler
   */
  private static final class CacheHelper {
    private UrlData urlData;
    private CrawlSettings crawlSettings;
    private CrawlCache crawlCache;
    private boolean addedEntry;

    CacheHelper(UrlData urlData, CrawlSettings crawlSettings, CrawlCache crawlCache) {
      this.urlData = urlData;
      this.crawlSettings = crawlSettings;
      this.crawlCache = crawlCache;
      this.addedEntry = false;
    }

    /**
     * Write the content to the cache if warranted.
     */
    void writeContent(CrawledPage crawledPage, String content) throws IOException {
      if (crawlSettings.skipContent()) return;  // skipping content

      if (crawlCache == null) {
        // won't dump content, but will set it on the instance for consumers
        crawledPage.setContent(content);
      }
      else {
        crawlCache.storePageInCache(crawledPage, content);
        addedEntry = true;

        if (crawlSettings.verbose()) {
          System.out.println(new Date() + ": PageCrawler stored '" + urlData.toString() + "' in cache.");
        }
      }
    }

    /**
     * Create a map entry for the result (if not already done).
     * <p>
     * Note that if the result is null, we couldn't create a connection and
     * still want to record an appropriate entry.
     */
    void addMapEntry(CrawledPage crawledPage) {
      // write entry in cache map for the result here (if warranted)
      if (crawlCache != null && !addedEntry) {
        if (crawledPage == null) {
          // couldn't get a connection. write an appropriate entry in the cache map.
          crawledPage = new CrawledPage(urlData.getCleanString(),
                                        new IllegalStateException("Couldn't connect! (" +
                                                                  crawlSettings.getNumRetries() +
                                                                  " tries)"));
        }
        crawlCache.writeMetaData(crawledPage);
      }
    }
  }


  public static final void main(String[] args) throws IOException {
    // properties:
    //
    //   cacheDir -- (optional, default=null) dir for caching crawl
    //
    //   args: batchFiles with urls or urls to fetch
    //
    //   connectTimeout -- (default=4500) Connect timeout in millis.
    //   readTimeout -- (default=4500) Read timeout in millis.
    //   numRetries -- (default=3) Number of times to retry before giving up.
    //   forceRefreshTime -- If this amount of time has past since a cached
    //                       item was stored in the cache, then fetch it anew.
    //                       If 0 (default), always use the cached item.
    //   allowRedirects -- (default=true) Allow automatic redirects through the url connection.
    //   skipContent -- If "true" (default="false"), then only fetch header info (ignoring cache).
    //   verbose -- If "true" (default="true"), then print progress information to stdout.
    //


    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();
    args = pp.getArgs();

    final PageCrawler crawler = new PageCrawler(properties);

    if (properties.getProperty("verbose") == null) {
      // flip the default verbosity from false to true when run through this main.
      crawler.getCrawlSettings().setVerbose(true);
    }

    for (String arg : args) {
      if (new File(arg).exists()) {
        final BufferedReader reader = FileUtil.getReader(arg);
        String line = null;
        while ((line = reader.readLine()) != null) {
          if (!"".equals(line) && !line.startsWith("#")) {
            doMain(crawler, line);
          }
        }
      }
      else {
        doMain(crawler, arg);
      }
    }
  }
}
