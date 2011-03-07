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


import java.util.Map;
import org.sd.token.AbstractTokenClassifier;
import org.sd.token.Normalizer;
import org.sd.token.Token;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;

/**
 * Abstract implementation of the AtnStateTokenClassifier interface.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractAtnStateTokenClassifier extends AbstractTokenClassifier implements AtnStateTokenClassifier {

  private boolean consume;

  protected AbstractAtnStateTokenClassifier() {
    super();
  }

  protected AbstractAtnStateTokenClassifier(Normalizer normalizer, int maxWordCount) {
    this(normalizer, maxWordCount, true);
  }

  protected AbstractAtnStateTokenClassifier(Normalizer normalizer, int maxWordCount, boolean consume) {
    super(normalizer, maxWordCount);
    this.consume = consume;
  }

  protected AbstractAtnStateTokenClassifier(DomElement classifierIdElement, Map<String, Normalizer> id2Normalizer) {
    super();

    // check for consume attribute; default to 'true'
    this.consume = classifierIdElement.getAttributeBoolean("consume", true);
  }

  protected boolean consume() {
    return consume;
  }

  protected void setConsume(boolean consume) {
    this.consume = consume;
  }

  /**
   * Default implementation ignores atnState and returns result of
   * 'doClassify(token)' through 'super.classify(token)'.
   * <p>
   * Note that this can be overridden to incorporate context information
   * from atnState.
   */
  public MatchResult classify(Token token, AtnState atnState) {
    return new MatchResult(super.classify(token), consume);
  }

  public void supplement(DomNode supplementNode) {
  }
}
