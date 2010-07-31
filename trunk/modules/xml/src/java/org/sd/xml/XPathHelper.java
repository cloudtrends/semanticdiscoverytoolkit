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


import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sd.util.MathUtil;
import org.sd.util.tree.Tree;


/**
 * A helper class for working with XPaths.
 * <p>
 * @author Spence Koehler
 */
public class XPathHelper {

  private Tree<XmlLite.Data> xmlTree;
  private boolean showAttributes;
  private boolean showNonTextLeaves;
  private Set<String> includeAttributes;
  private Set<String> excludeAttributes;
  private int numLeafNodes;
  private int numLeafDigits;

  public XPathHelper(Tree<XmlLite.Data> xmlTree) {
    this.xmlTree = xmlTree;
    this.showAttributes = false;
    this.showNonTextLeaves = false;
    this.includeAttributes = null;
    this.excludeAttributes = null;

    // initialize transientID on all leaf nodes
    int transientId = 0;
    for (Iterator<Tree<XmlLite.Data>> iter = xmlTree.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      final Tree<XmlLite.Data> node = iter.next();
      if (node.getData().asText() != null) {
        node.setTransientId(transientId++);
      }
    }

    this.numLeafNodes = transientId;
    this.numLeafDigits = MathUtil.getNumDigits(numLeafNodes);
  }

  public Tree<XmlLite.Data> getXmlTree() {
    return xmlTree;
  }

  public XPathHelper setShowAttributes(boolean showAttributes) {
    this.showAttributes = showAttributes;
    return this;
  }

  public XPathHelper setShowNonTextLeaves(boolean showNonTextLeaves) {
    this.showNonTextLeaves = showNonTextLeaves;
    return this;
  }

  public XPathHelper addIncludeAttribute(String attribute) {
    if (includeAttributes == null) includeAttributes = new LinkedHashSet<String>();
    includeAttributes.add(attribute);

    if (excludeAttributes != null && excludeAttributes.contains(attribute)) {
      removeExcludeAttribute(attribute);
    }

    return this;
  }

  public XPathHelper removeIncludeAttribute(String attribute) {
    if (includeAttributes != null) {
      includeAttributes.remove(attribute);
      if (includeAttributes.size() == 0) includeAttributes = null;
    }
    return this;
  }

  public XPathHelper addExcludeAttribute(String attribute) {
    if (excludeAttributes == null) excludeAttributes = new HashSet<String>();
    excludeAttributes.add(attribute);

    if (includeAttributes != null && includeAttributes.contains(attribute)) {
      removeIncludeAttribute(attribute);
    }

    return this;
  }

  public XPathHelper removeExcludeAttribute(String attribute) {
    if (excludeAttributes != null) {
      excludeAttributes.remove(attribute);
      if (excludeAttributes.size() == 0) excludeAttributes = new HashSet<String>();
    }
    return this;
  }

  /**
   * Represent the node from this instance's tree as a string.
   */
  public String asString(Tree<XmlLite.Data> node) {
    final StringBuilder result = new StringBuilder();

    final List<Tree<XmlLite.Data>> leaves = node.gatherLeaves();
    final int numLocalLeafDigits = MathUtil.getNumDigits(leaves.size());
    int leafNum = 0;
    for (Tree<XmlLite.Data> leaf : leaves) {
      if (leaf.getData().asText() == null && !showNonTextLeaves) continue;
      final String pathString = buildPathString(node, leaf, leafNum++, numLocalLeafDigits);
      if (result.length() > 0) result.append('\n');
      result.append(pathString);
    }

    return result.toString();
  }

