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
 * Container class for an EntryItem
 * <p>
 * @author Spence Koehler
 */
class EntryItem<T> {
  
  private Tree<T> sourceRoot;
  Tree<T> getSourceRoot() {
    return sourceRoot;
  }

  private LeafWrapper<T> leafWrapper;
  LeafWrapper<T> getLeafWrapper() {
    return leafWrapper;
  }

  private EntryItem<T> matchingItem;
  EntryItem<T> getMatchingItem() {
    return matchingItem;
  }
  void setMatchingItem(EntryItem<T> matchingItem) {
    this.matchingItem = matchingItem;
  }

  boolean hasMatchingItem() {
    return matchingItem != null;
  }

  EntryItem(Tree<T> sourceRoot, LeafWrapper<T> leafWrapper) {
    this.sourceRoot = sourceRoot;
    this.leafWrapper = leafWrapper;
  }
}
