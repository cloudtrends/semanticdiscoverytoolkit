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
 * A general break strategy implementation suitable for most applications.
 * <p>
 * <ul>
 * <li>All letters and digits are non-breaking characters.</li>
 * <li>A single dash is soft; multiple consecutives are hard.</li>
 * <li>Space, plus, and ampersand are always soft</li>
 * <li>Colon is hard if followed by a space; otherwise, it is soft.
 * <li>Period is hard if followed by a space; otherwise, period is soft unless
 *     at the beginning of a word and followed by a digit or between two
 *     digits, in which case it is non-breaking.</li>
 * <li>Single-quote is hard unless between two letters, when it isn't a break</li>
 * <li>Slash (forward or back) is no break if preceded or followed by a digit or
 *     if preceded and followed by a single character; otherwise, it is
 *     a soft break.</li>
 * <li>All other symbols are hard unless between two non-breaking chars, in
 *     which case they are soft.</li>
 * <li>The boundary between consecutive Lowercase and Uppercase (camelCase)
 *       is a soft split break.</li>
 * <li>The boundary between a consecutive letter and digit is a soft split break.</li>
 * <li>The boundary between a consecutive digit and letter is *NOT* a break.
 *       This allows for lexicalization of ordinals such as "1st", "2nd", etc.</li>
 * </ul>
 *
 * @author Spence Koehler
 */
public class GeneralBreakStrategy implements BreakStrategy {

  private static final GeneralBreakStrategy INSTANCE = new GeneralBreakStrategy();

  public static GeneralBreakStrategy getInstance() {
    return INSTANCE;
  }

  protected GeneralBreakStrategy() {
  }

  public Break[] computeBreaks(int[] codePoints) {
    final Break[] result = new Break[codePoints.length];

    for (int i = 0; i < codePoints.length; ++i) {
      final Integer nextIndex = setBreak(i, codePoints, result);
      if (nextIndex != null) i = nextIndex;
    }
    return result;
  }

  /**
   * Set result[index] to the appropriate value.
   *
   * @return null if the next index will be index + 1; otherwise, return nextIndex - 1.
   */
  protected Integer setBreak(int index, int[] codePoints, Break[] result) {
    Break curBreak = Break.HARD;

    final int cp = codePoints[index];
    if (cp == ' ' || cp == '+' || cp == '&') {
      curBreak = Break.SOFT_FULL;
    }
    else if (cp == '-') {
      if ((index + 1) < codePoints.length && (codePoints[index + 1] == '-' /*|| codePoints[index + 1] == ' '*/) /*||
                                                                                                                  (index - 1) >= 0 && (codePoints[index - 1] == ' ')*/) {
        while (index < codePoints.length && codePoints[index] == '-') result[index++] = Break.HARD;
        return index - 1;  // reset for next go 'round
      }
      else {
        curBreak = Break.SOFT_FULL;
      }
    }
    else if (cp == ':') {
      if (isChar(index + 1, codePoints, ' ')) {
        curBreak = Break.HARD;
      }
      else {
        curBreak = Break.SOFT_FULL;
      }
    }
    else if (cp == '.') {
      if (isChar(index + 1, codePoints, ' ')) {
        if (isChar(index + 2, codePoints, '-')) {
          curBreak = Break.SOFT_FULL;
        }
        else {
          curBreak = Break.HARD;
        }
      }
      else if (((index == 0 || result[index - 1] == null || result[index - 1].breaks()) && isDigit(index + 1, codePoints)) ||
               (isDigit(index - 1, codePoints) && isDigit(index + 1, codePoints))) {
        curBreak = Break.NONE;
      }
      else {
        curBreak = Break.SOFT_FULL;
      }
    }
    else if (cp == '\'') {  // no break if between letters; otherwise, hard.
      if (isLetter(index - 1, codePoints, result) && isLetter(index + 1, codePoints, null)) {
        curBreak = Break.NONE;
      }
    }
    else if (cp == '/' || cp == '\\') {  // no break or soft break.
      curBreak = Break.SOFT_FULL;
      if (isDigit(index - 1, codePoints) || isDigit(index + 1, codePoints) ||
          isLetter(index - 1, codePoints, result) && isLetter(index + 1, codePoints, null) &&
          isBreak(index - 2, codePoints, result) && isBreak(index + 2, codePoints, null)) {
        curBreak = Break.NONE;
      }
    }
    else if (Character.isLetterOrDigit(cp)) {
      curBreak = Break.NONE;

      // check for camelCase
      if (index > 0) {
        // lower followed by upper
        if (Character.isUpperCase(cp) && result[index - 1] == Break.NONE && Character.isLowerCase(codePoints[index - 1])) {
          curBreak = Break.SOFT_SPLIT;
        }
        // letter followed by digit
        else if (Character.isDigit(cp) && result[index - 1] == Break.NONE && Character.isLetter(codePoints[index - 1])) {
          curBreak = Break.SOFT_SPLIT;
        }
      }
    }
    else {  // isa symbol. soft if we're between letters and/or digits; otherwise, hard
      if (isLetterOrDigit(index - 1, codePoints, result) && isLetterOrDigit(index + 1, codePoints, null)) {
        result[index]  = Break.SOFT_FULL;
      }
    }

    result[index] = curBreak;
    return null;
  }

  protected final boolean isDigit(int index, int[] codePoints) {
    if (index >= 0 && index < codePoints.length) {
      final int cp = codePoints[index];
      return (cp <= '9' && cp >= '0');
    }
    return false;
  }

  protected final boolean isSpace(int index, int[] codePoints) {
    boolean result = false;

    if (index >= 0 && index < codePoints.length && codePoints[index] == ' ') {
      result = true;
    }

    return result;
  }

  protected final boolean isLetter(int index, int[] codePoints, Break[] breaks) {
    if (index >= 0 && index < codePoints.length) {
      if (breaks != null && breaks[index] != null && !breaks[index].skip()) {
        return true;
      }
      else {
        final int cp = codePoints[index];
        return Character.isLetter(cp);
      }
    }
    return false;
  }

  protected boolean isLetterOrDigit(int index, int[] codePoints, Break[] breaks) {
    if (index >= 0 && index < codePoints.length) {
      if (breaks != null && breaks[index] != null && !breaks[index].skip()) {
        return true;
      }
      else {
        final int cp = codePoints[index];
        return Character.isLetterOrDigit(cp);
      }
    }
    return false;
  }

  protected boolean isBreak(int index, int[] codePoints, Break[] breaks) {
    if (index < 0 || index >= codePoints.length) return true;
    if (breaks != null && breaks[index].breaks()) return true;
    final int cp = codePoints[index];
    return !Character.isLetterOrDigit(cp);
  }

  protected boolean isChar(int index, int[] codePoints, char c) {
    if (index >= 0 && index < codePoints.length) {
      return codePoints[index] == c;
    }
    return false;
  }

  public boolean equals(Object o) {
    boolean result = (this == o);

    if (!result && (o != null) && (o instanceof GeneralBreakStrategy)) {
      result = true;
    }

    return result;
  }

  public int hashCode() {
    return 42;
  }
}
