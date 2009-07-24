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
package org.sd.extract;


import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;

import java.util.HashSet;
import java.util.Set;

/**
 * A link acceptor that accepts only relative links.
 * 
 * <p>
 * @author Spence Koehler
 */
public class RelativeLinksAcceptor implements TagNodeAcceptor {
  
  private boolean offsiteLinks;

  /**
   * Construct to accept only relative links.
   */
  public RelativeLinksAcceptor() {
    this(false);
  }

  /**
   * Construct to accept only relative links (if param is false); otherwise,
   * only offsite links (if param is true).
   */
  public RelativeLinksAcceptor(boolean offsiteLinks) {
    this.offsiteLinks = offsiteLinks;
  }

  /**
   * Accept the link if it does not have "://" in its href text.
   */
  public boolean acceptTagNode(Tree<XmlLite.Data> linkNode) {

//todo: properly handle image and area tags...

    final String href = XmlTreeHelper.getAttribute(linkNode, "href");
    return isRelativeReference(href);
  }

  protected final boolean isRelativeReference(String href) {
    if (href == null || "".equals(href)) return false;

    // look for "whatever://" from front... not relative if it has it.
    final int cPos = href.indexOf(':');
    return offsiteLinks ? cPos >= 0 : cPos < 0;
  }
}
