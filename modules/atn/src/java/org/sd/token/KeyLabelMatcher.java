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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility for analyzing keyLabel sequences for similarity.
 * <p>
 * @author Spence Koehler
 */
public class KeyLabelMatcher {
  
  private Map<KeyLabel, String> keyLabel2pattern;
  private Map<KeyLabel, Set<KeyLabel>> keyLabel2matches;

  public static final KeyLabel[] KEY_LABELS = new KeyLabel[] {
    KeyLabel.AllLower,
    KeyLabel.Capitalized,
    KeyLabel.AllCaps,
    KeyLabel.SingleLower,
    KeyLabel.SingleUpper,
    KeyLabel.LowerMixed,
    KeyLabel.UpperMixed,
    KeyLabel.Number,
    KeyLabel.MixedNumber,
    KeyLabel.Special,
  };

  /**
   * Initialize for keyLabel comparisons according to the given pattern.
   * <p>
   * Where the patternString holds a comparisonValue for each possible KeyLabel
   * in enum definition order and each comparisonValue is of the form:
   * <p>
   * 'x' or '[xyz]'
   * <p>
   * Where 'x' is a character to which the corresponding keyLabel is mapped
   * to be matched directly against other mapped values; and
   * <p>
   * '[xyz]' is a bag of characters to which the corresponding keyLabel is
   * mapped, any of which would be considered a match against other mapped
   * values.
   */
  public KeyLabelMatcher(String patternString) {
    init(patternString);
  }

  private final void init(String patternString) {
    this.keyLabel2pattern = unpack(patternString);
    this.keyLabel2matches = new HashMap<KeyLabel, Set<KeyLabel>>();

    for (KeyLabel keyLabel1 : KEY_LABELS) {
      final Set<KeyLabel> matches = new HashSet<KeyLabel>();
      final String pattern1 = keyLabel2pattern.get(keyLabel1);
      for (KeyLabel keyLabel2 : KEY_LABELS) {
        final String pattern2 = keyLabel2pattern.get(keyLabel2);
        if (keyLabel1 == keyLabel2 || matches(pattern1, pattern2)) {
          matches.add(keyLabel2);
        }
      }
      keyLabel2matches.put(keyLabel1, matches);
    }
  }

  private final Map<KeyLabel, String> unpack(String patternString) {
    final Map<KeyLabel, String> result = new HashMap<KeyLabel, String>();

    final int len = patternString.length();
    final StringBuilder buffer = new StringBuilder();
    boolean inBrackets = false;
    int curLabelIdx = 0;

    for (int i = 0; i < len; ++i) {
      final char curC = patternString.charAt(i);
      if (inBrackets) {
        if (curC == ']') {
          result.put(KEY_LABELS[curLabelIdx], buffer.toString());
          buffer.setLength(0);
          inBrackets = false;
          ++curLabelIdx;
        }
        else {
          buffer.append(curC);
        }
      }
      else if (curC == '[') {
        inBrackets = true;
      }
      else {
        buffer.append(curC);
        result.put(KEY_LABELS[curLabelIdx], buffer.toString());
        buffer.setLength(0);
        ++curLabelIdx;
      }
    }

    return result;
  }

  public String getPattern(KeyLabel keyLabel) {
    return keyLabel2pattern.get(keyLabel);
  }

  public boolean matches(KeyLabel[] wordLabels1, KeyLabel[] wordLabels2) {
    boolean result = false;

    // collapse matching consecutive labels
    final KeyLabel[] collapsed1 = collapse(wordLabels1);
    final KeyLabel[] collapsed2 = collapse(wordLabels2);

    if (collapsed1.length == collapsed2.length) {
      result = true;
      for (int i = 0; i < collapsed1.length; ++i) {
        if (!matches(collapsed1[i], collapsed2[i])) {
          result = false;
          break;
        }
      }
    }

    return result;
  }

  /**
   * Determine whether the two labels match each other according to this
   * instance.
   */
  public boolean matches(KeyLabel wordLabel1, KeyLabel wordLabel2) {
    return keyLabel2matches.get(wordLabel1).contains(wordLabel2);
  }

  /**
   * Collapse consecutive matching labels.
   */
  public final KeyLabel[] collapse(KeyLabel[] wordLabels) {
    if (wordLabels.length <= 1) return wordLabels;

    final List<KeyLabel> result = new ArrayList<KeyLabel>();

    int equivStart = 0;
    KeyLabel prevLabel = null;
    for (int i = 0; i < wordLabels.length; ++i) {
      final KeyLabel wordLabel = wordLabels[i];

      if (!isEquiv(wordLabels, equivStart, i)) {
        result.add(wordLabel);
        equivStart = i;
      }
    }

    return result.toArray(new KeyLabel[result.size()]);
  }

  private final boolean isEquiv(KeyLabel[] wordLabels, int equivStart, int equivEnd) {
    if (equivEnd <= equivStart) return false;

    // determine wether wordLabels[equivEnd] matches all wordLabels from
    // equivStart to equivEnd

    boolean result = true;

    final KeyLabel endLabel = wordLabels[equivEnd];
    for (int i = equivStart; i < equivEnd; ++i) {
      final KeyLabel curLabel = wordLabels[i];
      if (!matches(curLabel, endLabel)) {
        result = false;
        break;
      }
    }

    return result;
  }

  /**
   * Two patterns match if any char in one pattern matches any char in the
   * other.
   */
  private final boolean matches(String pattern1, String pattern2) {
    boolean result = false;

    int len1 = pattern1.length();
    int len2 = pattern2.length();

    if (len2 < len1) {
      // swap patterns
      final String pswap = pattern1;
      pattern1 = pattern2;
      pattern2 = pswap;

      final int lswap = len1;
      len1 = len2;
      len2 = lswap;
    }

    // true if any char in pattern1 matches any char in pattern2
    for (int i = 0; i < len1; ++i) {
      final char c = pattern1.charAt(i);
      if (pattern2.indexOf(c) >= 0) {
        result = true;
        break;
      }
    }

    return result;
  }
}
