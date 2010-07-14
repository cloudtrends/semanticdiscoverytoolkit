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


import org.sd.util.tree.NodePath;
import org.sd.util.tree.TraversalIterator;
import org.sd.util.tree.Tree;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Extension of NodePath for XmlLite trees.
 * <p>
 * @author Spence Koehler
 */
public class XPath {

  //note: implementing as "hasa" nodePath instead of "isa" nodePath.
  private NodePath<XmlLite.Data> nodePath;
  private NodePath<XmlLite.Data> shortNodePath;  // null or nodePath w/out trailing ".**"

  private final NodePath.DataMatcherMaker<XmlLite.Data> xmlDataMatcherMaker = new NodePath.DataMatcherMaker<XmlLite.Data>() {
    public NodePath.DataMatcher<XmlLite.Data> makeDataMatcher(final String dataString) {
      return new XmlDataMatcher(dataString);
    }
  };

  private final NodePath.PatternSplitter patternSplitter = new XmlPatternSplitter();

  /**
   * Construct with a NodePath pattern.
   * <p>
   * Constructs a path from a string of the form p1.p2. ... .pN
   * <p>
   * where pi is of the form:
   * <ul>
   * <li>"x" -- matches when x.equals(node.getData().asTag().name) or </li>
   * <li>"x[subscript]" -- matches as for "x" AND the 0-based local sibling
   *                       index is a member of the comma-delimited list of
   *                       'subscript' integers and/or hyphenated ranges or </li>
   * <li>"**" -- matches any "x" down any number of nodes along the path.</li>
   * </ul>
   */
  public XPath(String patternString) {
    buildNodePath(patternString);
  }

  private final void buildNodePath(String patternString) {
    this.nodePath = new NodePath<XmlLite.Data>(patternString, xmlDataMatcherMaker, patternSplitter);

    this.shortNodePath = patternString.endsWith(".**") ?
      new NodePath<XmlLite.Data>(patternString.substring(0, patternString.length() - 3), xmlDataMatcherMaker, patternSplitter) :
      null;
  }

  /**
   * Get the nodes that match this xpath starting from the given node.
   * <p>
   * Note: subscripts are ignored for the first element in the path.
   *
   * @return the matching node(s) or null.
   */
  public List<Tree<XmlLite.Data>> getNodes(Tree<XmlLite.Data> node) {
    return nodePath.apply(node);
  }

  /**
   * Convenience method for getting the first matching node against this xpath.
   */
  public Tree<XmlLite.Data> getFirstNode(Tree<XmlLite.Data> node) {
    Tree<XmlLite.Data> result = null;
    final List<Tree<XmlLite.Data>> nodes = getNodes(node);
    if (nodes != null && nodes.size() > 0) {
      result = nodes.get(0);
    }
    return result;
  }

  /**
   * Get the nodes that match this xpath and have the given attribute starting
   * from the given node.
   * <p>
   * Note: subscripts are ignored for the first element in the path.
   * 
   * @param node       The xml node to start matching this xpath from.
   * @param attribute  The attribute that must exist for the node to match. If the
   *                   attribute is of the form "attribute=value", then the attribute
   *                   must not only exist, but its value must equal 'value'.
   *
   * @return the matching node(s) or null.
   */
  public List<Tree<XmlLite.Data>> getNodes(Tree<XmlLite.Data> node, String attribute) {
    List<Tree<XmlLite.Data>> result = getNodes(node);
    if (result != null && attribute != null) {
      final String[] attval = attribute.split("=");
      for (Iterator<Tree<XmlLite.Data>> iter = result.iterator(); iter.hasNext(); ) {
        final Tree<XmlLite.Data> curNode = iter.next();
        final String value = XmlTreeHelper.getAttribute(curNode, attval[0]);

        boolean valueMatches = (value != null);
        if (valueMatches && attval.length == 2) {
          valueMatches = value.equals(attval[1]);
        }
        if (!valueMatches) iter.remove();
      }
      if (result.size() == 0) result = null;
    }
    return result;
  }

  /**
   * Convenience method for getting the first matching node with the given attribute
   * against this xpath.
   * 
   * @param node       The xml node to start matching this xpath from.
   * @param attribute  The attribute that must exist for the node to match. If the
   *                   attribute is of the form "attribute=value", then the attribute
   *                   must not only exist, but its value must equal 'value'.
   */
  public Tree<XmlLite.Data> getFirstNode(Tree<XmlLite.Data> node, String attribute) {
    Tree<XmlLite.Data> result = null;
    final List<Tree<XmlLite.Data>> nodes = getNodes(node, attribute);
    if (nodes != null && nodes.size() > 0) {
      result = nodes.get(0);
    }
    return result;
  }

