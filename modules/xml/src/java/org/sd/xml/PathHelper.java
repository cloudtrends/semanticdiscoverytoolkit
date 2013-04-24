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

import org.sd.util.PropertiesParser;
import org.sd.util.tree.KeyFunction;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeAnalyzer;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Helper class for working with xml paths.
 * <p>
 * @author Spence Koehler
 */
public class PathHelper {

  private Tree<XmlLite.Data> xmlTree;
  private TreeAnalyzer<XmlLite.Data> treeAnalyzer;

  public PathHelper(Tree<XmlLite.Data> xmlTree, TreeAnalyzer<XmlLite.Data> treeAnalyzer) {
    this.xmlTree = xmlTree;
    this.treeAnalyzer = (treeAnalyzer != null) ? treeAnalyzer
      : new TreeAnalyzer<XmlLite.Data>(xmlTree, new KeyFunction<XmlLite.Data>() {
        public String getKey(XmlLite.Data data) {
          String result = null;

          if (data != null) {
            final XmlLite.Tag tag = data.asTag();
            if (tag != null) {
              result = tag.name;
            }
          }

          return result;
        }
      });
  }

  public TreeAnalyzer<XmlLite.Data> getTreeAnalyzer() {
    return treeAnalyzer;
  }

  public List<Tree<XmlLite.Data>> getLeaves() {
    return xmlTree.gatherLeaves();
  }

  public List<Tree<XmlLite.Data>> getPathToNode(Tree<XmlLite.Data> node) {
    return treeAnalyzer.getPathTo(node);
  }

  public static final int LEVEL                          = (1 << 0);
  public static final int NUM_CHILDREN                   = (1 << 1);
  public static final int LOCAL_INDEX                    = (1 << 2);
  public static final int LEVEL_INDEX                    = (1 << 3);
  public static final int LOCAL_REPEAT_INDEX             = (1 << 4);
  public static final int LEVEL_REPEAT_INDEX             = (1 << 5);
  public static final int LOCAL_REPEAT_ID                = (1 << 6);
  public static final int LEVEL_REPEAT_ID                = (1 << 7);
  public static final int CONSECUTIVE_LOCAL_REPEAT_INDEX = (1 << 8);
  public static final int CONSECUTIVE_LEVEL_REPEAT_INDEX = (1 << 9);
  public static final int CONSECUTIVE_LOCAL_REPEAT_ID    = (1 << 10);
  public static final int CONSECUTIVE_LEVEL_REPEAT_ID    = (1 << 11);
  public static final int NUM_SIBLINGS                   = (1 << 12);
  public static final int NUM_LEVEL_NODES                = (1 << 13);
  public static final int NUM_LOCAL_REPEAT_SIBS          = (1 << 14);
  public static final int NUM_LEVEL_REPEAT_SIBS          = (1 << 15);
  public static final int NUM_CONSEC_LOCAL_SIBS          = (1 << 16);
  public static final int NUM_CONSEC_LEVEL_SIBS          = (1 << 17);
  public static final int TEXT_DATA                      = (1 << 18);
  public static final int CLASS_ATTRIBUTE                = (1 << 19);

  public String buildPathKey(Tree<XmlLite.Data> node) {
    return buildPathKey(getPathToNode(node));
  }

  public String buildPathKey(Tree<XmlLite.Data> node, int includeMask, boolean showAttributeNames) {
    return buildPathKey(getPathToNode(node), includeMask, showAttributeNames);
  }

  /**
   * default: show local_repeat_index and text w/out names.
   */
  public String buildPathKey(List<Tree<XmlLite.Data>> path) {
    return buildPathKey(path, LOCAL_REPEAT_INDEX | TEXT_DATA | CLASS_ATTRIBUTE, false);
  }

