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


import java.util.List;
import org.sd.util.tree.Tree;

/**
 * TreeAlignment class for consolidating access to StructureMatcher and
 * LeafDiffer instances over two trees.
 * <p>
 * High-level accessor class that consolidates using StructureMatcher and
 * LeafDiffer functionality for aligning (including matching and diff'ing)
 * two trees.
 * <p>
 * @author Spence Koehler
 */
public class TreeAlignment<T> {
  
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

  private StructureMatcher<T> structureMatcher;
  protected StructureMatcher<T> getStructureMatcher() {
    return structureMatcher;
  }

  private LeafDiffer<T> leafDiffer;
  protected LeafDiffer<T> getLeafDiffer() {
    return leafDiffer;
  }

  /**
   * The template built from the intersection of the two trees.
   */
  public Tree<T> getTemplate() {
    return structureMatcher.getTemplate();
  }

  /**
   * Accessor for the "best" MatchRatio for the two trees.
   */
  public MatchRatio getMatchRatio() {
    return structureMatcher.getMatchRatio();
  }

  /**
   * Accessor for the disjunction nodes from the first tree.
   */
  public List<Tree<T>> getDisjunctionNodes1() {
    return leafDiffer.getDisjunctionNodes1();
  }

  /**
   * Accessor for the disjunction nodes from the second tree.
   */
  public List<Tree<T>> getDisjunctionNodes2() {
    return leafDiffer.getDisjunctionNodes2();
  }

  /**
   * Construct with the given parameters.
   */
  public TreeAlignment(Tree<T> tree1, Tree<T> tree2, NodeComparer<T> nodeComparer, TextExtractor<T> textExtractor) {
    this.tree1 = tree1;
    this.tree2 = tree2;

    this.structureMatcher = new StructureMatcher<T>(tree1, tree2, nodeComparer);
    this.leafDiffer = new LeafDiffer<T>(tree1, tree2, textExtractor);
  }
}
