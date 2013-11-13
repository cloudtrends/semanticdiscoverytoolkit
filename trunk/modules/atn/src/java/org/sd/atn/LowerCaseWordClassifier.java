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


import java.util.Map;
import org.sd.atn.ResourceManager;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.xml.DomNode;
import org.sd.xml.DomElement;

/**
 * Simple classifier that recognizes lowercase words.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "Simple org.sd.atn.RoteListClassifier that recognizes lowercase words\n" +
       "with the following options:\n" +
       "  excludeMixedCase -- (default=false) true to reject mixed-cased words.\n" +
       "  acceptDash -- (default=true) true to accept a word containing an\n" +
       "                embedded dash\n" +
       "  rejectDigit -- (default=true) true to reject a word containing a digit"
  )
public class LowerCaseWordClassifier extends RoteListClassifier {
  
  private boolean excludeMixedCase;

  private boolean lowerCaseInitial;
  private boolean acceptDash;
  private boolean rejectDigit;

  public LowerCaseWordClassifier(DomElement classifierIdElement, ResourceManager resourceManager, Map<String, Normalizer> id2Normalizer) {
    super(classifierIdElement, resourceManager, id2Normalizer);

    // ignore any maxWordCount specified by the element and set to 1
    getTokenClassifierHelper().setMaxWordCount(1);

    this.excludeMixedCase = false;
    this.acceptDash = true;
    this.rejectDigit = true;

    doMySupplement(classifierIdElement);
  }

  private final void doMySupplement(DomNode classifierIdElement) {

    // iff <excludeMixedCase>true</excludeMixedCase>, then don't treat mixed case words as lowercased
    final DomElement emcNode = (DomElement)classifierIdElement.selectSingleNode("excludeMixedCase");
    if (emcNode != null) {
      this.excludeMixedCase = "true".equalsIgnoreCase(emcNode.getTextContent());
    }
    else {
      this.excludeMixedCase = classifierIdElement.getAttributeBoolean("excludeMixedCase", this.excludeMixedCase);
    }

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

  public void setExcludeMixedCase(boolean excludeMixedCase) {
    this.excludeMixedCase = excludeMixedCase;
  }

  public boolean getExcludeMixedCase() {
    return excludeMixedCase;
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
      // check lower first letter
      result = Character.isLowerCase(tokenText.codePointAt(0));

      // check exclude mixed case, accept dash, reject digit
      if (result && (excludeMixedCase || !acceptDash || rejectDigit)) {
        for (int i = 1; i < len; ++i) {
          final int c = tokenText.codePointAt(i);
          if (c == '-') {
            if (!acceptDash) {
              result = false;
              break;
            }
          }
          else if (Character.isDigit(c)) {
            if (rejectDigit) {
              result = false;
              break;
            }
          }
          else if (Character.isUpperCase(c)) {
            if (excludeMixedCase) {
              result = false;
              break;
            }
          }
        }
      }
    }

    if (result) {
      token.setFeature("lowercase", "true", this);
    }

    return result;
  }
}
