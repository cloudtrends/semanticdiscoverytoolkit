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


import org.sd.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Container for a normalized string mapping it back to its original form.
 * <p>
 * Typically, a Normalizer will create an appropriate GeneralNormalizedString, but
 * the "buildLowerCaseInstance" factory method is available for use in trivial
 * cases.
 *
 * @author Spence Koehler
 */
public final class GeneralNormalizedString implements NormalizedString {

  public static final GeneralNormalizedString EMPTY = new GeneralNormalizedString();

  public static final GeneralNormalizedString buildLowerCaseInstance(String string) {
    return (string == null) ? EMPTY : new GeneralNormalizedString(new StringWrapper(string), string.toLowerCase(), (int[])null, true);
  }

  private enum CharClass {UPPER, LOWER, DIGIT, ASIAN, OTHER};

  private StringWrapper fullOriginal;       // full original string
  private String normalized;         // normalized (sub)string
  private int[] n2oIndexes;          // normalized char index to original char index (based on full original string)
  private boolean splitOnCamelCase;  // flag for whether to split on camel-case
  private int nlen;                  // normalized (sub)string length
  private int olen;                  // original (sub)string length
  private int[] normalizedCodePoints;  // code points of normalized string
  private String _original;          // original (sub)string
  private char[] _nchars;            // normalized chars

  /**
   * Construct an empty normalized string.
   */
  public GeneralNormalizedString() {
    this.fullOriginal = null;
    this.normalized = "";
    this.n2oIndexes = null;
    this.nlen = 0;
    this.olen = 0;
    this.normalizedCodePoints = new int[0];
    this._original = null;
    this._nchars = null;
  }

  /**
   * Construct an instance where the string is its own normalization.
   */
  public GeneralNormalizedString(String string) {
    this(new StringWrapper(string), string, (int[])null, true);
  }

  /**
   * Construct a normalized string with the given data.
   */
  public GeneralNormalizedString(StringWrapper fullOriginal, String normalized, List<Integer> n2oIndexList) {
    this(fullOriginal, normalized, convert(n2oIndexList, normalized.length()), true);
  }

  /**
   * Construct a normalized string with the given data.
   */
  public GeneralNormalizedString(StringWrapper fullOriginal, String normalized, List<Integer> n2oIndexList, boolean splitOnCamelCase) {
    this(fullOriginal, normalized, convert(n2oIndexList, normalized.length()), splitOnCamelCase);
  }

  /**
   * Construct a normalized string with the given data.
   */
  public GeneralNormalizedString(StringWrapper fullOriginal, String normalized, int[] n2oIndexes, boolean splitOnCamelCase) {
    this.fullOriginal = fullOriginal;
    this.normalized = normalized;
    this.n2oIndexes = n2oIndexes;
    this.splitOnCamelCase = splitOnCamelCase;
    this.olen = fullOriginal.length();
    this.normalizedCodePoints = StringUtil.toCodePoints(normalized);
    this.nlen = normalizedCodePoints.length;
    this._original = null;
  }

// method for debugging
  protected final void dump() {
    System.out.println(normalized.length() + ": " + normalized);
    System.out.println(fullOriginal.string);

    System.out.println("ncp=" + normalizedCodePoints.length + ", n2o=" + n2oIndexes.length);

    for (int i = 0; i < nlen; ++i) {
      final int ncp = (i < normalizedCodePoints.length) ? normalizedCodePoints[i] : -1;
      final int n2o = (i < n2oIndexes.length) ? n2oIndexes[i] : -1;
      final int oi = getOriginalIndex(i);

      final int ocp = fullOriginal.getCodePoint(oi);

      System.out.println(i + ": " + ncp + "(" + ((char)ncp) + ") [" + n2o + "/" + oi + "] " + ocp + "(" + ((char)ocp) + ")");
    }
  }

  protected static final int[] convert(List<Integer> n2oIndexList, int len) {
    if (n2oIndexList == null) return null;
    final int[] n2oIndexes = new int[Math.max(len, n2oIndexList.size())];
    int pos = 0;
    for (Integer n2oIndex : n2oIndexList) {
      n2oIndexes[pos++] = n2oIndex;
    }
    return n2oIndexes;
  }

