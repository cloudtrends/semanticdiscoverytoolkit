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

import java.util.List;

/**
 * Interface for scraping hit (and next) information from a search result page.
 * <p>
 * @author Spence Koehler
 */
public interface HitScraper {

  /**
   * Get the nodes from the xmlTree that are links to query result pages.
   */
  public List<Tree<XmlLite.Data>> getHitLinkNodes(Tree<XmlLite.Data> xmlTree);

  /**
   * Get the url from the node with the link to the next results summary page.
   *
   * @return null if there are no more "next" pages.
   */
  public String getNextResultsPageUrl(Tree<XmlLite.Data> xmlTree);
  
}
