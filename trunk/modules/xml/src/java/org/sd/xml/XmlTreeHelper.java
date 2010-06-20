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


import org.sd.util.tree.TraversalIterator;
import org.sd.util.tree.Tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
//import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for manipulating and interacting with xml trees and data.
 * <p>
 * @author Spence Koehler
 */
public class XmlTreeHelper {

  public static String getTagName(Tree<XmlLite.Data> node) {
    String result = null;
    final XmlLite.Tag tag = node.getData().asTag();
    if (tag != null) {
      result = tag.name;
    }
    return result;
  }

  /**
   * Determine whether the tag list has the tag name.
   *
   * @param tagList  a list of xml tag instances.
   * @param tagName  an already lowercased tag name.
   *
   * @return true if the tagName appears in the tagList; otherwise, false.
   */
  public static final boolean hasTag(List<XmlLite.Tag> tagList, String tagName) {
    for (XmlLite.Tag tag : tagList) {
      if (tagName.equals(tag.name)) {
        return true;
      }
    }
    return false;
  }

  public static Object getProperty(Tree<XmlLite.Data> node, String property) {
    Object result = null;

    if (node != null) {
      final XmlLite.Data data = node.getData();
      result = data.getProperty(property);
    }

    return result;
  }

  /**
   * Clear all properties in the node.
   */
  public static void clearProperties(Tree<XmlLite.Data> node) {
    if (node != null) {
      final XmlLite.Data data = node.getData();
      data.clearProperties();
    }
  }

  /**
   * Clear the properties in the node and in all of its descendants.
   */
  public static void clearAllProperties(Tree<XmlLite.Data> node) {
    if (node != null) {
      clearProperties(node);

      final List<Tree<XmlLite.Data>> children = node.getChildren();
      if (children != null) {
        for (Tree<XmlLite.Data> child : children) {
          clearAllProperties(child);
        }
      }
    }
  }

  /**
   * Get all (deep) text under the node, concatenating text from multiple
   * non-empty text node strings with a single space between.
   */
  public static String getAllText(Tree<XmlLite.Data> node) {
    final StringBuilder result = new StringBuilder();

    if (node != null) {
      for (Iterator<Tree<XmlLite.Data>> iter = node.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
        final Tree<XmlLite.Data> curNode = iter.next();
        final XmlLite.Text nodeText = curNode.getData().asText();
        if (nodeText != null && nodeText.text != null && nodeText.text.length() > 0) {
          if (result.length() > 0) result.append(' ');
          result.append(nodeText.text);
        }
      }
    }

    return result.toString();
  }

  /**
   * Get all (deep) text under the nodes, concatenating text from multiple
   * non-empty text node strings with a single space between.
   */
  public static String getAllText(List<Tree<XmlLite.Data>> nodes) {
    final StringBuilder result = new StringBuilder();

    if (nodes != null) {
      for (Tree<XmlLite.Data> node : nodes) {
        final String text = getAllText(node);
        if (text != null && !"".equals(text)) {
          if (result.length() > 0) result.append(' ');
          result.append(text);
        }
      }
    }

    return result.toString();
  }

