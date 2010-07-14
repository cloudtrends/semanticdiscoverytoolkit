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


import org.sd.text.DetailedUrl;
import org.sd.util.LRU;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Container for crawl parameters and settings.
 * <p>
 * @author Spence Koehler
 */
public class CrawlSettings {
  
  public static final int DEFAULT_CONNECT_TIMEOUT = 4500;
  public static final int DEFAULT_READ_TIMEOUT = 4500;
  public static final int DEFAULT_NUM_RETRIES = 3;
  public static final boolean DEFAULT_ALLOW_REDIRECTS = true;
  public static final long DEFAULT_CRAWL_DELAY = 5000;
  public static final int DEFAULT_MAX_NUM_ROBOTS = 20;
  public static final String DEFAULT_USER_AGENT = "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)";
  public static final String DEFAULT_ROBOT_NAME = "semanticdiscovery";


  private int connectTimeout;
  private int readTimeout;
  private long forceRefreshTime;  // force a refresh if the cached file was retrieved more than this time ago. 0 means never refresh.
  private int numRetries;
  private boolean allowRedirects;
  private boolean skipContent;
  private boolean verbose;
  private File cacheDir;
  private String userAgent;

  private boolean ignoreRobots;
  private long crawlDelay;       // delay to apply when revisiting a site
  private long maxCrawlDelay;    // maximum crawl delay
  private int maxNumRobots;      // at least as many as crawling threads
  private String robotName;      // as would be found in a user-agent string in a robots.txt

  private Map<String, Long> host2time;
  private boolean cleaningUpDelays;
  private LRU<String, RobotsDotText> host2robots;

