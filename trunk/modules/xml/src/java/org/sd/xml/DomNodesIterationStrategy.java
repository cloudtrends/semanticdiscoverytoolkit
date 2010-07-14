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


import java.util.HashSet;
import java.util.Set;

/**
 * An iteration strategy that only offers specific nodes.
 * <p>
 * @author Spence Koehler
 */
public class DomNodesIterationStrategy implements DomIterationStrategy {
  
  private Set<DomNode> iterableNodes;
  public Set<DomNode> getIterableNodes() {
    return iterableNodes;
  }


  public DomNodesIterationStrategy(DomNode iterableNode) {
    this.iterableNodes = new HashSet<DomNode>();
    iterableNodes.add(iterableNode);
  }


  public DomNodesIterationStrategy(Set<DomNode> iterableNodes) {
    this.iterableNodes = iterableNodes;
  }


  /**
   *  Only listed nodes are iterable.
   */
  public boolean isIterableNode(DomNode curNode) {
    return iterableNodes.contains(curNode);
  }


  /**
   *  Only queue a node if it is an ancestor (inclusive) of an iterable node.
   */
  public boolean shouldQueueNode(DomNode childNode) {
    boolean result = false;

    for (DomNode iterableNode : iterableNodes) {
      if (childNode.isAncestor(iterableNode, true)) {
        result = true;
        break;
      }
    }

    return result;
  }
}
