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


/**
 * Utility to generate numbered names.
 * <p>
 * @author Spence Koehler
 */
public class NumberedNameGenerator implements NameGenerator {
  
  public static final int DEFAULT_NUM_DIGITS = 5;

  private String prefix;
  private String postfix;
  private int numDigits;

  private int preLen;
  private int postLen;

  /**
   * Construct for names like [prefix].xxx.[postfix] with the
   * default number of digits (=5).
   * <p>
   * Note that unless the prefix ends in a non-letter/digit character
   * then a '.' will be postpended and that unless the postfix begins
   * with a non-letter/digit character then a '.' will be prepended.
   *
   * @param prefix  The prefix.
   * @param postfix The postfix.
   */
  public NumberedNameGenerator(String prefix, String postfix) {
    this(prefix, postfix, DEFAULT_NUM_DIGITS);
  }

  /**
   * Construct for names like [prefix].xxx.[postfix].
   * <p>
   * Note that unless the prefix ends in a non-letter/digit character
   * then a '.' will be postpended and that unless the postfix begins
   * with a non-letter/digit character then a '.' will be prepended.
   *
   * @param prefix  The prefix.
   * @param postfix The postfix.
   * @param numDigits  The number of digits for the counter.
   */
  public NumberedNameGenerator(String prefix, String postfix, int numDigits) {
    this.prefix = fixPrefix(prefix);
    this.postfix = fixPostfix(postfix);
    this.numDigits = numDigits;

    this.preLen = this.prefix.length();
    this.postLen = this.postfix.length();
  }

  /**
   * Generate the next name after the given name.
   *
   * @param name preceding the next name; if null, generate the first name.
   *
   * @return the next name, or null if there are no more names.
   */
  public String getNextName(String name) {
    StringBuilder result = null;

    final long nextNumber = getNextNumber(name);

    if (nextNumber >= 0) {
      result =
        new StringBuilder().
        append(prefix).
        append(MathUtil.longString(nextNumber, 10, numDigits, '0')).
        append(postfix);
    }

    return (result == null) ? null : result.toString();
  }

  /**
   * Determine whether the given name is valid for this generator.
   */
  public boolean isValidName(String name) {
    return getCurNumber(name) >= 0 && name.startsWith(prefix) && name.endsWith(postfix);
  }


  /** Get the prefix. */
  public final String getPrefix() {
    return prefix;
  }

  /** Set the prefix. */
  public final void setPrefix(String prefix) {
    this.prefix = fixPrefix(prefix);
    this.preLen = this.prefix.length();
  }

  /** Get the postfix. */
  public String getPostfix() {
    return postfix;
  }

  /** Set the postfix. */
  public final void setPostfix(String postfix) {
    this.postfix = fixPostfix(postfix);
    this.postLen = this.postfix.length();
  }

  /** Get the number of digits. */
  public final int getNumDigits() {
    return numDigits;
  }


  /**
   * Make sure prefix ends with a symbol, using '.' if necessary.
   * <p>
   * NOTE: empty is treated as a symbol.
   */
  private final String fixPrefix(String prefix) {
    String result = (prefix == null) ? "" : prefix;
    String extra = "";

    if (result.length() > 0) {
      final char c = result.charAt(result.length() - 1);
      if (Character.isLetterOrDigit(c)) {
        extra = ".";
      }
    }

    return result + extra;
  }

  /**
   * Make sure postfix begins with a symbol, using '.' if necessary.
   * <p>
   * NOTE: empty is treated as a symbol.
   */
  private final String fixPostfix(String postfix) {
    String result = (postfix == null) ? "" : postfix;
    String extra = "";

    if (result.length() > 0) {
      final char c = result.charAt(0);
      if (Character.isLetterOrDigit(c)) {
        extra = ".";
      }
    }

    return extra + result;
  }

  protected final long getNextNumber(String name) {
    if (name == null) return 0;

    long result = -1;

    final long curNumber = getCurNumber(name);
    if (curNumber >= 0) {
      result = curNumber + 1;
    }

    return result;
  }

  protected final long getCurNumber(String name) {
    long result = -1;

    final int len = name.length();
    final int leftP = preLen;
    final int rightP = len - postLen;

    if (leftP < len && ((rightP - leftP) >= numDigits)) {
      result = stripAndConvert(name.substring(leftP, rightP));
    }

    return result;
  }

  protected final long stripAndConvert(String string) {
    long result = -1;

    final int len = string.length();
    int startC = 0;
    while (startC < len && string.charAt(startC) == '0') ++startC;

    if (startC > 0 && string.length() > numDigits) {
      // invalid string: has too many digits AND padded with 0's
      result = -1;
    }
    else if (startC < len) {
      try {
        result = Long.parseLong(string.substring(startC));
      }
      catch (NumberFormatException e) {
        result = -1;  // illegal
      }
    }
    else {
      // string is all 0's
      result = 0;
    }

    return result;
  }
}
