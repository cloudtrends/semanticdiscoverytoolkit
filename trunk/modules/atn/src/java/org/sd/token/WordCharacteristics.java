/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.util.HashMap;
import java.util.TreeSet;
import java.util.Map;
import java.util.Set;

/**
 * Container for word (token) characteristics.
 * <p>
 * @author Spence Koehler
 */
public class WordCharacteristics {

  public enum Type { LOWER, UPPER, DIGIT, OTHER };


  private static final boolean ALL_UPPER_WITH_SYMBOLS_IS_ALL_CAPS = true;


  private String word;
  private int len;
  private Map<Integer, Type> pos2type;
  private Map<Type, Integer> type2maxConsecutive;

  private TreeSet<Integer> lowers;
  private TreeSet<Integer> uppers;
  private TreeSet<Integer> digits;
  private TreeSet<Integer> others;

  private KeyLabel _keyLabel;
  private String _startDelims;
  private String _endDelims;

  private final Object keyLabelMutex = new Object();

  public WordCharacteristics(String word) {
    this.word = word;
    this.len = word.length();
    this.pos2type = new HashMap<Integer, Type>();
    this.type2maxConsecutive = new HashMap<Type, Integer>();
    this.lowers = null;
    this.uppers = null;
    this.digits = null;
    this.others = null;
    this._startDelims = null;
    this._endDelims = null;

    Type curType = null;
    Type lastType = null;
    int numConsecutive = 1;

    for (int i = 0; i < len; ++i) {
      final char c = word.charAt(i);
      boolean isOther = false;

      if (Character.isLetterOrDigit(c)) {
        if (Character.isDigit(c)) {
          pos2type.put(i, curType = Type.DIGIT);
          if (digits == null) digits = new TreeSet<Integer>();
          digits.add(i);
        }
        else if (Character.isLowerCase(c)) {
          pos2type.put(i, curType = Type.LOWER);
          if (lowers == null) lowers = new TreeSet<Integer>();
          lowers.add(i);
        }
        else if (Character.isUpperCase(c)) {
          pos2type.put(i, curType = Type.UPPER);
          if (uppers == null) uppers = new TreeSet<Integer>();
          uppers.add(i);
        }
        else {
          isOther = true;
        }
      }
      else {
        isOther = true;
      }

      if (isOther) {
        pos2type.put(i, curType = Type.OTHER);
        if (others ==  null) others = new TreeSet<Integer>();
        others.add(i);
      }

      if (curType == lastType) {
        ++numConsecutive;
      }
      else {
        tally(curType, numConsecutive);
        lastType = curType;
        numConsecutive = 1;
      }
    }
    tally(curType, numConsecutive);
  }

  private final void tally(Type curType, int numConsecutive) {
    if (numConsecutive > 0) {
      Integer curMax = type2maxConsecutive.get(curType);
      type2maxConsecutive.put(curType, curMax == null ? numConsecutive : Math.max(curMax, numConsecutive));
    }
  }

  public String getWord() {
    return word;
  }

  public int len() {
    return len;
  }

  public KeyLabel getKeyLabel() {
    synchronized (keyLabelMutex) {
      if (_keyLabel == null) {
        _keyLabel = determineKeyLabel();
      }
    }
    return _keyLabel;
  }

  public Type getType(int pos) {
    return pos2type.get(pos);
  }

  public Type getFirstType(boolean skipOthers) {
    final int idx = skipOthers ? skip(Type.OTHER, 0) : 0;
    return idx == len ? Type.OTHER : pos2type.get(idx);
  }

  public Type getLastType(boolean skipOthers) {
    final int idx = skipOthers ? skipBack(Type.OTHER, len - 1) : len - 1;
    return idx < 0 ? Type.OTHER : pos2type.get(idx);
  }

  public TreeSet<Integer> getLowers() {
    return lowers;
  }

  public TreeSet<Integer> getUppers() {
    return uppers;
  }

  public TreeSet<Integer> getDigits() {
    return digits;
  }

  public TreeSet<Integer> getOthers() {
    return others;
  }

  public boolean hasStartDelims() {
    return others != null && others.contains(0);
  }

  public String getStartDelims() {
    if (_startDelims == null) {
      _startDelims = buildStartDelims();
    }
    return _startDelims;
  }

  public boolean hasEndDelims() {
    return others != null && others.contains(len - 1);
  }

  public String getEndDelims() {
    if (_endDelims == null) {
      _endDelims = buildEndDelims();
    }
    return _endDelims;
  }

  public boolean hasLower() {
    return lowers != null;
  }

  public boolean hasUpper() {
    return uppers != null;
  }

  public boolean hasDigit() {
    return digits != null;
  }

  public boolean firstIsLower() {
    return firstIsLower(false);
  }

  public boolean firstIsLower(boolean skipOthers) {
    boolean result = false;

    if (lowers != null) {
      final int idx = skipOthers ? skip(Type.OTHER, 0) : 0;
      result = lowers.contains(0);
    }

    return result;
  }

  public boolean firstIsUpper() {
    return firstIsUpper(false);
  }

  public boolean firstIsUpper(boolean skipOthers) {
    boolean result = false;

    if (uppers != null) {
      final int idx = skipOthers ? skip(Type.OTHER, 0) : 0;
      result = uppers.contains(0);
    }

    return result;
  }

  /**
   * Check to see if there is an upper after the first, regardless of whether
   * the first is upper.
   */
  public boolean laterIsUpper() {
    return laterIsUpper(false);
  }