  /**
   * Set the split on camel case flag.
   */
  public final void setSplitOnCamelCase(boolean splitOnCamelCase) {
    this.splitOnCamelCase = splitOnCamelCase;
  }

  public final boolean getSplitOnCamelCase() {
    return splitOnCamelCase;
  }

  /**
   * Get the full original string.
   * <p>
   * Note that the normalized string may apply to only a portion of the full
   * original string.
   */
  public final StringWrapper getFullOriginal() {
    return fullOriginal;
  }

  /**
   * Get the length of the normalized string.
   */
  public final int getNormalizedLength() {
    return nlen;
  }

  /**
   * Get the normalized string.
   * <p>
   * Note that the normalized string may apply to only a portion of the full
   * original string.
   */
  public final String getNormalized() {
    return normalized;
  }

  /**
   * Get the normalized string from the start (inclusive) to end (exclusive).
   * <p>
   * Note that the normalized string may apply to only a portion of the full
   * original string.
   */
  public final String getNormalized(int startPos, int endPos) {
    final StringBuilder result = new StringBuilder();

    for (int i = startPos; i < endPos; ++i) {
      result.appendCodePoint(normalizedCodePoints[i]);
    }

    return result.toString();
  }

  /**
   * Get the original string that applies to the normalized string.
   */
  public final String getOriginal() {
    if (_original == null) {
      _original = getOriginal(0, nlen);
    }
    return _original;
  }

  /**
   * Get the original string that applies to the normalized string from the
   * given index for the given number of normalized characters.
   */
  public final String getOriginal(int normalizedStartIndex, int normalizedLength) {
    if (normalizedLength == 0) return null;

    String result = null;

    int oStart = getOriginalIndex(normalizedStartIndex);
    int oEnd = getOriginalIndex(normalizedStartIndex + normalizedLength - 1);
    final StringWrapper.SubString substring = fullOriginal.getSubString(oStart, oEnd + 1);
    if (substring != null) {
      result = substring.originalSubString;
    }

    return result;
  }

  public final StringWrapper.SubString asSubString() {
    return fullOriginal.getSubString(0);
  }

  public final int getOriginalIndex(int normalizedIndex) {
    int result = normalizedIndex;

    if (n2oIndexes != null) {
      result = normalizedIndex == nlen ? olen : n2oIndexes[normalizedIndex];
    }

    return result;
  }

  /**
   * Get a new normalized string for the portion of this normalized string
   * preceding the normalized start index (exclusive). Remove extra whitespace
   * at the end of the returned string.
   *
   * @return the preceding normalized string or null if empty (after skipping white).
   */
  public final GeneralNormalizedString getPreceding(int normalizedStartIndex) {
    return getPreceding(normalizedStartIndex, true);
  }

