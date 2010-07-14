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
import java.util.List;

/**
 * Container class for holding EntryItem instances associated with leaf text.
 * <p>
 * @author Spence Koehler
 */
class IndexEntry<T> {
  
  private String text;
  String getText() {
    return text;
  }

  private List<EntryItem<T>> entryItems;
  public List<EntryItem<T>> getEntryItems() {
    return entryItems;
  }

  /**
   * Construct with the given text and EntryItem.
   */
  IndexEntry(String text, EntryItem<T> entryItem) {
    this.text = text;
    this.entryItems = new ArrayList<EntryItem<T>>();
    this.entryItems.add(entryItem);
  }

  /**
   * Add another EntryItem having the same text, aligning matching EntryItems
   * from different trees.
   */
  void addItem(EntryItem<T> entryItem) {
    boolean added = false;

    for (EntryItem<T> curEntryItem : entryItems) {
      if (curEntryItem.getSourceRoot() != entryItem.getSourceRoot() && !curEntryItem.hasMatchingItem()) {
        curEntryItem.setMatchingItem(entryItem);
        added = true;
        break;
      }
    }

    if (!added) {
      entryItems.add(entryItem);
    }
  }

  /**
   * Determine whether there is a matched entry for the given leaf wrapper.
   */
  boolean hasMatchedEntryFor(LeafWrapper<T> leafWrapper) {
    boolean result = false;

    for (EntryItem<T> entryItem : entryItems) {
      if (entryItem.hasMatchingItem()) {
        if (entryItem.getLeafWrapper() == leafWrapper || entryItem.getMatchingItem().getLeafWrapper() == leafWrapper) {
          result = true;
          break;
        }
      }
    }

    return result;
  }
}
