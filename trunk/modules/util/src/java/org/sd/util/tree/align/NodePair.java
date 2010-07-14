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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.sd.util.tree.Tree;

/**
 * Data structure for recursively pairing nodes from two trees used by
 * StructureMatcher.
 * <p>
 * @author Spence Koehler
 */
class NodePair<T> {
  
  private Tree<T> node1;
  public Tree<T> getNode1() {
    return node1;
  }

  private Tree<T> node2;
  public Tree<T> getNode2() {
    return node2;
  }

  private NodeComparer<T> nodeComparer;
  private boolean computedBasicMatch;
  private boolean basicMatch;
  private List<ChildWrapper<T>> childWrappers1;
  private List<ChildWrapper<T>> childWrappers2;

  NodePair(Tree<T> node1, Tree<T> node2, NodeComparer<T> nodeComparer) {
    this.node1 = node1;
    this.node2 = node2;
    this.nodeComparer = nodeComparer;
    this.computedBasicMatch = false;
    this.basicMatch = false;
    this.childWrappers1 = null;
    this.childWrappers2 = null;
  }

  /**
   * Build a copy of the matching portions of the trees.
   */
  Tree<T> getIntersectionTree() {
    Tree<T> result = null;

    if (isBasicMatch()) {
      result = copyNode(node1);

      for (ChildWrapper<T> childWrapper : getChildWrappers1()) {
        if (childWrapper.getMatchingChildren() != null) {
          final Tree<T> intersectingChild = childWrapper.getMatchingChildren().getIntersectionTree();
          if (intersectingChild != null) {
            result.addChild(intersectingChild);
          }
        }
      }
    }

    return result;
  }

  /**
   * Count matches and mismatches of Node1's subtree.
   */
  MatchRatio getMatchRatio1() {
    int[] matchingNodes = new int[]{0};
    int[] mismatchingNodes = new int[]{0};

    getMatchRatio1(matchingNodes, mismatchingNodes);

    return new MatchRatio(matchingNodes[0], mismatchingNodes[0]);
  }

  /**
   * Count matches and mismatches of Node2's subtree.
   */
  MatchRatio getMatchRatio2() {
    int[] matchingNodes = new int[]{0};
    int[] mismatchingNodes = new int[]{0};

    getMatchRatio2(matchingNodes, mismatchingNodes);

    return new MatchRatio(matchingNodes[0], mismatchingNodes[0]);
  }

  /**
   * Recursive auxiliary for traversing through Node1 descendants.
   */
  private void getMatchRatio1(int[] matchingNodes, int[] mismatchingNodes) {
    if (!isBasicMatch()) {
      countNodes(node1, mismatchingNodes);
    }
    else {
      ++matchingNodes[0];

      for (ChildWrapper<T> childWrapper : getChildWrappers1()) {
        if (childWrapper.getMatchingChildren() == null) {
          countNodes(childWrapper.getChildNode(), mismatchingNodes);
        }
        else {
          childWrapper.getMatchingChildren().getMatchRatio1(matchingNodes, mismatchingNodes);
        }
      }
    }
  }

  /**
   * Recursive auxiliary for traversing through Node2 descendants.
   */
  private void getMatchRatio2(int[] matchingNodes, int[] mismatchingNodes) {
    if (!isBasicMatch()) {
      countNodes(node2, mismatchingNodes);
    }
    else {
      ++matchingNodes[0];

      for (ChildWrapper<T> childWrapper : getChildWrappers2()) {
        if (childWrapper.getMatchingChildren() == null) {
          countNodes(childWrapper.getChildNode(), mismatchingNodes);
        }
        else {
          childWrapper.getMatchingChildren().getMatchRatio2(matchingNodes, mismatchingNodes);
        }
      }
    }
  }

  /**
   * Determine whether this instance's nodes are a "basic" match, meaning the Trees'
   * Data matches, without regard to the surrounding tree structure.
   */
  boolean isBasicMatch() {
    if (!computedBasicMatch) {
      basicMatch = basicMatch(node1, node2);
      computedBasicMatch = true;
    }
    return basicMatch;
  }

  /**
   * Get this instance's first node's children, each paired with their
   * second node matches or single. Lazily create on the first request
   * and cache for reuse.
   */
  List<ChildWrapper<T>> getChildWrappers1() {
    if (childWrappers1 == null) {
      createChildWrappers();
    }
    return childWrappers1;
  }


