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

/**
 * Interface for finding patterns in text.
 * <p>
 * @author Spence Koehler
 */
public interface PatternFinder {

  public static final int ACCEPT_PARTIAL = 0;
  public static final int     BEGIN_WORD = 1;
  public static final int       END_WORD = 2;
  public static final int      FULL_WORD = 3;  // BEGIN_WORD | END_WORD


  /**
   * Get this instance's type.
   */
  public String getType();

  /**
   * Find the position of a pattern within the (normalized) input.
   *
   * @return an array with the normalized index of the first substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findPatternPos(String input, int acceptPartial);

  /**
   * Find the position of a pattern within the (normalized) input.
   *
   * @return an array with the normalized index of the first substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findPatternPos(NormalizedString input, int acceptPartial);

  /**
   * Find the non-normalized text matching a pattern within the input.
   *
   * @return the matching non-normalized text or null if there is no pattern found.
   */
  public String findPattern(String input, int acceptPartial);

  /**
   * Find the non-normalized text matching a pattern within the input.
   *
   * @return the matching non-normalized text or null if there is no pattern found.
   */
  public String findPattern(NormalizedString input, int acceptPartial);

  /**
   * Determine whether the input has a pattern within it.
   */
  public boolean hasPattern(String input, int acceptPartial);

  /**
   * Determine whether the input has a pattern within it.
   */
  public boolean hasPattern(NormalizedString input, int acceptPartial);
  
  /**
   * Find a pattern between the fromPos (inclusive) and the toPos (exclusive).
   *
   * @return an array with the normalized index of the first substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findPatternPos(String input, int fromPos, int toPos, int acceptPartial);

  /**
   * Find a pattern between the fromPos (inclusive normalized position) and the
   * toPos (exclusive normalized position).
   *
   * @return an array with the normalized index of the first substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findPatternPos(NormalizedString input, int fromPos, int toPos, int acceptPartial);

  /**
   * Find the next position of a pattern after the lastPatternPos until the toPos
   * (exclusive normalized position).
   */
  public int[] findNextPatternPos(NormalizedString input, int[] lastPatternPos, int toPos, int acceptPartial);

  /**
   * Find the last position of a pattern within the (normalized) input.
   *
   * @return an array with the normalized index of the last substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findLastPatternPos(String input, int acceptPartial);

  /**
   * Find the last position of a pattern within the (normalized) input.
   *
   * @return an array with the normalized index of the last substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findLastPatternPos(NormalizedString input, int acceptPartial);

  /**
   * Find the last position of a pattern within the (normalized) input between the indexes.
   *
   * @return an array with the normalized index of the last substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findLastPatternPos(String input, int fromPos, int toPos, int acceptPartial);

  /**
   * Find the last position of a pattern within the (normalized) input between the indexes.
   *
   * @return an array with the normalized index of the last substring to match (at index 0),
   *         and its normalized length (at index 1) or null.
   */
  public int[] findLastPatternPos(NormalizedString input, int fromPos, int toPos, int acceptPartial);

  /**
   * Normalize the string using this instance's normalizer.
   */
  public NormalizedString normalize(String string);

  /**
   * Split the input on the patterns, keeping only the data between.
   */
  public NormalizedString[] split(String input);

  /**
   * Split the input on the patterns, keeping only the data between.
   */
  public NormalizedString[] split(NormalizedString input);
}
