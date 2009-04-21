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
package org.sd.nlp;


/**
 * A simple, illustrative, default break strategy implementation.
 * <p>
 * Most applications will implement their own break strategy.
 *
 * @author Spence Koehler
 */
public class DefaultBreakStrategy implements BreakStrategy {

  public DefaultBreakStrategy() {
  }

  public Break[] computeBreaks(int[] codePoints) {
    final Break[] result = new Break[codePoints.length];
    for (int i = 0; i < codePoints.length; ++i) {
      Break curBreak = Break.NONE;
      final int cp = codePoints[i];
      if (cp == ' ') {
        curBreak = Break.SOFT_FULL;
      }
      else if (cp == '.' || cp == ',' || cp == '!' || cp == ';' || cp == ':') {
        curBreak = Break.HARD;
      }
      else if (Character.isLetterOrDigit(cp)) {
        // check for camelCase
        if (i > 0) {
          // lower followed by upper
          if (Character.isUpperCase(cp) && result[i - 1] == Break.NONE && Character.isLowerCase(codePoints[i - 1])) {
            curBreak = Break.SOFT_SPLIT;
          }
          // letter followed by digit
          else if (Character.isDigit(cp) && result[i - 1] == Break.NONE && Character.isLetter(codePoints[i - 1])) {
            curBreak = Break.SOFT_SPLIT;
          }
        }
      }
      else {
        curBreak = Break.SOFT_FULL;
      }
      result[i] = curBreak;
    }
    return result;
  }

  public boolean equals(Object o) {
    boolean result = (this == o);

    if (!result && (o != null) && (o instanceof DefaultBreakStrategy)) {
      result = true;
    }

    return result;
  }

  public int hashCode() {
    return 41;
  }
}
