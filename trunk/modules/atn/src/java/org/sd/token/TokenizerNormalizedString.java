/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.token;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.sd.nlp.NormalizedString;

/**
 * An nlp.NormalizedString implementation based on this package's tokenization.
 * <p>
 * @author Spence Koehler
 */
public class TokenizerNormalizedString implements NormalizedString {

  private TokenizerBasedNormalizer normalizer;
  private StandardTokenizer tokenizer;
  private boolean lowerCaseFlag;
  private boolean computed;

  private Normalization _normalization;

  public TokenizerNormalizedString(TokenizerBasedNormalizer normalizer, StandardTokenizer tokenizer, boolean lowerCaseFlag) {
    this.normalizer = normalizer;
    this.tokenizer = tokenizer;
    this.lowerCaseFlag = lowerCaseFlag;
    this.computed = false;
    reset();
  }
  
  public StandardTokenizer getTokenizer() {
    return tokenizer;
  }

  public void setLowerCaseFlag(boolean lowerCaseFlag) {
    if (lowerCaseFlag != this.lowerCaseFlag) {
      this.lowerCaseFlag = lowerCaseFlag;
      reset();
    }
  }

  public boolean getLowerCaseFlag() {
    return lowerCaseFlag;
  }

  /**
   * Set a flag indicating whether to split on camel-casing.
   */
  public void setSplitOnCamelCase(boolean splitOnCamelCase) {
    final Break curBreak = tokenizer.getOptions().getLowerUpperBreak();
    final Break nextBreak = splitOnCamelCase ? Break.ZERO_WIDTH_SOFT_BREAK : Break.NO_BREAK;
    if (curBreak != nextBreak) {
      final StandardTokenizerOptions newOptions = new StandardTokenizerOptions(tokenizer.getOptions());
      newOptions.setLowerUpperBreak(nextBreak);
      this.tokenizer = new StandardTokenizer(tokenizer.getText(), newOptions);
      reset();
    }
  }

  private final void reset() {
    this.computed = false;
  }

  /**
   * Get the flag indicating whether to split on camel-casing.
   */
  public boolean getSplitOnCamelCase() {
    return tokenizer.getOptions().getLowerUpperBreak() != Break.NO_BREAK;
  }

  /**
   * Get the length of the normalized string.
   */
  public int getNormalizedLength() {
    return getNormalized().length();
  }

  /**
   * Get the normalized string.
   * <p>
   * Note that the normalized string may apply to only a portion of the full
   * original string.
   */
  public String getNormalized() {
    if (!computed) {
      computeNormalization();
    }
    return _normalization.getNormalized();
  }

  public String toString() {
    return getNormalized();
  }

  /**
   * Get the normalized string from the start (inclusive) to end (exclusive).
   * <p>
   * Note that the normalized string may apply to only a portion of the full
   * original string.
   */
  public String getNormalized(int startPos, int endPos) {
    return getNormalized().substring(startPos, endPos);
  }

  /**
   * Get the original string that applies to the normalized string.
   */
  public String getOriginal() {
    return tokenizer.getText();
  }

  /**
   * Get the original string that applies to the normalized string from the
   * given index for the given number of normalized characters.
   */
  public String getOriginal(int normalizedStartIndex, int normalizedLength) {
    final int origStartIdx = getOriginalIndex(normalizedStartIndex);
    final int origEndIdx = getOriginalIndex(normalizedStartIndex + normalizedLength);
    return getOriginal().substring(origStartIdx, origEndIdx);
  }

  /**
   * Get the index in the original string corresponding to the normalized index.
   */
  public int getOriginalIndex(int normalizedIndex) {
    if (!computed) {
      computeNormalization();
    }

    final Integer result = _normalization.getOriginalIndex(normalizedIndex);
    return result == null ? -1 : result;
  }

