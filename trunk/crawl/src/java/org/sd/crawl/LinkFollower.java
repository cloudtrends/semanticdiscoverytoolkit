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


/**
 * Interface for accepting/rejecting links to follow while crawling.
 * <p>
 * @author Spence Koehler
 */
public interface LinkFollower {

  public enum Priority {HIGH, NORMAL};

  /**
   * Determine whether to fetch the given link.
   * <p>
   * Note that some links will only be fetched and not followed.
   *
   * @return true to fetch the destination url of the given link.
   */
  public boolean shouldFetchLink(SiteContext siteContext, SiteData siteData, Link link);

  /**
   * Determine whether to follow links from the target of the given link.
   * <p>
   * Given that the link has been fetched, determine whether its links should
   * be followed.
   *
   * @return null to NOT follow the links on the destination url of the given link;
   *         otherwise, return a NORMAL priority to follow in normal breadth-first
   *         fashion or HIGH priority to follow the link sooner.
   */
  public Priority shouldFollowLink(SiteContext siteContext, SiteData siteData, Link link);
}
