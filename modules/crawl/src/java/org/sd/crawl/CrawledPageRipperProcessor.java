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
import org.sd.xml.XmlNodeRipper;

import java.io.IOException;
import java.io.InputStream;

/**
 * A crawled page processor that uses an XmlNodeRipper to process
 * page content.
 * <p>
 * @author Spence Koehler
 */
public abstract class CrawledPageRipperProcessor implements CrawledPageProcessor {
  
  /**
   * Process the ripped node.
   *
   * @return true if successful.
   */
  protected abstract boolean processRippedNode(CrawledPage crawledPage, Tree<XmlLite.Data> rippedNode);

  /**
   * A hook to run after all nodes have been ripped.
   */
  protected abstract void postRippingHook(CrawledPage crawledPage);

  /**
   * Access method to dump results after running.
   */
  protected abstract void dumpResults();

  /**
   * Process the given crawled page.
   *
   * @return true if successful; otherwise, false.
   */
  public final boolean processPage(CrawledPage crawledPage) throws IOException {
    boolean result = false;

    if (crawledPage == null) return result;

    final InputStream inputStream = crawledPage.getInputStream();
    if (inputStream == null) return result;

    final XmlNodeRipper ripper = new XmlNodeRipper(inputStream, true, null, false);
    while (ripper.hasNext()) {
      final Tree<XmlLite.Data> curNode = ripper.next();
      result |= processRippedNode(crawledPage, curNode);
    }

    // run the post ripping hook
    postRippingHook(crawledPage);

    ripper.close();
    inputStream.close();

    return result;
  }
}
