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
package org.sd.xml.xel;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TraversalIterator;
import org.sd.xml.XmlTreeHelper;
import org.sd.xml.XPathApplicator;
import org.sd.xml.XPathHelper;
import org.sd.xml.XmlLite;

/**
 * Class for extracting nodes and text through XPath keys.
 * <p>
 * @author Spence Koehler
 */
public class XPathExtractor implements XelExtraction {

  private XPathHelper xpathHelper;
  private XPathApplicator xpathApplicator;

  // extractor data
  private Tree<XmlLite.Data> rootNode;
  private List<Tree<XmlLite.Data>> excludeNodes;

  // extraction data
  private String xpathKey;
  private List<Tree<XmlLite.Data>> nodes;
  private XPathExtractor parent;

  /**
   * Construct a new extractor to work over the given tree.
   */
  public XPathExtractor(Tree<XmlLite.Data> xmlTree) {
    this(new XPathHelper(xmlTree), new XPathApplicator(), xmlTree, null, null, null, null);
  }

  /**
   * Make an extraction instance.
   */
  private XPathExtractor(XPathHelper xpathHelper, XPathApplicator xpathApplicator, Tree<XmlLite.Data> rootNode,
                         List<Tree<XmlLite.Data>> excludeNodes, String xpathKey, List<Tree<XmlLite.Data>> nodes,
                         XPathExtractor parent) {
    this.xpathHelper = xpathHelper;
    this.xpathApplicator = xpathApplicator;
    this.rootNode = rootNode;
    this.excludeNodes = excludeNodes;
    this.xpathKey = xpathKey;
    this.nodes = nodes;
    this.parent = parent;
  }

  /**
   * Get this extractor's root node.
   */
  public Tree<XmlLite.Data> getRootNode() {
    return rootNode;
  }

  /**
   * Perform extraction according to the given xpath as a key.
   */
  public XelExtraction extract(String xpathKey) {
    XelExtraction result = null;

    if (nodes != null) {
      result = extractFromNodes(xpathKey);
    }
    else {
      result = extractFromRoot(xpathKey);
    }

    return result;
  }

  /**
   * Exclude nodes in this extractor identified by the exclusionary xpath
   * relative to this extractor's root node.
   * <p>
   * Note that excluded nodes and their children will not be selected for
   * bindings or for text or attribute extraction.
   * <p>
   * Note that this overrides any prior excluded node value.
   *
   * @return true if nodes were found by the pattern to exclude; otherwise, false.
   */
  public boolean setExcludes(String excludeXPath, boolean append) {

    final List<Tree<XmlLite.Data>> curExcludes = xpathApplicator.getNodes(excludeXPath, rootNode);

    if (curExcludes != null) {
      if (append && this.excludeNodes != null) {
        this.excludeNodes.addAll(curExcludes);
      }
      else {
        this.excludeNodes = curExcludes;
      }
    }

    return curExcludes != null;
  }

  /**
   * Get the excluded nodes.
   */
  public List<Tree<XmlLite.Data>> getExcludes() {
    return excludeNodes;
  }

  /**
   * A node is excluded if it is an excluded node or if an excluded node is its parent.
   */
  public boolean isExcluded(Tree<XmlLite.Data> node, boolean inherit) {
    boolean result = false;

    if (excludeNodes != null) {
      for (Tree<XmlLite.Data> excludeNode : excludeNodes) {
        if (node == excludeNode || (inherit && node.isDescendant(excludeNode))) {
          result = true;
          break;
        }
      }
    }

    return result;
  }
  
  /**
   * Get the key that identifies this extraction.
   */
  public String getKey() {
    return xpathKey;
  }

  /**
   * Get this instance's extracted nodes (ignoring local exclusions).
   */
  public List<Tree<XmlLite.Data>> getNodes() {
    return nodes;
  }

  /**
   * Get the attribute from the (non-excluded) extracted nodes.
   *
   * @return null if there are no extracted nodes; an empty list if there are
   *         extracted nodes, but none have the attribute; or the list of values
   *         for existing attributes within extracted nodes.
   */
  public List<String> getAttribute(String attribute) {
    List<String> result = null;

    if (nodes != null) {
      result = new ArrayList<String>();
      for (Tree<XmlLite.Data> node : nodes) {
        if (!isExcluded(node, false)) {
          final String attributeValue = XmlTreeHelper.getAttribute(node, attribute);
          if (attributeValue != null) {
            result.add(attributeValue);
          }
        }
      }
    }
    else if (rootNode != null) {
      final String attributeValue = XmlTreeHelper.getAttribute(rootNode, attribute);
      if (attributeValue != null && !"".equals(attributeValue)) {
        result = new ArrayList<String>();
        result.add(attributeValue);
      }
    }

    return result;
  }

  /**
   * Get the text from the (non-excluded) extracted nodes.
   */
  public List<String> getText() {
    List<String> result = null;

    if (nodes != null) {
      result = new ArrayList<String>();
      for (Tree<XmlLite.Data> node : nodes) {
        if (!isExcluded(node, false)) {
          final String text = getText(node);
          result.add(text);
        }
      }
    }
    else if (rootNode != null) {
      final String text = getText(rootNode);
      if (text != null && !"".equals(text)) {
        result = new ArrayList<String>();
        result.add(text);
      }
    }

    return result;
  }

