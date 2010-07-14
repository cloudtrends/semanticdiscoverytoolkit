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
package org.sd.token.plugin;


import java.util.HashSet;
import java.util.Map;
import org.sd.token.AbstractTokenClassifier;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.util.StringUtil;
import org.sd.xml.DomElement;

/**
 * Simple classifier that recognizes capitalized words.
 * <p>
 * @author Spence Koehler
 */
public class CapitalizedWordClassifier extends RoteListClassifier {
  
  private boolean excludeAllCaps;

  public CapitalizedWordClassifier(DomElement classifierIdElement, Map<String, Normalizer> id2Normalizer) {
    super(classifierIdElement, id2Normalizer);

    // ignore any maxWordCount specified by the element and set to 1
    super.setMaxWordCount(1);

    // iff <excludeAllCaps>true</excludeAllCaps>, then don't treat all caps words as capitalized
    final DomElement eacNode = (DomElement)classifierIdElement.selectSingleNode("excludeAllCaps");
    this.excludeAllCaps = eacNode != null ? "true".equalsIgnoreCase(eacNode.getTextContent()) : false;

    // NOTE: super loads acceptable non-capitalized words  <terms><term>...</term><term>...</term></terms>
  }

  public boolean doClassify(Token token) {
    final String tokenText = token.getText();
    boolean result = Character.isUpperCase(tokenText.codePointAt(0));

    if (result && excludeAllCaps && StringUtil.allCaps(tokenText)) {
      result = false;
    }

    if (!result) {
      result = super.doClassify(token);
    }

    if (result) {
      token.setFeature("namePart", "true", this);
    }

    return result;
  }
}
