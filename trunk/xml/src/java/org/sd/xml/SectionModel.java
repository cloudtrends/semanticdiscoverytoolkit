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
package org.sd.xml;

import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeAnalyzer;
import org.sd.util.tree.Tree2Dot;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for building a section model around an xml (html) document.
 * <p>
 * @author Spence Koehler
 */
public class SectionModel {

  /** Text found under these tags cannot be consecutive with the next text node. */
  public static final String[] DEFAULT_NON_CONSECUTIVE_TEXT_TAGS = new String[]{"title", "h1", "h2", "h3", "h4", "h5", "h6", "pre", "address", "code", "li", "dt", "dd", "caption", "thead", "tfoot", "td", "th"};

  /** Any text found under these tags are considered headings. */
  public static final String[] DEFAULT_HEADING_TAGS = new String[] {
    // since these are also non_consecutive_text_tags, they  are always headings
    "title", "h1", "h2", "h3", "h4", "h5", "h6", "thead", "th",

    // these are only considered as heading when found over non consecutive text
    "font@size", "em", "b", "strong"
  };

  /**
   * Default mappings from heading values to heading 'strength':
   * <p>
   *        h       h1   h2   h3        h4   h5  h6  <p>
   *   size-N        6    5    4    3    2    1   0  <p>
   *   size-%      200  150  120  100   80   70  60  <p>
   *                                      thead  th  <p>
   *                                             em  <p>
   *                                         strong  <p>
   *                                              b  <p>
   *          title
   * strength   7    6    5    4    0    3    2   1  <p>
   * 
   */
  public static final Map<String, Integer> VALUE_TO_STRENGTH = new HashMap<String, Integer>();
  static {
    VALUE_TO_STRENGTH.put("h1", 6);
    VALUE_TO_STRENGTH.put("h2", 5);
    VALUE_TO_STRENGTH.put("h3", 4);
    VALUE_TO_STRENGTH.put("h4", 3);
    VALUE_TO_STRENGTH.put("h5", 2);
    VALUE_TO_STRENGTH.put("h6", 1);

    VALUE_TO_STRENGTH.put("200%", 6);
    VALUE_TO_STRENGTH.put("150%", 5);
    VALUE_TO_STRENGTH.put("120%", 4);
    VALUE_TO_STRENGTH.put("100%", 0);
    VALUE_TO_STRENGTH.put( "80%", 3);
    VALUE_TO_STRENGTH.put( "70%", 2);
    VALUE_TO_STRENGTH.put( "60%", 1);

    VALUE_TO_STRENGTH.put("thead", 2);
    VALUE_TO_STRENGTH.put("th", 1);
    VALUE_TO_STRENGTH.put("em", 1);
    VALUE_TO_STRENGTH.put("strong", 1);
    VALUE_TO_STRENGTH.put("b", 1);

    VALUE_TO_STRENGTH.put("title", 7);
  }


  private Tree<XmlLite.Data> xmlTree;
  private PathHelper pathHelper;
  private TreeAnalyzer<XmlLite.Data> treeAnalyzer;
  private Set<String> nonConsecutiveTags;
  private Set<String> headingTags;
  private Map<String, String> tag2attribute;  // i.e. font@size -> font -> size
  private Map<String, Integer> hVal2Strength;

  private Path[] paths;
  private Map<Tree<XmlLite.Data>, Path> leaf2path;
  private ModelStats modelStats;
  private List<Tree<XmlLite.Data>> recordSectionNodes;

  /**
   * Construct a new section model using defaults.
   */
  public SectionModel(Tree<XmlLite.Data> xmlTree) {
    this(xmlTree, null, DEFAULT_NON_CONSECUTIVE_TEXT_TAGS, DEFAULT_HEADING_TAGS, VALUE_TO_STRENGTH);
  }

  /**
   * Construct a new section model, reusing the given treeAnalyzer (default created if null).
   */
  public SectionModel(Tree<XmlLite.Data> xmlTree,
                      TreeAnalyzer<XmlLite.Data> treeAnalyzer,
                      String[] nonConsecutiveTextTags,
                      String[] headingTags,
                      Map<String, Integer> hVal2Strength) {
    this.xmlTree = xmlTree;
    this.pathHelper = new PathHelper(xmlTree, treeAnalyzer);
    this.treeAnalyzer = pathHelper.getTreeAnalyzer();
    this.nonConsecutiveTags = buildTagSet(nonConsecutiveTextTags);
    buildHeadingTags(headingTags);
    this.hVal2Strength = hVal2Strength;

    this.modelStats = null;
    this.recordSectionNodes = new ArrayList<Tree<XmlLite.Data>>();

    init();
  }
  
