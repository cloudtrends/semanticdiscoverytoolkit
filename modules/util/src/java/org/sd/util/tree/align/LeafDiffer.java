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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sd.util.tree.Tree;

/**
 * Utility class for finding differences between two trees based on leaf
 * text.
 * <p>
 * A LeafDiffer instance is creatd with two trees whose leaf texts are
 * to be compared.
 * <p>
 * If the ToString method of the tree's leaf data is not appropriate for
 * comparisons, then a TextExtractor should be set on the LeafDiffer
 * instance. Note that (re)setting the TextExtractor will affect the results
 * provided by the instance.
 * <p>
 * The diff results are in the form of highest-level nodes from the respective
 * Trees that contain consecutive textual differences in the leaves. Other
 * than finding the deepest common ancestor between start and finish consecutive
 * mismatches, the structure of the trees is ignored.
 * <p>
 * @author Spence Koehler
 */
public class LeafDiffer<T> {
  
  private boolean initialized = false;

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


  private TextExtractor<T> _textExtractor;
  /**
   * TextExtractor to be set if the ToString method of the Tree's Data (of type T)
   * is not the proper function to apply for determining leaf equality.
   */
  public TextExtractor<T> getTextExtractor() {
    return _textExtractor;
  }

  public final void setTextExtractor(TextExtractor<T> value) {
    this._textExtractor = value;

    // these will need to be recomputed
    this._leafWrappers1 = null;
    this._leafWrappers2 = null;
    this._text2entry = null;
  }

  private List<LeafWrapper<T>> _leafWrappers1;
  /**
   * LeafWrapper instances around each leaf from Tree1.
   */
  public List<LeafWrapper<T>> getLeafWrappers1() {
    if (!initialized) {
      initialize();
    }
    return _leafWrappers1;
  }

  private List<LeafWrapper<T>> _leafWrappers2;
  /**
   * LeafWrapper instances around each leaf from Tree2.
   */
  public List<LeafWrapper<T>> getLeafWrappers2() {
    if (!initialized) {
      initialize();
    }
    return _leafWrappers2;
  }

  private Map<String, IndexEntry<T>> _text2entry;
  /**
   * Index mapping every leaf text to an IndexEntry instance.
   */
  public Map<String, IndexEntry<T>> getText2Entry() {
    if (!initialized) {
      initialize();
    }
    return _text2entry;
  }

  /**
   * Construct with the two trees to compare.
   * 
   * If the ToString method of the tree's data is not appropriate for aligning
   * leaf nodes, then a TextExtractor should be set on the instance.
   */
  public LeafDiffer(Tree<T> tree1, Tree<T> tree2) {
    this(tree1, tree2, null);
  }

  /**
   * Construct with the two trees to compare using the given TextExtractor.
   */
  public LeafDiffer(Tree<T> tree1, Tree<T> tree2, TextExtractor<T> textExtractor) {
    this.tree1 = tree1;
    this.tree2 = tree2;
    this.setTextExtractor(textExtractor);
  }

  /**
   * Get nodes from Tree1 containing leaf text that is not found in Tree2.
   */
  public List<Tree<T>> getDisjunctionNodes1() {
    return getDisjunctionNodes(getLeafWrappers1());
  }

  /**
   * Get nodes from Tree2 containing leaf text that is not found in Tree1.
   */
  public List<Tree<T>> getDisjunctionNodes2() {
    return getDisjunctionNodes(getLeafWrappers2());
  }

  /**
   * Auxiliary for scanning for consecutive leaf text mismatches and finding
   * the deepest node encapsulating those nodes.
   */
  public List<Tree<T>> getDisjunctionNodes(List<LeafWrapper<T>> leafWrappers) {
    final List<Tree<T>> result = new ArrayList<Tree<T>>();
    final Map<String, IndexEntry<T>> text2entry = this.getText2Entry();

    LeafWrapper<T> disjunctionStart = null;
    LeafWrapper<T> disjunctionEnd = null;

    for (LeafWrapper<T> leafWrapper : leafWrappers) {
      final IndexEntry<T> indexEntry = text2entry.get(leafWrapper.getLeafText());

      final boolean hasMatch = indexEntry.hasMatchedEntryFor(leafWrapper);
      if (!hasMatch) {
        if (disjunctionStart != null) {
          disjunctionEnd = leafWrapper;
        }
        else {
          disjunctionStart = disjunctionEnd = leafWrapper;
        }
      }
      else {
        if (disjunctionStart != null) {
          addDisjunction(disjunctionStart, disjunctionEnd, result);
        }

        disjunctionStart = null;
        disjunctionEnd = null;
      }
    }

    if (disjunctionStart != null) {
      addDisjunction(disjunctionStart, disjunctionEnd, result);
    }

    return result;
  }

