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


import java.util.HashSet;
import java.util.Set;

/**
 * Container for contextual information for a site being crawled.
 * <p>
 * @author Spence Koehler
 */
public class SiteContext {
  
  private UrlData startUrl;
  private Set<String> visitedUrls;

  public SiteContext(UrlData startUrl) {
    this.startUrl = startUrl;
    this.visitedUrls = new HashSet<String>();
  }

  public UrlData getStartUrl() {
    return startUrl;
  }

  public int getNumVisited() {
    return visitedUrls.size();
  }

  /**
   * Determine whether the given url has been visited.
   */
  public boolean visited(String url) {
    return visitedUrls.contains(url);
  }

  /**
   * Add the url to the context.
   * 
   * @return true if the url hasn't been seen yet; otherwise, false.
   */
  public boolean addVisited(String url) {
    return visitedUrls.add(url);
  }
}
