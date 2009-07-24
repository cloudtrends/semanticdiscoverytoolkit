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


import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Container class for a query page.
 */
public final class QueryPage {
  public final String query;
  public final String queryUrl;
  public final int maxHits;     // if 0 unlimited; positive=max hit count; negative=max page count
  public final PageCrawler pageCrawler;

  private List<Tree<XmlLite.Data>> hitNodes;
  private List<CrawledPage> queryResultsPages;

  public QueryPage(String query, String queryUrl, int maxHits, PageCrawler pageCrawler) {
    this.query = query;
    this.queryUrl = queryUrl;
    this.maxHits = maxHits;
    this.pageCrawler = pageCrawler;
  }

  public QueryPage(int maxHits) {
    this.query = null;
    this.queryUrl = null;
    this.maxHits = maxHits;
    this.pageCrawler = null;
  }

  /**
   * Get all of the scraped hit nodes.
   */
  public List<Tree<XmlLite.Data>> getHitNodes() {
    return hitNodes;
  }

  /**
   * Get the query results pages.
   */
  public List<CrawledPage> getQueryResultsPages() {
    return queryResultsPages;
  }

  /**
   * Convenience method to get just the first query results page.
   */
  public CrawledPage getQueryResultsPage() {
    CrawledPage result = null;

    if (queryResultsPages != null && queryResultsPages.size() > 0) {
      result = queryResultsPages.get(0);
    }

    return result;
  }

  /**
   * Crawl the given hit.
   */
  public CrawledPage crawlHit(Tree<XmlLite.Data> hitNode) {
    CrawledPage result = null;

    final String hitUrl = XmlTreeHelper.getAttribute(hitNode, "href");
    if (hitUrl != null) {
      final String referringText = XmlTreeHelper.getAllText(hitNode);
      result = pageCrawler.fetch(new UrlData(hitUrl, queryUrl, referringText));
    }

    return result;
  }

  /**
   * Crawl and process the hits using the given processor.
   *
   * @return false if no pages were successfully processed.
   */
  public boolean crawlAndProcessHits(CrawledPageProcessor processor) throws IOException {
    boolean result = false;

    if (hitNodes != null) {
      for (Tree<XmlLite.Data> hitNode : hitNodes) {
        final CrawledPage hitPage = crawlHit(hitNode);
        final boolean processed = processor.processPage(hitPage);

        if (!processed) { // and verbose?
          final String hitUrl = XmlTreeHelper.getAttribute(hitNode, "href");
          System.out.println(new Date() + ": QueryPage.crawlAndProcessHits couldn't process '" + hitUrl + "'");
        }

        result |= processor.processPage(hitPage);
      }
    }

    return result;
  }

  /**
   * Add a page.
   */
  void addPage(CrawledPage queryResultsPage) {
    if (queryResultsPages == null) queryResultsPages = new ArrayList<CrawledPage>();
    queryResultsPages.add(queryResultsPage);
  }

  /**
   * Add the crawled page to this instance, incorporating its information.
   * <p>
   * Compute and return the next page query url if warranted; otherwise, return null.
   */
  public String incorporateCrawledPage(CrawledPage crawledPage, HitScraper hitScraper) {
    String nextQueryUrl = null;

    if (crawledPage != null) {
      this.addPage(crawledPage);

      final Tree<XmlLite.Data> xmlTree = crawledPage.getXmlTree();
      if (xmlTree != null) {
        final List<Tree<XmlLite.Data>> hitNodes = hitScraper.getHitLinkNodes(xmlTree);
        if (hitNodes != null) {
          if (this.addHits(hitNodes)) {
            nextQueryUrl = hitScraper.getNextResultsPageUrl(xmlTree);
System.out.println("nextQueryUrl=" + nextQueryUrl);
          }
        }
      }
    }

    return nextQueryUrl;
  }

  /**
   * Add the hits to this instance up to the maximum.
   *
   * @return false if the maximum has been reached.
   */
  boolean addHits(List<Tree<XmlLite.Data>> hits) {
    boolean result = true;

    if (hits != null) {
      if (hitNodes == null) hitNodes = new ArrayList<Tree<XmlLite.Data>>();

      int numHits = hitNodes.size();
      if (maxHits > 0 && numHits >= maxHits) {
        result = false;
      }
      else {
        for (Tree<XmlLite.Data> hit : hits) {
          hitNodes.add(hit);

          ++numHits;
          if (maxHits > 0 && numHits >= maxHits) {
            result = false;
            break;
          }
        }

        if (maxHits < 0 && queryResultsPages.size() >= (-maxHits)) {
          result = false;
        }
      }
    }

    return result;
  }
}
