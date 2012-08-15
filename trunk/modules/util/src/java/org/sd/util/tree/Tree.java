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


import org.sd.util.Escaper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A tree data structure.
 * <p>
 * This class maintains the tree structure while providing for data of type T
 * to be stored in each node.
 *
 * @author Spence Koehler
 */
public class Tree<T> {

  public enum Traversal {BREADTH_FIRST, DEPTH_FIRST, REVERSE_BREADTH_FIRST, REVERSE_DEPTH_FIRST};

  private T data;
  private List<Tree<T>> children;
  private Tree<T> parent;
  private int transientId;
  private LinkedList<Tree<T>> rootPath;
  private Map<String, Object> attributes;


  private LinkedList<Tree<T>> _path;
  private boolean gotHashCode;
  private int _hashCode;

  public Tree(T data) {
    this.data = data;
    children = null;
    parent = null;
    this.transientId = 0;
    this.rootPath = null;
    this.attributes = null;

    this.gotHashCode = false;
    this._hashCode = 0;
  }

  public String toString() {
    return toString(SimpleTreeBuilder.getVisualEscaper());
  }

  public String toString(Escaper escaper) {
    String result = null;
    final String data = getData() == null ? "<null>" : getData().toString();

    List<Tree<T>> children = getChildren();
    if (children == null || children.size() == 0) {
      result = escaper.escape(data);
    }
    else {
      StringBuffer buffer = new StringBuffer();
      buffer.append('(');
      buffer.append(escaper.escape(data));
      for (Iterator<Tree<T>> it = children.iterator(); it.hasNext(); ) {
        Tree<T> child = it.next();
        buffer.append(' ');
        buffer.append(child.toString(escaper));
      }
      buffer.append(')');
      result = buffer.toString();
    }

    return result;
  }

  public void setTransientId(int id) {
    this.transientId = id;
  }

  public int getTransientId() {
    return transientId;
  }

  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    
    boolean result = false;
    Tree otherTree = null;

    try {
      otherTree = (Tree)other;
    }
    catch (ClassCastException e) {
      // if we get here, the objects are not equal!
      return false;
    }

    if (otherTree != null) {
      if (data == null && otherTree.data != null) {
        return false;
      }
      else if ((data == null && otherTree.data == null) || (data != null && data.equals(otherTree.data))) {
        if (children == null) {
          result = (otherTree.children == null);
        }
        else if (otherTree.children != null) {
          if (children.size() != otherTree.children.size()) {
            return false;
          }

          result = true;
          final int numChildren = children.size();
          for (int childNum = 0; childNum < numChildren; ++childNum) {
            final Tree<T> myChild = children.get(childNum);
            final Object otherChild = otherTree.children.get(childNum);
            if (!myChild.equals(otherChild)) {
              result = false;
              break;
            }
          }
        }
      }
    }