  /**
   * Get a new normalized string for the portion of this normalized string
   * preceding the normalized start index (exclusive). Remove extra whitespace
   * at the end of the returned string. Ensure that the returned string ends
   * on an end token boundary.
   *
   * @return the preceding normalized string or null if empty (after skipping white).
   */
  public NormalizedString getPreceding(int normalizedStartIndex) {
    return getPreceding(normalizedStartIndex, true);
  }

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
  public NormalizedString getPreceding(int normalizedStartIndex, boolean checkEndBreak) {
    NormalizedString result = null;

    final int origIdx = getOriginalIndex(normalizedStartIndex);
    if (origIdx >= 0) {
      final Token token = tokenizer.getToken(origIdx);
      if (token != null) {
        final String priorText = 
          checkEndBreak ? tokenizer.getPriorText(token) :
          tokenizer.getText().substring(0, token.getStartIndex()).trim();

        if (!"".equals(priorText)) {
          result = normalizer.normalize(priorText);
        }
      }
    }

    return result;
  }

  /**
   * Find the (normalized) index of the nth token preceding the normalizedPos.
   * <p>
   * If normalizedPos is -1, start from the end of the string.
   * If the beginning of the string is fewer than numTokens prior to normalizedPos,
   * return the beginning of the string.
   */
  public int getPrecedingIndex(int normalizedPos, int numTokens) {
    if (!computed) {
      computeNormalization();
    }

    int result = normalizedPos < 0 ? _normalization.getNormalizedLength() : normalizedPos;

    // skip back to the numTokens-th start break.
    int numStarts = 0;
    for (; result > 0 && numStarts < numTokens; result = findPrecedingTokenStart(result)) {
      ++numStarts;
    }

    return result;
  }


  /**
   * Find the start of the token before the normalizedPos.
   * <p>
   * If normalizedPos is at a token start, the prior token (or -1 if there is
   * no prior token) will be returned; otherwise, the start of the token of
   * which normalizedPos is a part will be returned.
   */
  public int findPrecedingTokenStart(int normalizedPos) {
    if (!computed) {
      computeNormalization();
    }

    Integer priorStart = _normalization.getBreaks().lower(normalizedPos);
    return priorStart == null ? -1 : priorStart;
  }

  /**
   * Get a new normalized string for the portion of this normalized string
   * following the normalized start index (inclusive). Remove extra whitespace
   * at the beginning of the returned string.
   *
   * @return the following normalized string or null if empty (after skipping white).
   */
  public NormalizedString getRemaining(int normalizedStartIndex) {
    NormalizedString result = null;

    final int origIdx = getOriginalIndex(normalizedStartIndex);
    if (origIdx >= 0) {
      final String origText = tokenizer.getText();
      final int origLen = origText.length();
      if (origIdx < origLen) {
        final String remainingText = origText.substring(origIdx).trim();
        if (!"".equals(remainingText)) {
          result = normalizer.normalize(remainingText);
        }
      }
    }

    return result;
  }

  /**
   * Build a normalized string from this using the given normalized index range.
   */
  public NormalizedString buildNormalizedString(int normalizedStartIndex, int normalizedEndIndex) {
    NormalizedString result = null;

    final int origStartIdx = getOriginalIndex(normalizedStartIndex);
    if (origStartIdx >= 0) {
      final int origEndIdx = getOriginalIndex(normalizedEndIndex);
      if (origEndIdx > origStartIdx) {
        final String origText = tokenizer.getText();
        final int origLen = origText.length();
        if (origStartIdx < origLen) {
          final String string = origText.substring(origStartIdx, Math.min(origEndIdx, origLen));
          result = normalizer.normalize(string);
        }
      }
    }

    return result;
  }

  /**
   * Lowercase the normalized form in this instance.
   *
   * @return this instance.
   */
  public NormalizedString toLowerCase() {
    if (!computed) {
      computeNormalization();
    }

    _normalization.toLowerCase();

    return this;
  }

  /**
   * Get the normalized string's chars.
   */
  public char[] getNormalizedChars() {
    if (!computed) {
      computeNormalization();
    }

    return _normalization.getNormalizedChars();
  }