  /**
   * Apply this xpath to the given node, extracting the text under each
   * matching node. All text under a matching node is concatenated. Text is trimmed
   * and concatenation adds a single space delimiter between texts.
   * <p>
   * NOTE: empty text nodes will be ignored.
   *
   * @param node  The node to apply this xpath from.
   * @param all   If all, get all (deep) text under the node; otherwise, just
   *              get text immediately under the node.
   *
   * @return null if no nodes match the pattern; empty if nodes match, but they
   *         contain no (non-empty) text; or the strings of texts found under
   *         each matching nodes.
   */
  public List<String> getText(Tree<XmlLite.Data> node, boolean all) {
    return getText(node, all, false);
  }

  /**
   * Apply this xpath to the given node, extracting the text under each
   * matching node. All text under a matching node is concatenated. Text is trimmed
   * and concatenation adds a single space delimiter between texts.
   *
   * @param node  The node to apply this xpath from.
   * @param all   If all, get all (deep) text under the node; otherwise, just
   *              get text immediately under the node.
   * @param includeEmpties If true, empty text nodes will appear with empty strings
   *                       as placeholders; otherwise, empty text nodes will be
   *                       ignored.
   *
   * @return null if no nodes match the pattern; empty if nodes match, but they
   *         contain no (non-empty) text; or the strings of texts found under
   *         each matching nodes.
   */
  public List<String> getText(Tree<XmlLite.Data> node, boolean all, boolean includeEmpties) {
    return getText(getNodes(node), all, includeEmpties);
  }

  private final List<String> getText(List<Tree<XmlLite.Data>> nodes, boolean all, boolean includeEmpties) {
    List<String> result = null;

    if (nodes != null) {
      result = new ArrayList<String>();
      for (Tree<XmlLite.Data> curNode : nodes) {
        final String nodeText = all ? XmlTreeHelper.getAllText(curNode) : XmlTreeHelper.getText(curNode);
        final boolean hasData = (nodeText != null && nodeText.length() > 0);
        if (hasData) {
          result.add(nodeText);
        }
        else if (includeEmpties) {
          // doesn't have data, but we are to include empties.
          result.add("");
        }
      }
    }

    return result;
  }

  /**
   * Convenience method for getting the first matching node's text.
   */
  public String getFirstText(Tree<XmlLite.Data> node) {
    String result = null;

    final List<String> texts = getText(node, false);
    if (texts != null && texts.size() > 0) {
      result = texts.get(0);
    }

    return result;
  }

  /**
   * Apply this xpath to the given node, extracting attribute values from the
   * results matching the given attribute.
   * <p>
   * NOTE: empty text nodes will be ignored.
   *
   * @param node       The node to apply this xpath from.
   * @param attribute  The attribute whose values are to be collected.
   * @param all        In the case the attribute is of the form att=val, the node's
   *                   text instead of attribute text will be returned. This parameter
   *                   specifies whether that should be shallow or deep text.
   *
   * @return null if no nodes match the pattern; empty list if nodes match
   *         and the attribute exists, but is empty; or the attribute values
   *         found in matching nodes.
   */
  public List<String> getText(Tree<XmlLite.Data> node, String attribute, boolean all) {
    return getText(node, attribute, all, false);
  }

  /**
   * Apply this xpath to the given node, extracting attribute values from the
   * results matching the given attribute.
   *
   * @param node       The node to apply this xpath from.
   * @param attribute  The attribute whose values are to be collected.
   * @param all        In the case the attribute is of the form att=val, the node's
   *                   text instead of attribute text will be returned. This parameter
   *                   specifies whether that should be shallow or deep text.
   * @param includeEmpties If true, empty text nodes will appear with empty strings
   *                       as placeholders; otherwise, empty text nodes will be
   *                       ignored.
   *
   * @return null if no nodes match the pattern; empty list if nodes match
   *         and the attribute exists, but is empty; or the attribute values
   *         found in matching nodes.
   */
  public List<String> getText(Tree<XmlLite.Data> node, String attribute, boolean all, boolean includeEmpties) {
    List<String> result = null;

    if (shortNodePath != null) {
      // a node path ending in "**" that is qualified with "attribute" searches for nodes
      // with the attribut under the last matching node in the path, not from among the leaves.
      final List<Tree<XmlLite.Data>> nodes = shortNodePath.apply(node);
      if (nodes != null) {
        for (Tree<XmlLite.Data> curNode : nodes) {
          final TraversalIterator<XmlLite.Data> descendantIter = curNode.iterator(Tree.Traversal.BREADTH_FIRST);
          descendantIter.next(); // throw away "parent" -- we only want to examine its descendands.
          while (descendantIter.hasNext()) {
            final Tree<XmlLite.Data> descendantNode = descendantIter.next();
            final String attributeValue = XmlTreeHelper.getAttribute(descendantNode, attribute);
            if (attributeValue != null) {
              if (result == null) result = new ArrayList<String>();
              if (attributeValue.length() > 0) {
                result.add(attributeValue);
              }
              descendantIter.skip();  // skip searching through this subtree's children.
            }
          }
        } 
      }
    }
    else {
      final List<Tree<XmlLite.Data>> nodes = getNodes(node, attribute);
      if (attribute.indexOf('=') >= 0) {
        // gather text under nodes
        result = getText(nodes, all, includeEmpties);
      }
      else {
        // gather text of attributes
        if (nodes != null) {
          for (Tree<XmlLite.Data> curNode : nodes) {
            final String attributeValue = XmlTreeHelper.getAttribute(curNode, attribute);
            if (attributeValue != null) {
              if (result == null) result = new ArrayList<String>();
              if (attributeValue.length() > 0) {
                result.add(attributeValue);
              }
            }
          }
        }
      }
    }

    return result;
  }