  /**
   * Get this instance's xml tree.
   */
  public Tree<XmlLite.Data> getXmlTree() {
    return xmlTree;
  }

  /**
   * Get this instance's treeAnalyzer.
   */
  public TreeAnalyzer<XmlLite.Data> getTreeAnalyzer() {
    return treeAnalyzer;
  }

  /**
   * Get this instance's pathHelper.
   */
  public PathHelper getPathHelper() {
    return pathHelper;
  }

  /**
   * Get all of the paths present in the xmlTree.
   */
  public Path[] getPaths() {
    return paths;
  }

  /**
   * Get all of the text paths present in the xmlTree.
   */
  public LinkedList<Path> collectTextPaths() {
    return collectTextPaths(null);
  }

  /**
   * Get all of the text paths selected by the visitor.
   * <p>
   * Note: the visitor may get/set special nodes in the path and/or
   *       get/set flags in tree analyzer node info's for nodes in
   *       the tree.
   */
  public LinkedList<Path> collectTextPaths(PathVisitor pathVisitor) {
    final LinkedList<Path> result = new LinkedList<Path>();

    for (Path path : paths) {
      if (path.text != null) {
        if (path.accept(pathVisitor)) {
          result.add(path);
        }
      }
    }

    return result;
  }

  /**
   * Get the path from a leaf (usually text) node.
   */
  public Path getPath(Tree<XmlLite.Data> leaf) {
    return leaf2path.get(leaf);
  }

  /**
   * Get the number of paths in this model.
   */
  public int getNumPaths() {
    return paths.length;
  }

  /**
   * Get the path with the given pathIndex.
   */
  public Path getPath(int pathIndex) {
    return paths[pathIndex];
  }

  public List<Tree<XmlLite.Data>> getRecordSectionNodes() {
    return recordSectionNodes;
  }

  public List<Tree<XmlLite.Data>> collectRecordNodes(Tree<XmlLite.Data> recordSectionNode) {
    final ArrayList<Tree<XmlLite.Data>> result = new ArrayList<Tree<XmlLite.Data>>();

    for (Tree<XmlLite.Data> child : recordSectionNode.getChildren()) {
      if (treeAnalyzer.getNodeInfo(child).hasFlag("record")) {
        result.add(child);
      }
    }

    return result;
  }

  /**
   * Collect text paths under the given node.
   */
  public final List<Path> collectPaths(Tree<XmlLite.Data> node) {
    // find range of path indeces under node
    final int[] range = findPathIndexRange(node);
    if (range == null) return null;

    // extract text nodes in range
    final List<Path> result = new ArrayList<Path>();
    for (int i = range[0]; i < range[1]; ++i) {
      final Path path = paths[i];
      if (path.text != null) result.add(path);
    }
    return result;
  }

  /**
   * Get the weight of paths having any one of the tagNames as a proportion
   * to all of the paths under the node. When onlyTextPaths, only consider
   * paths that have text.
   * <p>
   * This is intended for use as a feature for identifying navigation nodes.
   * It is generally expected that the linked ("a"-tag) text nodes will be
   * a higher proportion of the navigation (record section) nodes than for
   * content (record section) nodes.
   */
  public double tagWeight(Tree<XmlLite.Data> node, String[] tagNames, boolean onlyTextPaths) {
    // num (text) paths that have tag / num (text) paths

    if (tagNames == null) return 0.0;

    final Set<String> tagSet = new HashSet<String>();
    for (String tagName : tagNames) tagSet.add(tagName);

    final int[] range = findPathIndexRange(node);
    if (range == null) return 0.0;

    int numPathsWithTag = 0;
    int numPaths = 0;

    for (int i = range[0]; i < range[1]; ++i) {
      final Path path = paths[i];
      if (onlyTextPaths && path.text == null) continue;

      if (path.findTag(tagSet) != null) numPathsWithTag++;
      numPaths++;
    }

    if (numPaths == 0) return 0.0;

    return ((double)numPathsWithTag / (double)numPaths);
  }

