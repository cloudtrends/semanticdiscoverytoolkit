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
import org.sd.text.DetailedUrl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility to fetch and decode a site's robots.txt file.
 * <p>
 * @author Spence Koehler
 */
public class RobotsDotText {

  /** Default time for refreshing view of robots.txt (1 day) */
  public static final long DEFAULT_REFRESH_MILLIS = 1000 * 60 * 60 * 24;

  /**
   * Given an arbirary url, get its site's robots.txt url.
   */
  public static final UrlData getRobotsUrl(String urlString) {
    return getRobotsUrl(new UrlData(urlString));
  }

  /**
   * Given an arbirary url, get its site's robots.txt url.
   */
  public static final UrlData getRobotsUrl(UrlData urlData) {
    final DetailedUrl dUrl = urlData.getDetailedUrl();

    final StringBuilder builder = new StringBuilder();
    builder.
      append(dUrl.getProtocol(true)).
      append(dUrl.getHost(true, true, true)).
      append("/robots.txt");

    final UrlData robotsUrl = new UrlData(builder.toString());
    robotsUrl.setOverrideCrawlDelay(0L); // don't enforce crawl delay when grabbing robots.txt
    robotsUrl.setOverrideForceRefreshTime(DEFAULT_REFRESH_MILLIS);  // refresh robots.txt once a day

    return robotsUrl;
  }


  private Long crawlDelay;
  private Set<String> disallowPatterns;
  private String robotName;

  /**
   * Construct an instance for the site of the given url.
   *
   * @param urlData  any url on the site for which we wish to retrieve robots.txt
   * @param pageCrawler  the page crawler to use to retrieve robots.txt
   */
  public RobotsDotText(UrlData urlData, PageCrawler pageCrawler) throws IOException {

    final UrlData robotsUrl = getRobotsUrl(urlData);
    final CrawledPage robotsPage = pageCrawler.fetch(robotsUrl);
    final String robotsTextContent = robotsPage.getContent();
    final InputStream robotsTextStream = robotsPage.getInputStream();

    // the name of the robot as a (partial) user-agent in a robots.txt file
    this.robotName = pageCrawler.getCrawlSettings().getRobotName();

    init(robotsTextStream);

    if (robotsTextStream != null) {
      robotsTextStream.close();
    }
  }

  /**
   * Construct with the input stream for the robots.txt data.
   */
  public RobotsDotText(InputStream robotsTextStream, String robotName) throws IOException {
    this.robotName = robotName;
    init(robotsTextStream);
  }

  private final void init(InputStream robotsTextStream) throws IOException {
    this.disallowPatterns = new HashSet<String>();

    if (robotsTextStream == null) return;

    final BufferedReader reader = FileUtil.getReader(robotsTextStream);
    String line = null;

    while ((line = reader.readLine()) != null) {
      if ("".equals(line)) continue;

      line = line.toLowerCase().trim();

      // Find "our" directive(s):
      // User-agent: *
      // User-agent: semanticdiscovery
      if (line.startsWith("user-agent:")) {
        if (appliesToUs(line)) {
          boolean allowAll = false;

          // this is our section
          while ((line = reader.readLine()) != null) {
            if ("".equals(line)) continue;
            line = line.trim().toLowerCase();

            // parse "Disallow: X"
            if (line.startsWith("disallow:")) {
              final String disallow = line.substring(9).trim();
              if (!"".equals(disallow)) {
                disallowPatterns.add(disallow);
              }
              else {
                // empty disallow string means allow everything.
                allowAll = true;
              }
            }

            // parse "Crawl-delay: X"
            else if (line.startsWith("crawl-delay:")) {
              final String crawlDelayString = line.substring(12).trim();
              if (!"".equals(crawlDelayString)) {
                try {
                  this.crawlDelay = new Long(crawlDelayString);
                }
                catch (NumberFormatException ignore) {}
              }
            }

            else if (line.startsWith("user-agent:")) {
              if (!appliesToUs(line)) break;
            }
          }

          if (allowAll) {
            disallowPatterns.clear();
            return;
          }
        }
      }
    }
  }

  private final boolean appliesToUs(String userAgentLine) {
    final String attr = userAgentLine.substring(11).trim();
    return ("*".equals(attr) || attr.indexOf(robotName) >= 0);
  }

  /**
   * Determine whether the robots.text allows the given url.
   *
   * @param urlData  the url to test.
   *
   * @return null if the urlData is allowed or a RobotsDeniedException if not.
   */
  public RobotsDeniedException allows(UrlData urlData) {
    RobotsDeniedException result = null;

    if (disallowPatterns.size() > 0) {
      final String pathString = urlData.getDetailedUrl().getPath(false);
      for (String disallowPattern : disallowPatterns) {
        if (pathString.startsWith(disallowPattern)) {
          result = new RobotsDeniedException(urlData.getUrlString(), disallowPattern);
          break;
        }
      }
    }

    if (result == null) {
      // if crawlDelay is specified, then override it for the url
      if (crawlDelay != null) urlData.setOverrideCrawlDelay(crawlDelay);
    }

    return result;
  }

  /**
   * Get the crawlDelay.
   */
  public Long getCrawlDelay() {
    return crawlDelay;
  }

  /**
   * Get the disallow patterns.
   */
  public Set<String> getDisallowPatterns() {
    return disallowPatterns;
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
}