  /**
   * Convenience method for getting the first matching node's attribute value.
   */
  public String getFirstText(Tree<XmlLite.Data> node, String attribute) {
    String result = null;

    final List<String> texts = getText(node, attribute, false);
    if (texts != null && texts.size() > 0) {
      result = texts.get(0);
    }

    return result;
  }

  /**
   * Apply this xpath to the node to find nodes (equivalent to getNodes),
   * but create nodes that would match if they do not exist.
   * <p>
   * Note that if this xpath ends in "**", the "**" is ignored. If this path has
   * wildcards (i.e. "**") in any of its (other) elements, an exception will be
   * thrown.
   *
   * @return the found or created nodes.
   */
  public List<Tree<XmlLite.Data>> findOrCreateNodes(Tree<XmlLite.Data> node) {
    final NodePath<XmlLite.Data> theNodePath = (shortNodePath != null) ? shortNodePath : nodePath;

    List<Tree<XmlLite.Data>> nodes = null;
    int len = theNodePath.length();

    while (len > 0) {
      nodes = theNodePath.apply(node, len);
      if (nodes != null) {
        break;
      }
      else {
        --len;
      }
    }

    if (nodes == null) {
      nodes = new ArrayList<Tree<XmlLite.Data>>();
      nodes.add(node);
    }

    for (int i = len; i < theNodePath.length(); ++i) {
      // create children for each of nodes that will match nodePath.path[i]
      nodes = createChildren(nodes, theNodePath, i);
    }

    return nodes;
  }

  private List<Tree<XmlLite.Data>> createChildren(List<Tree<XmlLite.Data>> nodes, NodePath nodePath, int nodePathIndex) {
    List<Tree<XmlLite.Data>> result = new ArrayList<Tree<XmlLite.Data>>();

    final XmlDataMatcher matcher = (XmlDataMatcher)nodePath.getDataMatcher(nodePathIndex);
    final String tagString = matcher.getTagString();

    if (tagString != null) {
      for (Tree<XmlLite.Data> parentNode : nodes) {
        final boolean commonCase = parentNode.getData().asTag().commonCase;
        final Tree<XmlLite.Data> child = new Tree<XmlLite.Data>(new XmlLite.Tag(tagString, commonCase));
        child.getData().setContainer(child);
        parentNode.addChild(child);
        result.add(child);
      }
    }

    return result;
  }


//java -Xmx640m -classpath `cpgen /home/sbk/co/googlecode/semanticdiscoverytoolkit/modules/xml` org.sd.xml.XPath ~/tmp/seo/2009-10-28/2.html '**.body.**.ol.li{class=g}[0-9]'
//java -Xmx640m -classpath `cpgen /home/sbk/co/googlecode/semanticdiscoverytoolkit/modules/xml` org.sd.xml.XPath ~/tmp/seo/2009-10-28/2.html '**.body.div.div/~\bWeb Wesuwts\b'

  public static void main(String[] args) throws IOException {
    //arg0: xmlFile
    //args1+: xpaths

    final String filename = args[0];
    final File file = new File(filename);
    final Tree<XmlLite.Data> xmlTree = XmlFactory.readXmlTree(file, true, filename.endsWith(".html"), false);

    final XPathHelper xpathHelper =
      new XPathHelper(xmlTree).
      setShowAttributes(true).
//      setShowNonTextLeaves(true).
      addIncludeAttribute("id").
      addIncludeAttribute("class");

    for (int i = 1; i < args.length; ++i) {
      final String pattern = args[i];

      System.out.println("pattern: " + pattern);

      final XPath xpath = new XPath(pattern);
      final List<Tree<XmlLite.Data>> nodes = xpath.getNodes(xmlTree);

      if (nodes == null) {
        System.out.println("\tNo results.");
      }
      else {
        System.out.println("\t" + nodes.size() + " results:");
        int num = 0;
        for (Tree<XmlLite.Data> node : nodes) {
          System.out.println("\nPath #" + (num++) + ":");
          System.out.println(xpathHelper.asString(node));
        }
      }
    }
  }
}
