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
import org.sd.util.StringSplitter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A simple tree builder.
 * <p>
 * @author Spence Koehler
 */
public class SimpleTreeBuilder<T> implements TreeBuilder<T> {
  
  private TreeBuilderStrategy<T> strategy;

  public SimpleTreeBuilder(TreeBuilderStrategy<T> strategy) {
    this.strategy = strategy;
  }

  /**
   * Our formatting is (A (B "C")), so symbols to escape are
   * the delimiters: ( ) space " [ ]
   * <p>
   * For completely reversible escaping.
   */
  private static final Escaper escaper = new Escaper(
    new String[][]{
      {"&", "&amp;"}, // ampersand -- this MUST be one of FIRST 2!
      {";", "&sc;"}, // semicolon -- this MUST be one of FIRST 2!

      {" ", "&space;"}, // space
      {"\"", "&dq;"}, // double quotes

      {"(", "&lp;"},  // left paren
      {")", "&rp;"},  // right paren
      {"[", "&lsb;"}, // left square brace
      {"]", "&rsb;"}, // right square brace

    }, 2);

  /**
   * Formatting for visual escaping, which may not be completely reversible.
   */
  private static final Escaper visualEscaper = new Escaper(
    new String[][]{
      {"(", "\\("},
      {")", "\\)"},
      {"\"", "\\\""},
    }, 2);

  public static Escaper getEscaper() {
    return escaper;
  }

  public static Escaper getVisualEscaper() {
    return visualEscaper;
  }

  /**
   * Apply escaping operations on inner components to control for delimiters
   * used by the Objectifier in the data between delimiters.
   */
  protected String escape(String component) {
    return escaper.escape(component);
  }

  /**
   * Unapply escaping operations done on inner components to control for
   * delimiters used by the Objectifier in the data between delimiters.
   */
  protected String unescape(String component) {
    return escaper.unescape(component);
  }

  /**
   * Build a tree from the given string.
   * <p>
   * The returned Tree can be used as input to the buildString method
   * to create a String.
   * <p>
   * The returned Tree will will hold to the property that
   * treeString.equals(buildString(tree)), but this won't necessarily
   * correspond to the original tree from which the string was created
   * because only the toString version of objects will have been
   * captured.
   */
  public Tree<T> buildTree(String treeString) {
    Tree<T> result = null;
    if (treeString != null) {
      result = doBuild(treeString.trim());
    }
    return result;
  }

  // given string of form "node" or "(node child1 child2 ... childN)" create a tree for node with its children
  private final Tree<T> doBuild(String treeString) {
    Tree<T> result = null;
    if (treeString.length() == 0) return result;

    // extract for a(b)c
    final String[] listMatches = StringSplitter.splitOnParens(treeString);

    if (listMatches != null) {
      // ignore a & c, split b (has parens stripped) into elements
      final List<String> elements = splitElements(listMatches[1]);
      if (elements != null) {
        final Iterator<String> iter = elements.iterator();

        // first element is a parent to all following elements
        if (iter.hasNext()) result = buildNode(iter.next().trim());

        while (iter.hasNext()) {
          final Tree<T> child = doBuild(iter.next().trim());
          if (child != null) {
            strategy.addChild(result, child);
          }
        }
      }
    }
    else {
      final boolean hasParen = treeString.charAt(0) == '(';
      if (!hasParen) result = buildNode(treeString);
    }

    return result;
  }

  private final Tree<T> buildNode(String treeString) {
    final T coreData = strategy.constructCoreNodeData(unescape(treeString));
    return strategy.constructNode(coreData);
  }

  // string is of form "x1 x2 ... xN", where xi may be of form "node" or "(node child1 child2 ... childM)".
  private final List<String> splitElements(String elementsString) {
    List<String> result = null;

    while (elementsString != null && elementsString.length() > 0) {
      final boolean hasParen = elementsString.charAt(0) == '(';
      if (hasParen) {
        final String[] listMatches = StringSplitter.splitOnParens(elementsString);
        if (listMatches != null) {
          if (result == null) result = new ArrayList<String>();
          result.add('(' + listMatches[1].trim() + ')');
          elementsString = listMatches[2].trim();
        }
        else elementsString = null;  // bad form
      }
      else {
        final String[] pieces = StringSplitter.splitOnFirstSpace(elementsString);
        if (pieces != null) {
          if (result == null) result = new ArrayList<String>();
          result.add(pieces[0].trim());
          elementsString = (pieces.length > 1) ? pieces[1].trim() : null;
        }
        else elementsString = null;
      }
    }

    return result;
  }

  /**
   * Build a string representing the given tree.
   * <p>
   * The returned string will always serve as input to the buildTree
   * method to generate a tree.
   * <p>
   * This will be of the form (A child1 child2 ... childN)
   * <p>
   * Where A = the constrained feature's asString form, childi is a child parseComponent,
   * <p>
   * The returned string will not necessarily hold to the property that
   * tree.equals(buildTree(string)) because only the "toString" version
   * of objects will be preserved and regenerated as strings instead of
   * as the original objects.
   */
  public String buildString(Tree<T> tree) {
    return strategy.buildString(tree, escaper);
  }

}