  /**
   * Create an instance with default values.
   */
  public CrawlSettings() {
    this(new Properties());
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
   * <li>cacheDir -- (default=null) path to cache directory. If empty, then no cache will be used.</li>
   * <li>userAgent -- (default="Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)")
   *                  The user agent string to send identifying the crawler.
   * <li>ignoreRobots -- If "true" (default=false), then ignore robots.txt directives.</li>
   * <li>crawlDelay -- (default=5000) millis to wait before revisiting a domain.</li>
   * <li>maxNumRobots -- (default=20) maximum number of robots.txt instances to
   *                     cache; should be at least as many as the number of
   *                     threads that will be used to crawl using these
   *                     settings.</li>
   * <li>robotName -- (default=semanticdiscovery) robot name as a (partial) user-agent in a robots.txt file.
   * </ul>
   */
  public CrawlSettings(Properties properties) {
    this.connectTimeout = Integer.parseInt(properties.getProperty("connectTimeout", Integer.toString(DEFAULT_CONNECT_TIMEOUT)));
    this.readTimeout = Integer.parseInt(properties.getProperty("readTimeout", Integer.toString(DEFAULT_READ_TIMEOUT)));
    this.numRetries = Integer.parseInt(properties.getProperty("numRetries", Integer.toString(DEFAULT_CONNECT_TIMEOUT)));
    this.forceRefreshTime = Integer.parseInt(properties.getProperty("forceRefreshTime", "0"));
    this.allowRedirects = "true".equalsIgnoreCase(properties.getProperty("allowRedirects", Boolean.toString(DEFAULT_ALLOW_REDIRECTS)));
    this.skipContent = "true".equalsIgnoreCase(properties.getProperty("skipContent", "false"));
    this.verbose = "true".equalsIgnoreCase(properties.getProperty("verbose", "false"));

    final String cacheDirString = properties.getProperty("cacheDir", "");
    this.cacheDir = "".equals(cacheDirString) ? null : new File(cacheDirString);
    this.userAgent = properties.getProperty("userAgent", DEFAULT_USER_AGENT);

    this.ignoreRobots = "true".equals(properties.getProperty("ignoreRobots", "false"));
    this.crawlDelay = Long.parseLong(properties.getProperty("crawlDelay", Long.toString(DEFAULT_CRAWL_DELAY)));
    this.maxCrawlDelay = crawlDelay;
    this.maxNumRobots = Integer.parseInt(properties.getProperty("maxNumRobots", Integer.toString(DEFAULT_MAX_NUM_ROBOTS)));
    this.robotName = properties.getProperty("robotName", DEFAULT_ROBOT_NAME);

    this.host2time = null;
    this.cleaningUpDelays = false;
    this.host2robots = null;
  }

  /**
   * Copy constructor.
   */
  public CrawlSettings(CrawlSettings other) {
    this.connectTimeout = other.connectTimeout;
    this.readTimeout = other.readTimeout;
    this.numRetries = other.numRetries;
    this.forceRefreshTime = other.forceRefreshTime;
    this.allowRedirects = other.allowRedirects;
    this.skipContent = other.skipContent;
    this.verbose = other.verbose;
    this.cacheDir = other.cacheDir;
    this.userAgent = other.userAgent;
    this.ignoreRobots = other.ignoreRobots;
    this.crawlDelay = other.crawlDelay;
    this.maxCrawlDelay = other.crawlDelay;
    this.maxNumRobots = other.maxNumRobots;
    this.robotName = other.robotName;
    this.host2time = other.host2time;
    this.cleaningUpDelays = false;
    this.host2robots = other.host2robots;
  }

  /**
   * Set the connect timeout.
   */
  public void setConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  /**
   * Get the connect timeout.
   */
  public int getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * Set the read timeout.
   */
  public void setReadTimeout(int readTimeout) {
    this.readTimeout = readTimeout;
  }

  /**
   * Get the read timeout.
   */
  public int getReadTimeout() {
    return readTimeout;
  }

  /**
   * Set the amount of time after which to recrawl a page.
   */
  public void setForceRefreshTime(long forceRefreshTime) {
    this.forceRefreshTime = forceRefreshTime;
  }

  /**
   * Get the amount of time after which to recrawl a page.
   */
  public long getForceRefreshTime() {
    return forceRefreshTime;
  }

  /**
   * Set the number of retries.
   */
  public void setNumRetries(int numRetries) {
    this.numRetries = numRetries;
  }

  /**
   * Get the number of retries.
   */
  public int getNumRetries() {
    return numRetries;
  }

  /**
   * Set allow redirects.
   */
  public void setAllowRedirects(boolean allowRedirects) {
    this.allowRedirects = allowRedirects;
  }

  /**
   * Get allow redirects.
   */
  public boolean allowRedirects() {
    return allowRedirects;
  }

  /**
   * Set skip content.
   */
  public void setSkipContent(boolean skipContent) {
    this.skipContent = skipContent;
  }

  /**
   * Get skip content.
   */
  public boolean skipContent() {
    return skipContent;
  }

  /**
   * Set verbose.
   */
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  /**
   * Get verbose.
   */
  public boolean verbose() {
    return verbose;
  }

  /**
   * Set the cache dir.
   */
  public void setCacheDir(File cacheDir) {
    this.cacheDir = cacheDir;
  }

  /**
   * Get the cache dir.
   */
  public File getCacheDir() {
    return cacheDir;
  }

  /**
   * Set the user agent.
   */
  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  /**
   * Get the user agent.
   */
  public String getUserAgent() {
    return userAgent;
  }


  /**
   * Set the ignore robots flag.
   */
  public void setIgnoreRobots(boolean ignoreRobots) {
    this.ignoreRobots = ignoreRobots;
  }

  /**
   * Determine whether to ignore robots.
   */
  public boolean ignoreRobots() {
    return ignoreRobots;
  }

  /**
   * Set the number of millis to delay between grabs from a domain.
   */
  public void setCrawlDelay(long crawlDelay) {
    this.crawlDelay = crawlDelay;

    if (crawlDelay > maxCrawlDelay) maxCrawlDelay = crawlDelay;
  }

  /**
   * Get the number of millis to delay between grabs from a domain.
   */
  public long getCrawlDelay() {
    return crawlDelay;
  }

  /**
   * Set the maximum number of robots to cache.
   * <p>
   * NOTE: This should be at least as many threads are being used to crawl
   *       using these settings.
   */
  public void setMaxNumRobots(int maxNumRobots) {
    this.maxNumRobots = maxNumRobots;

    if (host2robots != null) {
      host2robots.setCacheSize(maxNumRobots);
    }
  }

  /**
   * Get the maximum number of robots to cache.
   */
  public int getMaxNumRobots() {
    return maxNumRobots;
  }


  /**
   * Set the robot name as would be found as a (partial) user-agent in a
   * robots.txt file.
   */
  public void setRobotName(String robotName) {
    this.robotName = robotName;
  }

  /**
   * Set the robot name as would be found as a (partial) user-agent in a
   * robots.txt file.
   */
  public String getRobotName() {
    return robotName;
  }


  /**
   * Close the crawl cache associated with this instance's cache dir.
   */
  public void closeCrawlCache() {
    CrawlCache.closeInstance(cacheDir);
  }

  /**
   * Determine whether the page can be crawled.
   * <p>
   * Note that this checks robots.txt (if indicated).
   *
   * @return null if it is okay to crawl the page; otherwise, a
   *         RobotsDeniedException specifying why not.
   */
  public RobotsDeniedException canCrawl(UrlData urlData, PageCrawler pageCrawler) {
    if (ignoreRobots) return null;  // not inhibited

    RobotsDeniedException result = null;

    // check robots.txt (unless trying to grab robots.txt)
    if (!urlData.getUrlString().endsWith("robots.txt")) {
      final DetailedUrl dUrl = urlData.getDetailedUrl();
      final String hostKey = dUrl.getHost(true, false, false);

      // retrieve or build robots.text instance
      RobotsDotText robots = (host2robots != null) ? host2robots.get(hostKey) : null;
      if (robots == null && (host2robots == null || !host2robots.containsKey(hostKey))) {
        try {
          robots = new RobotsDotText(urlData, pageCrawler);
        }
        catch (IOException ignore) {}

        if (host2robots == null) host2robots = new LRU<String, RobotsDotText>(maxNumRobots);
        host2robots.put(hostKey, robots);
      }

      // check robots.txt
      result = robots != null ? robots.allows(urlData) : null;
    }

    return result;
  }

  /**
   * Enforce delaying before revisiting a site according to these
   * settings if warranted.
   */
  public void enforceSiteDelay(UrlData urlData) {
    final Long overrideCrawlDelay = urlData.getOverrideCrawlDelay();

    if (crawlDelay <= 0 && overrideCrawlDelay == null) return;  // no delay!

    // clean up the host2time map
    final long curTime = System.currentTimeMillis();
    cleanUpDelays(curTime);
    
    // check delays
    final DetailedUrl dUrl = urlData.getDetailedUrl();
    final String host = dUrl.getHost(true, false, false);
    final Long lastVisitTime = (host2time == null) ? null : host2time.get(host);

    // wait if warranted
    if (lastVisitTime != null) {
      final long theCrawlDelay = overrideCrawlDelay != null ? overrideCrawlDelay : crawlDelay;
      if (theCrawlDelay > maxCrawlDelay) maxCrawlDelay = theCrawlDelay;
      if (curTime - lastVisitTime < theCrawlDelay) {
        // sleep the rest of the time
        final long sleepTime = theCrawlDelay - (System.currentTimeMillis() - lastVisitTime);
        if (sleepTime > 0) {
          try {
            Thread.sleep(sleepTime);
          }
          catch (InterruptedException ignore) {}
        }
      }
    }

    // keep track of this visit time
    if (host2time == null) host2time = new HashMap<String, Long>();
    host2time.put(host, System.currentTimeMillis());
  }

  /**
   * Walk through the last visit times for sites, discarding those we no longer
   * need to keep track of as determined by their not having been visited for
   * longer than the crawl delay.
   * <p>
   * NOTE: This method is thread safe.
   */
  private final void cleanUpDelays(long curTime) {
    if (cleaningUpDelays) return;

    if (host2time != null && maxCrawlDelay > 0) {
      synchronized (host2time) {
        cleaningUpDelays = true;
        for (Iterator<Long> iter = host2time.values().iterator(); iter.hasNext(); ) {
          final Long visitTime = iter.next();
          if (curTime - visitTime >= maxCrawlDelay) iter.remove();
        }
        cleaningUpDelays = false;
      }
    }
  }
}