    return result;
  }

  public synchronized int hashCode() {
    if (!gotHashCode) {
      this._hashCode = computeHashCode();
      gotHashCode = true;
    }
    return _hashCode;
  }

  private final int computeHashCode() {
    int result = 1;

    result = result * 31 + (data == null ? 0 : data.hashCode());
    if (children != null) {
      for (Tree<T> child : children) {
        result = result * 31 + child.hashCode();
      }
    }

    return result;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }

  public List<Tree<T>> getChildren() {
    return children;
  }

  public boolean hasChildren() {
    return children != null && children.size() > 0;
  }

  public int numChildren() {
    return children != null ? children.size() : 0;
  }

  public Tree<T> getParent() {
    return parent;
  }

  public Map<String, Object> getAttributes() {
    if (this.attributes == null) this.attributes = new HashMap<String, Object>();
    return this.attributes;
  }

  public boolean hasAttributes() {
    return attributes != null && attributes.size() > 0;
  }

  /**
   * Get the depth of this node in its tree, where the root is at depth 0.
   */
  public int depth() {
    int result = 0;
    Tree<T> p = parent;

    while (p != null) {
      p = p.parent;
      ++result;
    }

    return result;
  }

  /**
   * Get the depth of this node in its tree from the given ancestor node.
   */
  public int depth(Tree<T> ancestor) {
    int result = 0;
    Tree<T> p = parent;

    while (p != ancestor && p != null) {
      p = p.parent;
      ++result;
    }

    return result;
  }

  /**
   * Get the number of siblings (including this node).
   */
  public int getNumSiblings() {
    int result = 0;

    if (parent != null && parent.children != null) {
      result = parent.children.size();
    }

    return result;
  }

  /**
   * Get this tree's siblings (including this tree), all having the same parent.
   */
  public List<Tree<T>> getSiblings() {
    List<Tree<T>> result = new ArrayList<Tree<T>>();

    if (parent == null) {
      result.add(this);
    }
    else {
      result.addAll(parent.children);
    }

    return result;
  }

  /**
   * Get this node's sibling position.
   */
  public int getSiblingPosition() {
    int result = 0;

    if (parent != null) {
      result = getPosition(this, parent.children);
    }

    return result;
  }

  /**
   * Get the sibling node that follows this sibling or null if this is the last
   * sibling.
   */
  public Tree<T> getNextSibling() {
    Tree<T> result = null;

    if (parent != null) {
      final List<Tree<T>> siblings = parent.children;
      final int sibPos = getPosition(this, siblings) + 1;
      if (sibPos < siblings.size()) {
        result = siblings.get(sibPos);
      }
    }

    return result;
  }

  /**
   * Get the sibling node that precedes this sibling or null if this is the first
   * sibling.
   */
  public Tree<T> getPrevSibling() {
    Tree<T> result = null;

    if (parent != null) {
      final List<Tree<T>> siblings = parent.children;
      final int sibPos = getPosition(this, siblings) - 1;
      if (sibPos >= 0) {
        result = siblings.get(sibPos);
      }
    }

    return result;
  }

  private final int getPosition(Tree<T> node, List<Tree<T>> siblings) {
    int result = 0;

    //NOTE: need to do instance equality, not .equals to find the right node!
    for (Tree<T> sibling : siblings) {
      if (node == sibling) return result;
      ++result;
    }

    return result;
  }

  /**
   * Get all nodes at this node's depth (including this tree).
   */
  public List<Tree<T>> getGlobalSiblings() {
    return getRoot().treesAtDepth(depth());
  }

  /**
   * Get this node's global sibling position.
   */
  public int getGlobalSiblingPosition() {
    return getPosition(this, getGlobalSiblings());
  }

  /**
   * Get the root of this tree.
   */
  public Tree<T> getRoot() {
    Tree<T> root = this;

    while (root.parent != null) {
      root = root.parent;
    }

    return root;
  }

  /**
   * The RootPath holds tree nodes from the root to this node (inclusive).
   */
  public LinkedList<Tree<T>> getRootPath() {
    if (this.rootPath == null) {
      this.rootPath = new LinkedList<Tree<T>>();
      Tree<T> curNode = this;
      while (curNode != null) {
        this.rootPath.addFirst(curNode);
        curNode = curNode.parent;
      }
    }
    return this.rootPath;
  }

  public boolean isAncestor(Tree<T> other, boolean selfIsAncestor) {
    boolean result = false;

    Tree<T> parent = selfIsAncestor ? other : other.parent;
    while (parent != null) {
      if (parent == this) {
        result = true;
        break;
      }
      parent = parent.parent;
    }

    return result;
  }

  /**
   * Determine whether this node is an ancestor of the other.
   * <p>
   * Note that a node is not its own ancestor.
   *
   * @param other  The other node.
   *
   * @return true if this node is an ancestor of other.
   */
  public boolean isAncestor(Tree<T> other) {
    return isAncestor(other, false);
  }

  /**
   * Determine whether this node is a descendant of the other.
   * <p>
   * Note that a node is not its own descendant.
   *
   * @param other  The other node.
   *
   * @return true if this node is an descendant of other.
   */
  public boolean isDescendant(Tree<T> other) {
    return other.isAncestor(this);
  }

  /**
   * Determine whether this node is a descendant of the other.
   * <p>
   * Note that a node is not its own descendant.
   *
   * @param other  The other node.
   *
   * @return true if this node is an descendant of other.
   */
  public boolean isDescendant(Tree<T> other, boolean selfIsDescendant) {
    return other.isAncestor(this, selfIsDescendant);
  }

  /**
   * Return all trees at the given depth (0-based) from this node, where
   * this node is depth 0, its children are depth 1, and so on.
   * <p>
   * @return the nodes at the specified depth (possibly empty, but not null).
   */
  public List<Tree<T>> treesAtDepth(int depth) {
    List<Tree<T>> result = new ArrayList<Tree<T>>();

    if (depth == 0) {
      result.add(this);
    }
    else if (depth > 0) {
      if (children != null) {
        for (Tree<T> child : children) {
          result.addAll(child.treesAtDepth(depth - 1));
        }
      }
    }

    return result;
  }

  /**
   * Compute (1+) the maximum depth of nodes under this tree (0-based).
   * <p>
   * The depth of a tree with no children is 1; etc.
   */
  public int maxDepth() {
    int depth = 1;

    if (children != null) {
      int maxChildDepth = 0;
      for (Tree<T> child : children) {
        int childDepth = child.maxDepth();
        if (childDepth > maxChildDepth) {
          maxChildDepth = childDepth;
        }
      }
      depth += maxChildDepth;
    }

    return depth;
  }

  /**
   * Compute the number of leaf nodes in this tree.
   * <p>
   * If this tree is a leaf itself(has no children) the number of leaves is 1
   */
  public int numLeaves() {
    int leaves = 1;

    if (children != null) {
      int childLeafCount = 0;
      for (Tree<T> child : children) {
        childLeafCount += child.numLeaves();
      }
      return childLeafCount;
    }

    return leaves;
  }
  
  /**
   * Determine whether this node is 'equidepth', defined as all terminal
   * nodes under this node being at the same depth relative to this node.
   */
  public boolean equidepth() {
    boolean result = true;

    int depth = -1;
    final List<Tree<T>> leafNodes = gatherLeaves();
    for (Tree<T> leafNode : leafNodes) {
      final int curDepth = leafNode.depth(this);
      if (depth == -1) {
        depth = curDepth;
      }
      else if (depth != curDepth) {
        result = false;
        break;
      }
    }

    return result;
  }

  /**
   * Ascend to an ancestor that has siblings. 
   */
  public Tree<T> ascend() {
    Tree<T> node = this;
    Tree<T> priorNode = node;
    while (node.getNumSiblings() == 1 && node.getParent() != null) {
      priorNode = node;
      node = node.getParent();
    }
    return priorNode;
  }

  public Tree<T> addChild(T childData) {
    Tree<T> result = new Tree<T>(childData);
    addChild(result);
    return result;
  }

  public Tree<T> addChild(Tree<T> child) {
    this.gotHashCode = false;
    if (children == null) {
      children = new ArrayList<Tree<T>>();
    }
    children.add(child);
    child.parent = this;
    return child;
  }

  /**
   * Get the path of nodes from the root to this node.
   */
  public List<Tree<T>> getPath() {
    if (_path == null) {
      _path = new LinkedList<Tree<T>>();

      Tree<T> curNode = this;
      while (curNode != null) {
        _path.addFirst(curNode);
        curNode = curNode.getParent();
      }
    }

    return _path;
  }

  public List<List<T>> getPaths() {
    return getPathsAux(new ArrayList<T>());
  }

  public String getPathString() {
    return buildPathString(false, ".");
  }

  public String buildPathString(boolean indexFlag, String pathDelim) {
    final StringBuilder result = new StringBuilder();

    Tree<T> node = this;
    while (node != null) {
      if (result.length() > 0) result.insert(0, pathDelim);
      if (indexFlag) {
        final int sibNum = getSiblingPosition();
        result.insert(0, "[" + sibNum + "]");
      }
      result.insert(0, node.getData().toString());
      node = node.getParent();
    }

    return result.toString();
  }


  /**
   * Get a TraversalIterator<T>, which is an Iterator<Tree<T>>, starting from this node.
   */
  public TraversalIterator<T> iterator(Traversal traversal) {
    TraversalIterator<T> result = null;

    switch (traversal) {
    case REVERSE_DEPTH_FIRST:
        result = new ReverseDepthFirstIterator<T>(this);
        break;
    case DEPTH_FIRST:
        result = new DepthFirstIterator<T>(this);
        break;
    case REVERSE_BREADTH_FIRST:
        result = new ReverseBreadthFirstIterator<T>(this);
        break;
      default :
        result = new BreadthFirstIterator<T>(this);
    }

    return result;
  }


  /**
   * Collect the nodes whose data equals the given data.
   */
  public List<Tree<T>> findNodes(T data, Traversal traversal) {
    List<Tree<T>> result = null;

    for (Iterator<Tree<T>> it = iterator(traversal); it.hasNext(); ) {
      final Tree<T> node = it.next();
      final T nodeData = node.getData();
      if (nodeData == data || (nodeData != null && nodeData.equals(data))) {
        if (result == null) result = new ArrayList<Tree<T>>();
        result.add(node);
      }
    }

    return result;
  }

  public Tree<T> findFirst(T data, Traversal traversal) {
    Tree<T> result = null;

    for (Iterator<Tree<T>> it = iterator(traversal); it.hasNext(); ) {
      final Tree<T> node = it.next();
      final T nodeData = node.getData();
      if (nodeData == data || (nodeData != null && nodeData.equals(data))) {
        result = node;
        break;
      }
    }

    return result;
  }

  /**
   * Count the number of nodes in the tree where this node is root, including
   * this node.
   */
  public int countNodes() {
    int result = 1;

    for (Iterator<Tree<T>> it = iterator(Traversal.DEPTH_FIRST); it.hasNext(); ) {
      final Tree<T> node = it.next();
      ++result;
    }

    return result;
  }

  /**
   * Prune this node from its tree.
   */
  public void prune() {
    this.parent = null;
  }

  /**
   * Prune this node from its tree as specified.
   */
  public void prune(boolean disconnectParent, boolean disconnectAsChild) {
    if (disconnectAsChild && parent != null && parent.children != null) {
      gotHashCode = false;
      parent.children.remove(this);
      if (parent.children.size() == 0) parent.children = null;
    }
    if (disconnectParent) this.parent = null;
  }

  /**
   * Move this node's children to another parent.
   */
  public void moveChildrenTo(Tree<T> newParent) {
    if (this.children != null && this.children.size() > 0) {
      final List<Tree<T>> children = new ArrayList<Tree<T>>(this.children);
      for (Tree<T> child : children) {
        child.prune(true, true);
        newParent.addChild(child);
      }
    }
  }

  /**
   * Prune this node's children from the tree.
   * <p>
   * Note: each child's "parent" will still point to this node. Fix them if
   *       needed.
   *
   * @return the pruned children.
   */
  public List<Tree<T>> pruneChildren() {
    final List<Tree<T>> result = children;
    // don't worry about cleaning up back-references in children to this as parent?
    this.children = null;
    return result;
  }

  public List<Tree<T>> gatherLeaves() {
    final List<Tree<T>> result = new ArrayList<Tree<T>>();
    gatherLeaves(result);
    return result;
  }

  private final void gatherLeaves(List<Tree<T>> result) {
    if (this.children == null) result.add(this);
    else {
      for (Tree<T> child : children) {
        child.gatherLeaves(result);
      }
    }
  }

  public Tree<T> getFirstLeaf() {
    Tree<T> result = this;

    if (hasChildren()) {
      result = children.get(0).getFirstLeaf();
    }

    return result;
  }

  public Tree<T> getLastLeaf() {
    Tree<T> result = this;

    if (hasChildren()) {
      result = children.get(children.size() - 1).getLastLeaf();
    }

    return result;
  }

