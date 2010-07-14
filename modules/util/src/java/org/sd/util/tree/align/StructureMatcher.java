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
 * Utility class for finding the common structure between two trees.
 * <p>
 * A StructureMatcher instance is created with two trees whose structure
 * is to be compared.
 * <p>
 * If the Equals method of the tree's data is not appropriate for aligning
 * nodes, then a NodeComparer should be set on the StructureMatcher instance.
 * Note that (re)setting the NodeComparer will affect the results provided by
 * the instance.
 * <p>
 * The Structurematcher instance can then provide matching and mismatching
 * ratios between the two trees, where MatchRatio1 shows counts relative to
 * Tree1's nodes and MatchRatio2 shows counts relative to Tree2.
 * <p>
 * The StructureMatcher instance can also provide a Template, which is a new
 * Tree containing only the intersection between the two input Trees.
 * <p>
 * NOTE: Type "T" is the type of data contained in the trees.
 *
 * @author Spence Koehler
 */
public class StructureMatcher<T> {
  
  /**
   * The first Tree to be compared with the second.
   */
  private Tree<T> tree1;
  public Tree<T> getTree1() {
    return tree1;
  }

  /**
   * The second Tree to be compared with the first.
   */
  private Tree<T> tree2;
  public Tree<T> getTree2() {
    return tree2;
  }

  private NodeComparer<T> _nodeComparer;
  /**
   * NodeComparer to be set if the Equals method of the Tree's Data (of type T)
   * is not the proper function to apply for determining node equality.
   */
  public NodeComparer<T> getNodeComparer() {
    return _nodeComparer;
  }

  public final void setNodeComparer(NodeComparer<T> value) {
    this._nodeComparer = value;

    // these will need to be recomputed
    this._nodePair = null;
    this._template = null;
    this._matchRatio1 = null;
    this._matchRatio2 = null;
  }

  private NodePair<T> _nodePair;
  /**
   * Data structure for recursively pairing nodes from the two trees.
   */
  private NodePair<T> getNodePair() {
    if (this._nodePair == null) {
      this._nodePair = new NodePair<T>(tree1, tree2, getNodeComparer());
    }
    return this._nodePair;
  }

  private void setNodePair(NodePair<T> value) {
    this._nodePair = value;
  }

  private Tree<T> _template;
  /**
   * Contains the intersection between the two Trees.
   */
  public Tree<T> getTemplate() {
    if (_template == null) {
      _template = getNodePair().getIntersectionTree();
    }
    return _template;
  }

  private MatchRatio _matchRatio1;
  /**
   * Contains the matching and mismatching node counts from Tree1 as compared to Tree2.
   */
  public MatchRatio getMatchRatio1() {
    if (_matchRatio1 == null) {
      _matchRatio1 = getNodePair().getMatchRatio1();
    }
    return _matchRatio1;
  }

  private MatchRatio _matchRatio2;
  /**
   * Contains the matching and mismatching node counts from Tree2 as compared to Tree1.
   */
  public MatchRatio getMatchRatio2() {
    if (_matchRatio2 == null) {
      _matchRatio2 = getNodePair().getMatchRatio2();
    }
    return _matchRatio2;
  }

  /**
   * Selects the MatchRatio perspective (from Tree1 or Tree2) yieling the highest overall
   * alignment.
   */
  public MatchRatio getMatchRatio() {
    MatchRatio result = null;

    final MatchRatio matchRatio1 = getMatchRatio1();
    final MatchRatio matchRatio2 = getMatchRatio2();
    final double match1 = matchRatio1.getMatch();
    final double match2 = matchRatio2.getMatch();

    if (match1 == match2) {
      if (matchRatio1.getMismatch() <= matchRatio2.getMismatch()) {
        result = matchRatio1;
      }
      else {
        result = matchRatio2;
      }
    }
    else if (match1 > match2) {
      result = matchRatio1;
    }
    else {
      result = matchRatio2;
    }

    return result;
  }


  /**
   * Construct with the two trees to compare.
   * 
   * If the Equals method of the tree's data is not appropriate for aligning
   * nodes, then a NodeComparer should be set on the instance.
   */
  public StructureMatcher(Tree<T> tree1, Tree<T> tree2) {
    this(tree1, tree2, null);
  }

  /**
   * Construct with the two trees to compare according to the given NodeComparer.
   */
  public StructureMatcher(Tree<T> tree1, Tree<T> tree2, NodeComparer<T> nodeComparer) {
    this.tree1 = tree1;
    this.tree2 = tree2;
    this.setNodeComparer(nodeComparer);
  }
}
