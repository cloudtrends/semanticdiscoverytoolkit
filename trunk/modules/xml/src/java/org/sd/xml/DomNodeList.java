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

import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Implementation of the dom NodeList interface.
 * <p>
 * @author Spence Koehler
 */
public class DomNodeList 
  implements NodeList, Iterable<DomNode>
{
  private List<DomNode> domNodes;

  DomNodeList(List<DomNode> domNodes) {
    this.domNodes = domNodes;
  }
  
  public int getLength() {
    return domNodes == null ? 0 : domNodes.size();
  }

  public Node item(int index) {
    Node result = null;

    if (domNodes != null && index >= 0 && index < domNodes.size()) {
      result = domNodes.get(index);
    }

    return result;
  }

  public Iterator<DomNode> iterator() { 
    if(domNodes == null)
      return null;
    else
      return domNodes.iterator();
  }
}
