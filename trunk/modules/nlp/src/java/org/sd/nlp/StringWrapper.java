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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Thread-safe wrapper around a string to make normalization, tokenization,
 * and comparisons more efficient.
 * <p>
 * @author Spence Koehler
 */
public class StringWrapper {

  public final String string;
  private BreakStrategy breakStrategy;

  private int[] codePoints;
  private Map<Integer, Map<Integer, SubString>> s2e2ss;
  private Map<String, Boolean> flags;
  private Break[] _breaks;
  private int _numWords;
  private final Object numWordsMutex = new Object();

  public StringWrapper(String string) {
    this(string, null);
  }

  public StringWrapper(String string, BreakStrategy breakStrategy) {
    this.string = string;
    this.breakStrategy = (breakStrategy == null) ? GeneralBreakStrategy.getInstance() : breakStrategy;

    this.codePoints = StringUtil.toCodePoints(string);
    this.s2e2ss = new TreeMap<Integer, Map<Integer, SubString>>();
    this._breaks = null;  // lazy load
    this._numWords = -1;
  }

  public StringWrapper(int[] codePoints, String string, BreakStrategy breakStrategy) {
    this.string = string;
    this.breakStrategy = breakStrategy;
    this.codePoints = codePoints;

    this.s2e2ss = new TreeMap<Integer, Map<Integer, SubString>>();
    this._breaks = null;  // lazy load
    this._numWords = -1;
  }

  public BreakStrategy getBreakStrategy() {
    return breakStrategy;
  }

  public final int length() {
    return codePoints.length;
  }

  public final int[] getCodePoints() {
    return codePoints;
  }

  public final int getCodePoint(int index) {
    return index < codePoints.length ? codePoints[index] : 0;
  }

  public final Break getBreak(int index) {
    final Break[] breaks = getBreaks();
    return breaks[index];
  }

  public final SubString getSubString(int startPos) {
    return getSubString(startPos, codePoints.length);
  }

  public final SubString getSubString(int startPos, int endPos) {
    if (startPos >= endPos || startPos < 0 || endPos > codePoints.length) return null;

    SubString subString = null;

    synchronized (s2e2ss) {
      Map<Integer, SubString> e2ss = s2e2ss.get(startPos);
      if (e2ss == null) {
        e2ss = new TreeMap<Integer, SubString>();
        s2e2ss.put(startPos, e2ss);
      }
      subString = e2ss.get(endPos);
      if (subString == null) {
        subString = new SubString(this, startPos, endPos, buildString(startPos, endPos));
        e2ss.put(endPos, subString);
      }
    }

    return subString;
  }

  public final String getNormalizedString(int startPos, int endPos, AbstractNormalizer normalizer) {
    final SubString subString = getSubString(startPos, endPos);
    return (subString == null) ? null : subString.getNormalizedString(normalizer);
  }


  /**
   * Get the the start index for a substring after the given substring.
   */
  public final int getNextStartIndex(SubString subString) {
    return getNextStartIndex(subString.endPos);
  }

  /**
   * Get the start index for a substring at or after the given position.
   */
  public final int getNextStartIndex(int fromPos) {
    if (fromPos < 0 || fromPos == codePoints.length) return -1;  // there isn't another start index

    final Break[] breaks = getBreaks();
    while (fromPos < breaks.length && breaks[fromPos].skip()) ++fromPos;
    
    return fromPos == codePoints.length ? -1 : fromPos;
  }

  public final int numWords() {
    if (_numWords < 0) {
      synchronized (numWordsMutex) {
        _numWords = 0;
        for (SubString nextWord = getShortestSubString(0); nextWord != null; nextWord = nextWord.getNextShortestSubString()) {
          ++_numWords;
        }
      }
    }
    return _numWords;
  }

