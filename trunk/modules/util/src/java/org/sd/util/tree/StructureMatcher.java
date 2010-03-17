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


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.sd.util.MathUtil;

/**
 * Utility to find matching structure between two trees.
 * <p>
 * @author Spence Koehler
 */
public class StructureMatcher <T> {

  public static final class MatchRatio {
    public final int totalNodes;
    public final int matchingNodes;
    public final int mismatchingNodes;

    public MatchRatio(int matchingNodes, int mismatchingNodes) {
      this.matchingNodes = matchingNodes;
      this.mismatchingNodes = mismatchingNodes;
      this.totalNodes = matchingNodes + mismatchingNodes;
    }

    public double getMatchRatio() {
      return ((double)matchingNodes) / ((double)totalNodes);
    }

    public double getMismatchRatio() {
      return ((double)mismatchingNodes) / ((double)totalNodes);
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.
        append('[').
        append(matchingNodes).
        append(',').
        append(mismatchingNodes).
        append('/').
        append(totalNodes).
        append("] ").
        append(MathUtil.doubleString(getMatchRatio(), 4)).
        append(" / ").
        append(MathUtil.doubleString(getMismatchRatio(), 4));

      return result.toString();
    }
  }


  private Tree<T> tree1;
  private Tree<T> tree2;
  private NodePair nodePair;
  private NodeComparer<T> nodeComparer;

  private Tree<T> _template;
  private MatchRatio _matchRatio1;
  private MatchRatio _matchRatio2;

  public StructureMatcher(Tree<T> tree1, Tree<T> tree2) {
    this.tree1 = tree1;
    this.tree2 = tree2;
    this.nodePair = new NodePair(tree1, tree2);
    this.nodeComparer = null;

    this._template = null;
    this._matchRatio1 = null;
    this._matchRatio2 = null;
  }

  public void setNodeComparer(NodeComparer<T> nodeComparer) {
    this.nodeComparer = nodeComparer;
  }

  /**
   * Get a new tree that serves as a template of the shared structure between
   * the two trees. Note that this template is the intersection of the two trees.
   */
  public Tree<T> getTemplate() {
    if (this._template == null) {
      this._template = nodePair.getIntersectionTree();
    }
    return this._template;
  }


  /**
   * Get the match ratio for the first tree compared to the second.
   */
  public MatchRatio getMatchRatio1() {
    if (this._matchRatio1 == null) {
      this._matchRatio1 = nodePair.getMatchRatio1();
    }
    return this._matchRatio1;
  }

  /**
   * Get the match ratio for the second tree compared to the first.
   */
  public MatchRatio getMatchRatio2() {
    if (this._matchRatio2 == null) {
      this._matchRatio2 = nodePair.getMatchRatio2();
    }
    return this._matchRatio2;
  }

  /**
   * Get the match ratio that best represents the amount of structural overlap
   * between the two trees.
   */
  public MatchRatio getMatchRatio() {
    MatchRatio result = null;

    final MatchRatio mr1 = getMatchRatio1();
    final MatchRatio mr2 = getMatchRatio2();
    final double mr1Match = mr1.getMatchRatio();
    final double mr2Match = mr2.getMatchRatio();

    if (mr1Match == mr2Match) {
      final double mr1Mismatch = mr1.getMismatchRatio();
      final double mr2Mismatch = mr2.getMismatchRatio();

      if (mr1Mismatch <= mr2Mismatch) {
        result = mr1;
      }
      else {
        result = mr2;
      }
    }
    else if (mr1Match > mr2Match) {
      result = mr1;
    }
    else {  // mr1Match < mr2Match
      result = mr2;
    }

    return result;
  }


  private final boolean basicMatch(Tree<T> node1, Tree<T> node2) {
    boolean result = false;

    if (nodeComparer == null) {
      result = defaultBasicMatch(node1, node2);
    }
    else {
      result = nodeComparer.matches(node1, node2);
    }

    return result;
  }