  /**
   * Get the total number of chars of text under the given node, excluding
   * text found under the given exclusion tag(s).
   * <p>
   * This is intended for use as a feature for identifying content nodes.
   * It is generally expected that the non-linked ("a"-tag) textWeight of
   * content (record section) nodes will be greater than that of navigation
   * (record section) nodes.
   */
  public int textWeight(Tree<XmlLite.Data> node, String[] excludeTagNames) {
    // total length of text under paths that don't have tag
    
    Set<String> tagSet = null;
    if (excludeTagNames != null) {
      tagSet = new HashSet<String>();
      for (String tagName : excludeTagNames) tagSet.add(tagName);
    }

    final int[] range = findPathIndexRange(node);
    if (range == null) return 0;

    int result = 0;

    for (int i = range[0]; i < range[1]; ++i) {
      final Path path = paths[i];
      if (path.text != null) {
        if (tagSet == null || path.findTag(tagSet) == null) {
          result += path.text.length();
        }
      }
    }

    return result;
  }

  public ModelStats getModelStats() {
    if (modelStats == null) {
      modelStats = createModelStats();
    }
    return modelStats;
  }


  private final Set<String> buildTagSet(String[] tags) {
    final Set<String> result = new HashSet<String>();
    for (String tag : tags) result.add(tag);
    return result;
  }

  private final void buildHeadingTags(String[] headingTags) {
    this.headingTags = new HashSet<String>();
    this.tag2attribute = new HashMap<String, String>();

    for (String headingTag : headingTags) {
      final String[] tagAtt = headingTag.split("@");
      this.headingTags.add(tagAtt[0]);
      if (tagAtt.length == 2) this.tag2attribute.put(tagAtt[0], tagAtt[1]);
    }
  }

  private final void init() {
    final List<Tree<XmlLite.Data>> leaves = pathHelper.getLeaves();
    this.paths = new Path[leaves.size()];
    this.leaf2path = new LinkedHashMap<Tree<XmlLite.Data>, Path>();

    int index = 0;
    for (Tree<XmlLite.Data> leaf : leaves) {
      paths[index] = new Path(leaf, index);
      leaf2path.put(leaf, paths[index]);
      ++index;
    }

    // mark sections
    mostFirstSectionStrategy();
//todo: make model configurable for alternate section strategies when needed.


    //
    // Here's how nodes are currently marked for sections:
    //
    //   pivotNodeInfo.setFlag("recordSection");
    //   pivotChildInfo.setFlag("record");
    //   curTextInfo.setFlag("data");
    //   prevTextInfo.setFlag("consecutive");
    //
    // Here are special nodes marked in paths:
    //   path.specialNodes.put("maxLocalRepeatSiblingsNode", maxLocalRepeatSiblingsNode);
    //   curTextPath.specialNodes.put("record", pivotChild);
    //   curTextPath.specialNodes.put("recordSection", pivotNode);
    //
    // Here are some useful pieces of information to access
    //   to find numRecordsInSection:
    //     from a recordSection node (node whose nodeInfo has recordSection flag),
    //       iterate over children to find a record node (node whose nodeInfo has record flag),
    //         int numRecordsInSection = nodeInfo.numLocalRepeatSiblings
    //     from a text path in a record of the section,
    //         int numRecordsInSection = treeAnalyzer.getNodeInfo(path.specialNodes.get("maxLocalRepeatSiblingsNode")).numLocalRepeatSiblings;
    //


    // mark headings (needs to be done after section strategy has finished
    // setting 'consecutive' flags on paths.)
    markHeadingsStrategy();
  }