  /**
   * Get the index of the end of the substring prior to that which contains
   * or ends with fromIndex.
   */
  public final int getPrevEndIndex(int fromIndex) {
    // fromIndex may be the end or the beginning of (or within) a substring.
    // get to the end of the previous substring.

    if (fromIndex <= 1) return -1;  // there isn't a previous end index

    final Break[] breaks = getBreaks();

    --fromIndex;
    while (fromIndex >= 0 && breaks[fromIndex] == Break.NONE) --fromIndex;  // get to the previous break
    if (fromIndex < 0) return fromIndex;

    // skip over breaks.
    Break lastBreak = null;
    for (; fromIndex >= 0; --fromIndex) {
      final Break curBreak = breaks[fromIndex];
      // if a soft split comes right after a soft full, stop at the soft full.
      if (curBreak == Break.SOFT_SPLIT && lastBreak == Break.SOFT_FULL) {
        return fromIndex + 1;
      }
      if (!curBreak.skip()) break;
      lastBreak = curBreak;
    }

    if (fromIndex < 0) return fromIndex;

    int result = (breaks[fromIndex] == Break.SOFT_SPLIT) ? fromIndex : fromIndex + 1;

    return result;
  }

  /**
   * Get the longest subString starting at (or after) the given index.
   *
   * @param index          the index to start from.
   * @param maxSoftBreaks  the maximum number of soft breaks (i.e. max number of words). 0 for no limit.
   *
   * @return a subString or null.
   */
  public final SubString getLongestSubString(int index, int maxSoftBreaks) {
    SubString result = null;
    if (index == codePoints.length || index < 0) return result;

    final int startIndex = getNextStartIndex(index);
    if (startIndex < 0) return result;

    final Break[] breaks = getBreaks();
    
    // go forward over max soft breaks or until a hard break
    int numSoftFulls = (maxSoftBreaks <= 0) ? maxSoftBreaks - 1 : 0;
    int softFullInc = (maxSoftBreaks <= 0) ? 0 : 1;
    int endIndex = startIndex + 1;
    for (; endIndex < breaks.length && numSoftFulls < maxSoftBreaks; ++endIndex) {
      final Break curBreak = breaks[endIndex];
      if (maxSoftBreaks > 0 && curBreak == Break.SOFT_FULL) numSoftFulls += softFullInc;
      else if (curBreak == Break.HARD) break;
    }

    // come back over spaces
    for (; endIndex > startIndex + 1; --endIndex) {
      if (codePoints[endIndex - 1] != ' ') break;
    }

    result = getSubString(startIndex, endIndex);

    return result;
  }

  /**
   * Get the shortest subString starting at (or after) the given index.
   */
  public final SubString getShortestSubString(int index) {
    SubString result = null;
    if (index == codePoints.length) return result;

    final int startIndex = getNextStartIndex(index);
    if (startIndex < 0) return result;

    final Break[] breaks = getBreaks();

    int endIndex = startIndex + 1;
    for (; endIndex < breaks.length; ++endIndex) {
      if (breaks[endIndex].breaks()) break;
    }
    result = getSubString(startIndex, endIndex);

    return result;
  }

  /**
   * Get a longer substring (over a soft break) starting with the given
   * substring or null (if run into a hard break).
   */
  public final SubString getLongerSubString(SubString subString) {
    SubString result = null;
    if (subString.endPos == codePoints.length) return result;   // at end of string

    final Break[] breaks = getBreaks();
    final Break endBreak = breaks[subString.endPos];
    if (endBreak == Break.HARD) return result;  // can't grow beyond a hard break

    int endIndex = getNextStartIndex(subString.endPos) + 1;
    if (endIndex <= 0) return result;

    for (; endIndex < breaks.length; ++endIndex) {
      if (breaks[endIndex].breaks()) break;
    }
    result = getSubString(subString.startPos, endIndex);

    return result;
  }

