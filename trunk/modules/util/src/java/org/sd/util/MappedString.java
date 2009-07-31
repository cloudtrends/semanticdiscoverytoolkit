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
package org.sd.util;


import java.util.HashMap;
import java.util.Map;

/**
 * A container class for referencing an original string from its mapped
 * or transformed version.
 * <p>
 * @author Spence Koehler
 */
public class MappedString {

  private StringBuilder mappedString;
  private StringBuilder originalString;
  private Map<Integer, int[]> reverseMap;  // mapped index to original {index, length}

  /**
   * Construct a new empty instance.
   */
  public MappedString() {
    this.mappedString = new StringBuilder();
    this.originalString = new StringBuilder();
    this.reverseMap = new HashMap<Integer, int[]>();
  }

  /**
   * Append the given string with identity mapping (as-is).
   *
   * @param string  The string (original and mapped) to append.
   *
   * @return this instance.
   */
  public MappedString append(String string) {
    int origPos = originalString.length();
    int mappedPos = mappedString.length();

    originalString.append(string);
    mappedString.append(string);

    for (int i = 0; i < string.length(); ++i) {
      reverseMap.put(mappedPos++, new int[]{origPos++, 1});
    }

    return this;
  }

  /**
   * Append the given mapped code point to this mapped string.
   *
   * @param mappedCodePoint  The code point of the next mapped character.
   * @param origSubString  The original string being mapped to the code point.
   *
   * @return this instance.
   */
  public MappedString append(int mappedCodePoint, String origSubString) {
    final int mappedPos = mappedString.length();
    final int origPos = originalString.length();

    originalString.append(origSubString);
    mappedString.appendCodePoint(mappedCodePoint);

    reverseMap.put(mappedPos, new int[]{origPos, origSubString.length()});

    return this;
  }

  /**
   * Get the mapped string.
   *
   * @return the mapped string.
   */
  public String getMappedString() {
    return mappedString.toString();
  }

  /**
   * Get the length of the mapped string.
   *
   * @return the mapped string's length.
   */
  public int getMappedLength() {
    return mappedString.length();
  }

  /**
   * Get the mapped string.
   *
   * @return the mapped string.
   */
  public String toString() {
    return getMappedString();
  }

  /**
   * Get the original string.
   *
   * @return the original string.
   */
  public String getOriginalString() {
    return originalString.toString();
  }

  /**
   * Get the length of the original string.
   *
   * @return the original string's length.
   */
  public int getOriginalLength() {
    return originalString.length();
  }

  /**
   * Get the start index and length in the original string of the character
   * at the given index in the mapped string.
   * <p>
   * NOTE: This method will return null for non-existent positions, including
   *       the length of the string.
   *
   * @param mappedIndex  The index of a character in the mapped string.
   *
   * @return the start index in result[0] and the length in the original string
   *         of the character at the given index in the mapped string or null
   *         if out of range.
   */
  public int[] getOriginalIndex(int mappedIndex) {
    return reverseMap.get(mappedIndex);
  }

  /**
   * Convert the mapped index to an original index.
   * <p>
   * NOTE: This method properly maps the end of the strings to each other.
   *
   * @param mappedIndex  An index in the mapped string.
   *
   * @return the original index or -1 if out of range.
   */
  public int convertIndex(int mappedIndex) {
    int result = -1;

    final int[] origIndex = getOriginalIndex(mappedIndex);
    if (origIndex != null) {
      result = origIndex[0];
    }
    else {
      if (mappedIndex == mappedString.length()) {  // at end of string
        result = originalString.length();
      }
    }

    return result;
  }

  /**
   * Get the original substring corresponding to the mapped indexes.
   *
   * @param mappedStart  The mapped start index (inclusive).
   * @param mappedEnd  The mapped end index (exclusive).
   *
   * @return the corresponding original substring.
   */
  public String originalSubstring(int mappedStart, int mappedEnd) {
    return originalString.substring(convertIndex(mappedStart), convertIndex(mappedEnd));
  }
}
