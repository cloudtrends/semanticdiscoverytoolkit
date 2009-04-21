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
import java.util.Collection;
import java.util.List;

/**
 * Utility class to get a tree's hierarchy.
 * <p>
 * @author Spence Koehler
 */
public class Hierarchy <T> {
  
  private Tree<T> tree;

  public Hierarchy(Tree<T> tree) {
    this.tree = tree;
  }

  /**
   * Get the hierarchy strings in the form:
   * "<indent> node.data.toString() child1.data.toString() child2.data.toString()"
   */
  public Collection<String> getHierarchyStrings(int numIndentSpaces) {
    final List<String> result = new ArrayList<String>();
    getHierarchyStrings(result, tree, numIndentSpaces, 0);
    return result;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    final Collection<String> hierarchyStrings = getHierarchyStrings(2);
    for (String hierarchyString : hierarchyStrings) {
      result.append(hierarchyString).append('\n');
    }

    return result.toString();
  }

  private void getHierarchyStrings(List<String> result, Tree<T> node, int numIndentSpaces, int currentIndent) {
    final List<Tree<T>> children = node.getChildren();
    if (children != null && children.size() > 0) {
      result.add(buildString(currentIndent, node, children));

      for (Tree<T> child : children) {
        getHierarchyStrings(result, child, numIndentSpaces, currentIndent + numIndentSpaces);
      }
    }
  }

  private String buildString(int indent, Tree<T> node, List<Tree<T>> children) {
    final StringBuilder result = new StringBuilder();

    for (int i = 0; i < indent; ++i) result.append(' ');
    result.append(node.getData().toString());

    for (Tree<T> child : children) {
      result.append(' ').append(child.getData().toString());
    }

    return result.toString();
  }
}