  /**
   * Get the normalized char at the given (normalized) index.
   * <p>
   * NOTE: Bounds checking is left up to the caller.
   */
  public char getNormalizedChar(int index) {
    if (!computed) {
      computeNormalization();
    }

    return _normalization.getNormalizedChars()[index];
  }

  /**
   * Get the original code point corresponding to the normalized char at the
   * (normalized) index.
   * <p>
   * NOTE: Bounds checking is left up to the caller.
   */
  public int getOriginalCodePoint(int nIndex) {
    int result = 0;

    final int origIdx = getOriginalIndex(nIndex);
    if (origIdx >= 0) {
      final String origText = tokenizer.getText();
      final int origLen = origText.length();
      if (origIdx < origLen) {
        result = origText.codePointAt(origIdx);
      }
    }

    return result;
  }

  /**
   * Determine whether the original character corresponding to the normalized
   * index is a letter or digit.
   */
  public boolean isLetterOrDigit(int nIndex) {
    return Character.isLetterOrDigit(getOriginalCodePoint(nIndex));
  }

  /**
   * Get the ORIGINAL index of the first symbol (non-letter, digit, or white
   * character) prior to the NORMALIZED index in the full original string.
   *
   * @return -1 if no symbol is found or the index of the found symbol in the
   *         original input string.
   */
  public int findPreviousSymbolIndex(int nIndex) {
    int result = -1;

    final int origIdx = getOriginalIndex(nIndex);
    if (origIdx >= 0) {
      final String origText = tokenizer.getText();
      final int origLen = origText.length();
      for (result = Math.min(origIdx, origLen) - 1; result >= 0; --result) {
        final int cp = origText.codePointAt(result);
        if (cp != ' ' && !Character.isLetterOrDigit(cp)) break;
      }
    }

    return result;
  }

