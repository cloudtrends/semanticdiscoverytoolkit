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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 'Diff' utility for trees.
 * <p>
 * @author Spence Koehler
 */
public class LeafDiffer <T> {

  private Tree<T> tree1;
  private Tree<T> tree2;
  private TextExtractor<T> textExtractor;

  private final AtomicBoolean initialized = new AtomicBoolean(false);
  private List<LeafWrapper<T>> _leafWrappers1;
  private List<LeafWrapper<T>> _leafWrappers2;
  private Map<String, IndexEntry> _text2entry;

  public LeafDiffer(Tree<T> tree1, Tree<T> tree2) {
    this.tree1 = tree1;
    this.tree2 = tree2;
    this.textExtractor = null;

    this._leafWrappers1 = null;
    this._leafWrappers2 = null;
    this._text2entry = null;
  }

  public void setTextExtractor(TextExtractor<T> textExtractor) {
    this.textExtractor = textExtractor;
  }

  public Map<String, IndexEntry> getText2Entry() {
    if (!initialized.get()) {
      initialize();
    }
    return this._text2entry;
  }

  public List<LeafWrapper<T>> getLeafWrappers1() {
    if (!initialized.get()) {
      initialize();
    }
    return this._leafWrappers1;
  }

  public List<LeafWrapper<T>> getLeafWrappers2() {
    if (!initialized.get()) {
      initialize();
    }
    return this._leafWrappers2;
  }


  public List<Tree<T>> getDisjunctionNodes1() {
    return getDisjunctionNodes(getLeafWrappers1());
  }

  public List<Tree<T>> getDisjunctionNodes2() {
    return getDisjunctionNodes(getLeafWrappers2());
  }


  private List<Tree<T>> getDisjunctionNodes(List<LeafWrapper<T>> leafWrappers) {
    final List<Tree<T>> result = new ArrayList<Tree<T>>();

    final Map<String, IndexEntry> text2entry = getText2Entry();

    LeafWrapper<T> disjunctionStart = null;
    LeafWrapper<T> disjunctionEnd = null;

    for (LeafWrapper<T> leafWrapper : leafWrappers) {
      final IndexEntry indexEntry = text2entry.get(leafWrapper.leafText);
      if (indexEntry == null) continue;  // should never happen

      final boolean hasMatch = indexEntry.hasMatchedEntryFor(leafWrapper);
      if (!hasMatch) {  // found mismatch
        if (disjunctionStart != null) {  // already in disjunction
          disjunctionEnd = leafWrapper;
        }
        else {  // starting disjunction
          disjunctionStart = leafWrapper;
          disjunctionEnd = leafWrapper;
        }
      }
      else {  // found match
        if (disjunctionStart != null) {  // ended disjunction
          addDisjunction(disjunctionStart, disjunctionEnd, result);
        }

        // no longer in disjunction
        disjunctionStart = null;
        disjunctionEnd = null;
      }
    }

    if (disjunctionStart != null) {  // record last disjunction
      addDisjunction(disjunctionStart, disjunctionEnd, result);
    }

    return result;
  }

  private final void addDisjunction(LeafWrapper<T> disjunctionStart, LeafWrapper<T> disjunctionEnd, List<Tree<T>> result) {
    result.add(disjunctionStart.leafNode.getDeepestCommonAncestor(disjunctionEnd.leafNode));
  }

  private final void initialize() {
    if (this.initialized.compareAndSet(false, true)) {
      this._leafWrappers1 = computeLeafWrappers(tree1);
      this._leafWrappers2 = computeLeafWrappers(tree2);
      this._text2entry = computeText2Entry(_leafWrappers1, _leafWrappers2);
    }
  }

  private final List<LeafWrapper<T>> computeLeafWrappers(Tree<T> root) {
    final List<LeafWrapper<T>> result = new ArrayList<LeafWrapper<T>>();
    computeLeafWrappers(root, new AtomicInteger(0), result);
    return result;
  }

