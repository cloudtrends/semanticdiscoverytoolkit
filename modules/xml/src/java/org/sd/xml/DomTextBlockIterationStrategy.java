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
package org.sd.xml;


import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * <p>
 * @author Spence Koehler
 */
public class DomTextBlockIterationStrategy implements DomIterationStrategy {
  

  public static final DomTextBlockIterationStrategy INSTANCE = new DomTextBlockIterationStrategy();

  private DomTextBlockIterationStrategy() {
  }


  public boolean isIterableNode(DomNode curNode) {
    return hasNonEmptyText(curNode);
  }


  public boolean shouldQueueNode(DomNode childNode) {
    return childNode.getNodeType() == DomNode.ELEMENT_NODE;
  }


  private boolean hasNonEmptyText(DomNode curNode) {
    boolean result = false;

    if (curNode.hasChildNodes()) {
      final NodeList childNodes = curNode.getChildNodes();
      for (int childIndex = 0; childIndex < childNodes.getLength(); ++childIndex) {
        final Node childNode = childNodes.item(childIndex);
        if (DomUtil.isNonEmptyTextNode(childNode)) {
          result = true;
          break;
        }
      }
    }
    else {
      result = DomUtil.isNonEmptyTextNode(curNode);
    }

    return result;
  }
}
