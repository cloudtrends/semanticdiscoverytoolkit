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
 * A link acceptor that accepts links to domain roots.
 * <p>
 * @author Spence Koehler
 */
public class DomainLinkAcceptor implements TagNodeAcceptor {
  
  private static final Set<String> HOME_PAGES = new HashSet<String>();

  static {

//todo: parameterize these...

    HOME_PAGES.add("index");
    HOME_PAGES.add("main");
    HOME_PAGES.add("home");
    HOME_PAGES.add("top");
    HOME_PAGES.add("root");
  }

  public DomainLinkAcceptor() {
  }

  /**
   * Accept the link if it references a domain root.
   */
  public boolean acceptTagNode(Tree<XmlLite.Data> linkNode) {

//todo: properly handle image and area tags...

    final String href = XmlTreeHelper.getAttribute(linkNode, "href");
    return isDomainRoot(href);
  }

  protected final boolean isDomainRoot(String href) {
    if (href == null || "".equals(href)) return false;

    boolean hadPrefix = false;

    // remove "whatever://" from front
    final int cPos = href.indexOf(':');
    if (cPos >= 0) {
      if (href.length() > cPos + 3) {
        href = href.substring(cPos + 3);
        hadPrefix = true;
      }
      else {
        return false;
      }
    }

    final boolean hasSlash = hasSlash(href);
    String piece = href;

    boolean result = false;

    if (hasSlash) {
      final String[] parts = href.split("[/\\\\]");
      if (hadPrefix && parts.length == 1) return true;
      if (parts.length == 0 || parts.length > 2) return false;

      if (hadPrefix) {
        piece = parts[1];
      }
      else {
        piece = parts[0];
      }
    }
    else {
      result = true;
    }

    if (!result) {
      final String[] pieces = piece.split("\\.");
      piece = pieces[0].toLowerCase();

      result = HOME_PAGES.contains(piece);

      if (!result) {
        for (String homePage : HOME_PAGES) {
          if (piece.startsWith(homePage)) {
            result = true;
            break;
          }
        }
      }
    }

    return result;
  }

  private final boolean hasSlash(String href) {
    final int len = href.length();

    for (int i = 0; i < len; ++i) {
      final char c = href.charAt(i);
      if (c == '/' || c == '\\') return true;
    }
    return false;
  }
}