  private final void mostFirstSectionStrategy() {
    // most-first traversal
    //   find the (text) nodes with the most numLocalRepeatSiblings > 1 (in a node in their paths)
    //     mark these as record nodes, defining data sections beneath, marking parent as record section, marking other parent nodes as data sections
    //     remove marked nodes from consideration and repeat
    //   mark remaining data sections
    //
    final LinkedList<Path> textPaths = collectTextPaths(new PathVisitor() {
        public boolean visit(Path path) {
          Tree<XmlLite.Data> maxLocalRepeatSiblingsNode = null;
          int maxLocalRepeatSiblings = -1;

          Tree<XmlLite.Data> node = path.leaf;
          while (node != null) {
            final TreeAnalyzer.NodeInfo nodeInfo = treeAnalyzer.getNodeInfo(node);
            if (nodeInfo != null) {
              final int curLocalRepeatSiblings = nodeInfo.numLocalRepeatSiblings;
              if (curLocalRepeatSiblings > maxLocalRepeatSiblings) {
                maxLocalRepeatSiblings = curLocalRepeatSiblings;
                maxLocalRepeatSiblingsNode = node;
              }
            }
            node = node.getParent();
          }

          path.specialNodes.put("maxLocalRepeatSiblingsNode", maxLocalRepeatSiblingsNode);

          return true;
        }
      });

    // sort descending max(numLocalRepeatSiblings in path))
    Collections.sort(textPaths, new Comparator<Path>() {
      public int compare(Path path1, Path path2) {
        final int maxLocalRepeatSiblings1 = treeAnalyzer.getNodeInfo(path1.specialNodes.get("maxLocalRepeatSiblingsNode")).numLocalRepeatSiblings;
        final int maxLocalRepeatSiblings2 = treeAnalyzer.getNodeInfo(path2.specialNodes.get("maxLocalRepeatSiblingsNode")).numLocalRepeatSiblings;
        return maxLocalRepeatSiblings2 - maxLocalRepeatSiblings1;
      }

      public boolean equals(Object obj) {
        return obj == this;
      }
    });

    while (textPaths.size() > 0) {
      final Path curMaxPath = textPaths.getFirst();
      final TreeAnalyzer<XmlLite.Data>.NodeInfo nodeInfo = treeAnalyzer.getNodeInfo(curMaxPath.specialNodes.get("maxLocalRepeatSiblingsNode"));
      final Tree<XmlLite.Data> pivotNode = nodeInfo.node.getParent();

      if (pivotNode == null) {
        textPaths.removeFirst();
        continue;
      }

      final int numLocalRepeatSiblings = nodeInfo.numLocalRepeatSiblings;
      final int localRepeatId = nodeInfo.localRepeatId;
      final int[] pathInds = findPathIndexRange(pivotNode, curMaxPath.pathIndex);

      // check whether ancestor of marked nodes...
      boolean isDataSection = numLocalRepeatSiblings == 1;

      // if any nodes under pivotNode are already marked into sections, then flip isDataSection to true
      if (!isDataSection) {
        for (int i = pathInds[0]; i < pathInds[1]; ++i) {
          if (paths[i].isSectioned()) {
            isDataSection = true;
            break;
          }
        }
      }

      // if !isDataSection, mark pivotNode as a recordSection
      if (!isDataSection) {
        final TreeAnalyzer.NodeInfo pivotNodeInfo = treeAnalyzer.getNodeInfo(pivotNode);
        pivotNodeInfo.setFlag("recordSection");

        recordSectionNodes.add(pivotNode);
      }

      // ignore a pivot node w/out children
      if (pivotNode.getChildren() == null) {
        textPaths.removeFirst();
        continue;
      }
      
      // for each pivotNode.child,
      //   if isDataSection || wrong localRepeatId, mark text nodes as data section(s)
      //   else mark node as record, marking text nodes as data section(s)
      // ...remove marked text nodes
      for (Tree<XmlLite.Data> pivotChild : pivotNode.getChildren()) {
        final boolean isRecord = !isDataSection &&
          (treeAnalyzer.getNodeInfo(pivotChild).localRepeatId == localRepeatId);

        if (isRecord) {
          // mark pivotChild as record
          final TreeAnalyzer.NodeInfo pivotChildInfo = treeAnalyzer.getNodeInfo(pivotChild);
          pivotChildInfo.setFlag("record");
        }

        final List<Path> curTextPaths = findTextPaths(pivotChild, pathInds);

        // mark curTextPaths as data section(s)
        int prevPathIndex = -2;
        for (Path curTextPath : curTextPaths) {
          int curPathIndex = curTextPath.pathIndex;
          try {
            if (curTextPath.isSectioned()) continue;

            if (!isDataSection) curTextPath.specialNodes.put("recordSection", pivotNode);
            if (isRecord) {
              curTextPath.specialNodes.put("record", pivotChild);
            }

            curTextPath.setSectioned();
            final TreeAnalyzer.NodeInfo curTextInfo = treeAnalyzer.getNodeInfo(curTextPath.leaf);
            curTextInfo.setFlag("data");

            if (curPathIndex == prevPathIndex + 1) {  // consecutive
              final Path prevPath = paths[prevPathIndex];
              final Tree<XmlLite.Data> prevRecord = prevPath.specialNodes.get("record");

              if ((isRecord && prevRecord == pivotChild) || (!isRecord && prevRecord == null)) {
                if (prevPath.setNextConsecutive()) {
                  if (curTextPath.setPrevConsecutive()) {
                    final TreeAnalyzer.NodeInfo prevTextInfo = treeAnalyzer.getNodeInfo(prevPath.leaf);
                    prevTextInfo.setFlag("consecutive");
                    curTextInfo.setFlag("prevConsecutive");
                  }
                  else {
                    prevPath.clearNextConsecutive();
                  }
                }
              }
            }
          }
          finally {
            prevPathIndex = curPathIndex;
            textPaths.remove(curTextPath);
          }
        }
      }
    }
  }