  public String buildPathKey(List<Tree<XmlLite.Data>> path, int includeMask, boolean showAttributeNames) {
    final StringBuilder sb = new StringBuilder();

    for (Iterator<Tree<XmlLite.Data>> iter = path.iterator(); iter.hasNext(); ) {
      final Tree<XmlLite.Data> pathElement = iter.next();

      final TreeAnalyzer.NodeInfo info = treeAnalyzer.getNodeInfo(pathElement);
      if (info == null) continue;

      final XmlLite.Data data = pathElement.getData();
      final XmlLite.Text text = data.asText();
      final XmlLite.Tag tag = data.asTag();

      if (tag != null) {
        boolean didOne = false;
        sb.append(tag.name);

        if (includeMask != 0) {
          if (includeMask != TEXT_DATA) sb.append('[');
          
          if ((includeMask & LEVEL) == LEVEL) didOne = doAppend(sb, "level", info.level, didOne, showAttributeNames);
          if ((includeMask & NUM_CHILDREN) == NUM_CHILDREN) didOne = doAppend(sb, "numc", info.numChildren, didOne, showAttributeNames);
          if ((includeMask & LOCAL_INDEX) == LOCAL_INDEX) didOne = doAppend(sb, "loci", info.localIndex, didOne, showAttributeNames);
          if ((includeMask & LEVEL_INDEX) == LEVEL_INDEX) didOne = doAppend(sb, "levi", info.levelIndex, didOne, showAttributeNames);
          if ((includeMask & LOCAL_REPEAT_INDEX) == LOCAL_REPEAT_INDEX) didOne = doAppend(sb, "locri", info.localRepeatIndex, didOne, showAttributeNames);
          if ((includeMask & LEVEL_REPEAT_INDEX) == LEVEL_REPEAT_INDEX) didOne = doAppend(sb, "levri", info.levelRepeatIndex, didOne, showAttributeNames);
          if ((includeMask & LOCAL_REPEAT_ID) == LOCAL_REPEAT_ID) didOne = doAppend(sb, "locrid", info.localRepeatId, didOne, showAttributeNames);
          if ((includeMask & LEVEL_REPEAT_ID) == LEVEL_REPEAT_ID) didOne = doAppend(sb, "levrid", info.levelRepeatId, didOne, showAttributeNames);
          if ((includeMask & CONSECUTIVE_LOCAL_REPEAT_INDEX) == CONSECUTIVE_LOCAL_REPEAT_INDEX) didOne = doAppend(sb, "clocri", info.consecutiveLocalRepeatIndex, didOne, showAttributeNames);
          if ((includeMask & CONSECUTIVE_LEVEL_REPEAT_INDEX) == CONSECUTIVE_LEVEL_REPEAT_INDEX) didOne = doAppend(sb, "clevri", info.consecutiveLevelRepeatIndex, didOne, showAttributeNames);
          if ((includeMask & CONSECUTIVE_LOCAL_REPEAT_ID) == CONSECUTIVE_LOCAL_REPEAT_ID) didOne = doAppend(sb, "clocrid", info.consecutiveLocalRepeatId, didOne, showAttributeNames);
          if ((includeMask & CONSECUTIVE_LEVEL_REPEAT_ID) == CONSECUTIVE_LEVEL_REPEAT_ID) didOne = doAppend(sb, "clevrid", info.consecutiveLevelRepeatId, didOne, showAttributeNames);
          if ((includeMask & NUM_SIBLINGS) == NUM_SIBLINGS) didOne = doAppend(sb, "nums", info.numSiblings, didOne, showAttributeNames);
          if ((includeMask & NUM_LEVEL_NODES) == NUM_LEVEL_NODES) didOne = doAppend(sb, "numl", info.numLevelNodes, didOne, showAttributeNames);
          if ((includeMask & NUM_LOCAL_REPEAT_SIBS) == NUM_LOCAL_REPEAT_SIBS) didOne = doAppend(sb, "nlocrs", info.numLocalRepeatSiblings, didOne, showAttributeNames);
          if ((includeMask & NUM_LEVEL_REPEAT_SIBS) == NUM_LEVEL_REPEAT_SIBS) didOne = doAppend(sb, "nlevrs", info.numLevelRepeatSiblings, didOne, showAttributeNames);
          if ((includeMask & NUM_CONSEC_LOCAL_SIBS) == NUM_CONSEC_LOCAL_SIBS) didOne = doAppend(sb, "nclocs", info.numConsecutiveLocalSiblings, didOne, showAttributeNames);
          if ((includeMask & NUM_CONSEC_LEVEL_SIBS) == NUM_CONSEC_LEVEL_SIBS) didOne = doAppend(sb, "nclevs", info.numConsecutiveLevelSiblings, didOne, showAttributeNames);
          if ((includeMask & CLASS_ATTRIBUTE) == CLASS_ATTRIBUTE && tag.getAttribute("class") != null) didOne = doAppend(sb, "class", tag.getAttribute("class"), didOne, true);

          if (includeMask != TEXT_DATA) sb.append(']');
        }

        if (iter.hasNext()) sb.append('.');
      }
      else if (text != null) {
        sb.append("text");
        if ((includeMask & TEXT_DATA) == TEXT_DATA) sb.append("='").append(text.text).append("'");
      }
    }

    return sb.toString();
  }

