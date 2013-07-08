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
package org.sd.text;


import org.sd.nlp.GeneralNormalizer;
import org.sd.nlp.NormalizedString;

/**
 * Utility to find a number pattern in a string.
 * <p>
 * @author Spence Koehler
 */
public class NumberFinder extends AbstractPatternFinder {
  
  /**
   * Construct with the given type.
   */
  public NumberFinder(String type) {
    super(type, GeneralNormalizer.getCaseInsensitiveInstance());
  }

  /**
   * Find a number between the fromPos (inclusive normalized position) and the
   * toPos (exclusive normalized position).
   *
   * @return an array with the normalized index of the first substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findPatternPos(NormalizedString input, int fromPos, int toPos, int acceptPartial) {
    int[] result = null;

    if (input == null || input.getNormalizedLength() == 0) return null;
    
    NormalizedString.NormalizedToken firstNumber = null;
    NormalizedString.NormalizedToken lastNumber = null;
    boolean gotMoreThanJustSymbols = false;

    for (NormalizedString.NormalizedToken token = input.getToken(fromPos, true); token != null; token = token.getNext(true)) {
      if (token.getStartPos() >= toPos || token.getEndPos() > toPos) break;

      final boolean[] isNumber = isNumber(token, lastNumber);

      if (isNumber[0]) {
        if (lastNumber == null) {
          firstNumber = token;
        }
        lastNumber = token;

        if (!isNumber[1]) {  // not onlySymbols
          gotMoreThanJustSymbols = true;
        }
      }
      else if (lastNumber != null) {
        if (gotMoreThanJustSymbols) break;  // found a number and hit end
        firstNumber = null;
        lastNumber = null;
      }
    }


    if (firstNumber != null && gotMoreThanJustSymbols) {
      result = new int[]{firstNumber.getStartPos(), lastNumber.getEndPos() - firstNumber.getStartPos()};
    }

    return result;
  }

  // lastNumber is the prior token if classified as a number or null.
  // return {isNumber, onlySymbols}
  private final boolean[] isNumber(NormalizedString.NormalizedToken token, NormalizedString.NormalizedToken lastNumber) {
    final String normalized = token.getNormalized();
    boolean isNumber = TextNumber.isNumber(normalized);  // check for "one, two, three, ..., first, second, ..."
    boolean onlySymbols = false;

    // NOTE: because of how the NormalizedString tokenization works, all of
    //       the characters in a token will have the same character "class".
    //
    //       this means we can check just one of the characters to see what
    //       the others are like.

    if (!isNumber) {
      final int nLen = normalized.length();
      final char lastLetter = normalized.charAt(nLen - 1);

      // check for digits
      isNumber = (lastLetter <= '9' && lastLetter >= '0');

      // check for number endings "st", "nd", "rd", or "th"
      if (!isNumber && lastNumber != null) {
        isNumber = TextNumber.isNumberEnding(normalized);
      }

      // check for roman numerals
      if (!isNumber) {
        isNumber = TextNumber.isRomanNumeral(token.getOriginal(), true);
      }

      // count letters. if <= 1, count as a number. (one letter or just symbols).
      if (!isNumber) {
        final int numLetters = (Character.isLetter(lastLetter)) ? nLen : 0;
        isNumber = (numLetters <= 1);
        onlySymbols = isNumber;
      }
    }
    return new boolean[]{isNumber, onlySymbols};
  }
}
