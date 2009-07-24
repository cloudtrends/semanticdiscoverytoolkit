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
package org.sd.extract;


import org.sd.nlp.SentenceSplitter;
import org.sd.text.PatternFinder;
import org.sd.text.TermFinder;
import org.sd.text.lucene.SdQuery;
import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Container for a portion (or "bite") of a post.
 * <p>
 * @author Spence Koehler
 */
public class Bite {
  
  private static final Set<String> BLOCK_ELEMENTS = new HashSet<String>();
  static {
    BLOCK_ELEMENTS.add("div");
//    BLOCK_ELEMENTS.add("p");
    BLOCK_ELEMENTS.add("table");
    BLOCK_ELEMENTS.add("ul");
    BLOCK_ELEMENTS.add("ol");
    BLOCK_ELEMENTS.add("dl");
    BLOCK_ELEMENTS.add("menu");
    BLOCK_ELEMENTS.add("dir");
    BLOCK_ELEMENTS.add("hr");
    BLOCK_ELEMENTS.add("form");
    BLOCK_ELEMENTS.add("address");
    BLOCK_ELEMENTS.add("pre");
    BLOCK_ELEMENTS.add("center");
    BLOCK_ELEMENTS.add("blockquote");
  }

  /**
   * Factory method to build bites from an xml tree.
   */
  public static final List<Bite> buildBites(Tree<XmlLite.Data> xmlNode) {
    final List<Bite> result = new ArrayList<Bite>();
    final List<Tree<XmlLite.Data>> biteNodes = XmlTreeHelper.getTextBiteNodes(xmlNode);

    //
    // further divide the "bites" as demarcated by "block elements" in their tag path.
    //

    int biteIndex = 0;
    for (Tree<XmlLite.Data> biteNode : biteNodes) {
      final List<Tree<XmlLite.Data>> deepNodes = XmlTreeHelper.getDeepestNodes(biteNode, BLOCK_ELEMENTS);

      for (Tree<XmlLite.Data> deepNode : deepNodes) {
        if (deepNode.getData().asText() != null) {
          result.add(new Bite(XmlTreeHelper.getHighestContainingNode(deepNode), biteIndex++));
        }
        else {
          result.add(new Bite(deepNode, biteIndex++));
        }
      }
    }

    return result;
  }

  /**
   * Strategy for extracting indexable content from a list of bites.
   */
  public static final String getIndexableContent(List<Bite> bites) {
    final StringBuilder result = new StringBuilder();

    for (Bite bite : bites) {
      if (bite.isFooter()) break;  // all done
      if (bite.isBody()) {
        if (result.length() > 0) result.append('\n');
        result.append(bite.getBiteText());
      }
    }

    return result.toString();
  }

  private static final String[] NON_BODY_CLASSES = new String[] {
    // by-line classes
    "byline",
    "posted",

    // tag classes
    "ljtags",
    "categor",  // category, categories

    // mood classes
    "mood",
    "feeling",

    // other non-body classes
    "meta",
    "footer",
    "header",
    "comment",
    "date",
    "timestamp",
  };

  private static final TermFinder NON_BODY_CLASS_FINDER = new TermFinder("Non-Body Div", false, NON_BODY_CLASSES);
  private static final SentenceSplitter SENTENCE_SPLITTER = new SentenceSplitter();


  private Tree<XmlLite.Data> biteNode;
  private int biteIndex;
  private Boolean isBody;
  private Boolean isFooter;

  private String _biteText;
  private String[] _sentences;
  private List<Tree<XmlLite.Data>> _textNodes;

  public Bite(Tree<XmlLite.Data> biteNode, int biteIndex) {
    this.biteNode = biteNode;
    this.biteIndex = biteIndex;
  }

  public Tree<XmlLite.Data> getBiteNode() {
    return biteNode;
  }

  public int getBiteIndex() {
    return biteIndex;
  }

  public String getBiteText() {
    if (_biteText == null) {
      _biteText = XmlTreeHelper.getAllText(biteNode);
    }
    return _biteText;
  }

  public String[] getSentences() {
    if (_sentences == null) {
      _sentences = SENTENCE_SPLITTER.split(getBiteText());
    }
    return _sentences;
  }

  public void setIsBody(boolean isBody) {
    this.isBody = isBody;
  }

  public boolean isBody() {
    if (isBody == null) {
      // this currently only identifies snippet date/time
      boolean hasNonBodyExtraction = (getNodesWithPropertyOrAttribute(ExtractionProperties.NON_BODY_EXTRACTION, ExtractionProperties.DATE_TIME_EXTRACTION_ATTRIBUTE) != null);

      // identify other non-body pieces
      if (!hasNonBodyExtraction) {
        // identify non-body div classes
        hasNonBodyExtraction = classifyAsNonBody();
      }

      this.isBody = !hasNonBodyExtraction;
    }
    return isBody;
  }

  public boolean isFooter() {
    if (isFooter == null) {
      isFooter = classifyAsFooter();
    }
    return isFooter;
  }


