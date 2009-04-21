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
import org.sd.util.Histogram;
import org.sd.util.StatsAccumulator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Container for crawled site information.
 * <p>
 * @author Spence Koehler
 */
public class SiteInfo implements SiteInfoModule {
  
  public static final SiteInfo buildSiteInfo() {
    final List<SiteInfoModule> modules = new ArrayList<SiteInfoModule>();

    modules.add(new AnalyzedUrlExtensionModule());    // analyzed url extensions
    modules.add(new UnanalyzedUrlExtensionModule());  // unanalyzed url extensions
    modules.add(new ErrorPageModule());               // error & robots denied pages & missing content
    modules.add(new QueryPageModule());               // query pages & urls
    modules.add(new MissingTitleModule());            // missing title
    modules.add(new MissingDescriptionModule());      // missing description
    modules.add(new MissingKeywordModule());          // missing keywords
    modules.add(new ExcessTitleModule());             // excess title
    modules.add(new ExcessDescriptionModule());       // excess description

//todo: make/add page relevance modules
/*
    modules.add(new DuplicateContentModule());        // duplicate content
    modules.add(new DuplicateTitleModule());          // duplicate title
    modules.add(new DuplicateDescriptionModule());    // duplicate description
    modules.add(new DuplicateKeywordModule());        // duplicate keywords
*/

    return new SiteInfo(modules);
  }


//todo: access the collected info
//todo: keep track of branching factors, etc. here with the download stats.


// total pages found
//   pages queued for analysis
//   pages analyzed
//     extensions histogram
//   pages not analyzed
//     robots denied pages    crawledPage.getError instance of RobotsDeniedException
//     query pages            url.indexOf('?') > 0
//     site error pages       crawledPage.getResponseCode() >= 400
//     crawler error pages    crawledPage.hasError
//     redirect pages         crawledPage.getResponseCode() >= 300 & < 400
//     pdf pages              extensions histogram
//     shockwave flash pages  extensions histogram
//     feed (rss) pages       extensions histogram
//     empty pages            contentLength || actualLength == 0
//
// total indexable pages
//
// download stats
//   pages downloaded for analysis
//   average server response time  crawledPage.getDownloadHeaderTime
//   average page load time        crawledPage.getDownloadContentTime
//
// error page detail
//   error page count
//     error code[i] (400+)  (<-- errorpages: keep all error page urls)
//     error code[i] (300-399) (<-- redirectpages: keep all redirect error page urls)
//     [crawler error for internal use]
//
//   robots denied count
//     url pattern[i], freq[i]  (download all robots denied urls)
//
//   query page count
//     url pattern[i], freq[i] (download all query page urls)
//
//
// "Site Structure Detail" (use siteTree to compute)
//    label + urlPattern --
//      pageVisitIndex (or "crawlDepth") stats (min,max,ave)
//      clickDepth stats (min,max,ave)
//        frequency and percent pages at 1, 2, ... clicks from home page
//    max click depth
//    average branching factor
//
// Alerts
//    duplicate page count
//       duplicate content (download all duplicate url sets)
//       duplicate title
//       duplicate description
//       duplicate keywords
//    missing data page count
//       missing content (download all missing page urls)
//       missing page title count
//       missing description
//       missing keyword
//       missing alt attribute ???<-- what's this?
//    excess data page count
//      excess title length page count (> 60 chars) (download all excess page urls)
//      excess description length page count (> 200 chars)
//    
// Page Relevance
//   Top keyword
//   Optimized Keyword Count
//   Optimized Keywords
//   Link relevance (1 click, 2 clicks, 3 clicks, ...)
//     by page type x {headings, body, other)
//   Title relevance (1 click, 2 clicks, 3 clicks, ...)
//     by page type x {headings, body, other)
//   Keyword Density
//     Title, Headers, Meta, Alt Text, Link Text, Body Text
//    for home page
//    for top cross linked page
//

  private List<SiteInfoModule> modules;
  private StatsAccumulator serverResponseStats;
  private StatsAccumulator pageLoadStats;

  public SiteInfo(List<SiteInfoModule> modules) {
    this.modules = modules;

    this.serverResponseStats = new StatsAccumulator("ServerResponse");
    this.pageLoadStats = new StatsAccumulator("PageLoad");
  }

  /**
   * Add stats pertaining to a newly fetched page.
   */
  public void addFetchedPage(CrawledPage crawledPage, int crawlDepth, DetailedUrl dUrl) {
    for (SiteInfoModule module : modules) {
      module.addFetchedPage(crawledPage, crawlDepth, dUrl);
    }

    final PageMetaData metaData = crawledPage.getMetaData();
    serverResponseStats.add(metaData.getDownloadHeaderTime());  // in millis
    pageLoadStats.add(metaData.getDownloadContentTime());       // in millis
  }

  /**
   * Add stats pertaining to links from a page that will not be followed.
   */
  public void addUnfollowedLinks(List<Link> links, SiteContext siteContext) {
    for (SiteInfoModule module : modules) {
      module.addUnfollowedLinks(links, siteContext);
    }
  }

  /**
   * Add stats pertaining to a page that was previously visited.
   */
  public void addRevisitedPage(CrawledPage crawledPage, int crawlDepth, DetailedUrl dUrl) {
    // NOTE: modules will count revisited pages as if they were fetched, which won't
    //       give us the info we're after here. Just count the number of revisited pages?
//todo: count and keep the revisited page urls? (use a special site info module?)
//      maybe the only thing we need from this is the "top crosslinked page" -- which we could get from the site tree before pruning?
//todo: decide whether to call other modules in this case as opposed to just collecting stats here.
//     for (SiteInfoModule module : modules) {
//       module.addRevisitedPage(crawledPage, crawlDepth, dUrl);
//     }
  }

  /**
   * Add stats pertaining to a link whose destUrl couldn't be fetched.
   * <p>
   * Note that the crawledPage could be null, hold an error or no content.
   */
  public void addFailedLink(Link link, CrawledPage crawledPage) {
    for (SiteInfoModule module : modules) {
      module.addFailedLink(link, crawledPage);
    }
  }

  /**
   * Add stats pertaining to a link that was not fetched (because the
   * link follower indicated that it should not be fetched.)
   */
  public void addIgnoredLink(Link link) {
    for (SiteInfoModule module : modules) {
      module.addIgnoredLink(link);
    }
  }

  /**
   * Get the histogram of (string) data associated with this module.
   *
   * @return the histogram, or null if not applicable.
   */
  public Histogram<String> getHistogram() {
    return null;
  }

  /**
   * Get the urls collected through this module.
   */
  public Set<String> getUrls() {
    final Set<String> result = new HashSet<String>();

    for (SiteInfoModule module : modules) {
      final Set<String> urls = module.getUrls();
      if (urls != null) result.addAll(urls);
    }

    return result;
  }
}