  /**
   * Get each (deep) text under the node in document order, ignoring empties.
   */
  public static String[] getEachText(Tree<XmlLite.Data> node) {
    final List<String> result = new ArrayList<String>();

    for (Iterator<Tree<XmlLite.Data>> iter = node.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      final Tree<XmlLite.Data> curNode = iter.next();
      final XmlLite.Text nodeText = curNode.getData().asText();
      if (nodeText != null && nodeText.text != null && nodeText.text.length() > 0) {
        result.add(nodeText.text);
      }
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Get each (deep) text node under the node in document order, ignoring empties.
   */
  public static List<Tree<XmlLite.Data>> getTextNodes(Tree<XmlLite.Data> node) {
    final List<Tree<XmlLite.Data>> result = new ArrayList<Tree<XmlLite.Data>>();

    for (Iterator<Tree<XmlLite.Data>> iter = node.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      final Tree<XmlLite.Data> curNode = iter.next();
      final XmlLite.Text nodeText = curNode.getData().asText();
      if (nodeText != null && nodeText.text != null && nodeText.text.length() > 0) {
        result.add(curNode);
      }
    }

    return result;
  }

  /**
   * Get each text "bite" under the node in document order, where a text "bite"
   * is a node encapsulating contiguous text as determined by the following
   * algorithm:
   * <p>
   * Any node that has an immediate non-empty text node child is a text bite.
   * <p>
   * Note that this will typically identify paragraphs, ignoring formatting like
   * bold in html. At times, this may fail to find any smaller bites than the
   * root node.
   */
  public static List<Tree<XmlLite.Data>> getTextBiteNodes(Tree<XmlLite.Data> node) {
    final List<Tree<XmlLite.Data>> result = new ArrayList<Tree<XmlLite.Data>>();

    if (node == null) return result;

    for (TraversalIterator<XmlLite.Data> iter = node.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      final Tree<XmlLite.Data> curNode = iter.next();
      final List<Tree<XmlLite.Data>> children = curNode.getChildren();
      if (children != null) {
        for (Tree<XmlLite.Data> child : children) {
          final XmlLite.Text text = child.getData().asText();
          if (text != null && text.text.length() > 0) {
            result.add(curNode);
            iter.skip();
            break;
          }
        }
      }
      else {
        final XmlLite.Text text = curNode.getData().asText();
        if (text != null && text.text.length() > 0) {
          result.add(curNode);
        }
      }
    }

    return result;
  }

  /**
   * Clear all (deep) text under the node by deleting all text nodes.
   */
  public static void clearAllText(Tree<XmlLite.Data> node) {
    for (TraversalIterator<XmlLite.Data> iter = node.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      final Tree<XmlLite.Data> curNode = iter.next();
      final XmlLite.Text nodeText = curNode.getData().asText();
      if (nodeText != null) {
        iter.remove();
      }
    }
  }

  /**
   * Clear all (immediate) text under the node by deleting text nodes.
   */
  public static void clearText(Tree<XmlLite.Data> node) {
    final List<Tree<XmlLite.Data>> children = node.getChildren();
    if (children != null) {
      for (Tree<XmlLite.Data> child : children) {
        final XmlLite.Text nodeText = child.getData().asText();
        if (nodeText != null) {
          child.prune(true, true);
        }
      }
    }    
  }

  /**
   * Get all (immediate) text under the node, concatenating text from multiple
   * non-empty text node strings with a single space between.
   */
  public static String getText(Tree<XmlLite.Data> node) {
    final StringBuilder result = new StringBuilder();

    final List<Tree<XmlLite.Data>> children = node.getChildren();
    if (children != null) {
      for (Tree<XmlLite.Data> child : children) {
        final XmlLite.Text nodeText = child.getData().asText();
        if (nodeText != null && nodeText.text != null && !"".equals(nodeText.text)) {
          if (result.length() > 0) result.append(' ');
          result.append(nodeText.text);
        }
      }
    }

    return result.toString();
  }

  /**
   * Add a text node (immediately) under the node as a new child (at the
   * end of its children).
   *
   * @return the new text node.
   */
  public static Tree<XmlLite.Data> addText(Tree<XmlLite.Data> node, String text) {
    final Tree<XmlLite.Data> result = new Tree<XmlLite.Data>(new XmlLite.Text(text));
    result.getData().setContainer(result);
    node.addChild(result);
    return result;
  }

  /**
   * Get the attributes on the node or null if there are none.
   */
  public static Set<String> getAttributes(Tree<XmlLite.Data> node) {
    Set<String> result = null;

    final XmlLite.Tag tag = node.getData().asTag();
    if (tag != null) {
      final Set<Map.Entry<String, String>> attributeEntries = tag.getAttributeEntries();
      if (attributeEntries != null && attributeEntries.size() > 0) {
        result = new LinkedHashSet<String>();
        for (Map.Entry<String, String> attributeEntry : attributeEntries) {
          final String att = attributeEntry.getKey();
          if (att != null && att.length() > 0) {
            result.add(attributeEntry.getKey());
          }
        }
      }
    }

    return result;
  }

  /**
   * Get the attribute's value from the given (tag) node.
   */
  public static String getAttribute(Tree<XmlLite.Data> node, String attribute) {
    String result = null;
    final XmlLite.Tag tag = node.getData().asTag();
    if (tag != null) {
      result = tag.getAttribute(attribute);
    }
    return result;
  }

  /**
   * Remove the attribute on the (tag) node.
   */
  public static void removeAttribute(Tree<XmlLite.Data> node, String attribute) {
    final XmlLite.Tag tag = node.getData().asTag();
    if (tag != null) {
      tag.removeAttribute(attribute);
    }
  }

  /**
   * Clear the attribute on the (tag) node.
   * <p>
   * Note that the attribute will still be present in the tag mapped to an empty string.
   */
  public static void clearAttribute(Tree<XmlLite.Data> node, String attribute) {
    final XmlLite.Tag tag = node.getData().asTag();
    if (tag != null) {
      tag.clearAttribute(attribute);
    }
  }

  /**
   * Set the (tag) node's attribute to the value.
   */
  public static void setAttribute(Tree<XmlLite.Data> node, String attribute, String value) {
    final XmlLite.Tag tag = node.getData().asTag();
    if (tag != null) {
      tag.setAttribute(attribute, value);
    }
  }

  /**
   * Remove the property on the node.
   */
  public static void removeProperty(Tree<XmlLite.Data> node, String property) {
    final XmlLite.Data data = node.getData();
    data.removeProperty(property);
  }

  /**
   * Set the property on the node to the value.
   */
  public static void setProperty(Tree<XmlLite.Data> node, String property, Object value) {
    final XmlLite.Data data = node.getData();
    data.setProperty(property, value);
  }

  /**
   * Determine whether the node has the property.
   *
   * @return true if the node has the property, even if the property value is null;
   *         otherwise, false.
   */
  public static boolean hasProperty(Tree<XmlLite.Data> node, String property) {
    final XmlLite.Data data = node.getData();
    return data.hasProperty(property);
  }

  /**
   * Build an xpath from the shallow (closer to root) node down to the deep
   * (closer to leaf) node. If include sibling numbers, then create a specific
   * xpath to identify just the given deep node; otherwise, create a general
   * xpath. If include shallow node, add the shallow node's tag to the xpath;
   * otherwise, start from the shallow node's child leading down to deep node.
   */
  public static final String getPath(Tree<XmlLite.Data> shallowNode, Tree<XmlLite.Data> deepNode, boolean includeSiblingNumbers, boolean includeShallowNode) {
    final StringBuilder result = new StringBuilder();

    if (deepNode == null) return null;

    final String tagName = getTagName(deepNode);
    if (tagName != null) {
      if ((deepNode == shallowNode && includeShallowNode) || (deepNode != shallowNode)) {
        result.append(tagName);
      }
      if (includeSiblingNumbers) {
        final int sibNum = deepNode.getSiblingPosition();
        result.append('[').append(sibNum).append(']');
      }
    }

    String parentPath = null;

    if (shallowNode != deepNode) {
      parentPath = getPath(shallowNode, deepNode.getParent(), includeSiblingNumbers, includeShallowNode);
    }

    if (parentPath != null) {
      if (result.length() > 0) result.insert(0, '.');
      result.insert(0, parentPath);
    }

    return result.length() == 0 ? null : result.toString();
  }

  /**
   * Find and collect nodes in the tree with the given tags.
   */
  public static final List<Tree<XmlLite.Data>> findTags(Tree<XmlLite.Data> xmlNode, Set<String> tags, boolean skip) {
    List<Tree<XmlLite.Data>> result = null;

    if (xmlNode != null) {
      for (TraversalIterator<XmlLite.Data> it = xmlNode.iterator(Tree.Traversal.BREADTH_FIRST); it.hasNext(); ) {
        final Tree<XmlLite.Data> node = it.next();
        final XmlLite.Tag nodeTag = node.getData().asTag();
        if (nodeTag != null && tags.contains(nodeTag.name)) {
          if (result == null) result = new ArrayList<Tree<XmlLite.Data>>();
          result.add(node);
          if (skip) it.skip();
        }
      }
    }

    return result;
  }

  /**
   * Find and collect text nodes in the tree that are not under the given tags.
   */
  public static final List<Tree<XmlLite.Data>> excludeTags(Tree<XmlLite.Data> xmlNode, Set<String> tags) {
    List<Tree<XmlLite.Data>> result = null;

    for (TraversalIterator<XmlLite.Data> it = xmlNode.iterator(Tree.Traversal.DEPTH_FIRST); it.hasNext(); ) {
      final Tree<XmlLite.Data> node = it.next();
      final XmlLite.Data data = node.getData();
      if (data.asText() != null) {
        if (result == null) result = new ArrayList<Tree<XmlLite.Data>>();
        result.add(node);
      }
      else {
        final XmlLite.Tag nodeTag = data.asTag();
        if (nodeTag != null && tags.contains(nodeTag.name)) {
          it.skip();  // skip excluded tag.
        }
      }
    }

    return result;
  }

  /**
   * Build an xml tree node starting from the indicated depth in the tag stack
   * down to its full depth, adding the xmlNode at the bottom.
   */
  public static final Tree<XmlLite.Data> buildXmlTree(TagStack tagStack, Tree<XmlLite.Data> xmlNode, int fromDepth) {
    final List<XmlLite.Tag> tags = tagStack.getTags();

    final int depth = tags.size();
    if (fromDepth == depth) return xmlNode;
    if (fromDepth > depth) return null;

    Tree<XmlLite.Data> result = null;

    final XmlLite.Tag theTag = tags.get(fromDepth);
    result = new Tree<XmlLite.Data>(theTag);

    Tree<XmlLite.Data> curNode = result;
    for (int i = fromDepth + 1; i < depth; ++i) {
      curNode = curNode.addChild(tags.get(i));
    }

    curNode.addChild(xmlNode);

    return result;
  }

  /**
   * Find the (first) node in the tree with the given data.
   */
  public static final Tree<XmlLite.Data> findNode(Tree<XmlLite.Data> xmlTree, XmlLite.Data withData) {
    Tree<XmlLite.Data> result = null;

    for (Iterator<Tree<XmlLite.Data>> it = xmlTree.iterator(Tree.Traversal.BREADTH_FIRST); it.hasNext(); ) {
      final Tree<XmlLite.Data> xmlNode = it.next();
      final XmlLite.Data nodeData = xmlNode.getData();
      if (nodeData == withData) {
        result = xmlNode;
        break;
      }
    }

    return result;
  }

  /**
   * Find the (first) node in the tree with the given tag name.
   */
  public static final Tree<XmlLite.Data> findNode(Tree<XmlLite.Data> xmlTree, String tagName) {
    Tree<XmlLite.Data> result = null;

    for (Iterator<Tree<XmlLite.Data>> it = xmlTree.iterator(Tree.Traversal.BREADTH_FIRST); it.hasNext(); ) {
      final Tree<XmlLite.Data> xmlNode = it.next();
      final XmlLite.Tag tag = xmlNode.getData().asTag();
      if (tag != null && tagName.equals(tag.name)) {
        result = xmlNode;
        break;
      }
    }

    return result;
  }

  /**
   * Find the (first) node in the tree with the given text.
   */
  public static final Tree<XmlLite.Data> findNode(Tree<XmlLite.Data> xmlTree, String text, boolean caseInsensitive) {
    Tree<XmlLite.Data> result = null;

    for (Iterator<Tree<XmlLite.Data>> it = xmlTree.iterator(Tree.Traversal.DEPTH_FIRST); it.hasNext(); ) {
      final Tree<XmlLite.Data> xmlNode = it.next();
      final XmlLite.Text nodeText = xmlNode.getData().asText();
      if (nodeText != null && nodeText.text != null && !"".equals(nodeText.text)) {
        final boolean matches = (caseInsensitive) ? nodeText.text.equalsIgnoreCase(text) : nodeText.text.equals(text);
        if (matches) {
          result = xmlNode;
          break;
        }
      }
    }

    return result;
  }

  /**
   * Collect the xml nodes from the tree that have the given property.
   *
   * @return nodes with the property or null.
   */
  public static final List<Tree<XmlLite.Data>> getNodesWithProperty(Tree<XmlLite.Data> xmlTree, String property) {
    List<Tree<XmlLite.Data>> result = null;

    for (Iterator<Tree<XmlLite.Data>> it = xmlTree.iterator(Tree.Traversal.DEPTH_FIRST); it.hasNext(); ) {
      final Tree<XmlLite.Data> xmlNode = it.next();
      if (xmlNode.getData().hasProperty(property)) {
        if (result == null) result = new ArrayList<Tree<XmlLite.Data>>();
        result.add(xmlNode);
      }
    }

    return result;
  }

  /**
   * Collect the xml nodes from the tree that have the given property.
   *
   * @return nodes with the property or null.
   */
  public static final List<Tree<XmlLite.Data>> getNodesWithPropertyOrAttribute(Tree<XmlLite.Data> xmlTree, String property, String attribute) {
    List<Tree<XmlLite.Data>> result = null;

    for (Iterator<Tree<XmlLite.Data>> it = xmlTree.iterator(Tree.Traversal.DEPTH_FIRST); it.hasNext(); ) {
      final Tree<XmlLite.Data> xmlNode = it.next();
      final XmlLite.Data xmlData = xmlNode.getData();
      boolean foundIt = xmlData.hasProperty(property);
      if (!foundIt) {
        final XmlLite.Tag xmlTag = xmlData.asTag();
        if (xmlTag != null) {
          foundIt = xmlTag.getAttribute(attribute) != null;
        }
      }

      if (foundIt) {
        if (result == null) result = new ArrayList<Tree<XmlLite.Data>>();
        result.add(xmlNode);
      }
    }

    return result;
  }

  /**
   * Get the deepest nodes in the tree that have one of the tags.
   * <p>
   * If the tag is not found in a path with a non-empty text node, include
   * the text node.
   */
  public static final List<Tree<XmlLite.Data>> getDeepestNodes(Tree<XmlLite.Data> xmlTree, Set<String> withTag) {
    final List<Tree<XmlLite.Data>> result = new ArrayList<Tree<XmlLite.Data>>();
    splitOrAdd(xmlTree, withTag, result, 0);
    return result;
  }

  private static final boolean splitOrAdd(Tree<XmlLite.Data> xmlNode, Set<String> withTag, List<Tree<XmlLite.Data>> result, int depth) {
    boolean splitIt = false;
    if (depth >= 512) return splitIt;  // avoid stack overflow with badly formatted data (stack actually overflows at about 1022.)
    if (!hasNonEmptyText(xmlNode)) return splitIt;

    final List<Tree<XmlLite.Data>> children = xmlNode.getChildren();
    List<Tree<XmlLite.Data>> nodesToAdd = new ArrayList<Tree<XmlLite.Data>>();

    if (children != null && children.size() > 0) {
      for (Tree<XmlLite.Data> child : children) {
        if (!hasNonEmptyText(child)) continue;

        final XmlLite.Data xmlData = child.getData();
        final XmlLite.Tag xmlTag = xmlData.asTag();

        if (xmlTag != null) {
          splitIt |= withTag.contains(xmlTag.name);
        }

        splitIt |= splitOrAdd(child, withTag, nodesToAdd, depth + 1);
      }
    }

    if (!splitIt) {
      result.add(xmlNode);
    }
    else {
      result.addAll(nodesToAdd);
    }

    return splitIt;
  }

  public static final boolean hasNonEmptyText(Tree<XmlLite.Data> xmlNode) {
    boolean result = false;

    for (Iterator<Tree<XmlLite.Data>> iter = xmlNode.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      final Tree<XmlLite.Data> curNode = iter.next();
      final XmlLite.Text nodeText = curNode.getData().asText();
      if (nodeText != null && nodeText.text != null && !"".equals(nodeText.text)) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Get the highest ancestor to the deepNode that contains only that node.
   */
  public static final Tree<XmlLite.Data> getHighestContainingNode(Tree<XmlLite.Data> deepNode) {
    if (deepNode == null) return null;

    Tree<XmlLite.Data> result = deepNode;
    while (result.getNumSiblings() <= 1 && result.getParent() != null) {
      result = result.getParent();
    }

    return result;
  }

  /**
   * Get the next node in the given node's tree that does not include the node.
   */
  public static final Tree<XmlLite.Data> getNextNode(Tree<XmlLite.Data> node) {
    Tree<XmlLite.Data> result = null;

    while (result == null && node != null) {
      result = node.getNextSibling();
      if (result == null) {
        node = node.getParent();
      }
    }

    return result;
  }
}