  public static final List<Tree<XmlLite.Data>> gatherLeaves(Tree<XmlLite.Data> xmlTree, Set<String> ignoreTags) {
    final List<Tree<XmlLite.Data>> result = new ArrayList<Tree<XmlLite.Data>>();
    final LinkedList<Tree<XmlLite.Data>> queue = new LinkedList<Tree<XmlLite.Data>>();
    queue.add(xmlTree);

    while (queue.size() > 0) {
      final Tree<XmlLite.Data> node = queue.removeFirst();
      if (node == null) continue;
      final List<Tree<XmlLite.Data>> children = node.getChildren();
      if (children == null) {  // found a leaf
        final XmlLite.Text textData = node.getData().asText();
        if (textData != null) {
          result.add(node);
        }
      }
      else {
        final XmlLite.Tag tagData = node.getData().asTag();
        if (tagData != null) {
          if (ignoreTags == null || !ignoreTags.contains(tagData.name)) {  // include this branch
            queue.addAll(0, children);  // depth-first
          }
        }
      }
    }

    return result;
  }

  private boolean doAppend(StringBuilder sb, String name, Object value, boolean didOne, boolean showAttributeNames) {
    if (didOne) sb.append(',');
    if (showAttributeNames && name != null) sb.append(name).append('=');
    sb.append(value);
    return true;
  }

  public static final boolean isHorizontalRule(Path path)
  {
    if(path == null || path.hasText() || !path.hasTagStack())
      return false;

    boolean result = false;
    TagStack stack = path.getTagStack();
    XmlLite.Tag tag = stack.getTag(stack.depth() - 1);
    if(tag != null)
      result = "hr".equals(tag.name);
    return result;
  }

  public static final boolean isBreak(Path path)
  {
    if(path == null || path.hasText() || !path.hasTagStack())
      return false;

    boolean result = false;
    TagStack stack = path.getTagStack();
    XmlLite.Tag tag = stack.getTag(stack.depth() - 1);
    if(tag != null)
      result = HtmlHelper.INLINE_BREAK_TAGS.contains(tag.name);
    return result;
  }

  // check to see if either path is inline the other
  public static final boolean inlinePaths(Path path1, Path path2)
  {
    if(path1.isInline(path2) || path2.isInline(path1))
    {
      if(isBreak(path1))
        return false;
      else
        return true;
    }
    else
      return false;
  }

  public static final void dumpPaths(Tree<XmlLite.Data> xmlTree) {
    dumpPaths(xmlTree, System.out);
  }

  public static final void dumpPaths(Tree<XmlLite.Data> xmlTree, PrintStream out) {
    final PathHelper pathHelper = new PathHelper(xmlTree, null);
    
    final List<Tree<XmlLite.Data>> leaves = pathHelper.getLeaves();

    for (Tree<XmlLite.Data> leaf : leaves) {
      out.println(pathHelper.buildPathKey(leaf));
    }
  }

  public static void main(String[] args) throws IOException {
    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();
    args = pp.getArgs();

    final boolean ignoreComments = "true".equalsIgnoreCase(properties.getProperty("ignoreComments", "true"));
    final boolean htmlFlag = "true".equalsIgnoreCase(properties.getProperty("htmlFlag", "false"));
    final boolean requireXmlTag = "true".equalsIgnoreCase(properties.getProperty("requireXmlTag", "false"));

    final Tree<XmlLite.Data> xmlTree = XmlFactory.readXmlTree(new File(args[0]), ignoreComments, htmlFlag, requireXmlTag);
    dumpPaths(xmlTree);

/*
    final PathHelper pathHelper = new PathHelper(xmlTree, null);
    final List<Tree<XmlLite.Data>> leaves = pathHelper.getLeaves();

//    final int flag = NUM_LOCAL_REPEAT_SIBS | NUM_CONSEC_LOCAL_SIBS;

    for (Tree<XmlLite.Data> leaf : leaves) {
//      System.out.println(pathHelper.buildPathKey(leaf, flag, true));
      System.out.println(pathHelper.buildPathKey(leaf));
    }
*/
  }
}
