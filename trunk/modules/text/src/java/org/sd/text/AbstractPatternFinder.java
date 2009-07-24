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


import org.sd.nlp.NormalizedString;
import org.sd.nlp.Normalizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract implementation of a pattern finder.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractPatternFinder implements PatternFinder {

  private String type;
  private Normalizer normalizer;

  protected AbstractPatternFinder(String type, Normalizer normalizer) {
    this.type = type;
    this.normalizer = normalizer;
  }

  /**
   * Get this instance's type.
   */
  public String getType() {
    return type;
  }

  /**
   * Find the position of a pattern within the (normalized) input.
   *
   * @return an array with the normalized index of the first substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findPatternPos(String input, int acceptPartial) {
    return findPatternPos(normalize(input), acceptPartial);
  }

  /**
   * Find the position of a pattern within the (normalized) input.
   *
   * @return an array with the normalized index of the first substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findPatternPos(NormalizedString input, int acceptPartial) {
    if (input == null || input.getNormalizedLength() == 0) return null;
    return findPatternPos(input, 0, input.getNormalizedLength(), acceptPartial);
  }

  /**
   * Find the non-normalized text matching a pattern within the input.
   *
   * @return the matching non-normalized text or null if there is no pattern found.
   */
  public String findPattern(String input, int acceptPartial) {
    return findPattern(normalize(input), acceptPartial);
  }

  /**
   * Find the non-normalized text matching a number within the input.
   *
   * @return the matching non-normalized text or null if there is no number found.
   */
  public String findPattern(NormalizedString input, int acceptPartial) {
    String result = null;
    final int[] pos = findPatternPos(input, acceptPartial);
    if (pos != null) {
      result = input.getOriginal(pos[0], pos[1]);
    }
    return result;
  }

  /**
   * Determine whether the input has a pattern within it.
   */
  public boolean hasPattern(String input, int acceptPartial) {
    return findPatternPos(input, acceptPartial) != null;
  }

  /**
   * Determine whether the input has a pattern within it.
   */
  public boolean hasPattern(NormalizedString input, int acceptPartial) {
    return findPatternPos(input, acceptPartial) != null;
  }

  /**
   * Find a pattern between the fromPos (inclusive) and the toPos (exclusive).
   *
   * @return an array with the normalized index of the first substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findPatternPos(String input, int fromPos, int toPos, int acceptPartial) {
    return findPatternPos(normalize(input), fromPos, toPos, acceptPartial);
  }

  /**
   * Find the next position of a number after the lastPatternPos until the toPos
   * (exclusive normalized position).
   */
  public int[] findNextPatternPos(NormalizedString input, int[] lastPatternPos, int toPos, int acceptPartial) {
    if (input == null || lastPatternPos == null) return null;
    int fromPos = lastPatternPos[0] + lastPatternPos[1];
    final int nLen = input.getNormalizedLength();
    if (fromPos >= nLen) return null;
    while (fromPos < nLen && input.getNormalizedChar(fromPos) == ' ') ++fromPos;  // skip space(s)
    if (fromPos >= nLen) return null;
    return findPatternPos(input, fromPos, toPos, acceptPartial);
  }

  /**
   * Find the last position of a pattern within the (normalized) input.
   *
   * @return an array with the normalized index of the last substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findLastPatternPos(String input, int acceptPartial) {
    return findLastPatternPos(normalize(input), acceptPartial);
  }

  /**
   * Find the last position of a pattern within the (normalized) input.
   *
   * @return an array with the normalized index of the last substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findLastPatternPos(NormalizedString input, int acceptPartial) {
    if (input == null) return null;
    return findLastPatternPos(input, 0, input.getNormalizedLength(), acceptPartial);
  }

  /**
   * Find the last position of a pattern within the (normalized) input between the indexes.
   *
   * @return an array with the normalized index of the last substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findLastPatternPos(String input, int fromPos, int toPos, int acceptPartial) {
    return findLastPatternPos(normalize(input), fromPos, toPos, acceptPartial);
  }

  /**
   * Find the last position of a pattern within the (normalized) input between the indexes.
   *
   * @return an array with the normalized index of the last substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findLastPatternPos(NormalizedString input, int fromPos, int toPos, int acceptPartial) {
    if (input == null) return null;

    int[] result = null;
    final int len = input.getNormalizedLength();

    for (int[] patternPos = findPatternPos(input, fromPos, toPos, acceptPartial);
         patternPos != null;
         patternPos = findNextPatternPos(input, patternPos, toPos, acceptPartial)) {
      result = patternPos;
    }

    return result;
  }

  /**
   * Normalize the string using this instance's normalizer.
   */
  public final NormalizedString normalize(String string) {
    return (string == null || "".equals(string)) ? null : normalizer.normalize(string);
  }

  /**
   * Split the input on the patterns, keeping only the data between.
   */
  public NormalizedString[] split(String input) {
    return split(normalize(input));
  }

  /**
   * Split the input on the patterns, keeping only the data between.
   */
  public NormalizedString[] split(NormalizedString input) {
    return split(input, ACCEPT_PARTIAL, false);
  }

  /**
   * Split the input on the patterns, keeping only the data between.
   * <p>
   * If keepMatch, matches will be in odd indexes of the result;
   * Note that result[evenIndexes] could be null if the first match was at the
   * beginning or between different consecutive matches.
   */
  public NormalizedString[] split(NormalizedString input, int acceptPartial, boolean keepMatch) {
    List<NormalizedString> result = null;

    while (input != null) {
      final int[] pos = findPatternPos(input, acceptPartial);
      if (pos != null) {
        final NormalizedString preString = input.getPreceding(pos[0], false);
        if (preString != null) {
          if (result == null) result = new ArrayList<NormalizedString>();
          result.add(preString);
        }
        else {  // preString == null
          if (keepMatch) {
            if (result == null) result = new ArrayList<NormalizedString>();
            result.add(preString);
          }
        }
        int endPos = pos[0] + pos[1];
        if (keepMatch) {
          result.add(input.buildNormalizedString(pos[0], endPos));
        }
        input = input.getRemaining(endPos);
      }
      else {
        if (result == null) result = new ArrayList<NormalizedString>();
        result.add(input);
        break;
      }
    }

    return result == null ? null : result.toArray(new NormalizedString[result.size()]);
  }
}