  /**
   * Get text resulting from applying the given xpath. If the xpath identifies
   * an attribute, get the attribute's value(s) as the text; otherwise, get
   * the text under the identified node (with respect to this instance's root
   * node.)
   *
   * @return the xpath-identified text or null if the xpath failed to select
   *         any nodes.
   */
  public List<String> getText(String xpath) {
    List<String> result = null;

    final List<Tree<XmlLite.Data>> selectedNodes = xpathApplicator.getNodes(xpath, rootNode);
    if (selectedNodes != null) {
      result = new ArrayList<String>();
      final String attribute = getXPathAttribute(xpath);

      for (Tree<XmlLite.Data> selectedNode : selectedNodes) {
        if (!isExcluded(selectedNode, true)) {
          final String selectedText = getText(selectedNode, attribute);
          if (selectedText != null) {
            result.add(selectedText);
          }
        }
      }
    }

    return result;
  }

  /**
   * Get text resulting from applying the given xpaths, associating results
   * with the same labels. For example, for each label=foo, xpath=bar, the
   * result will hold key=foo, value=getText(bar).
   */
  public Map<String, List<String>> getText(Map<String, String> label2xpath) {
    Map<String, List<String>> result = null;

    if (label2xpath != null) {
      for (Map.Entry<String, String> entry : label2xpath.entrySet()) {
        final String label = entry.getKey();
        final String xpath = entry.getValue();

        final List<String> text = getText(xpath);
        if (text != null) {
          if (result == null) result = new HashMap<String, List<String>>();
          result.put(label, text);
        }
      }
    }

    return result;
  }

  /**
   * Get the extractor that produced this extraction.
   */
  public XelExtractor getExtractor() {
    return parent;
  }

  /**
   * Get this instance's extracted nodes as extractors.
   */
  public List<XelExtractor> asExtractors() {
    List<XelExtractor> result = null;

    if (nodes != null) {
      result = new ArrayList<XelExtractor>();
      for (Tree<XmlLite.Data> node : nodes) {
        result.add(new XPathExtractor(xpathHelper, xpathApplicator, node,
                                      excludeNodes == null ? null : new ArrayList<Tree<XmlLite.Data>>(excludeNodes), // copy
                                      null, null, this));
      }
    }

    return result;
  }

  /**
   * Extract from the rootNode.
   */
  private final XelExtraction extractFromRoot(String xpathKey) {
    XPathExtractor result = null;

    final List<Tree<XmlLite.Data>> resultNodes = doExtract(xpathKey, rootNode, null);
    if (resultNodes != null) {
      result = new XPathExtractor(xpathHelper, xpathApplicator, rootNode,
                                  excludeNodes == null ? null : new ArrayList<Tree<XmlLite.Data>>(excludeNodes),  // copy
                                  xpathKey, resultNodes, this);
    }

    return result;
  }

  /**
   * Extract from the nodes.
   */
  private final XelExtraction extractFromNodes(String xpathKey) {
    XPathExtractor result = null;
    List<Tree<XmlLite.Data>> resultNodes = null;
    
    for (Tree<XmlLite.Data> node : nodes) {
      resultNodes = doExtract(xpathKey, node, resultNodes);
    }

    if (resultNodes != null) {
      result = new XPathExtractor(xpathHelper, xpathApplicator, rootNode,
                                  excludeNodes == null ? null : new ArrayList<Tree<XmlLite.Data>>(excludeNodes),  // copy
                                  xpathKey, resultNodes, this);
    }

    return result;
  }

  /**
   * Do the extraction, adding to the result.
   */
  private final List<Tree<XmlLite.Data>> doExtract(String xpathKey, Tree<XmlLite.Data> node, List<Tree<XmlLite.Data>> resultNodes) {

    final List<Tree<XmlLite.Data>> selectedNodes = xpathApplicator.getNodes(xpathKey, node);

    if (selectedNodes != null) {
      if (resultNodes == null) resultNodes = new ArrayList<Tree<XmlLite.Data>>();
      for (Tree<XmlLite.Data> selectedNode : selectedNodes) {
        if (!isExcluded(selectedNode, true)) {
          resultNodes.add(selectedNode);
        }
      }
    }    

    return resultNodes;
  }

  /**
   * Get this extractor's node's non-excluded text.
   */
  private final String getText(Tree<XmlLite.Data> node) {
    final StringBuilder result = new StringBuilder();

    for (TraversalIterator<XmlLite.Data> iter = node.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      final Tree<XmlLite.Data> curNode = iter.next();
      if (!isExcluded(curNode, false)) {
        final XmlLite.Text nodeText = curNode.getData().asText();
        if (nodeText != null && nodeText.text != null && nodeText.text.length() > 0) {
          if (result.length() > 0) result.append(' ');
          result.append(nodeText.text);
        }
      }
      else {
        iter.skip();
      }
    }

    return result.toString();
  }

  /**
   * Get the attribute (if any) identified by the xpath, or null.
   */
  private final String getXPathAttribute(String xpath) {
    final String[] pieces = XPathApplicator.splitPatternAttribute(xpath);
    return pieces.length == 2 ? pieces[1] : null;
  }

  /**
   * Get the (non-excluded) text under the node or from the node's
   * attribute (if non-null).
   */
  private final String getText(Tree<XmlLite.Data> node, String attribute) {
    String result = null;

    if (attribute == null) {
      result = getText(node);
    }
    else {
      result = XmlTreeHelper.getAttribute(node, attribute);
    }

    return result;
  }
}
