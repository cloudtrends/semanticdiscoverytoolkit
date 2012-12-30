/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.util.HashMap;
import java.util.Map;
import org.sd.token.TokenClassifierHelper;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;

/**
 * Abstract implementation of the AtnStateTokenClassifier interface.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes = "Base class for classifying tokens through an org.sd.atn.AtnGrammar.")
public abstract class AbstractAtnStateTokenClassifier implements AtnStateTokenClassifier {

  public static final Map<String, String> EMPTY_MAP = new HashMap<String, String>();


  /** Do the basic classification work. */
  protected abstract boolean doClassify(Token token, AtnState atnState);

  /** Do text classification over already normalized text if possible. */
  protected abstract Map<String, String> doClassify(String normalizedText);

  private boolean consume;
  private TokenClassifierHelper tokenClassifierHelper;

  protected AbstractAtnStateTokenClassifier() {
    this.consume = true;
    this.tokenClassifierHelper = new TokenClassifierHelper();
  }

  protected AbstractAtnStateTokenClassifier(Normalizer normalizer, int maxWordCount) {
    this(normalizer, maxWordCount, true);
  }

  protected AbstractAtnStateTokenClassifier(Normalizer normalizer, int maxWordCount, boolean consume) {
    this.tokenClassifierHelper = new TokenClassifierHelper(normalizer, maxWordCount);
    this.consume = consume;
  }

  protected AbstractAtnStateTokenClassifier(DomElement classifierIdElement, Map<String, Normalizer> id2Normalizer) {
    this.tokenClassifierHelper = new TokenClassifierHelper(classifierIdElement, id2Normalizer);

    // check for consume attribute; default to 'true'
    this.consume = classifierIdElement.getAttributeBoolean("consume", true);
  }

  protected boolean consume() {
    return consume;
  }

  protected void setConsume(boolean consume) {
    this.consume = consume;
  }

  protected final TokenClassifierHelper getTokenClassifierHelper() {
    return tokenClassifierHelper;
  }

  /**
   * Default implementation ignores atnState and returns result of
   * 'doClassify(token)' through 'super.classify(token)'.
   * <p>
   * Note that this can be overridden to incorporate context information
   * from atnState.
   */
  public MatchResult classify(Token token, AtnState atnState) {
    boolean result = false;

    if (tokenClassifierHelper.meetsConstraints(token)) {
      result = doClassify(token, atnState);
    }

    return new MatchResult(result, consume);
  }

  /**
   * Classify just the given text regardless of token or state context, if
   * possible.
   *
   * @return a (possibly empty) map of feature keys to values if matched;
   *         otherwise, null if didn't or couldn't match.
   */
  public Map<String, String> classify(String text) {
    return doClassify(tokenClassifierHelper.normalize(text));
  }

  public void supplement(DomNode supplementNode) {
  }

  public int getMaxWordCount() {
    return tokenClassifierHelper.getMaxWordCount();
  }

  /**
   * Get the classifier's name.
   */
  public String getName() {
    String result = tokenClassifierHelper.getName();
    return result == null ? "unknown" : result;
  }
}