  /**
   * Get a new normalized string for the portion of this normalized string
   * preceding the normalized start index (exclusive). Remove extra whitespace
   * at the end of the returned string.
   *
   * @return the preceding normalized string or null if empty (after skipping white).
   */
  public final GeneralNormalizedString getPreceding(int normalizedStartIndex, boolean checkEndBreak) {
    GeneralNormalizedString result = null;

    // skip back until starting from an end break.
    while (normalizedStartIndex > 0 && (normalizedCodePoints[normalizedStartIndex] == ' ' || (checkEndBreak && !isEndBreak(normalizedStartIndex)))) --normalizedStartIndex;
    
    if (normalizedStartIndex > 0) {
      result = buildNormalizedString(0, normalizedStartIndex);
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
  public final int getPrecedingIndex(int normalizedPos, int numTokens) {
    int index = normalizedPos < 0 ? nlen : normalizedPos;

    int numStarts = 0;

    // skip back to the numTokens-th start break.
    for (; index > 0 && numStarts < numTokens; index = findPrecedingTokenStart(index)) {
      ++numStarts;
    }

    return index;
  }

  /**
   * Find the start of the token before the normalizedPos.
   * <p>
   * If normalizedPos is at a token start, the prior token will be returned;
   * otherwise, the start of the token of which normalizedPos is a part will
   * be returned.
   */
  public final int findPrecedingTokenStart(int normalizedPos) {
    for (normalizedPos--; normalizedPos > 0; --normalizedPos) {
      if (isLetterOrDigit(normalizedPos) && isStartBreak(normalizedPos)) break;
    }
    return normalizedPos;
  }

  /**
   * Get a new normalized string for the portion of this normalized string
   * following the normalized start index (inclusive). Remove extra whitespace
   * at the beginning of the returned string.
   *
   * @return the following normalized string or null if empty (after skipping white).
   */
  public final GeneralNormalizedString getRemaining(int normalizedStartIndex) {
    GeneralNormalizedString result = null;

    // skip back until starting from an end break.
    while (normalizedStartIndex < nlen && (normalizedCodePoints[normalizedStartIndex] == ' ' || !isStartBreak(normalizedStartIndex))) ++normalizedStartIndex;
    
    if (normalizedStartIndex < nlen) {
      result = buildNormalizedString(normalizedStartIndex, nlen);
    }

    return result;
  }

  /**
   * Build a normalized string from this using the given normalized index range.
   */
  public final GeneralNormalizedString buildNormalizedString(int normalizedStartIndex, int normalizedEndIndex) {
    final int normalizedLength = normalizedEndIndex - normalizedStartIndex;
    final StringWrapper newFullOriginal = fullOriginal;

    final String newNormalized = getNormalized(normalizedStartIndex, normalizedEndIndex);

    final int[] newN2oIndexes = new int[normalizedLength];
    for (int i = normalizedStartIndex; i < normalizedEndIndex; ++i) {
      newN2oIndexes[i - normalizedStartIndex] = n2oIndexes[i];
    }

    return new GeneralNormalizedString(newFullOriginal, newNormalized, newN2oIndexes, splitOnCamelCase);
  }

  /**
   * Lowercase the normalized string.
   */
  public final GeneralNormalizedString toLowerCase() {
    this.normalized = normalized.toLowerCase();
    this._nchars = null;  // reset
    return this;
  }

  /**
   * Get the normalized string's chars.
   */
  public final char[] getNormalizedChars() {
    if (_nchars == null) {
      _nchars = normalized.toCharArray();
    }
    return _nchars;
  }

  /**
   * Get the normalized char at the given (normalized) index.
   * <p>
   * NOTE: Bounds checking is left up to the caller.
   */
  public final char getNormalizedChar(int index) {
    final char[] nchars = getNormalizedChars();
    return nchars[index];
  }

  /**
   * Get the original code point corresponding to the normalized char at the
   * (normalized) index.
   * <p>
   * NOTE: Bounds checking is left up to the caller.
   */
  public final int getOriginalCodePoint(int nIndex) {
    int result = 0;

    try {
      result = fullOriginal.getCodePoint(n2oIndexes == null ? nIndex : n2oIndexes[nIndex]);
    }
    catch (ArrayIndexOutOfBoundsException e) {
      System.err.println("GeneralNormalizedString: original '" + fullOriginal.string + "', normalized '" + normalized + "' nIndex=" + nIndex);
      e.printStackTrace(System.err);
    }

    return result;
  }

  /**
   * Determine whether the original character corresponding to the normalized
   * index is a letter or digit.
   */
  public final boolean isLetterOrDigit(int nIndex) {
    final CharClass charClass = getCharClass(getOriginalCodePoint(nIndex));
    return (charClass != CharClass.OTHER);
  }

  /**
   * Get the ORIGINAL index of the first symbol (non-letter, digit, or white
   * character) prior to the NORMALIZED index in the full original string.
   *
   * @return -1 if no symbol is found or the index of the found symbol in the
   *         original input string.
   */
  public final int findPreviousSymbolIndex(int nIndex) {
    int oIndex = (n2oIndexes == null) ? nIndex : n2oIndexes[nIndex];
    for (--oIndex; oIndex >= 0; --oIndex) {
      final int cp = fullOriginal.getCodePoint(oIndex);
      if (cp != ' ' && !Character.isLetterOrDigit(cp)) break;
    }
    return oIndex;
  }

  public final boolean hasDigit(int nStartIndex, int nEndIndex) {
    for (int i = nStartIndex; i < nEndIndex; ++i) {
      final int c = normalizedCodePoints[i];
      if (c <= '9' && c >= '0') return true;
    }
    return false;
  }

  public final int numWords(int nStartIndex, int nEndIndex) {
    // don't count first space
    while (nStartIndex < nEndIndex && normalizedCodePoints[nStartIndex] == ' ') ++nStartIndex;

    if (nEndIndex <= nStartIndex) return 0;
    int result = 1;
    int c = 0;

    for (int i = nStartIndex; i < nEndIndex; ++i) {
      c = normalizedCodePoints[i];
      if (c == ' ') ++result;
    }

    if (c == ' ') --result;  // don't count last space

    return result;
  }

  /**
   * Determine whether there is a break before the normalized startIndex.
   */
  public final boolean isStartBreak(int startIndex) {
    if (startIndex <= 0) return true;

    final CharClass prevClass = getCharClass(getOriginalCodePoint(startIndex - 1));
    final CharClass startClass = getCharClass(getOriginalCodePoint(startIndex));

    boolean result = (startClass == CharClass.ASIAN);  // break at every asian char

    if (!result) {
      if (prevClass != startClass) {  // usually signifies a start break
        // except when dealing with capitalized words.
        result = !(prevClass == CharClass.UPPER && startClass == CharClass.LOWER);
      }
    }

    return result;
  }

  /**
   * Determine whether there is a break before the normalized endIndex.
   */
  public final boolean isEndBreak(int endIndex) {
    if (endIndex >= nlen) return true;

    final CharClass nextClass = getCharClass(getOriginalCodePoint(endIndex));
    final CharClass endClass = getCharClass(getOriginalCodePoint(endIndex - 1));

    // non-equal char classes except upper followed by lower.
    boolean result = nextClass != endClass;

    if (result) {
      if (endClass == CharClass.UPPER && nextClass == CharClass.LOWER) {
        result = false;
      }
    }
    else {
      result = endClass == CharClass.ASIAN;
    }

    return result;
  }

  /**
   * Get the normalized index that best corresponds to the original index.
   */
  public final int getNormalizedIndex(int originalIndex) {
    int result = -1;

    for (int nIndex = 0; nIndex < n2oIndexes.length; ++nIndex) {
      final int oIndex = n2oIndexes[nIndex];
      if (oIndex >= originalIndex) {
        result = nIndex;
        break;
      }
    }

    return result;
  }

  private final CharClass getCharClass(int cp) {
    CharClass result = null;

    if (StringUtil.isAsianCodePoint(cp)) {
      result = CharClass.ASIAN;
    }
    else if (!splitOnCamelCase && Character.isLetter(cp)) {
      result = CharClass.LOWER;
    }
    else if (Character.isUpperCase(cp)) {
      result = CharClass.UPPER;
    }
    else if (Character.isLowerCase(cp)) {
      result = CharClass.LOWER;
    }
    else if (Character.isDigit(cp)) {
      result = CharClass.DIGIT;
    }
    else {
      result = CharClass.OTHER;
    }

    return result;
  }

  /**
   * Get this instance as a string.
   * <p>
   * The normalized string is returned.
   */
  public final String toString() {
    return normalized;
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
   * Split into normalized token strings.
   */
  public String[] split() {
    final List<String> result = new ArrayList<String>();
    for (NormalizedToken token = getToken(0, true); token != null; token = token.getNext(true)) {
      result.add(token.getNormalized());
    }
    return result.toArray(new String[result.size()]);
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
   * Get the token starting from the start position, optionally skipping to a
   * start break first.
   *
   * @return the token or null if there are no tokens to get.
   */
  public NormalizedToken getToken(int startPos, boolean skipToBreak) {
    NormalizedToken result = null;

    if (skipToBreak) {
      while (startPos < nlen && (!isLetterOrDigit(startPos) || !isStartBreak(startPos))) ++startPos;
    }

    if (startPos < nlen) {
      int endPos = startPos + 1;
      while (endPos < nlen && normalizedCodePoints[endPos] != ' ' && !isEndBreak(endPos)) ++endPos;

      result = new MyToken(this, startPos, endPos);

//System.out.println("[" + startPos + "," + endPos + "] n=" + result.getNormalized() + " (o=" + result.getOriginal() + ")");
    }

    return result;
  }

  /**
   * Get the token after the given token, optionally skipping to a start
   * break first.
   */
  public NormalizedToken getNextToken(NormalizedToken curToken, boolean skipToBreak) {
    return ((MyToken)curToken).getNext(skipToBreak);
  }


  /**
   * Container class for a normalized string token.
   */
  public final class MyToken extends NormalizedToken {
    private GeneralNormalizedString nString;
    private MyToken _next;
    private boolean _gotAlt;

    public MyToken(GeneralNormalizedString nString, int startPos, int endPos) {
      super(nString, startPos, endPos);
      this.nString = nString;
      this._next = null;
      this._gotAlt = false;
    }

    private MyToken(GeneralNormalizedString nString, int startPos, int endPos, MyToken next) {
      this(nString, startPos, endPos);
      this._next = next;
    }

    /**
     * Get this token's normalized string.
     */
    public GeneralNormalizedString getGeneralNormalizedString() {
      return nString;
    }

    /**
     * Get the normalized token following this token, optionally skipping to a
     * start break first.
     */
    public NormalizedToken getNext(boolean skipToBreak) {
      NormalizedToken result = null;

      if (nString.splitOnCamelCase) {
        if (_next != null) {
          // this is an alt token. time to get deferred from _next
          result = _next.doGetNext(skipToBreak);
          //System.out.println("next(" + getNormalized() + ")=" + (result != null ? result.getNormalized() : "<null>") + " [from alt]");
        }
        else if (hasAlt()) {
          // defer next until after getting the alt token
          result = getAltToken();
          //System.out.println("next(" + getNormalized() + ")=" + (result != null ? result.getNormalized() : "<null>") + " [to alt]");
        }
      }

      if (result == null) {
        result = doGetNext(skipToBreak);
        //System.out.println("next(" + getNormalized() + ")=" + (result != null ? result.getNormalized() : "<null>") + " [no alt]");
      }

      return result;
    }

    private final NormalizedToken doGetNext(boolean skipToBreak) {
      return nString.getToken(getEndPos(), skipToBreak);
    }

    /**
     * Determine whether this token has a meaningful alternate
     * presentation.
     * <p>
     * This occurs for initial partial camel-cased tokens, where the
     * alternate is the full word as if camel-casing had not been
     * applied to split the token.
     */
    public boolean hasAlt() {
      final int startPos = getStartPos();
      final int endPos = getEndPos();

      // true if startPos is a letter and its prior is not and
      // after endPos is a letter.
      return
        (Character.isLetter(getNormalizedChar(startPos)) &&
         (startPos == 0 || !Character.isLetter(getNormalizedChar(startPos - 1))) &&
         (endPos < nlen && Character.isLetter(getNormalizedChar(endPos))));
    }

    /**
     * Get this token's alternate presentation, which is the token's
     * start position up to a non-char.
     * <p>
     * This is useful for initial partial camel-cased tokens, where
     * the alternate is the full word as if camel-casing had not been
     * applied to split the token. But there is no limitation on
     * accessing the alternate form.
     */
    public NormalizedToken getAltToken() {
      int newEndPos = getEndPos();
      while (newEndPos < nlen && Character.isLetter(getNormalizedChar(newEndPos))) ++newEndPos;
      return new MyToken(nString, getStartPos(), newEndPos, this);
    }

    /**
     * Get this token's alternate presentation, which is the token's
     * start position up to a non-char.
     * <p>
     * This is useful for initial partial camel-cased tokens, where
     * the alternate is the full word as if camel-casing had not been
     * applied to split the token. But there is no limitation on
     * accessing the alternate form.
     */
    public String getAlt() {
      return getAltToken().getNormalized();
    }
  }


  public static void main(String[] args) {
    for (String arg : args) {
      final GeneralNormalizedString nstring = GeneralNormalizedString.buildLowerCaseInstance(arg);
      final String[] pieces = nstring.split();

      final org.sd.util.LineBuilder builder = new org.sd.util.LineBuilder();
      builder.append(arg);
      for (String piece : pieces) builder.append(piece);

      System.out.println(builder.toString());
    }
  }
}