  /**
   * Auxiliary for finding the node for a consecutive span of differences
   * and adding it to the result.
   */
  private void addDisjunction(LeafWrapper<T> disjunctionStart, LeafWrapper<T> disjunctionEnd, List<Tree<T>> result) {
    result.add(disjunctionStart.getLeafNode().getDeepestCommonAncestor(disjunctionEnd.getLeafNode()));
  }

  /**
   * Initialize the leafWrappers and text2entry map using the current TextExtractor.
   */
  private void initialize() {
    if (!initialized) {
      initialized = true;
      this._leafWrappers1 = computeLeafWrappers(tree1);
      this._leafWrappers2 = computeLeafWrappers(tree2);
      this._text2entry = computeText2Entry(_leafWrappers1, _leafWrappers2);
    }
  }

  /**
   * Auxiliary to compute LeafWrapper instances for a tree.
   */
  private List<LeafWrapper<T>> computeLeafWrappers(Tree<T> root) {
    final List<LeafWrapper<T>> result = new ArrayList<LeafWrapper<T>>();
    int[] leafNum = new int[]{0};

    computeLeafWrappers(root, leafNum, result);

    return result;
  }

  /**
   * Recursive auxiliary for walking the tree to find leaf nodes.
   */
  private void computeLeafWrappers(Tree<T> node, int[] leafNum, List<LeafWrapper<T>> leafWrappers) {
    if (!node.hasChildren()) {
      final String nodeText = extractText(node);

      if (nodeText != null && !"".equals(nodeText)) {
        leafWrappers.add(new LeafWrapper<T>(leafNum[0]++, nodeText, node));
      }
    }
    else {
      for (Tree<T> child : node.getChildren()) {
        computeLeafWrappers(child, leafNum, leafWrappers);
      }
    }
  }

  /**
   * Auxiliary for creating the index map for both trees.
   */
  private Map<String, IndexEntry<T>> computeText2Entry(List<LeafWrapper<T>> leafWrappers1, List<LeafWrapper<T>> leafWrappers2) {
    Map<String, IndexEntry<T>> result = new HashMap<String, IndexEntry<T>>();

    computeText2Entry(tree1, leafWrappers1, result);
    computeText2Entry(tree2, leafWrappers2, result);

    return result;
  }

  /**
   * Auxiliary for creating the index map for a tree.
   */
  private void computeText2Entry(Tree<T> root, List<LeafWrapper<T>> leafWrappers, Map<String, IndexEntry<T>> text2entry) {
    for (LeafWrapper<T> leafWrapper : leafWrappers) {
      addEntry(leafWrapper.getLeafText(), new EntryItem<T>(root, leafWrapper), text2entry);
    }
  }

  /**
   * Auxiliary for adding an index entry to the map.
   */
  private void addEntry(String nodeText, EntryItem<T> entryItem, Map<String, IndexEntry<T>> text2entry) {
    IndexEntry<T> indexEntry = text2entry.get(nodeText);

    if (indexEntry == null) {
      indexEntry = new IndexEntry<T>(nodeText, entryItem);
      text2entry.put(nodeText, indexEntry);
    }
    else {
      indexEntry.addItem(entryItem);
    }
  }


  /**
   * Utility to apply default or overridden text extraction from a leaf node.
   */
  private String extractText(Tree<T> node) {
    String result = null;

    final TextExtractor<T> textExtractor = getTextExtractor();

    if (textExtractor == null) {
      result = defaultExtractText(node);
    }
    else {
      result = textExtractor.extractText(node);
    }

    return result;
  }

  /**
   * Perform default text extraction from a leaf node.
   */
  private String defaultExtractText(Tree<T> node) {
    String result = null;

    final T data = node.getData();
    if (data != null) {
      result = data.toString();
    }

    return result;
  }
}
