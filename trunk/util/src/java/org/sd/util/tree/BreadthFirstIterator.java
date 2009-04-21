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
package org.sd.util.tree;


import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * Breadth first tree traversal iterator.
 * <p>
 * @author Spence Koehler
 */
public class BreadthFirstIterator<T> implements TraversalIterator<T> {

  private LinkedList<Tree<T>> queue;
  private Tree<T> lastNode;

  public BreadthFirstIterator(Tree<T> node) {
    this.queue = new LinkedList<Tree<T>>();
    queue.add(node);
    this.lastNode = null;
  }

  public boolean hasNext() {
    return (queue.size() > 0);
  }

  public Tree<T> next() {
    if (!hasNext()) throw new NoSuchElementException("no more nodes!");
    Tree<T> result = queue.removeFirst();
    final List<Tree<T>> children = result.getChildren();
    if (children != null) queue.addAll(children);
    lastNode = result;
    return result;
  }

  /**
   * Prune the current iterator node from the tree.
   */
  public void remove() {
    if (lastNode != null) {
      lastNode.prune(true, true);
      skip();
    }
  }
  
  /**
   * Skip traversing into the last "next" node's children.
   */
  public void skip() {
    if (lastNode != null) {
      // we just added the children to the end of the queue, so lop 'em off.
      final List<Tree<T>> children = lastNode.getChildren();
      if (children != null) {
        for (Tree<T> child : children) queue.removeLast();
      }
    }
  }
}
