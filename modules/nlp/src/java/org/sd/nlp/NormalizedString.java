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


import java.util.Set;

/**
 * Container for a normalized string mapping it back to its original form.
 *
 * @author Spence Koehler
 */
public interface NormalizedString {

  /**
   * Set a flag indicating whether to split on camel-casing.
   */
  public void setSplitOnCamelCase(boolean splitOnCamelCase);

  /**
   * Get the flag indicating whether to split on camel-casing.
   */
  public boolean getSplitOnCamelCase();

  /**
   * Get the length of the normalized string.
   */
  public int getNormalizedLength();

  /**
   * Get the normalized string.
   * <p>
   * Note that the normalized string may apply to only a portion of the full
   * original string.
   */
  public String getNormalized();

  /**
   * Get the normalized string from the start (inclusive) to end (exclusive).
   * <p>
   * Note that the normalized string may apply to only a portion of the full
   * original string.
   */
  public String getNormalized(int startPos, int endPos);

  /**
   * Get the original string that applies to the normalized string.
   */
  public String getOriginal();

  /**
   * Get the original string that applies to the normalized string from the
   * given index for the given number of normalized characters.
   */
  public String getOriginal(int normalizedStartIndex, int normalizedLength);

  /**
   * Get the index in the original string corresponding to the normalized index.
   */
  public int getOriginalIndex(int normalizedIndex);

  /**
   * Get a new normalized string for the portion of this normalized string
   * preceding the normalized start index (exclusive). Remove extra whitespace
   * at the end of the returned string. Ensure that the returned string ends
   * on an end token boundary.
   *
   * @return the preceding normalized string or null if empty (after skipping white).
   */
  public NormalizedString getPreceding(int normalizedStartIndex);

  /**
   * Get a new normalized string for the portion of this normalized string
   * preceding the normalized start index (exclusive). Remove extra whitespace
   * at the end of the returned string.
   *
   * @param normalizedStartIndex  a token start position in the normalized string beyond the result
   * @param checkEndBreak  when true, skip back over non breaking chars to ensure result ends at a break.
   *
   * @return the preceding normalized string or null if empty (after skipping white).
   */
  public NormalizedString getPreceding(int normalizedStartIndex, boolean checkEndBreak);

  /**
   * Find the (normalized) index of the nth token preceding the normalizedPos.
   * <p>
   * If normalizedPos is -1, start from the end of the string.
   * If the beginning of the string is fewer than numTokens prior to normalizedPos,
   * return the beginning of the string.
   */
  public int getPrecedingIndex(int normalizedPos, int numTokens);

  /**
   * Find the start of the token before the normalizedPos.
   * <p>
   * If normalizedPos is at a token start, the prior token (or -1 if there is
   * no prior token) will be returned; otherwise, the start of the token of
   * which normalizedPos is a part will be returned.
   */
  public int findPrecedingTokenStart(int normalizedPos);

  /**
   * Get a new normalized string for the portion of this normalized string
   * following the normalized start index (inclusive). Remove extra whitespace
   * at the beginning of the returned string.
   *
   * @return the following normalized string or null if empty (after skipping white).
   */
  public NormalizedString getRemaining(int normalizedStartIndex);

  /**
   * Build a normalized string from this using the given normalized index range.
   */
  public NormalizedString buildNormalizedString(int normalizedStartIndex, int normalizedEndIndex);

  /**
   * Lowercase the normalized form in this instance.
   *
   * @return this instance.
   */
  public NormalizedString toLowerCase();

  /**
   * Get the normalized string's chars.
   */
  public char[] getNormalizedChars();

  /**
   * Get the normalized char at the given (normalized) index.
   * <p>
   * NOTE: Bounds checking is left up to the caller.
   */
  public char getNormalizedChar(int index);

  /**
   * Get the original code point corresponding to the normalized char at the
   * (normalized) index.
   * <p>
   * NOTE: Bounds checking is left up to the caller.
   */
  public int getOriginalCodePoint(int nIndex);

  /**
   * Determine whether the original character corresponding to the normalized
   * index is a letter or digit.
   */
  public boolean isLetterOrDigit(int nIndex);

  /**
   * Get the ORIGINAL index of the first symbol (non-letter, digit, or white
   * character) prior to the NORMALIZED index in the full original string.
   *
   * @return -1 if no symbol is found or the index of the found symbol in the
   *         original input string.
   */
  public int findPreviousSymbolIndex(int nIndex);

  /**
   * Determine whether the normalized string has a digit between the normalized
   * start (inclusive) and end (exclusive).
   */
  public boolean hasDigit(int nStartIndex, int nEndIndex);

  /**
   * Count the number of normalized words in the given range.
   */
  public int numWords(int nStartIndex, int nEndIndex);

  /**
   * Determine whether there is a break before the normalized startIndex.
   */
  public boolean isStartBreak(int startIndex);

  /**
   * Determine whether there is a break after the normalized endIndex.
   */
  public boolean isEndBreak(int endIndex);

  /**
   * Get the normalized index that best corresponds to the original index.
   */
  public int getNormalizedIndex(int originalIndex);

  /**
   * Split into normalized token strings.
   */
  public String[] split();

  /**
   * Split into normalized token strings, removing stopwords.
   */
  public String[] split(Set<String> stopwords);

  /**
   * Split this normalized string into tokens.
   */
  public NormalizedToken[] tokenize();

  /**
   * Get the token starting from the start position, optionally skipping to a
   * start break first.
   *
   * @return the token or null if there are no tokens to get.
   */
  public NormalizedToken getToken(int startPos, boolean skipToBreak);


  /**
   * Get the token after the given token, optionally skipping to a start
   * break first.
   */
  public NormalizedToken getNextToken(NormalizedToken curToken, boolean skipToBreak);


  /**
   * Container class for a normalized string token.
   */
  public static class NormalizedToken {
    private NormalizedString nString;
    private int startPos;
    private int endPos;

    public NormalizedToken(NormalizedString nString, int startPos, int endPos) {
      this.nString = nString;
      this.startPos = startPos;
      this.endPos = endPos;
    }

    /**
     * Get this token's normalized string.
     */
    public NormalizedString getNormalizedString() {
      return nString;
    }

    /**
     * Get the normalized starting position of this token.
     */
    public int getStartPos() {
      return startPos;
    }

    /**
     * Get the normalized ending position of this token (plus 1).
     */
    public int getEndPos() {
      return endPos;
    }

    /**
     * Get the normalized length of this token.
     */
    public int length() {
      return endPos - startPos;
    }

    /**
     * Get this token's original text.
     */
    public String getOriginal() {
      return nString.getOriginal(startPos, length());
    }

    /**
     * Get this token's normalized text.
     */
    public String getNormalized() {
      return nString.getNormalized(startPos, endPos);
    }

    /**
     * Get the normalized token following this token, optionally skipping to a
     * start break first.
     */
    public NormalizedToken getNext(boolean skipToBreak) {
      return nString.getNextToken(this, skipToBreak);
    }
  }
}