  /**
   * Determine whether the normalized string has a digit between the normalized
   * start (inclusive) and end (exclusive).
   */
  public boolean hasDigit(int nStartIndex, int nEndIndex) {
    boolean result = false;

    if (!computed) {
      computeNormalization();
    }
    final char[] nchars = _normalization.getNormalizedChars();
    nEndIndex = Math.min(nEndIndex, nchars.length);
    for (int idx = Math.max(nStartIndex, 0); idx < nEndIndex; ++idx) {
      final char c = nchars[idx];
      if (c <= '9' && c >= '0') {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Count the number of normalized words in the given range.
   */
  public int numWords(int nStartIndex, int nEndIndex) {
    int result = 0;

    if (!computed) {
      computeNormalization();
    }
    final TreeSet<Integer> breaks = _normalization.getBreaks();
    final int nLen = _normalization.getNormalizedLength();
    nEndIndex = Math.min(nEndIndex, nLen);
    for (int idx = Math.max(nStartIndex, 0); idx < nEndIndex && idx >= 0; idx = breaks.higher(idx)) {
      if (idx == nEndIndex - 1) break; // nEndIdex as at the beginning of a word -- doesn't count
      ++result;
    }

    return result;
  }

  /**
   * Determine whether there is a break before the normalized startIndex.
   */
  public boolean isStartBreak(int startIndex) {
    if (!computed) {
      computeNormalization();
    }

    return _normalization.isBreak(startIndex - 1);
  }

  /**
   * Determine whether there is a break after the normalized endIndex.
   */
  public boolean isEndBreak(int endIndex) {
    if (!computed) {
      computeNormalization();
    }

    return _normalization.isBreak(endIndex + 1);
  }

  /**
   * Get (first) the normalized index that best corresponds to the original index.
   */
  public int getNormalizedIndex(int originalIndex) {
    if (!computed) {
      computeNormalization();
    }

    return _normalization.getNormalizedIndex(originalIndex);
  }

  /**
   * Split into normalized token strings.
   */
  public String[] split() {
    if (!computed) {
      computeNormalization();
    }

    return _normalization.getNormalized().split("\\s+");
  }

  /**
   * Split into normalized token strings, removing stopwords.
   */
  public String[] split(Set<String> stopwords) {
    final List<String> result = new ArrayList<String>();
    for (NormalizedToken token = getToken(0, true); token != null; token = token.getNext(true)) {
      final String ntoken = token.getNormalized();
      if (stopwords == null || !stopwords.contains(ntoken)) {
        result.add(ntoken);
      }
    }
    return result.toArray(new String[result.size()]);
  }

  /**
   * Split this normalized string into tokens.
   */
  public NormalizedToken[] tokenize() {
    final List<NormalizedToken> result = new ArrayList<NormalizedToken>();
    for (NormalizedToken token = getToken(0, true); token != null; token = token.getNext(true)) {
      result.add(token);
    }
    return result.toArray(new NormalizedToken[result.size()]);
  }

  /**
   * Get the token starting from the start position, optionally skipping to a
   * start break first.
   *
   * @return the token or null if there are no tokens to get.
   */
  public NormalizedToken getToken(int startPos, boolean skipToBreak) {
    NormalizedToken result = null;

    startPos = getTokenStart(startPos, skipToBreak);
    if (startPos < _normalization.getNormalizedLength()) {
      final int endPos = getTokenEnd(startPos);
      result = new NormalizedToken(this, startPos, endPos);
    }

    return result;
  }

  /**
   * Get the token after the given token, optionally skipping to a start
   * break first.
   */
  public NormalizedToken getNextToken(NormalizedToken curToken, boolean skipToBreak) {
    NormalizedToken result = null;

    if (curToken != null) {
      if (!computed) {
        computeNormalization();
      }
      final TreeSet<Integer> breaks = _normalization.getBreaks();
      final int nLen = _normalization.getNormalizedLength();

      int curEndPos = curToken.getEndPos();
      if (skipToBreak && !_normalization.isBreak(curEndPos)) {
        final Integer nextBreak = breaks.higher(curEndPos);
        curEndPos = (nextBreak == null) ? nLen : nextBreak;
      }

      if (curEndPos < nLen) {
        final int startPos = getTokenStart(curEndPos + 1, true);
        if (startPos < nLen) {
          final int nextEndPos = getTokenEnd(startPos);
          result = new NormalizedToken(this, startPos, nextEndPos);
        }
      }
    }

    return result;
  }

  /**
   * Get the normalized token start pos at or after (normalized) startPos
   * after optionally skipping to a token start position (if not already
   * at one.)
   */
  private final int getTokenStart(int startPos, boolean skipToBreak) {
    if (!computed) {
      computeNormalization();
    }
    final TreeSet<Integer> breaks = _normalization.getBreaks();
    final int nLen = _normalization.getNormalizedLength();

    if (skipToBreak && !isStartBreak(startPos)) {
      final Integer nextBreak = breaks.ceiling(startPos);
      startPos = nextBreak == null ? nLen : nextBreak + 1;
    }

    return startPos;
  }

  /**
   * Get the normalized index just after the token starting at (normalized) startPos.
   */
  private final int getTokenEnd(int startPos) {
    if (!computed) {
      computeNormalization();
    }
    final TreeSet<Integer> breaks = _normalization.getBreaks();
    final int nLen = _normalization.getNormalizedLength();
    final Integer endPos = breaks.higher(startPos);

    return endPos == null ? nLen : endPos;
  }


  private final void computeNormalization() {
    this._normalization = buildNewNormalization(tokenizer, lowerCaseFlag);
   
    for (Token token = tokenizer.getToken(0); token != null; token = token.getNextToken()) {
      _normalization.updateWithToken(token);
    }

    this.computed = true;
  }

  protected Normalization buildNewNormalization(Tokenizer tokenizer, boolean lowerCaseFlag) {
    return new Normalization(tokenizer, lowerCaseFlag);
  }


  protected static class Normalization {
    private Tokenizer tokenizer;
    private boolean lowerCaseFlag;
    private StringBuilder normalized;
    private Map<Integer, Integer> norm2orig;
    private TreeSet<Integer> breaks;
    private char[] _nchars;

    protected Normalization(Tokenizer tokenizer, boolean lowerCaseFlag) {
      this.tokenizer = tokenizer;
      this.lowerCaseFlag = lowerCaseFlag;
      this.normalized = new StringBuilder();
      this.norm2orig = new HashMap<Integer, Integer>();
      this.breaks = new TreeSet<Integer>();
      this._nchars = null;
    }

    public final Tokenizer getTokenizer() {
      return tokenizer;
    }

    public final boolean getLowerCaseFlag() {
      return lowerCaseFlag;
    }

    /** Get original input. */
    public final String getInput() {
      return tokenizer.getText();
    }

    /** Get the normalized string. */
    public final String getNormalized() {
      return normalized.toString();
    }

    public final char[] getNormalizedChars() {
      if (_nchars == null) {
        _nchars = normalized.toString().toCharArray();
      }
      return _nchars;
    }

    public final int getNormalizedLength() {
      return normalized.length();
    }

    public final int getOriginalIndex(int normalizedIndex) {
      final Integer result =
        (normalizedIndex == normalized.length()) ?
        tokenizer.getText().length() :
        norm2orig.get(normalizedIndex);

      return result == null ? -1 : result;
    }

    public final int getNormalizedIndex(int originalIndex) {
      int result = -1;

      for (Map.Entry<Integer, Integer> entry : norm2orig.entrySet()) {
        final int normIdx = entry.getKey();
        final int origIdx = entry.getValue();

        if (originalIndex == origIdx) {
          // maps to normalized char
          result = normIdx;
          break;
        }
        else if (originalIndex > origIdx) {
          // maps back to normalized white (break)
          result = normIdx - 1;
          break;
        }
      }

      return result;
    }

    /**
     * Determine whether there is a break at the given index.
     */
    public final boolean isBreak(int normalizedIndex) {
      return !norm2orig.containsKey(normalizedIndex);
    }

    /** Get the normalized break positions (not including string start or end). */
    public final TreeSet<Integer> getBreaks() {
      return breaks;
    }

    /** Lowercase this instance's normalized chars. */
    public final void toLowerCase() {
      final String newNorm = normalized.toString().toLowerCase();
      this.normalized.setLength(0);
      this.normalized.append(newNorm);
      this._nchars = null;
    }


    /**
     * Build the next normalized chars from the given token using
     * the "appendX" method calls.
     */
    protected void updateWithToken(Token token) {
      final String tokenText = lowerCaseFlag ? token.getText().toLowerCase() : token.getText();
      appendNormalizedText(token.getStartIndex(), tokenText, true);
    }

    /**
     * Append each normalized character originally starting at startIdx.
     */
    protected final void appendNormalizedText(int startIdx, String normalizedTokenText) {
      appendNormalizedText(startIdx, normalizedTokenText, true);
    }

    /**
     * Append each normalized character originally starting at startIdx.
     */
    protected final void appendNormalizedText(int startIdx, String normalizedTokenText, boolean addWhite) {
      final int len = normalizedTokenText.length();
      for (int i = 0; i < len; ++i) {
        final char c = normalizedTokenText.charAt(i);
        appendNormalizedChar(startIdx++, c, addWhite && i == 0);
      }
    }

    /**
     * Append the normalized character originally starting at origIdx.
     */
    protected final void appendNormalizedChar(int origIdx, char c, boolean addWhite) {
      int normIdx = normalized.length();
      if (normIdx > 0 && addWhite) {
        normalized.append(' ');
        breaks.add(normIdx++);
      }
      norm2orig.put(normIdx, origIdx);
      normalized.append(c);
      _nchars = null;
    }

    /**
     * Append the normalized characters all expanding from the originalIdx.
     */
    protected final void appendExpandedText(int origIdx, String chars) {
      appendExpandedText(origIdx, chars, true);
    }

    /**
     * Append the normalized characters all expanding from the originalIdx.
     */
    protected final void appendExpandedText(int origIdx, String chars, boolean addWhite) {
      final int len = chars.length();
      for (int i = 0; i < chars.length(); ++i) {
        final char c = chars.charAt(i);
        appendNormalizedChar(origIdx, c, addWhite && i == 0);
      }
    }
  }
}
