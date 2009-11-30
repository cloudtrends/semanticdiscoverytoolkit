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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to cache and apply xpaths.
 * <p>
 * @author Spence Koehler
 */
public class XPathApplicator {
  
  private Map<String, XPath> pattern2xpath;

  public XPathApplicator() {
    this.pattern2xpath = new HashMap<String, XPath>();
  }

  /**
   * Get the cached (or create and cache) xpath for the given pattern.
   * <p>
   * If the pattern string includes an attribute, the attribute is ignored.
   */
  public XPath getXPath(String patternString) {
    if (patternString == null) return null;
    final String[] pattern = splitPatternAttribute(patternString);
    return getXPath(pattern);
  }

  /**
   * Get the cached (or create and cache) xpath for the pattern
   * in pattern[0].
   */
  private XPath getXPath(String[] pattern) {
    XPath result = pattern2xpath.get(pattern[0]);
    if (result == null) {
      result = new XPath(pattern[0]);
      pattern2xpath.put(pattern[0], result);
    }
    return result;
  }

  /**
   * Get the nodes that match this xpath pattern string starting from the
   * given node. If the patternString includes an attribute (i.e. is of the
   * form "xpath@attribute"), then only those nodes that have the given
   * attribute (possibly empty) will be included in the result.
   * <p>
   * Note: subscripts are ignored for the first element in the path.
   *
   * @return the matching node(s) or null.
   */
  public List<Tree<XmlLite.Data>> getNodes(String patternString, Tree<XmlLite.Data> node) {
    if (patternString == null) return null;
    final String[] pattern = splitPatternAttribute(patternString);
    final XPath xpath = getXPath(pattern);

    final List<Tree<XmlLite.Data>> result = (pattern.length == 2) ?
      xpath.getNodes(node, pattern[1]) :
      xpath.getNodes(node);

    return result;
  }

  /**
   * Convenience method for getting the first matching node against this xpath.
   * If the patternString includes an attribute (i.e. is of the form
   * "xpath@attribute"), then the first node among those having the given
   * attribute matching the xpath will be returned.
   */
  public Tree<XmlLite.Data> getFirstNode(String patternString, Tree<XmlLite.Data> node) {
    if (patternString == null) return null;
    final String[] pattern = splitPatternAttribute(patternString);
    final XPath xpath = getXPath(pattern);

    final Tree<XmlLite.Data> result = (pattern.length == 2) ?
      xpath.getFirstNode(node, pattern[1]) :
      xpath.getFirstNode(node);

    return result;
  }

  /**
   * Apply this xpath pattern to the given node, extracting the text under each
   * matching node. All text under a matching node is concatenated. Text is trimmed
   * and concatenation adds a single space delimiter between texts. If the
   * patternString includes an attribute (i.e. is of the form "xpath@attribute",
   * then only text from nodes matching the xpath that have the given attribute
   * will be returned.
   * <p>
   * NOTE: empty text nodes will be ignored.
   *
   * @param patternString  The xpath pattern with an optional attribute.
   * @param node           The node to apply this xpath from.
   * @param all            If all, get all (deep) text under the node; otherwise,
   *                       just get text immediately under the node.
   *
   * @return null if no nodes match the pattern; empty if nodes match, but they
   *         contain no (non-empty) text; or the strings of texts found under
   *         each matching nodes.
   */
  public List<String> getText(String patternString, Tree<XmlLite.Data> node, boolean all) {
    return getText(patternString, node, all, false);
  }

  /**
   * Apply this xpath pattern to the given node, extracting the text under each
   * matching node. All text under a matching node is concatenated. Text is trimmed
   * and concatenation adds a single space delimiter between texts. If the
   * patternString includes an attribute (i.e. is of the form "xpath@attribute",
   * then only text from nodes matching the xpath that have the given attribute
   * will be returned.
   *
   * @param patternString  The xpath pattern with an optional attribute.
   * @param node           The node to apply this xpath from.
   * @param all            If all, get all (deep) text under the node; otherwise,
   *                       just get text immediately under the node.
   * @param includeEmpties If true, empty text nodes will appear with empty strings
   *                       as placeholders; otherwise, empty text nodes will be
   *                       ignored.
   *
   * @return null if no nodes match the pattern; empty if nodes match, but they
   *         contain no (non-empty) text; or the strings of texts found under
   *         each matching nodes.
   */
  public List<String> getText(String patternString, Tree<XmlLite.Data> node, boolean all, boolean includeEmpties) {
    if (patternString == null) return null;
    final String[] pattern = splitPatternAttribute(patternString);
    final XPath xpath = getXPath(pattern);

    final List<String> result = (pattern.length == 2) ?
      xpath.getText(node, pattern[1], all, includeEmpties) :
      xpath.getText(node, all, includeEmpties);

    return result;
  }