  public String buildPathString(Tree<XmlLite.Data> node, Tree<XmlLite.Data> leaf, int leafNum, int numLocalLeafDigits) {
    final StringBuilder result = new StringBuilder();

    // leafTransientId:leafNum root{att1=val1,...}[idx]. ... =leafText
    // if leafNum > 0, then "root...node." are "invisible"
    boolean invisible = false;

    // build from leaf up to root
    for (Tree<XmlLite.Data> curNode = leaf; curNode != null; curNode = curNode.getParent()) {
      final String nodeString = buildNodeString(curNode);
      if (!invisible && leafNum > 0) {
        if (curNode == node) {
          invisible = true;
        }
      }

      if (invisible) {
        for (int i = 0; i < nodeString.length(); ++i) {
          // insert spaces for each invisible character
          result.insert(0, ' ');
        }
        if (curNode.getParent() != null) {
          // insert space for delimiter
          result.insert(0, ' ');
        }
      }
      else {
        // insert the node string
        result.insert(0, nodeString);
        // insert delimiter
        if (curNode.getParent() != null) {
          final char delim = (curNode.getData().asText() != null) ? '=' : '.';
          result.insert(0, delim);
        }
      }
    }

    result.insert(0, buildNodePrefix(leaf.getTransientId(), this.numLeafDigits, leafNum, numLocalLeafDigits));

    return result.toString();
  }

  public String buildNodeString(Tree<XmlLite.Data> node) {
    StringBuilder result = new StringBuilder();

    final XmlLite.Data xmlData = node.getData();
    if (xmlData.asText() != null) {
      result.append(xmlData.asText().text);
    }
    else if (xmlData.asTag() != null) {
      final XmlLite.Tag tag = xmlData.asTag();
      result.append(tag.name);

      final String attributesString = buildAttributesString(tag);
      if (attributesString != null) {
        result.append(attributesString);
      }

      result.
        append('[').
        append(getLocalRepeatIndex(node)).
        append(']');
      
    }

    return result.toString();
  }

  public int getLocalRepeatIndex(Tree<XmlLite.Data> node) {
    int result = -1;

    if (node.getData().asTag() == null) return result;

    final String tagName = node.getData().asTag().name;

    for (Tree<XmlLite.Data> sibling : node.getSiblings()) {
      final XmlLite.Tag tag = node.getData().asTag();
      if (tag != null) {
        if (tagName.equals(tag.name)) {
          ++result;
        }
        else {
          result = -1;
        }
      }
      if (sibling == node) break;
    }

    return result;
  }

  private final String buildNodePrefix(int globalLeafNum, int numGlobalLeafDigits, int localLeafNum, int numLocalLeafDigits) {
    final StringBuilder result = new StringBuilder();

    result.
      append(MathUtil.integerString(globalLeafNum, numGlobalLeafDigits)).
      append(':').
      append(MathUtil.integerString(localLeafNum, numLocalLeafDigits)).
      append(' ');

    return result.toString();
  }

  private final String buildAttributesString(XmlLite.Tag tag) {
    StringBuilder result = null;

    if (!showAttributes) return null;

    if (tag != null) {
      if (includeAttributes != null) {
        for (String attribute : includeAttributes) {
          if (excludeAttributes != null && excludeAttributes.contains(attribute)) continue;
          final String value = tag.getAttribute(attribute);
          if (value != null) {
            if (result == null) {
              result = new StringBuilder();
              result.append('{');
            }
            else {
              result.append(',');
            }
            result.append(attribute).append('=').append(value);
          }
        }
      }
      else {
        for (Map.Entry<String, String> attVal : tag.getAttributeEntries()) {
          final String attribute = attVal.getKey();
          if (excludeAttributes != null && excludeAttributes.contains(attribute)) continue;
          final String value = attVal.getValue();
          if (value != null) {
            if (result == null) {
              result = new StringBuilder();
              result.append('{');
            }
            else {
              result.append(',');
            }
            result.append(attribute).append('=').append(value);
          }
        }
      }
    }

    if (result != null) result.append('}');

    return (result == null) ? null : result.toString();
  }


  public static void main(String[] args) throws IOException {
    //arg0: xmlFile

    final String filename = args[0];
    final File file = new File(filename);
    final boolean isHtml = filename.endsWith(".html") || filename.endsWith(".htm");
    final Tree<XmlLite.Data> xmlTree = XmlFactory.readXmlTree(file, true, isHtml, false);

    final XPathHelper xpathHelper =
      new XPathHelper(xmlTree).
      setShowAttributes(true).
      setShowNonTextLeaves(true);

    System.out.println(xpathHelper.asString(xmlTree));
  }
}
