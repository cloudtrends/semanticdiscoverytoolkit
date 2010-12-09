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
package org.sd.atn;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sd.atn.ResourceManager;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.util.StringUtil;
import org.sd.xml.DomNode;
import org.sd.xml.DomElement;
import org.w3c.dom.NodeList;

/**
 * Simple classifier that recognizes capitalized words.
 * <p>
 * @author Spence Koehler
 */
public class CapitalizedWordClassifier extends RoteListClassifier {
  
  private boolean excludeAllCaps;
  private Set<String> stopWords;

  public CapitalizedWordClassifier(DomElement classifierIdElement, ResourceManager resourceManager, Map<String, Normalizer> id2Normalizer) {
    super(classifierIdElement, resourceManager, id2Normalizer);

    // ignore any maxWordCount specified by the element and set to 1
    super.setMaxWordCount(1);

    // iff <excludeAllCaps>true</excludeAllCaps>, then don't treat all caps words as capitalized
    final DomElement eacNode = (DomElement)classifierIdElement.selectSingleNode("excludeAllCaps");
    this.excludeAllCaps = eacNode != null ? "true".equalsIgnoreCase(eacNode.getTextContent()) : false;

    // NOTE: super loads acceptable non-capitalized words  <terms><term>...</term><term>...</term></terms>

    // load stopwords if specified
    final DomElement stopwords = (DomElement)classifierIdElement.selectSingleNode("stopwords");
    if (stopwords != null) {
      this.stopWords = new HashSet<String>();
      final NodeList stopwordNodes = stopwords.getChildNodes();
      if (stopwordNodes != null) {
        for (int nodeNum = 0; nodeNum < stopwordNodes.getLength(); ++nodeNum) {
          final DomNode curNode = (DomNode)stopwordNodes.item(nodeNum);
          if (curNode.getNodeType() != DomNode.ELEMENT_NODE) continue;

          final String curNodeName = curNode.getNodeName();
          final DomElement curElement = (DomElement)curNode;
          if ("textfile".equals(curNodeName)) {
            loadTextFile(curElement, stopWords, null);
          }
          else if ("terms".equals(curNodeName)) {
            loadTerms(curElement, stopWords, null);
          }
        }
      }
    }
  }

  public boolean doClassify(Token token) {
    if (token.getWordCount() > 1) return false;  // just take one word at a time

    final String tokenText = token.getText();

    if (stopWords != null && tokenText.length() > 1 &&
        stopWords.contains(caseSensitive() ? tokenText : tokenText.toLowerCase())) {
      return false;
    }

    boolean result = Character.isUpperCase(tokenText.codePointAt(0));

    if (result && excludeAllCaps && StringUtil.allCaps(tokenText)) {
      result = false;
    }

    if (!result) {
      result = super.doClassify(token);

      // accept a single letter followed by a '.', even if not capitalized.
      if (!result && tokenText.length() == 1) {
        final String postDelim = token.getPostDelim();
        if (postDelim.length() > 0 && postDelim.charAt(0) == '.') {
          result = true;
        }
      }
    }

    if (result) {
      token.setFeature("namePart", "true", this);
    }

    return result;
  }
}