  /**
   * Get all (deep) text under the node, concatenating using the given delim
   * as a single string.
   *
   * @param patternString the xpath pattern to apply.
   * @param node          the node to which to apply the pattern.
   * @param delim         the delimiter to use when concatenating multiple strings.
   */
  public String getAllText(String patternString, Tree<XmlLite.Data> node, String delim) {
    final StringBuilder result = new StringBuilder();

    final List<String> texts = getText(patternString, node, true, false);
    if (texts != null) {
      for (String text : texts) {
        if (result.length() > 0) result.append(delim);
        result.append(text);
      }
    }

    return result.toString();
  }

  /**
   * Convenience method for getting the first matching node's text. If the
   * patternString includes an attribute (i.e. is of the form "xpath@attribute"),
   * then the text from the first node among those having the given attribute
   * matching the xpath will be returned.
   */
  public String getFirstText(String patternString, Tree<XmlLite.Data> node) {
    if (patternString == null) return null;
    final String[] pattern = splitPatternAttribute(patternString);
    final XPath xpath = getXPath(pattern);

    final String result = (pattern.length == 2) ?
      xpath.getFirstText(node, pattern[1]) :
      xpath.getFirstText(node);

    return result;
  }

  /**
   * Apply this xpath pattern to the given node, extracting attribute values from the
   * results matching the given attribute. If the patternString includes an attribute
   * (i.e. is of the form "xpath@attribute"), then the attribute in the patternString
   * is ignored.
   *
   * @param node       The node to apply this xpath from.
   * @param attribute  The attribute whose values are to be collected.
   *
   * @return null if no nodes match the pattern; empty list if nodes match
   *         and the attribute exists, but is empty; or the attribute values
   *         found in matching nodes.
   */
  public List<String> getText(String patternString, Tree<XmlLite.Data> node, String attribute) {
    if (patternString == null) return null;
    final String[] pattern = splitPatternAttribute(patternString);
    final XPath xpath = getXPath(pattern);
    return xpath.getText(node, attribute, false);
  }

  /**
   * Convenience method for getting the first matching node's attribute value.
   * If the patternString includes an attribute (i.e. is of the form
   * "xpath@attribute"), then the attribute in the patternString is ignored.
   */
  public String getFirstText(String patternString, Tree<XmlLite.Data> node, String attribute) {
    if (patternString == null) return null;
    final String[] pattern = splitPatternAttribute(patternString);
    final XPath xpath = getXPath(pattern);
    return xpath.getFirstText(node, attribute);
  }

  /**
   * Set the text for the nodes found or created through the pattern. If the patternString ends in
   * an attribute, set the attribute's value.
   *
   * @return the found or created nodes.
   */
  public List<Tree<XmlLite.Data>> setText(String patternString, Tree<XmlLite.Data> node, String text, boolean firstOnly) {
    if (patternString == null) return null;
    final String[] pattern = splitPatternAttribute(patternString);
    final XPath xpath = getXPath(pattern);
    final List<Tree<XmlLite.Data>> nodes = xpath.findOrCreateNodes(node);

    if (nodes != null) {
      if (pattern.length == 2) {
        // set attribute
        for (Tree<XmlLite.Data> curNode : nodes) {
          XmlTreeHelper.setAttribute(curNode, pattern[1], text);
          if (firstOnly) break;
        }
      }
      else {
        // set text
        for (Tree<XmlLite.Data> curNode : nodes) {
          XmlTreeHelper.clearText(curNode);
          XmlTreeHelper.addText(curNode, text);
          if (firstOnly) break;
        }
      }
    }

    return nodes;
  }

  /**
   * Split the given patternString such that the xpath is in result[0].
   * If the patternString includes an attribute of the form "xpath@attribute",
   * then return the attribute in result[1].
   */
  public static final String[] splitPatternAttribute(String patternString) {
    String[] result = null;

    if (patternString != null) {
      final int atsetPos = patternString.lastIndexOf('@');
      if (atsetPos >= 0) {
        result = new String[] {
          patternString.substring(0, atsetPos),
          patternString.substring(atsetPos + 1),
        };
      }
      else {
        result = new String[]{patternString};
      }
    }

    return result;
  }

  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println("USAGE:\n\tjava " + XPathApplicator.class.getName() + " <xml filename> <xpath as string>+\n");
      return;
    }

    XPathApplicator x = new XPathApplicator();
    Tree<XmlLite.Data> tree = XmlFactory.readXmlTree(new File(args[0]), true, true, false);
    for (int i = 1; i < args.length; ++i) {
      List<String> matches = x.getText(args[i], tree, true);
      System.out.println("\n" + args[i] + "--(" + matches.size() + ")-->\n" + matches + "\n");
    }
  }
}
