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
import org.sd.util.Usage;
import org.sd.xml.DomNode;
import org.sd.xml.DomElement;
import org.w3c.dom.NodeList;

/**
 * Simple classifier that recognizes capitalized words.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "Simple org.sd.atn.RoteListClassifier that recognizes capitalized words\n" +
       "with the following options:\n" +
       "  allCapsMinLen -- (default=2) minimum length to be considered as allCaps\n" +
       "  excludeAllCaps -- (default=false) true to reject all-caps words;\n" +
       "                    Note that a single capital letter is not 'all-caps'.\n" +
       "  excludeProperCase -- (default=false) true to reject proper-cased words;\n" +
       "                       Note that a single capital letter is not 'proper-case'.\n" +
       "  singleLetter -- (default=true) true to accept a single capital letter;\n" +
       "                  false to reject single capital letter\n" +
       "  lowerCaseInitial -- (default=true) true to accept a single lower- (or\n" +
       "                      upper-) case letter followed by a '.', regardless\n" +
       "                      of the 'singleLetter' option.\n" +
       "  acceptDash -- (default=true) true to accept a word containing an\n" +
       "                embedded dash\n" +
       "  rejectDigit -- (default=true) true to reject a word containing a digit"
  )
public class CapitalizedWordClassifier extends RoteListClassifier {
  
  private int allCapsMinLen;
  private boolean excludeAllCaps;
  private boolean excludeProperCase;
  private boolean singleLetter;
  private boolean lowerCaseInitial;
  private boolean acceptDash;
  private boolean rejectDigit;

  public CapitalizedWordClassifier(DomElement classifierIdElement, ResourceManager resourceManager, Map<String, Normalizer> id2Normalizer) {
    super(classifierIdElement, resourceManager, id2Normalizer);

    // ignore any maxWordCount specified by the element and set to 1
    getTokenClassifierHelper().setMaxWordCount(1);

    this.allCapsMinLen = 1;
    this.excludeAllCaps = false;
    this.excludeProperCase = false;
    this.singleLetter = true;
    this.lowerCaseInitial = true;
    this.acceptDash = true;
    this.rejectDigit = true;

    doMySupplement(classifierIdElement);
  }

  private final void doMySupplement(DomNode classifierIdElement) {
    this.allCapsMinLen = classifierIdElement.getAttributeInt("allCapsMinLen", 2);

    // iff <excludeAllCaps>true</excludeAllCaps>, then don't treat all caps words as capitalized
    final DomElement eacNode = (DomElement)classifierIdElement.selectSingleNode("excludeAllCaps");
    if (eacNode != null) {
      this.excludeAllCaps = "true".equalsIgnoreCase(eacNode.getTextContent());
    }
    else {
      this.excludeAllCaps = classifierIdElement.getAttributeBoolean("excludeAllCaps", this.excludeAllCaps);
    }

    // iff <excludeProperCase>true</excludeProperCase>, then don't treat all caps words as capitalized
    final DomElement epcNode = (DomElement)classifierIdElement.selectSingleNode("excludeProperCase");
    if (epcNode != null) {
      this.excludeProperCase = "true".equalsIgnoreCase(epcNode.getTextContent());
    }
    else {
      this.excludeProperCase = classifierIdElement.getAttributeBoolean("excludeProperCase", this.excludeProperCase);
    }

    // if singleLetter, then accept a single capital letter
    this.singleLetter = classifierIdElement.getAttributeBoolean("singleLetter", this.singleLetter);

    // attribute to accept 'lowerCaseInitial' (default=true)
    this.lowerCaseInitial = classifierIdElement.getAttributeBoolean("lowerCaseInitial", this.lowerCaseInitial);

    // if acceptDash, then accept a word containing an embedded dash
    this.acceptDash = classifierIdElement.getAttributeBoolean("acceptDash", this.acceptDash);

    // if rejectDigit, then reject a word containing an embedded digit
    this.rejectDigit = classifierIdElement.getAttributeBoolean("rejectDigit", this.rejectDigit);

    // NOTE: super loads acceptable non-capitalized words and stopwords
    super.doSupplement(classifierIdElement);
  }

  /**
   * Supplement this classifier with the given dom node.
   */
  public void supplement(DomNode supplementNode) {
    super.supplement(supplementNode);
    doMySupplement(supplementNode);
  }

  public void setAllCapsMinLen(int allCapsMinLen) {
    this.allCapsMinLen = allCapsMinLen;
  }

  public int getAllCapsMinLen() {
    return allCapsMinLen;
  }

  public void setExcludeAllCaps(boolean excludeAllCaps) {
    this.excludeAllCaps = excludeAllCaps;
  }

  public boolean getExcludeAllCaps() {
    return excludeAllCaps;
  }

  public void setExcludeProperCase(boolean excludeProperCase) {
    this.excludeProperCase = excludeProperCase;
  }

  public boolean getExcludeProperCase() {
    return excludeProperCase;
  }

  public void setSingleLetter(boolean singleLetter) {
    this.singleLetter = singleLetter;
  }

  public boolean getSingleLetter() {
    return singleLetter;
  }

  public void setLowerCaseInitial(boolean lowerCaseInitial) {
    this.lowerCaseInitial = lowerCaseInitial;
  }

  public boolean getLowerCaseInitial() {
    return lowerCaseInitial;
  }

  public void setAcceptDash(boolean acceptDash) {
    this.acceptDash = acceptDash;
  }

  public boolean getAcceptDash() {
    return acceptDash;
  }

  public void setRejectDigit(boolean rejectDigit) {
    this.rejectDigit = rejectDigit;
  }

  public boolean getRejectDigit() {
    return rejectDigit;
  }

  public boolean doClassify(Token token, AtnState atnState) {
    if (token.getWordCount() > 1) return false;  // just take one word at a time

    final String tokenText = token.getText();
    final int len = tokenText.length();

    // check lookups first so attributes get added
    final boolean isStopword = super.doClassifyStopword(token, atnState);

    if (isStopword) return false;

    boolean result = super.doClassifyTerm(token, atnState);

    if (!result) {
      // check capital first letter
      result = Character.isUpperCase(tokenText.codePointAt(0));

      // check exclude all caps
      if (result && excludeAllCaps && len >= allCapsMinLen && StringUtil.allCaps(tokenText)) {
        result = false;
      }

      // check exclude proper case
      if (result && excludeProperCase && len >= 2) {
        // e.g., check "m" if looking at "Smith" -vs- "SMITH"
        char c = tokenText.charAt(1);

        if (c == '\'' && len >= 4) {
          // e.g., check "a" if looking at something like "O'Malley" -vs- "O'MALLEY"
          c = tokenText.charAt(3);
        }

        if (Character.isLowerCase(tokenText.charAt(1))) {
          // is propercase; to be excluded
          result = false;
        }
      }

      // check singleLetter
      if (result && !singleLetter && len == 1) {
        result = false;
      }
    }

    // check lowerCaseInitial:
    //   accept a single letter followed by a '.', even if not capitalized.
    if (!result && lowerCaseInitial && len == 1) {
      final String postDelim = token.getPostDelim();
      if (postDelim.length() > 0 && postDelim.charAt(0) == '.') {
        result = true;
      }
    }

    // check for embedded dash
    if (result && len > 1 && !acceptDash) {
      result = (tokenText.indexOf('-') < 0);
    }

    // check for embedded digit
    if (result && len > 1 && rejectDigit) {
      for (int i = 1; i < len; ++i) {
        if (Character.isDigit(tokenText.charAt(i))) {
          result = false;
          break;
        }
      }
    }

    if (result) {
      token.setFeature("namePart", "true", this);
    }

    return result;
  }
}
