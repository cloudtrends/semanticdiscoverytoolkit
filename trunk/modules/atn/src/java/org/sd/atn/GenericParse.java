/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.atn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sd.util.tree.NodePath;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

/**
 * Container for a single parse as accessed through a GenericParseHelper.
 * <p>
 * @author Spence Koehler
 */
public class GenericParse {
  
  private AtnParse atnParse;
  private ParseInterpretation interp;
  private GenericParseHelper genericParseHelper;
  private Map<String, String> id2text;
  private Map<String, String> xpath2text;

  private String _ruleId;
  private String _parsedText;
  private String _inputText;
  private String _remainingText;

  GenericParse(ParseInterpretation interp, GenericParseHelper genericParseHelper) {
    this.atnParse = interp.getSourceParse();
    this.interp = interp;
    this.genericParseHelper = genericParseHelper;
    this.id2text = null;
    this.xpath2text = null;

    this._ruleId = null;
    this._parsedText = null;
    this._inputText = null;
    this._remainingText = null;
  }

  public AtnParse getAtnParse() {
    return atnParse;
  }

  public ParseInterpretation getInterp() {
    return interp;
  }

  public String getRuleId() {
    if (_ruleId == null) {
      _ruleId = atnParse.getStartRule().getRuleId();
      if (_ruleId == null) _ruleId = "";
    }
    return _ruleId;
  }

  public String getParsedText() {
    if (_parsedText == null) {
      _parsedText = atnParse.getParsedText();
    }
    return _parsedText;
  }

  public String getRemainingText() {
    if (_remainingText == null) {
      _remainingText = atnParse.getRemainingText();
    }
    return _remainingText;
  }

  public boolean hasRemainingText() {
    final String remainingText = getRemainingText();
    return remainingText != null && !"".equals(remainingText);
  }

  /** @return [start, end) of parsed text. */
  public int[] getParsedTextPosition() {
    return new int[] {
      atnParse.getStartIndex(),
      atnParse.getEndIndex(),
    };
  }

  /**
   * Get the full input text submitted for parsing.
   */
  public String getInputText() {
    return atnParse.getFullText();
  }

  /**
   * Find the parse tree nodes indicated by the identifier.
   * <p>
   * Note that the identifier is turned into a nodePath with "**."
   * automatically prepended.
   *
   * @return the found nodes or null if no such nodes exist.
   */
  public List<Tree<String>> findParseNodes(String constituentIdentifier) {
    final NodePath<String> nodePath = genericParseHelper.getNodePath(constituentIdentifier);
    final List<Tree<String>> nodes = nodePath.apply(atnParse.getParseTree());
    return nodes;
  }

  /**
   * Get the parsed text indicated by the identifier.
   * <p>
   * Note that the identifier is turned into a nodePath with "**."
   * automatically prepended.
   * <p>
   * Text from all identified nodes is concatenated with a space between.
   * <p>
   * Text from all identified nodes is concatenated with delimiters
   * included between consecutive nodes. Non-white delimiters immediately
   * following the last node are added iff any non-white delimiter
   * was present between consecutive nodes.
   * <p>
   * If no text is selected, the result will be the empty string.
   */
  public String getParsedText(String constituentIdentifier) {
    String result = null;

    if (id2text != null) {
      result = id2text.get(constituentIdentifier);
    }

    if (result == null) {
      result = buildParsedText(constituentIdentifier);
      if (id2text == null) id2text = new HashMap<String, String>();
      id2text.put(constituentIdentifier, result);
    }

    return result;
  }

  private final String buildParsedText(String constituentIdentifier) {
    final StringBuilder result = new StringBuilder();

    final List<Tree<String>> nodes = findParseNodes(constituentIdentifier);
    if (nodes != null && nodes.size() > 0) {
      for (Tree<String> node : nodes) {
        if (result.length() > 0) result.append(' ');
        result.append(node.getLeafText());
      }
    }

    return result.toString();
  }

  /**
   * Find the interp tree nodes indicated by the xpath.
   * <p>
   * Note that the xpath is applied to the xml interpretation tree.
   *
   * @return the found nodes or null if no such nodes exist.
   */
  public List<Tree<XmlLite.Data>> findInterpNodes(String xpath) {
    final Tree<XmlLite.Data> interpTree = interp.getInterpTree();
    final List<Tree<XmlLite.Data>> nodes = genericParseHelper.getNodes(xpath, interpTree);
    return nodes;
  }

  /**
   * Get the parsed text indicated by the xpath.
   * <p>
   * Note that the xpath is applied to the xml interpretation tree.
   * <p>
   * Text from all identified nodes is concatenated with delimiters
   * included between consecutive nodes. Non-white delimiters immediately
   * following the last node are added iff any non-white delimiter
   * was present between consecutive nodes.
   * <p>
   * If no text is selected, the result will be the empty string.
   */
  public String getInterpText(String xpath) {
    String result = null;

    if (xpath2text != null) {
      result = xpath2text.get(xpath);
    }

    if (result == null) {
      result = buildInterpText(xpath);
      if (xpath2text == null) xpath2text = new HashMap<String, String>();
      xpath2text.put(xpath, result);
    }

    return result;
  }

  private final String buildInterpText(String xpath) {
    final StringBuilder result = new StringBuilder();

    final List<Tree<XmlLite.Data>> nodes = findInterpNodes(xpath);

    if (nodes != null && nodes.size() > 0) {
      for (Tree<XmlLite.Data> node : nodes) {
        ParseInterpretationUtil.getInterpText(result, node);
      }
    }

    return result.toString();
  }

  /**
   * Get the first interpretation attribute (named attName) from the given
   * named node (or from any node if nodeName == null).
   */
  public String getInterpAttribute(String nodeName, String attName) {
    return ParseInterpretationUtil.getInterpAttribute(interp.getInterpTree(), nodeName, attName);
  }

  /**
   * Get all interpretation attributes (named attName) from the given
   * named node(s) (or from any node if nodeName == null).
   */
  public List<String> getInterpAttributes(String nodeName, String attName) {
    return ParseInterpretationUtil.getInterpAttributes(interp.getInterpTree(), nodeName, attName);
  }

  /**
   * Get all interpretation attributes (named attName) from the given
   * named node(s) (or from any node if nodeName == null) starting with
   * (only at or below) the interp node selected by the given xpath.
   */
  public List<String> getInterpAttributes(String xpath, String nodeName, String attName) {
    List<String> result = null;

    final List<Tree<XmlLite.Data>> interpNodes = findInterpNodes(xpath);
    if (interpNodes != null) {
      for (Tree<XmlLite.Data> interpNode : interpNodes) {
        final List<String> atts = ParseInterpretationUtil.getInterpAttributes(interpNode, nodeName, attName);
        if (atts != null && atts.size() > 0) {
          if (result == null) result = new ArrayList<String>();
          result.addAll(atts);
        }
      }
    }

    return result;
  }
}
