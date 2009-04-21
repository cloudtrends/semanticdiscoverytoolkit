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

import java.util.List;

/**
 * A site info module for collecting extension stats for unanalyzed urls.
 * <p>
 * @author Spence Koehler
 */
public class UnanalyzedUrlExtensionModule extends UrlExtensionModule {

  public UnanalyzedUrlExtensionModule() {
    super();
  }

  /**
   * Add stats pertaining to a newly fetched page.
   */
  public void addFetchedPage(CrawledPage crawledPage, int crawlDepth, DetailedUrl dUrl) {
    //do nothing with analyzed urls
  }

  /**
   * Add stats pertaining to links from a page that will not be followed.
   */
  public void addUnfollowedLinks(List<Link> links, SiteContext siteContext) {
    if (links != null) {
      for (Link link : links) {
        addLink(link);
      }
    }
  }

  /**
   * Add stats pertaining to a page that was previously visited.
   */
  public void addRevisitedPage(CrawledPage crawledPage, int crawlDepth, DetailedUrl dUrl) {
    //do nothing with revisited urls
  }

  /**
   * Add stats pertaining to a link whose destUrl couldn't be fetched.
   * <p>
   * Note that the crawledPage could be null, hold an error or no content.
   */
  public void addFailedLink(Link link, CrawledPage crawledPage) {
    addLink(link);
  }

  /**
   * Add stats pertaining to a link that was not fetched (because the
   * link follower indicated that it should not be fetched.)
   */
  public void addIgnoredLink(Link link) {
    addLink(link);
  }  
}