  /**
   * Get this instance's second node's children, each paired with their
   * first node matches or single. Lazily create on the first request
   * and cache for reuse.
   */
  List<ChildWrapper<T>> getChildWrappers2() {
    if (childWrappers2 == null) {
      createChildWrappers();
    }
    return childWrappers2;
  }


  /**
   * Auxiliary for creating child wrappres, pairing nodes where possible.
   */
  private void createChildWrappers() {
    this.childWrappers1 = new ArrayList<ChildWrapper<T>>();
    this.childWrappers2 = new ArrayList<ChildWrapper<T>>();

    final LinkedList<Tree<T>> unmatchedChildren1 = new LinkedList<Tree<T>>();
    final LinkedList<Tree<T>> unmatchedChildren2 = new LinkedList<Tree<T>>();

    if (node1.hasChildren()) {
      for (Tree<T> child : node1.getChildren()) unmatchedChildren1.addLast(child);
    }
    if (node2.hasChildren()) {
      for (Tree<T> child : node2.getChildren()) unmatchedChildren2.addLast(child);
    }

    while (unmatchedChildren1.size() > 0 && unmatchedChildren2.size() > 0) {
      int match2Pos = scanForMatch(unmatchedChildren1, unmatchedChildren2);

      if (match2Pos < 0) {
        final Tree<T> child1 = unmatchedChildren1.removeFirst();
        childWrappers1.add(new ChildWrapper<T>(child1));
      }
      else {
        final Tree<T> matchedChild1 = unmatchedChildren1.removeFirst();

        for (int counter = 0; counter < match2Pos; ++counter) {
          final Tree<T> unmatchedChild2 = unmatchedChildren2.removeFirst();
          childWrappers2.add(new ChildWrapper<T>(unmatchedChild2));
        }

        final Tree<T> matchedChild2 = unmatchedChildren2.removeFirst();

        final ChildWrapper<T> matchedWrapper = new ChildWrapper<T>(new NodePair<T>(matchedChild1, matchedChild2, nodeComparer));
        childWrappers1.add(matchedWrapper);
        childWrappers2.add(matchedWrapper);
      }
    }

    for (Tree<T> unmatchedChild1 : unmatchedChildren1) {
      childWrappers1.add(new ChildWrapper<T>(unmatchedChild1));
    }
    for (Tree<T> unmatchedChild2 : unmatchedChildren2) {
      childWrappers2.add(new ChildWrapper<T>(unmatchedChild2));
    }
  }

  /**
   * Auxiliary for finding node matches.
   * 
   * This implementation assumes similar ordering in the tree to avoid full
   * N-squared complexity.
   */
  private int scanForMatch(LinkedList<Tree<T>> unmatchedChildren1, LinkedList<Tree<T>> unmatchedChildren2) {
    int result = -1;

    final Tree<T> child1 = unmatchedChildren1.getFirst();
    for (int childIndex = 0; childIndex < unmatchedChildren2.size(); ++childIndex) {
      final Tree<T> child2 = unmatchedChildren2.get(childIndex);
      if (basicMatch(child1, child2)) {
        result = childIndex;
        break;
      }
    }

    return result;
  }


  /**
   * Utility to apply default or overridden basic comparison between nodes.
   */
  boolean basicMatch(Tree<T> node1, Tree<T> node2) {
    boolean result = false;

    if (nodeComparer == null) {
      result = defaultBasicMatch(node1, node2);
    }
    else {
      result = nodeComparer.matches(node1, node2);
    }

    return result;
  }

  /**
   * Default determination of a basic match between two nodes.
   */
  private boolean defaultBasicMatch(Tree<T> node1, Tree<T> node2) {
    boolean result = false;

    final T data1 = node1.getData();
    final T data2 = node2.getData();

    if (data1 == null || data2 == null) {
      if (data1 == null && data2 == null) {
        result = true;
      }
    }
    else {
      result = data1.equals(data2);
    }

    return result;
  }

  /**
   * Auxiliary to create a new node containing another node's data.
   */
  private Tree<T> copyNode(Tree<T> node) {
    return new Tree<T>(node.getData());
  }

  /**
   * Auxiliary to count all nodes in a node's subtree.
   */
  private void countNodes(Tree<T> node, int[] counter) {
    
    for (Iterator<Tree<T>> it = node.iterator(Tree.Traversal.DEPTH_FIRST); it.hasNext(); ) {
      ++counter[0];
    }
  }
}
