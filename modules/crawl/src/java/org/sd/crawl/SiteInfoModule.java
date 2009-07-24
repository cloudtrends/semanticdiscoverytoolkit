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

import java.util.List;
import java.util.Set;

/**
 * Interface for modules that collect site info.
 * <p>
 * @author Spence Koehler
 */
public interface SiteInfoModule {

//
// NOTES:
//
// Each successfully fetched page is added using addFetchedPage.
// - Note that all page links can be accessed using crawledPage.getLinks, but each of
//   those links will be cycled through this class's other methods as described next.
//   We can use these to get stats, i.e., on branching factor.
//
// Each link on each successfully fetched page leads to one of the following:
// - a new fetched page (addFetchedPage)
// - an ignored link, or link for which linkFollower.shouldFetchLink returns false (addIgnoredLink)
// - a failed link, or unsuccessfully fetched paged (addFailedLink)
// - a revisited page, or page that has already been fetched (addRevisitedPage)
// - an unfollowed link, or link for which linkFollower.shouldFollowLink returns false (addUnfollowedLink)
//

  /**
   * Add stats pertaining to a newly fetched page.
   * <p>
   * NOTE: this is for crawledPages that have been fetched without error and
   *       with content.
   *
   * @param crawledPage  The successful fetched page without error and with content.
   * @param crawlDepth   The crawl depth of the fetched page.
   * @param dUrl         (optional, ok if null) The DetailedUrl instance of the crawledPage's url.
   */
  public void addFetchedPage(CrawledPage crawledPage, int crawlDepth, DetailedUrl dUrl);

  /**
   * Add stats pertaining to links from a page that will not be followed.
   * <p>
   * NOTE: this is for links, from a successfully fetched page, that will
   *       not be followed according to a link follower.
   */
  public void addUnfollowedLinks(List<Link> links, SiteContext siteContext);

  /**
   * Add stats pertaining to a page that was previously visited.
   * <p>
   * NOTE: this is for crawledPages that have been fetched without error and
   *       with content, but whose urls have been previously visited and added
   *       as fetched pages.
   *
   * @param crawledPage  The successful fetched page without error and with content.
   * @param crawlDepth   The crawl depth of the fetched page.
   * @param dUrl         (optional, ok if null) The DetailedUrl instance of the crawledPage's url.
   */
  public void addRevisitedPage(CrawledPage crawledPage, int crawlDepth, DetailedUrl dUrl);

  /**
   * Add stats pertaining to a link whose destUrl couldn't be fetched.
   * <p>
   * Note that the crawledPage could be null, hold an error or no content.
   */
  public void addFailedLink(Link link, CrawledPage crawledPage);

  /**
   * Add stats pertaining to a link that was not fetched (because the
   * link follower indicated that it should not be fetched.)
   */
  public void addIgnoredLink(Link link);


  /**
   * Get the histogram of (string) data associated with this module.
   *
   * @return the histogram, or null if not applicable.
   */
  public Histogram<String> getHistogram();

  /**
   * Get the urls collected through this module.
   */
  public Set<String> getUrls();
}
