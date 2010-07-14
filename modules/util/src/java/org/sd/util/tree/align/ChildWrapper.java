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
package org.sd.util.tree.align;


import org.sd.util.tree.Tree;

/**
 * Container class for holding either an unmatched child node or a pair of
 * matched child nodes used for recursing down the descendancy structure of
 * Nodes in the NodePair class.
 * <p>
 * @author Spence Koehler
 */
class ChildWrapper<T> {
  
  /**
   * An unmatched child node (null if a matching pair exists.)
   */
  private Tree<T> childNode;
  public Tree<T> getChildNode() {
    return childNode;
  }

  /**
   * A matching pair of children (null if an unpaired child exists.)
   */
  private NodePair<T> matchingChildren;
  public NodePair<T> getMatchingChildren() {
    return matchingChildren;
  }

  /**
   * Construct with an unmatched child node.
   */
  public ChildWrapper(Tree<T> childNode) {
    this.childNode = childNode;
    this.matchingChildren = null;
  }

  /**
   * Construct with a matching pair of child nodes.
   */
  public ChildWrapper(NodePair<T> matchingChildren) {
    this.childNode = null;
    this.matchingChildren = matchingChildren;
  }
}