// O(n) impl -- top down
  public Tree<T> getDeepestCommonAncestor(Tree<T> other) {
    if (other == null) return null;
    if (this == other) return this;

    Tree<T> result = null;

    final Iterator<Tree<T>> myPathIter = getPath().iterator();
    final Iterator<Tree<T>> otherPathIter = other.getPath().iterator();

    while (myPathIter.hasNext() && otherPathIter.hasNext()) {
      final Tree<T> myNode = myPathIter.next();
      final Tree<T> otherNode = otherPathIter.next();

      if (myNode != otherNode) {
        break;  // diverged.
      }
      else {
        result = myNode;
      }
    }

    return result;
  }

/**
// O(n-squared) impl -- bottom up
  public Tree<T> getDeepestCommonAncestor(Tree<T> other) {
    if (other == null) return null;
    if (this == other) return this;

    if (isAncestor(other)) return this;
    if (other.isAncestor(this)) return other;

    do {
      other = other.getParent();
      if (other != null && other.isAncestor(this)) {
        return other;
      }
    } while (other != null);

    return null;
  }
*/

//todo: include toString(String pictureString)? adding/inserting nodes?, deleting/renaming nodes?

  private List<List<T>> getPathsAux(List<T> basePath) {
    List<List<T>> result = new ArrayList<List<T>>();
    List<T> path = new ArrayList<T>(basePath);
    path.add(data);

    if (children != null) {
      for (Iterator<Tree<T>> it = children.iterator(); it.hasNext(); ) {
        Tree<T> child = it.next();
        result.addAll(child.getPathsAux(path));
      }
    }
    else {
      result.add(path);
    }

    return result;
  }

  /**
   * Get this node's leaves' data as a concatenated string (with a space for
   * the delimiter).
   */
  public final String getLeafText() {
    final StringBuilder result = new StringBuilder();

    final List<Tree<T>> leaves = gatherLeaves();
    for (Tree<T> leaf : leaves) {
      if (result.length() > 0) result.append(' ');
      result.append(leaf.data.toString());
    }

    return result.toString();
  }

  /**
   * Set the child at the given position to be the new child, returning the
   * oldChild or add the newChild, returning null.
   */
  public Tree<T> setChild(int childPos, Tree<T> newChild) {
    Tree<T> result = null;

    gotHashCode = false;

    if (children != null && childPos < children.size() && childPos >= 0) {
      result = children.get(childPos);
      result.prune();
      children.set(childPos, newChild);
      newChild.parent = this;
    }
    else {
      addChild(newChild);
    }

    return result;
  }
}