  private final boolean classifyAsFooter() {
    boolean result = false;

    if (biteIndex > 0) {
      // test for hyperlinked "number <text>" or "text(number)" (and not too much non-linked bulk)
      result = hasHyperlinkedNumberText();

      //todo: employ other strategies for finding footer
      //if (!result) {...}
    }

    return result;
  }

  private final boolean classifyAsNonBody() {
    return NON_BODY_CLASS_FINDER.hasPattern(getDivClasses(), PatternFinder.ACCEPT_PARTIAL);
  }

  private final boolean hasHyperlinkedNumberText() {
    boolean result = false;

//    int lcCount = 0;   // num hyperlinked chars
    int nlcCount = 0;  // num non-hyperlinked chars

    final List<Tree<XmlLite.Data>> textNodes = getTextNodes();
    
    if (textNodes.size() < 15) {  // can't be 15 text nodes or more
      for (Tree<XmlLite.Data> textNode : textNodes) {
        final String text = textNode.getData().asText().text;

        if (isHyperlinked(textNode)) {
          if (result = isNumberText(text)) break;
          if (result = isTextNumber(text)) break;
//          lcCount += text.length();
        }
        else {
          nlcCount += text.length();
        }

        if (nlcCount > 40) break;  // kick out if scanned more than 40 non-linked chars
      }
    }

    return result;
  }

  private final boolean isHyperlinked(Tree<XmlLite.Data> textNode) {
    final Tree<XmlLite.Data> parentNode = textNode.getParent();
    return (parentNode != null && "a".equals(parentNode.getData().asTag().name));
  }

  private final String getDivClass() {
    String result = XmlTreeHelper.getAttribute(biteNode, "class");
    if (result != null) {
      result = result.toLowerCase();
    }
    return result;
  }

  private final String getDivClasses() {
    final StringBuilder result = new StringBuilder();

    Tree<XmlLite.Data> curNode = biteNode;
    while (curNode != null) {
      String divClass = XmlTreeHelper.getAttribute(biteNode, "class");
      if (divClass != null) {
        if (result.length() > 0) result.append(' ');
        result.append(divClass.toLowerCase());
      }
      curNode = curNode.getParent();
    }

    return result.toString();
  }

  protected static final boolean isNumberText(String text) {
    // digits-space-letters and spaces  no symbols
    boolean result = false;

    final int len = text.length();
    int index = isDigits(text, 0, len);
    if (index > 0 && index < len) {
      index = isChars(text, index, len);
      result = (index == len);
    }

    return result;
  }

  protected static final boolean isTextNumber(String text) {
    // letters followed by '(' digits ')'
    boolean result = false;

    final int len = text.length();
    if (text.charAt(len - 1) == ')') {
      int index = isChars(text, 0, len);
      if (index > 0 && index < len && text.charAt(index) == '(') {
        index = isDigits(text, index + 1, len);
        result = (index == len - 1);
      }
    }

    return result;
  }

  /**
   * Spin over consecutive digits, returning the index after or startIndex if
   * the char at startIndex is not a digit.
   */
  private static final int isDigits(String text, int startIndex, int endIndex) {
    while (startIndex < endIndex && isDigit(text.charAt(startIndex))) ++startIndex;
    return startIndex;
  }

  /**
   * Spin over consecutive chars and white, returning the index after or
   * startIndex if the char at startIndex is not a digit.
   */
  private static final int isChars(String text, int startIndex, int endIndex) {
    while (startIndex < endIndex && isChar(text.charAt(startIndex))) ++startIndex;
    return startIndex;
  }

  private static final boolean isDigit(char c) {
    return (c <= '9' && c >= '0');
  }

  private static final boolean isChar(char c) {
    return (c <= 'z' && c >= 'A' && (c <= 'Z' || c >= 'a')) || c == ' ';
  }


  public List<Tree<XmlLite.Data>> getNodesWithPropertyOrAttribute(String property, String attribute) {
    return XmlTreeHelper.getNodesWithPropertyOrAttribute(biteNode, property, attribute);
  }

  public List<Tree<XmlLite.Data>> getNodesWithTags(Set<String> tags) {
    return XmlTreeHelper.findTags(biteNode, tags, true);
  }

  public List<Tree<XmlLite.Data>> getTextNodes() {
    if (_textNodes == null) {
      _textNodes = XmlTreeHelper.getTextNodes(biteNode);
    }
    return _textNodes;
  }


  public String toString() {
    return buildString(null);
  }

  public String buildString(SdQuery sdQuery) {
    final StringBuilder result = new StringBuilder();

    result.
      append(isFooter() ? " (F) " : isBody() ? " (B) " : "     ").
      append(biteIndex).append(": ");

    if (isBody()) {
      final String[] sentences = getSentences();
      for (int i = 0; i < sentences.length; ++i) {
        result.append(i).append(": ").append(buildString(sentences[i], sdQuery));
        if (i + 1 < sentences.length) {
          result.append("\n        ");
        }
      }
    }
    else {
      result.append(buildString(getBiteText(), sdQuery));
    }

    return result.toString();
  }

  private final String buildString(String string, SdQuery sdQuery) {
    return sdQuery == null ? string : sdQuery.highlightString(string);
  }
}
