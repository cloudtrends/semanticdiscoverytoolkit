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
import org.sd.token.Token;
import org.sd.xml.DomNode;

/**
 * Interface for classifying a token in context of its AtnState.
 * <p>
 * @author Spence Koehler
 */
public interface AtnStateTokenClassifier {
  
  /**
   * Classify the given token in the context of its AtnState.
   */
  public MatchResult classify(Token token, AtnState atnState);

  /**
   * Classify just the given text regardless of token or state context, if
   * possible.
   *
   * @return a (possibly empty) map of feature keys to values if matched;
   *         otherwise, null if didn't or couldn't match.
   */
  public Map<String, String> classify(String text);

  /**
   * Supplement this classifier with the given dom node.
   */
  public void supplement(DomNode supplementNode);

  /**
   * Get the maximum word count (0 for infinite) for token classification.
   */
  public int getMaxWordCount();

  /**
   * Get the classifier's name.
   */
  public String getName();
}
