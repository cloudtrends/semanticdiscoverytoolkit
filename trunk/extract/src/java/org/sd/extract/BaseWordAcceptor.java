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


import org.sd.nlp.NormalizedString;
import org.sd.nlp.Normalizer;

/**
 * Base implementation for the WordAcceptor interface.
 * <p>
 * @author Spence Koehler
 */
public abstract class BaseWordAcceptor implements WordAcceptor {
  
  protected abstract boolean doAccept(NormalizedString normalizedWord);

  protected abstract boolean doAccept(String normalizedWord);


  private Normalizer normalizer;

  protected BaseWordAcceptor(Normalizer normalizer) {
    this.normalizer = normalizer;
  }

  /**
   * Test whether the word is acceptable.
   *
   * @param word          the word to test.
   * @param isNormalized  true if the word is already normalized.
   *
   * @return true to accept the word; otherwise, false.
   */
  public final boolean accept(String word, boolean isSplit, boolean isNormalized) {
    boolean result = false;

    if (isSplit) {
      if (isNormalized) {
        result = doAccept(word);
      }
      else {
        result = doAccept(normalize(word));
      }
    }
    else {
      final String[] words = split(word, isNormalized);
      if (words != null && words.length > 0) {
        result = true;
        for (String w : words) {
          if (!doAccept(w)) {
            result = false;
            break;
          }
        }
      }
    }

    return result;
  }

  /**
   * Split the text into normalized words.
   * <p>
   * Default splitting uses the normalized string split method.
   */
  public String[] split(String text, boolean isNormalized) {
    final NormalizedString normalizedString = isNormalized ? new NormalizedString(text) : normalize(text);
    return normalizedString.split();
  }

  /**
   * Normalize the text.
   */
  public final NormalizedString normalize(String text) {
    return (normalizer == null) ? new NormalizedString(text) : normalizer.normalize(text);
  }

  /**
   * Get this instance's normalizer.
   */
  public final Normalizer getNormalizer() {
    return normalizer;
  }
}
