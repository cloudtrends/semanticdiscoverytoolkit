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

/**
 * Interface for accepting a word.
 * <p>
 * @author Spence Koehler
 */
public interface WordAcceptor {

  /**
   * Test whether the word is acceptable.
   *
   * @param word          the word to test.
   * @param isNormalized  true if the word is already normalized.
   *
   * @return true to accept the word; otherwise, false.
   */
  public boolean accept(String word, boolean isSplit, boolean isNormalized);

  /**
   * Split the text into words.
   *
   * @param text          The text to split.
   * @param isNormalized  true if the text is already normalized.
   */
  public String[] split(String text, boolean isNormalized);

  /**
   * Normalize the text.
   */
  public NormalizedString normalize(String text);
}
