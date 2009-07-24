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
 * Depth first tree traversal iterator.
 * <p>
 * @author Spence Koehler
 */
public class ReverseDepthFirstIterator<T> implements TraversalIterator<T> {

  private LinkedList<Tree<T>> stack;
  private Tree<T> lastNode;

  public ReverseDepthFirstIterator(Tree<T> node) {
    this.stack = new LinkedList<Tree<T>>();
    for(DepthFirstIterator<T> i = new DepthFirstIterator<T>(node); i.hasNext(); ){
      final Tree<T> tree = i.next();
      stack.addFirst(tree);
    }
    this.lastNode = null;
  }

  public boolean hasNext() {
    return (stack.size() > 0);
  }

  public Tree<T> next() {
    if (!hasNext()) throw new NoSuchElementException("no more nodes!");
    // Grab the first element in the stack, which is the last element in the tree
    Tree<T> result = stack.removeFirst();
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
   * Skip traversing to next adjacent sibling node, if no siblings, back to parent.
   */
  public void skip() {
    // todo: I'm not sure how exactly I want to implement this eventually...for now, this behavior doesn't make sense in
    // a reverse tree traversal, so I'll do it as described above
    stack.removeFirst();
  }
}