  /**
   * Check to see if there is an upper after the first, regardless of whether
   * the first is upper.
   */
  public boolean laterIsUpper(boolean skipOthers) {
    boolean result = false;

    if (uppers != null) {
      final int minSize = firstIsUpper(skipOthers) ? 2 : 1;
      result =uppers.size() >= minSize;
    }

    return result;
  }

  public boolean hasOther() {
    return others != null;
  }

  /**
   * Determine whether the word appears to be all caps.
   * <p>
   * If tolerant, result is true if num uppers &gt; num lowers; otherwise
   * must have uppers and no lowers.
   */
  public boolean isAllCaps(boolean tolerant) {
    boolean result = uppers != null;

    if (result && lowers != null) {
      if (tolerant) {
        result = uppers.size() > lowers.size();
      }
      else {
        result = false;
      }
    }

    return result;
  }

  public int getNumLetters() {
    int numLetters = 0;

    if (lowers != null) {
      numLetters += lowers.size();
    }

    if (uppers != null) {
      numLetters += uppers.size();
    }

    return numLetters;
  }

  public int getNumDigits() {
    int numDigits = 0;

    if (digits != null) {
      numDigits += digits.size();
    }

    return numDigits;
  }

  /**
   * Find the index of characters after all of the given type from the given
   * start index.
   * <p>
   * Note that if the type does not apply at the startIdx, then startIdx will be returned.
   */
  public int skip(Type type, int startIdx) {
    while (startIdx < len && pos2type.get(startIdx) == type) {
      ++startIdx;
    }
    return startIdx;
  }

  /**
   * Find the index of characters before all of the given type from input's end.
   * <p>
   * Note that if the type does not apply at the endIdx, then endIdx will be
   * returned and if the type never changes, -1 will be returned.
   */
  public int skipBack(Type type) {
    return skipBack(type, len - 1);
  }

  /**
   * Find the index of characters before all of the given type from the given
   * end index.
   * <p>
   * Note that if the type does not apply at the endIdx, then endIdx will be
   * returned and if the type never changes, -1 will be returned.
   */
  public int skipBack(Type type, int endIdx) {
    if (endIdx > len - 1) endIdx = len - 1;
    while (endIdx >= 0 && pos2type.get(endIdx) == type) {
      --endIdx;
    }
    return endIdx;
  }

  /**
   * Find the first index that has the given type from startIdx.
   * <p>
   * @return the index at which the type occurs or 'len' if it doesn't.
   */
  public int skipUntil(Type type, int startIdx) {
    while (startIdx < len && pos2type.get(startIdx) != type) {
      ++startIdx;
    }
    return startIdx;
  }

  /**
   * Find the index of characters at or before input's end that has the
   * given type.
   * <p>
   * @return the index at which the type occurs or -1 if it doesn't.
   */
  public int skipBackUntil(Type type) {
    return skipBackUntil(type, len - 1);
  }

  /**
   * Find the index of characters at or before the given endIdx that has the
   * given type.
   * <p>
   * @return the index at which the type occurs or -1 if it doesn't.
   */
  public int skipBackUntil(Type type, int endIdx) {
    if (endIdx > len - 1) endIdx = len - 1;
    while (endIdx >= 0 && pos2type.get(endIdx) != type) {
      --endIdx;
    }
    return endIdx;
  }

  private final KeyLabel determineKeyLabel() {
    KeyLabel result = KeyLabel.Special;

    if (hasDigit()) {
      if (hasLower() || hasUpper() || hasOther()) {
        result = KeyLabel.MixedNumber;
      }
      else {
        result = KeyLabel.Number;
      }
    }
    else if (hasLower()) {
      if (len() == 1) {
        result = KeyLabel.SingleLower;
      }
      else if (!hasUpper()) {
        result = KeyLabel.AllLower;
      }
      else if (firstIsLower()) {
        result = KeyLabel.LowerMixed;
      }
      else if (firstIsUpper()) {
        if (!laterIsUpper()) {
          result = KeyLabel.Capitalized;
        }
        else {  // hasUpper && !firstIsLower && laterIsUpper
          result = KeyLabel.UpperMixed;
        }
      }
      // otherwise, special
    }
    else if (hasUpper()) {
      if (len() == 1) {
        result = KeyLabel.SingleUpper;
      }

      // NOTE: hasLower=false && hasDigit=false here
      else if (ALL_UPPER_WITH_SYMBOLS_IS_ALL_CAPS || !hasOther()) {
        result = KeyLabel.AllCaps;
      }
      // otherwise, upper w/symbols is special
    }

    // Treat strings like "J.J." as SingleUpper instead of AllCaps
    if (result == KeyLabel.AllCaps) {
      final Integer max = type2maxConsecutive.get(Type.UPPER);
      if (max != null && max == 1) {
        result = KeyLabel.SingleUpper;
      }
    }
    // Treat strings like "j.j." as SingleLower instead of AllLower
    else if (result == KeyLabel.AllLower) {
      final Integer max = type2maxConsecutive.get(Type.LOWER);
      if (max != null && max == 1) {
        result = KeyLabel.SingleLower;
      }
    }

    return result;
  }

  private final String buildStartDelims() {
    String result = null;

    if (hasStartDelims()) {
      final int nonOther = skip(Type.OTHER, 0);
      result = word.substring(0, nonOther);
    }

    return result == null ? "" : result;
  }

  private final String buildEndDelims() {
    String result = null;

    if (hasEndDelims()) {
      final int nonOther = skipBack(Type.OTHER);
      result = word.substring(nonOther + 1);
    }

    return result == null ? "" : result;
  }
}