  private final void markHeadingsStrategy() {
    // mark each (non-consecutive) text path's heading strength
    collectTextPaths(new PathVisitor() {
        public boolean visit(Path path) {
          if (!path.hasPrevConsecutive()) {  // can only be heading when non-consecutive
            final Tree<XmlLite.Data> tagNode = path.findTag(headingTags);
            if (tagNode != null) {  // found a heading tag
              final XmlLite.Data data = tagNode.getData();
              final XmlLite.Tag tag = data.asTag();
              final String att = tag2attribute.get(tag.name);  // check for attribute (i.e. font@size=)
              final String value = (att == null) ? tag.name : tag.getAttribute(att);
              if (value != null) {  // found attribute or using tag name
                final Integer strength = hVal2Strength.get(value);
                path.setHeading(strength);  // accounts for null
              }
            }
          }
          return false;  // don't need to collect results.
        }
      });
  }


  /**
   * Use paths[] to find indexes of all paths under node, where onePathIndex is one of
   * the paths.
   *
   * @return result[0] is the first pathIndex under node; result[1] is the last+1.
   */
  private final int[] findPathIndexRange(Tree<XmlLite.Data> node, int onePathIndex) {
    int preStartIndex = onePathIndex - 1;
    while (preStartIndex >= 0) {
      final Tree<XmlLite.Data> leaf = paths[preStartIndex].leaf;
      if (!node.isAncestor(leaf) && node != leaf) break;
      --preStartIndex;
    }

    int postEndIndex = onePathIndex + 1;
    while (postEndIndex < paths.length) {
      final Tree<XmlLite.Data> leaf = paths[postEndIndex].leaf;
      if (!node.isAncestor(leaf) && node != leaf) break;
      ++postEndIndex;
    }

    return new int[]{preStartIndex + 1, postEndIndex};
  }

  /**
   * Use paths[] to find indexes of all paths under node, where onePathIndex is one of
   * the paths.
   *
   * @return result[0] is the first pathIndex under node; result[1] is the last+1.
   */
  private final int[] findPathIndexRange(Tree<XmlLite.Data> node, int[] pathIndRange) {
    int startIndex = pathIndRange[0];
    while (startIndex < pathIndRange[1]) {
      final Tree<XmlLite.Data> leaf = paths[startIndex].leaf;
      if (node == leaf || node.isAncestor(leaf)) break;
      ++startIndex;
    }

    int postEndIndex = startIndex + 1;
    if (postEndIndex >= pathIndRange[1]) {
      postEndIndex = pathIndRange[1];
    }
    else {
      while (postEndIndex < pathIndRange[1]) {
        final Tree<XmlLite.Data> leaf = paths[postEndIndex].leaf;
        if (!node.isAncestor(leaf) && node != leaf) break;
        ++postEndIndex;
      }
    }

    return new int[]{startIndex, postEndIndex};
  }

