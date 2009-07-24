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


import java.util.List;

/**
 * Container for data in the site tree being crawled.
 * <p>
 * @author Spence Koehler
 */
public class SiteData {

  private UrlData urlData;
  private int crawlDepth;
  private List<Link> links;           // links to follow from this instance

  private PageMetaData pageMetaData;  // Page meta data for this instance's urlData
  private Link link;                  // link followed to create this instance

  public SiteData(UrlData startUrl) {
    this(startUrl, 0);
  }

  public SiteData(UrlData urlData, int crawlDepth) {
    this.urlData = urlData;
    this.crawlDepth = crawlDepth;
    this.pageMetaData = null;
    this.link = null;
  }

  public UrlData getUrlData() {
    return urlData;
  }

  public int getCrawlDepth() {
    return crawlDepth;
  }

  public SiteData buildNext(Link link, PageMetaData pageMetaData) {
    final SiteData result = new SiteData(link.getDestUrl(), crawlDepth + link.getDepthInc());

    result.link = link;
    result.pageMetaData = pageMetaData;

    return result;
  }

  /**
   * Set the links to follow from this instance.
   */
  public void setLinks(List<Link> links) {
    this.links = links;
  }

  /**
   * Get the links to follow from this instance.
   */
  public List<Link> getLinks() {
    return links;
  }

  /**
   * Set the link followed to create this instance.
   */
  public void setLink(Link link) {
    this.link = link;
  }

  /**
   * Get the link followed to create this instance.
   */
  public Link getLink() {
    return link;
  }


  /**
   * Set the page meta data for this instance's urlData.
   */
  public void setPageMetaData(PageMetaData pageMetaData) {
    this.pageMetaData = pageMetaData;
  }

  /**
   * Get the page meta data for this instance's urlData.
   */
  public PageMetaData getPageMetaData() {
    return pageMetaData;
  }

  public String toString() {
    return urlData == null ? "?" : urlData.toString();
  }
}
