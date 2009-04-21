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
package org.sd.text.radixtree;


import org.sd.util.tree.Tree;

/**
 * Visitor interface used by {@link RadixTreeImpl} for performing tasks
 * on a searched node.
 * <p>
 * @author Spence Koehler
 */
public interface Visitor<T> {
  
  /**
   * This method gets called by @link{RadixTreeImpl#visit(String, Visitor)} 
   * when it finds a node matching key given to it.
   * 
   * @param key The key that got matched
   * @param node The matching node
   */
  public void visit(String key, Tree<RadixData<T>> node);

}
