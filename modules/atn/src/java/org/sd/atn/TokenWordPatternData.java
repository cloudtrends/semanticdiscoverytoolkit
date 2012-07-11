/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.util.List;
import org.sd.token.TokenWordPattern;
import org.sd.token.Tokenizer;
import org.sd.util.Usage;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Auxiliary class for managing TokenWordPattern data.
 * <p>
 * @author Spence Koehler
 */
public class TokenWordPatternData {

  //    <wordPattern keyLabels="" labelChars="" squashFlags="">
  //      <regexes>
  //        <regex type='find'>...</regex>
  //      </regexes>
  //    </wordPattern>

  private boolean verbose;
  private String squashFlags;
  private TokenWordPattern.PatternKey patternKey;
  private RegexDataContainer regexes;

  public TokenWordPatternData(DomNode domNode) {
    final DomElement domElement = (DomElement)domNode;

    this.verbose = domElement.getAttributeBoolean("verbose", false);
    this.squashFlags = domElement.getAttributeValue("squashFlags", null);
    this.patternKey = null;
    this.regexes = null;

    final String keyLabels = domElement.getAttributeValue("keyLabels", null);
    final String labelChars = domElement.getAttributeValue("labelChars", null);
    if (keyLabels != null && labelChars != null) {
      this.patternKey = new TokenWordPattern.PatternKey(keyLabels, labelChars);
    }

    final NodeList childNodes = domElement.getChildNodes();
    final int numChildNodes = childNodes.getLength();
    for (int childNodeNum = 0; childNodeNum < numChildNodes; ++childNodeNum) {
      final Node childNode = childNodes.item(childNodeNum);
      if (childNode.getNodeType() != Node.ELEMENT_NODE) continue;

      final DomElement childElement = (DomElement)childNode;
      final String childNodeName = childNode.getLocalName();

      if ("regexes".equalsIgnoreCase(childNodeName)) {
        this.regexes = new RegexDataContainer(childElement);
      }
    }
  }
  
  public boolean prequalify(Tokenizer tokenizer) {
    boolean result = false;

    String text = null;

    if (regexes != null) {
      final TokenWordPattern wordPattern = new TokenWordPattern(tokenizer, patternKey);
      text = wordPattern.getPattern(squashFlags);

      if (regexes.matches(text) != null) {
        result = true;
      }
    }

    if (verbose && !result) {
      System.out.println("TokenWordPatternData disallowing '" + tokenizer.getText() + "'/" + text);
    }

    return result;
  }
}
