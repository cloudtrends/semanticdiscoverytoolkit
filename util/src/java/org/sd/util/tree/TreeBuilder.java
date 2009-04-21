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


/**
 * Factory interface for creating trees from strings and vice versa.
 *
 * @author Spence Koehler
 */
public interface TreeBuilder <T> {

  /**
   * Build a tree from the given string.
   * <p>
   * The returned Tree can be used as input to the buildString method
   * to create a String.
   * <p>
   * The returned Tree will typically hold to the property that
   * treeString.equals(buildString(tree)). In some implementations,
   * the transformation can be lossy such that this property does
   * not hold.
   */
  public Tree<T> buildTree(String treeString);

  /**
   * Build a string representing the given tree.
   * <p>
   * The returned string will always serve as input to the buildTree
   * method to generate a tree.
   * <p>
   * The returned string will typically hold to the property that
   * tree.equals(buildTree(string)). In some implementations, the
   * transformation can be lossy such that this property does not
   * hold.
   */
  public String buildString(Tree<T> tree);
}
