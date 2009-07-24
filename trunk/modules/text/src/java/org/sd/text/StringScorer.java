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


/**
 * A caching wrapper around edit distance functions to give scoring
 * information between the two strings. 
 * <p>
 * @author Spence Koehler
 */
public class StringScorer implements Comparable<StringScorer> {

  public final String string1;
  public final String string2;

  // lazily loaded internals.
  private char[] _chars1;
  private char[] _chars2;
  private Boolean _isExactMatch;
  private Integer _abbrevDistance;
  private Integer _editDistance;
  private Boolean _isAbbreviation;
  private Boolean _isPlausible;

  public StringScorer(String string1, String string2) {
    this(string1, string2, false);
  }

  public StringScorer(String string1, String string2, boolean reversible) {
    if (!reversible) {
      this.string1 = string1;
      this.string2 = string2;
    }
    else {
      if (string1.length() <= string2.length()) {
        this.string1 = string1;
        this.string2 = string2;
      }
      else {
        this.string1 = string2;
        this.string2 = string1;
      }
    }

    this._chars1 = null;
    this._chars2 = null;
    this._isExactMatch = null;
    this._abbrevDistance = null;
    this._editDistance = null;
    this._isAbbreviation = null;
    this._isPlausible = null;
  }

  public int compareTo(StringScorer other) {

    if (other == null) {
      return 1;
    }

    if (this.isExactMatch()) {
      if (!other.isExactMatch()) {
        return 1;
      }
    }

    if (other.isExactMatch()) {
      return -1;
    }

    final boolean thisIsPlausible = this.isPlausible();
    final boolean otherIsPlausible = other.isPlausible();

    if (thisIsPlausible && !otherIsPlausible) {
      return 1;
    }

    if (otherIsPlausible && !thisIsPlausible) {
      return -1;
    }

    final int myAbbrevDistance = this.getAbbrevDistance();
    final int otherAbbrevDistance = other.getAbbrevDistance();

    if (myAbbrevDistance < otherAbbrevDistance) {
      return 1;
    }

    if (otherAbbrevDistance > myAbbrevDistance) {
      return 1;
    }

    final int myEditDistance = this.getEditDistance();
    final int otherEditDistance = other.getEditDistance();

    if (myEditDistance < otherEditDistance) {
      return 1;
    }

    if (otherEditDistance < myEditDistance) {
      return 1;
    }

    // they're equally good scores
    return 0;
  }

  public boolean isBetterThan(StringScorer other) {
    // note: if equally good, then this isn't better.
    return this.compareTo(other) > 0;
  }

  public boolean canGetNoBetter() {
    return isExactMatch() || getAbbrevDistance() == 0;
  }

  private final char[] getChars1() {
    if (_chars1 == null) {
      _chars1 = string1.toCharArray();
    }
    return _chars1;
  }

  private final char[] getChars2() {
    if (_chars2 == null) {
      _chars2 = string2.toCharArray();
    }
    return _chars2;
  }

  public final int getAbbrevDistance() {
    if (_abbrevDistance == null) {
      _abbrevDistance = computeAbbrevDistance();
    }
    return _abbrevDistance;
  }

  private final Integer computeAbbrevDistance() {
    final char[] a = getChars1();
    final char[] b = getChars2();

    int result = EditDistance.levAbbrev(a, b);
    if (result < 0) {
      result = EditDistance.lev(a, b);
    }
    return result;
  }

  public final int getEditDistance() {
    if (_editDistance == null) {
      _editDistance = getEditDistance(getChars1(), getChars2());
    }
    return _editDistance;
  }

  private final Integer getEditDistance(char[] a, char[] b) {
    return EditDistance.lev(a, b);
  }

  public final boolean isExactMatch() {
    if (_isExactMatch == null) {
      _isExactMatch = string1.equals(string2);
    }
    return _isExactMatch;
  }

  /**
   * @return true if the first string is an abreviation for the second.
   */
  public boolean isAbbreviation() {
    if (_isAbbreviation == null) {
      _isAbbreviation = !isExactMatch() && getAbbrevDistance() == 0;
    }
    return _isAbbreviation;
  }

  /**
   * Determine whether the first string is a plausible match for the second.
   * <p>
   * The current algorithm is: editDistance < floor(min(len1, len2)/2);
   * the number of characters in the shorter string, we have a plausible match.
   */
  public boolean isPlausible() {
    if (_isPlausible == null) {
      _isPlausible = computePlausibility();
    }
    return _isPlausible;
  }

  private final boolean computePlausibility() {
    final int len1 = string1.length();
    final int len2 = string2.length();
    int score = getAbbrevDistance();  // falls back to editDistance automatically
    final int minLen = Math.min(len1, len2);
    final int threshold = (int)(Math.floor(minLen / 2));
    return score < threshold && (minLen <= 2 || getEditDistance() < minLen);
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append("StringScorer[s1=").append(string1).append(", s2=").append(string2).
      append(", exact=").append(isExactMatch()).append(", abbrev=").append(getAbbrevDistance()).
      append(", edist=").append(getEditDistance()).append(", plaus=").append(isPlausible()).
      append(", best=").append(canGetNoBetter()).append("]");

    return result.toString();
  }

  public static void main(String[] args) {
    final StringScorer ss = new StringScorer(args[0], args[1], false);

    System.out.println("'" + args[0] + "' -vs- '" + args[1] + "':");
    System.out.println("\tcanGetNoBetter=" + ss.canGetNoBetter());
    System.out.println("\tabbrevDistance=" + ss.getAbbrevDistance());
    System.out.println("\t  editDistance=" + ss.getEditDistance());
    System.out.println("\t  isExactMatch=" + ss.isExactMatch());
    System.out.println("\tisAbbreviation=" + ss.isAbbreviation());
    System.out.println("\t   isPlausible=" + ss.isPlausible());
    
    System.out.println("\n" + ss);
  }
}