  private final void computeLeafWrappers(Tree<T> node, AtomicInteger leafNum, List<LeafWrapper<T>> leafWrappers) {
    if (!node.hasChildren()) {
      final String nodeText = extractText(node);

      if (nodeText != null && !"".equals(nodeText)) {
        leafWrappers.add(new LeafWrapper<T>(leafNum.incrementAndGet(), nodeText, node));
      }
    }
    else {
      for (Tree<T> child : node.getChildren()) {
        computeLeafWrappers(child, leafNum, leafWrappers);
      }
    }
  }

  private final Map<String, IndexEntry> computeText2Entry(List<LeafWrapper<T>> leafWrappers1, List<LeafWrapper<T>> leafWrappers2) {
    final Map<String, IndexEntry> result = new HashMap<String, IndexEntry>();

    computeText2Entry(tree1, leafWrappers1, result);
    computeText2Entry(tree2, leafWrappers2, result);

    return result;
  }

  private final void computeText2Entry(Tree<T> root, List<LeafWrapper<T>> leafWrappers, Map<String, IndexEntry> text2entry) {
    for (LeafWrapper<T> leafWrapper : leafWrappers) {
      addEntry(leafWrapper.leafText, new EntryItem(root, leafWrapper), text2entry);
    }
  }


  private final String extractText(Tree<T> node) {
    String result = null;

    if (textExtractor == null) {
      result = defaultExtractText(node);
    }
    else {
      result = textExtractor.extractText(node);
    }

    return result;
  }

  private final String defaultExtractText(Tree<T> node) {
    String result = null;

    final T data = node.getData();
    if (data != null) {
      result = data.toString();
    }

    return result;
  }


  private final void addEntry(String nodeText, EntryItem entryItem, Map<String, IndexEntry> text2entry) {
    IndexEntry indexEntry = text2entry.get(nodeText);

    if (indexEntry == null) {
      indexEntry = new IndexEntry(nodeText, entryItem);
      text2entry.put(nodeText, indexEntry);
    }
    else {
      indexEntry.addItem(entryItem);
    }
  }


  private final class IndexEntry {
    public final String text;
    public final List<EntryItem> entryItems;

    public IndexEntry(String text, EntryItem entryItem) {
      this.text = text;
      this.entryItems = new ArrayList<EntryItem>();
      this.entryItems.add(entryItem);
    }

    public void addItem(EntryItem entryItem) {
      boolean added = false;

      for (EntryItem curEntryItem : entryItems) {
        if (curEntryItem.sourceRoot != entryItem.sourceRoot && !curEntryItem.hasMatchingItem()) {
          curEntryItem.setMatchingItem(entryItem);
          added = true;
          break;
        }
      }

      if (!added) {
        entryItems.add(entryItem);
      }
    }

    public boolean hasMatchedEntryFor(LeafWrapper leafWrapper) {
      boolean result = false;

      for (EntryItem entryItem : entryItems) {
        if (entryItem.hasMatchingItem()) {
          if (entryItem.leafWrapper == leafWrapper || entryItem.getMatchingItem().leafWrapper == leafWrapper) {
            result = true;
            break;
          }
        }
      }

      return result;
    }
  }

  private final class EntryItem {
    public final Tree<T> sourceRoot;
    public final LeafWrapper<T> leafWrapper;

    private EntryItem matchingItem;

    public EntryItem(Tree<T> sourceRoot, LeafWrapper<T> leafWrapper) {
      this.sourceRoot = sourceRoot;
      this.leafWrapper = leafWrapper;
    }

    public void setMatchingItem(EntryItem matchingItem) {
      this.matchingItem = matchingItem;
    }

    public EntryItem getMatchingItem() {
      return this.matchingItem;
    }

    public boolean hasMatchingItem() {
      return this.matchingItem != null;
    }
  }

  public static final class LeafWrapper <T> {
    public final int leafNum;
    public final String leafText;
    public final Tree<T> leafNode;

    public LeafWrapper(int leafNum, String leafText, Tree<T> leafNode) {
      this.leafNum = leafNum;
      this.leafText = leafText;
      this.leafNode = leafNode;
    }
  }

}