  /**
   * Less efficient range finding method for when we don't have a hint.
   */
  private final int[] findPathIndexRange(Tree<XmlLite.Data> node) {
    // depth first search for first text node
    Tree<XmlLite.Data> leaf = null;
    for (Iterator<Tree<XmlLite.Data>> iter = node.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      final Tree<XmlLite.Data> curNode = iter.next();
      if (curNode.getChildren() == null && curNode.getData().asText() != null) {
        leaf = curNode;
        break;
      }
    }

    // getPath(leaf)
    if (leaf == null) return null;
    final Path path = getPath(leaf);
    if (path == null) return null;

    // find range of nodes under node
    return findPathIndexRange(node, path.pathIndex);
  }

  /**
   * Use paths[] to find all text paths under node, where the paths are known to
   * be  within the given range.
   */
  private final List<Path> findTextPaths(Tree<XmlLite.Data> node, int[] pathIndRange) {
    final List<Path> result = new LinkedList<Path>();

    final int[] pathInds = findPathIndexRange(node, pathIndRange);
    for (int i = pathInds[0]; i < pathInds[1]; ++i) {
      final Path path = paths[i];
      if (path.text != null) result.add(path);
    }

    return result;
  }

  private final ModelStats createModelStats() {
    final List<RecordSectionStats> rsstats = new ArrayList<RecordSectionStats>();

    final List<Tree<XmlLite.Data>> rsNodes = getRecordSectionNodes();
    for (Tree<XmlLite.Data> rsNode : rsNodes) {
      final List<Tree<XmlLite.Data>> recordNodes = collectRecordNodes(rsNode);
      final int[] range = findPathIndexRange(rsNode);
      final int numTextNodes = countTextPaths(range[0], range[1]);

      final RecordSectionStats rsStat =
        new RecordSectionStats(recordNodes.size(), range[1] - range[0],
                               numTextNodes, textWeight(rsNode, null),
                               textWeight(rsNode, new String[]{"a"}),
                               tagWeight(rsNode, new String[]{"a"}, true));
      rsstats.add(rsStat);
    }

    return new ModelStats(paths.length, countTextPaths(0, paths.length), rsstats);
  }

  private final int countTextPaths(int startIndex, int endIndex) {
    int numTextNodes = 0;
    for (int i = startIndex; i < endIndex; ++i) {
      if (paths[i].text != null) ++numTextNodes;
    }
    return numTextNodes;
  }

  public final class Path {
    public final Tree<XmlLite.Data> leaf;
    public final int pathIndex;
    public final String key;
    public final Map<String, Tree<XmlLite.Data>> specialNodes;
    public final String text;  // null if not a text leaf

    private boolean sectioned;        // flag used during construction; obsolete afterwards
    private boolean nextConsecutive;  // if set, next path is a text node in same data section
    private boolean prevConsecutive;  // if set, prev path is a text node in same data section
    private int heading;              // if heading strength of path; higher is stronger. 0 is no heading.

    Path(Tree<XmlLite.Data> leaf, int pathIndex) {
      this.leaf = leaf;
      this.pathIndex = pathIndex;
      this.key = pathHelper.buildPathKey(leaf, PathHelper.LOCAL_REPEAT_INDEX, false);
      this.specialNodes = new LinkedHashMap<String, Tree<XmlLite.Data>>();
      this.sectioned = false;
      this.nextConsecutive = false;
      this.prevConsecutive = false;
      this.heading = 0;  // to be set after consecutive

      final XmlLite.Text text = leaf.getData().asText();
      this.text = (text == null) ? null : text.text;
    }

    public boolean accept(PathVisitor visitor) {
      return (visitor != null) ? visitor.visit(this) : true;
    }

    private boolean isSectioned() {
      return sectioned;
    }

    private void setSectioned() {
      this.sectioned = true;
    }

    /**
     * Determine whether the next path is a text node in this path's data
     * section.
     */
    public boolean hasNextConsecutive() {
      return nextConsecutive;
    }

    /**
     * Determine whether the prev path is a text node in this path's data
     * section.
     */
    public boolean hasPrevConsecutive() {
      return prevConsecutive;
    }

    /**
     * Set the flag indicating the next path is a text node in this path's
     * data section, but only if there isn't a nonConsecutiveTag in the
     * path.
     */
    public boolean setNextConsecutive() {
      if (findTag(nonConsecutiveTags) == null) this.nextConsecutive = true;
      return this.nextConsecutive;
    }