  private final boolean defaultBasicMatch(Tree<T> node1, Tree<T> node2) {
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


  private final class NodePair {

    private Tree<T> node1;
    private Tree<T> node2;
    private Boolean basicMatch;
    private List<ChildWrapper> childWrappers1;
    private List<ChildWrapper> childWrappers2;

    public NodePair(Tree<T> node1, Tree<T> node2) {
      this.node1 = node1;
      this.node2 = node2;
      this.basicMatch = null;
      this.childWrappers1 = null;
      this.childWrappers2 = null;
    }

    public Tree<T> getNode1() {
      return node1;
    }

    public Tree<T> getNode2() {
      return node2;
    }

    public boolean isBasicMatch() {
      if (this.basicMatch == null) {
        this.basicMatch = basicMatch(node1, node2);
      }
      return this.basicMatch;
    }

    public List<ChildWrapper> getChildWrappers1() {
      if (this.childWrappers1 == null) {
        createChildWrappers();
      }
      return this.childWrappers1;
    }

    public List<ChildWrapper> getChildWrappers2() {
      if (this.childWrappers2 == null) {
        createChildWrappers();
      }
      return this.childWrappers2;
    }

    public Tree<T> getIntersectionTree() {
      Tree<T> result = null;

      if (isBasicMatch()) {
        result = copyNode(node1);

        for (ChildWrapper childWrapper : getChildWrappers1()) {
          if (childWrapper.matchingChildren != null) {
            final Tree<T> intersectingChild = childWrapper.matchingChildren.getIntersectionTree();
            if (intersectingChild != null) {
              result.addChild(intersectingChild);
            }
          }
        }
      }

      return result;
    }

    public MatchRatio getMatchRatio1() {
      final AtomicInteger matchingNodes = new AtomicInteger(0);
      final AtomicInteger mismatchingNodes = new AtomicInteger(0);

      getMatchRatio1(matchingNodes, mismatchingNodes);

      return new MatchRatio(matchingNodes.get(), mismatchingNodes.get());
    }

    public MatchRatio getMatchRatio2() {
      final AtomicInteger matchingNodes = new AtomicInteger(0);
      final AtomicInteger mismatchingNodes = new AtomicInteger(0);

      getMatchRatio2(matchingNodes, mismatchingNodes);

      return new MatchRatio(matchingNodes.get(), mismatchingNodes.get());
    }

    private final void getMatchRatio1(AtomicInteger matchingNodes, AtomicInteger mismatchingNodes) {
      if (!isBasicMatch()) {
        countNodes(node1, mismatchingNodes);
      }
      else {
        matchingNodes.incrementAndGet();

        for (ChildWrapper childWrapper : getChildWrappers1()) {
          if (childWrapper.matchingChildren == null) {
            countNodes(childWrapper.childNode, mismatchingNodes);
          }
          else {
            childWrapper.matchingChildren.getMatchRatio1(matchingNodes, mismatchingNodes);
          }
        }
      }
    }

    private final void getMatchRatio2(AtomicInteger matchingNodes, AtomicInteger mismatchingNodes) {
      if (!isBasicMatch()) {
        countNodes(node2, mismatchingNodes);
      }
      else {
        matchingNodes.incrementAndGet();

        for (ChildWrapper childWrapper : getChildWrappers2()) {
          if (childWrapper.matchingChildren == null) {
            countNodes(childWrapper.childNode, mismatchingNodes);
          }
          else {
            childWrapper.matchingChildren.getMatchRatio2(matchingNodes, mismatchingNodes);
          }
        }
      }
    }

    private final void createChildWrappers() {
      this.childWrappers1 = new ArrayList<ChildWrapper>();
      this.childWrappers2 = new ArrayList<ChildWrapper>();

      final List<Tree<T>> unmatchedChildren1 = new LinkedList<Tree<T>>();
      final List<Tree<T>> unmatchedChildren2 = new LinkedList<Tree<T>>();

      if (node1.hasChildren()) {
        unmatchedChildren1.addAll(node1.getChildren());
      }
      if (node2.hasChildren()) {
        unmatchedChildren2.addAll(node2.getChildren());
      }

      while (unmatchedChildren1.size() > 0 && unmatchedChildren2.size() > 0) {
        final int match2Pos = scanForMatch(unmatchedChildren1, unmatchedChildren2);

        if (match2Pos < 0) {
          // couldn't find match for curIndex1
          final Tree<T> child1 = unmatchedChildren1.remove(0);
          childWrappers1.add(new ChildWrapper(child1));
        }
        else {
          // found match for first child1 at match2Pos
          final Tree<T> matchedChild1 = unmatchedChildren1.remove(0);

          for (int i = 0 ; i < match2Pos; ++i) {
            final Tree<T> unmatchedChild2 = unmatchedChildren2.remove(0);
            childWrappers2.add(new ChildWrapper(unmatchedChild2));
          }

          final Tree<T> matchedChild2 = unmatchedChildren2.remove(0);

          final ChildWrapper matchedWrapper = new ChildWrapper(new NodePair(matchedChild1, matchedChild2));
          childWrappers1.add(matchedWrapper);
          childWrappers2.add(matchedWrapper);
        }
      }

      // account for any remaining unmatched children
      for (Tree<T> unmatchedChild1 : unmatchedChildren1) {
        childWrappers1.add(new ChildWrapper(unmatchedChild1));
      }

      for (Tree<T> unmatchedChild2 : unmatchedChildren2) {
        childWrappers2.add(new ChildWrapper(unmatchedChild2));
      }
    }

    private final int scanForMatch(List<Tree<T>> unmatchedChildren1, List<Tree<T>> unmatchedChildren2) {
      int result = -1;

      final Tree<T> child1 = unmatchedChildren1.get(0);
      for (int i = 0; i < unmatchedChildren2.size(); ++i) {
        final Tree<T> child2 = unmatchedChildren2.get(i);
        if (basicMatch(child1, child2)) {
          result = i;
          break;
        }
      }

      return result;
    }

    private final Tree<T> copyNode(Tree<T> node) {
      return new Tree<T>(node.getData());
    }

    private final void countNodes(Tree<T> node, AtomicInteger counter) {
      for (TraversalIterator<T> iter = node.iterator(Tree.Traversal.BREADTH_FIRST); iter.hasNext(); ) {
        final Tree<T> curNode = iter.next();
        counter.incrementAndGet();
      }
    }
  }

  private final class ChildWrapper {
    public final Tree<T> childNode;
    public final NodePair matchingChildren;

    public ChildWrapper(Tree<T> childNode) {
      this.childNode = childNode;
      this.matchingChildren = null;
    }

    public ChildWrapper(NodePair matchingChildren) {
      this.childNode = matchingChildren.getNode1();
      this.matchingChildren = matchingChildren;
    }
  }
}