  public final String getPostDelims(SubString subString) {
    final StringBuilder result = new StringBuilder();

    if (subString.endPos == codePoints.length) return result.toString();   // at end of string

    final Break[] breaks = getBreaks();
    final Break endBreak = breaks[subString.endPos];
    if (endBreak == Break.HARD) {
      result.appendCodePoint(codePoints[subString.endPos]);
      return result.toString();  // can't grow beyond a hard break
    }

    int endIndex = getNextStartIndex(subString.endPos);
    if (endIndex < 0) return result.toString();

    for (int i = subString.endPos; i < endIndex; ++i) {
      result.appendCodePoint(codePoints[i]);
    }

    return result.toString();
  }

  /**
   * Shorten the given subString if possible.
   */
  public final SubString getShorterSubString(SubString subString) {
    SubString result = null;

    int endIndex = getPrevEndIndex(subString.endPos);
    if (endIndex > subString.startPos) {
      result = getSubString(subString.startPos, endIndex);
    }

    return result;
  }

  public final boolean hasHardBreak(SubString subString) {
    final Break[] breaks = getBreaks();
    for (int i = subString.endPos - 1; i >= subString.startPos; --i) {
      if (breaks[i] == Break.HARD) return true;
    }
    return false;
  }

  private final String buildString(int startPos, int endPos) {
    final StringBuilder builder = new StringBuilder();
    for (int i = startPos; i < endPos; ++i) {
      builder.appendCodePoint(codePoints[i]);
    }
    return builder.toString();
  }

  public final Break[] getBreaks() {
    if (_breaks == null) {
      _breaks = breakStrategy.computeBreaks(codePoints);
    }
    return _breaks;
  }

  public final boolean getFlag(String flag) {
    Boolean result = null;

    if (flags != null) {
      result = flags.get(flag);
    }

    return result == null ? false : result;
  }

  public final void setFlag(String flag, boolean value) {
    if (flags == null) {
      flags = new HashMap<String, Boolean>();
    }

    flags.put(flag, value);
  }

  public final class SubString {

    public final StringWrapper stringWrapper;
    public final int startPos;
    public final int endPos;
    public final String originalSubString;

    private Map<AbstractNormalizer, String> n2ns;
    private Map<AbstractNormalizer, char[]> n2nc;
    private char[] _originalChars;

    private Categories categories;
    private boolean definitive;
    private int _numWords;
    private int _wordNum;
    private final Object categoryMutex = new Object();
    private final Object numWordsMutex = new Object();
    private final Object wordNumMutex = new Object();

    private Map<String, String> attributes;

    SubString(StringWrapper stringWrapper, int startPos, int endPos, String originalSubString) {
      this.stringWrapper = stringWrapper;
      this.startPos = startPos;
      this.endPos = endPos;
      this.originalSubString = originalSubString;
      this.n2ns = new HashMap<AbstractNormalizer, String>();
      this.n2nc = new HashMap<AbstractNormalizer, char[]>();
      this._numWords = -1;
      this._wordNum = -1;
      this.categories = null;
      this.definitive = false;
      this.attributes = null;
    }

    /**
     * Get the number of characters in this substring.
     */
    public int length() {
      return endPos - startPos;
    }

    /**
     * Get the number of words in this substring.
     */
    public int getNumWords() {
      if (_numWords < 0) {
        synchronized (numWordsMutex) {
          _numWords = computeNumWords(this.startPos, this.endPos);
        }
      }
      return _numWords;
    }

    public int getStartWordPosition() {
      if (_wordNum < 0) {
        final Break[] breaks = getBreaks();
        int count = 0;
        for (int i = 0; i < this.startPos; ++i) {
          if (breaks[i].breaks()) ++count;
        }
        synchronized (wordNumMutex) {
          _wordNum = count;
        }
      }
      return _wordNum;
    }

    private final int computeNumWords(int startPos, int endPos) {
      int result = 0;
      for (SubString nextWord = stringWrapper.getShortestSubString(startPos); nextWord != null;
           nextWord = nextWord.getLongerSubString()) {
        if (nextWord.endPos > endPos) break;
        ++result;
        if (nextWord.endPos == endPos) break;
      }
      return result;
    }