    public boolean setPrevConsecutive() {
      if (findTag(nonConsecutiveTags) == null) this.prevConsecutive = true;
      return this.prevConsecutive;
    }

    public void clearNextConsecutive() {
      this.nextConsecutive = false;
    }

    public void clearPrevConsecutive() {
      this.prevConsecutive = false;
    }

    public void setHeading(Integer strength) {
      if (strength != null) this.heading = strength;
    }

    /**
     * Get the heading strength (0==no heading; 1 (weak) to 7 (strongest)).
     */
    public int getHeading() {
      return heading;
    }

    public Tree<XmlLite.Data> findTag(Set<String> tagNames) {
      Tree<XmlLite.Data> node = leaf;
      while (node != null) {
        final XmlLite.Data data = node.getData();
        final XmlLite.Tag tag = data.asTag();
        if (tag != null && tagNames.contains(tag.name)) {
          return node;
        }
        node = node.getParent();
      }
      return null;
    }

    public boolean equals(Object o) {
      boolean result = (this == o);

      if (!result) {
        final Path other = (Path)o;
        return pathIndex == other.pathIndex;
      }

      return result;
    }

    public int hashCode() {
      return pathIndex;
    }
  }

  public final class RecordSectionStats {
    public final int numRecords;
    public final int numPaths;
    public final int numTextPaths;
    public final int textWeight;
    public final int nonLinkTextWeight;
    public final double linkTagWeight;

    RecordSectionStats(int numRecords, int numPaths, int numTextPaths,
                       int textWeight, int nonLinkTextWeight,
                       double linkTagWeight) {
      this.numRecords = numRecords;
      this.numPaths = numPaths;
      this.numTextPaths = numTextPaths;
      this.textWeight = textWeight;
      this.nonLinkTextWeight = nonLinkTextWeight;
      this.linkTagWeight = linkTagWeight;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.
        append("nr:").append(numRecords).append(';').
        append("np:").append(numPaths).append(';').
        append("ntp:").append(numTextPaths).append(';').
        append("tw:").append(textWeight).append(';').
        append("nltw:").append(nonLinkTextWeight).append(';').
        append("lw:").append(linkTagWeight);

      return result.toString();
    }
  }

  public final class ModelStats {
    public final int numPaths;
    public final int numTextPaths;
    public final RecordSectionStats[] recordSectionStats;

    ModelStats(int numPaths, int numTextPaths,
               List<RecordSectionStats> recordSectionStats) {
      this.numPaths = numPaths;
      this.numTextPaths = numTextPaths;
      this.recordSectionStats = recordSectionStats.toArray(new RecordSectionStats[recordSectionStats.size()]);
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.
        append("p=").append(numPaths).append(',').
        append("tp=").append(numTextPaths).append(',').
        append("nrs=").append(recordSectionStats.length).append(',');

      int index = 0;
      for (RecordSectionStats rss : recordSectionStats) {
        result.append("rss[").append(index).append("]=").append(recordSectionStats[index++]);
        if (index < recordSectionStats.length) result.append(',');
      }

      return result.toString();
    }
  }

  public static interface PathVisitor {
    public boolean visit(Path path);
  }


  // usage1: SectionModel xmlInputFile dotFileName   # create dot file
  // usage2: SectionModel xmlInputFile               # just get stats
  public static final void main(String[] args) throws IOException {
    if (args.length != 1 && args.length != 2) {
      System.out.println("Usage: SectionModel xmlInputFile outputFile");
    }
    else {
      final Tree<XmlLite.Data> xmlTree = XmlFactory.readXmlTree(new File(args[0]), true, true, false);

      final SectionModel sectionModel = new SectionModel(xmlTree);
      System.out.println(args[0] + "|" + sectionModel.getModelStats());

      if (args.length == 2) {
        final Writer writer = org.sd.io.FileUtil.getWriter(args[1]);
        final Tree2Dot<XmlLite.Data> tree2dot = new Tree2Dot<XmlLite.Data>(xmlTree, sectionModel.getTreeAnalyzer(), new Xml2Dot.XmlLabelMaker());
        tree2dot.writeDot(writer);
        writer.close();
      }
    }
  }
}