    public String getNormalizedString(AbstractNormalizer normalizer) {
      if (normalizer == null) return originalSubString;

      String result = n2ns.get(normalizer);
      if (result == null) {
        result = normalizer.normalize(this).getNormalized();
        n2ns.put(normalizer, result);
      }
      return result;
    }

    public char[] getNormalizedChars(AbstractNormalizer normalizer) {
      if (normalizer == null) return getOriginalChars();

      char[] result = n2nc.get(normalizer);
      if (result == null) {
        final String string = getNormalizedString(normalizer);
        result = string.toCharArray();
        n2nc.put(normalizer, result);
      }
      return result;
    }

    public char[] getOriginalChars() {
      if (_originalChars == null) {
        _originalChars = originalSubString.toCharArray();
      }
      return _originalChars;
    }

    public void addCategory(Category category) {
      synchronized (categoryMutex) {
        if (categories == null) categories = new Categories();
        categories.addType(category);
      }
    }

    public void addCategories(Categories categories) {
      synchronized (categoryMutex) {
        if (this.categories == null) this.categories = new Categories();
        this.categories.addAllTypes(categories);
      }
    }

    public Categories getCategories() {
      return categories;
    }

    public int getNumCategories() {
      return (categories == null) ? 0 : categories.size();
    }

    /**
     * Get the definitive marker on this substring {see setDefinitive}
     */
    public boolean hasDefinitiveDefinition() {
      return definitive;
    }

    /**
     * Set a marker on this substring that indicates that it has been fully
     * defined (or not) and no other definitions need to be pursued.
     */
    public void setDefinitive(boolean definitive) {
      this.definitive = definitive;
    }

    public final boolean hasHardBreak() {
      return stringWrapper.hasHardBreak(this);
    }

    public final SubString getShorterSubString() {
      return stringWrapper.getShorterSubString(this);
    }

    public final SubString getLongerSubString() {
      return stringWrapper.getLongerSubString(this);
    }

    public final String getPostDelims() {
      return stringWrapper.getPostDelims(this);
    }

    public final SubString getNextLongestSubString(int maxSoftBreaks) {
      return stringWrapper.getLongestSubString(stringWrapper.getNextStartIndex(endPos), maxSoftBreaks);
    }

    public final SubString getNextShortestSubString() {
      return stringWrapper.getShortestSubString(stringWrapper.getNextStartIndex(endPos));
    }

    public final SubString getRemainingSubStringAfter() {
      return stringWrapper.getSubString(endPos);
    }

    public final void setAttribute(String key, String value) {
      if (attributes == null) attributes = new LinkedHashMap<String, String>();
      attributes.put(key, value);
    }

    public final void addAttribute(String key, String value) {
      if (attributes == null) attributes = new LinkedHashMap<String, String>();
      String curValue = attributes.get(key);
      if (curValue != null) value = curValue + "," + value;
      attributes.put(key, value);
    }

    public final String getAttribute(String key) {
      return (attributes ==  null) ? null : attributes.get(key);
    }

    public boolean equals(Object o) {
      boolean result = this == o;
      if (!result && o instanceof SubString) {
        final SubString other = (SubString)o;
        int myOHash = originalSubString.hashCode();
        int otherOHash = other.originalSubString.hashCode();

        if (myOHash == otherOHash) result = originalSubString.equals(other.originalSubString);

        if (!result) {
          result = n2ns.values().contains(other.originalSubString) || other.n2ns.values().contains(this.originalSubString);
        }
      }
      return result;
    }

    public int hashCode() {
      return originalSubString.hashCode() * 17 + n2ns.values().hashCode();
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.append('(').append(startPos).append(',').append(endPos).append(')').
        append(originalSubString);

      return result.toString();
    }
  }
}
