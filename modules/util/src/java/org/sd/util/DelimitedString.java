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


import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulation of a split string with access to delimiter and text pieces.
 * <p>
 * @author Spence Koehler
 */
public class DelimitedString {

  private String input;
  private char[] stringDelims;
  private List<String> toggleStrings;

  /**
   * Construct with the given input and special delimiter characters.
   *
   * @param input         The input to split for letter and digit pieces.
   * @param stringDelims  The non-letter or digit chars that are to be
   *                      considered as part of a string when immediately
   *                      preceded and/or followed by a string character.
   */
  public DelimitedString(String input, char[] stringDelims) {
    this.input = input;
    this.stringDelims = stringDelims;
    this.toggleStrings = getToggleStrings(input, stringDelims);
  }

  /**
   * Get the original input string.
   */
  public String getInput() {
    return input;
  }

  /**
   * Get the delimiters that are considered to be a part of the string if adjacent
   * to a letter or digit.
   */
  public char[] getStringDelims() {
    return stringDelims;
  }

  /**
   * Get the clean (of spurious whitespace and delims) input string.
   *
   * @param keepDelims  if true, include clean delims; otherwise, discard
   *                    delims and delimit strings with a single space
   *                    character.
   *
   * @return the clean input string.
   */
  public String getCleanInput(boolean keepDelims) {
    return buildString(0, toggleStrings.size(), keepDelims);
  }

  private final String buildString(int startIndex, int endIndex, boolean keepDelims) {
    final StringBuilder result = new StringBuilder();
    final int inc = keepDelims ? 1 : 2;

    for (int i = keepDelims ? startIndex : startIndex + (1 - startIndex % 2); i < endIndex; i += inc) {
      final String curString = toggleStrings.get(i);
      result.append(curString);
      if ((i + inc) < endIndex) {
        if (!keepDelims) result.append(' ');
        else if (i > 0 && curString.length() == 0) result.append(' ');
      }
    }

    return result.toString();
  }

  /**
   * Get the number of delimited strings contained in this instance.
   */
  public int numStrings() {
    return toggleStrings.size() / 2;
  }

  /**
   * Get the indicated string.
   *
   * @return the string at the given position.
   */
  public String getString(int pos) {
    return getToggleIndex(pos * 2 + 1);
  }

  /**
   * Get the string from fromPos (inclusive) to toPos (exclusive).
   */
  public String getString(int fromPos, int toPos, boolean keepDelims) {
    return buildString(fromPos * 2 + 1, toPos * 2, keepDelims);
  }

  /**
   * Get the (non-whitespace) delimiters before the indicated string.
   *
   * @return the non-whitespace delimiters before the string, or null.
   */
  public String getPreDelim(int pos) {
    return getToggleIndex(pos * 2);
  }

  /**
   * Get the (non-whitespace) delimiters after the indicated string.
   *
   * @return the non-whitespace delimiters after the string, or null.
   */
  public String getPostDelim(int pos) {
    return getToggleIndex(pos * 2 + 2);
  }

  private final String getToggleIndex(int index) {
    if (index >= toggleStrings.size()) return null;
    final String result = toggleStrings.get(index);
    if ("".equals(result)) return null;
    return result.trim();
  }

  /**
   * Toggle between delims and strings.
   * Prune all whitespace on ends and collapse all whitespace between delims to a single space.
   */
  static final List<String> getToggleStrings(String input, char[] stringDelims) {
    List<String> toggle = new ArrayList<String>();  // delim,string,delim,string,...

    final int len = input.length();
    final StringBuilder stringBuffer = new StringBuilder();
    final StringBuilder delimBuffer = new StringBuilder();
    int specialsSince = -1;

    boolean wasDelim = true;
    boolean wasWhite = false;
    boolean wasSpecial = false;
    
    for (int pos = 0; pos < len; ++pos) {
      final int c = input.codePointAt(pos);
      if (c > 65535) ++pos;

      if (Character.isLetterOrDigit(c)) {
        // add to or start the 'string' buffer

        if (wasDelim) {
          if (wasWhite && delimBuffer.length() > 0) delimBuffer.append(' ');
          if (wasSpecial) {
            toggle.add(delimBuffer.substring(0, specialsSince)/*.trim()*/);
            stringBuffer.append(delimBuffer.substring(specialsSince));
          }
          else {
            toggle.add(delimBuffer.toString());
          }
          delimBuffer.setLength(0);
        }
        stringBuffer.appendCodePoint(c);

        wasDelim = false;
        wasWhite = false;
        wasSpecial = false;
      }
      else {
        final boolean isWhite = StringUtil.isWhite(c);
        final boolean isSpecial = isWhite ? false : isSpecial(c, stringDelims);

        if (isSpecial) {
          if (!wasDelim) {
            stringBuffer.appendCodePoint(c);
            specialsSince = -1;
          }
          else {
            if (wasWhite /*&& delimBuffer.length() > 0*/) delimBuffer.append(' ');
            if (!wasSpecial) specialsSince = delimBuffer.length();
            delimBuffer.appendCodePoint(c);
          }

          //wasDelim stays the same
          wasWhite = false;
          wasSpecial = true;
        }
        else {
          if (!wasDelim) {
            toggle.add(stringBuffer.toString());
            stringBuffer.setLength(0);
          }
          if (!isWhite) {
            if (wasWhite /*&& delimBuffer.length() > 0*/) delimBuffer.append(' ');
            delimBuffer.appendCodePoint(c);
          }

          wasDelim = true;
          wasWhite = isWhite;
          wasSpecial = false;
        }
      }
    }

    // dump last data.
    if (wasDelim) {
      if (delimBuffer.length() > 0) toggle.add(delimBuffer.toString());
    }
    else {
      if (wasSpecial && specialsSince >= 0) {
        stringBuffer.append(delimBuffer.substring(specialsSince));
      }
      if (stringBuffer.length() > 0) toggle.add(stringBuffer.toString());
    }

    return toggle;
  }

  private static final boolean isSpecial(int c, char[] stringDelims) {
    if (stringDelims != null) {
      for (char sdChar : stringDelims) {
        if (sdChar == c) return true;
      }
    }

    return false;
  }

  public ContinuousSegment getSegment(int startPos) {
    ContinuousSegment result = null;
    if (startPos < numStrings()) {
      result = new ContinuousSegment(startPos);
    }
    return result;
  }

  public class ContinuousSegment {
    private int startIndex;
    private int endIndex;

    private ContinuousSegment(int startIndex, int endIndex) {
      this.startIndex = startIndex;
      this.endIndex = endIndex;
    }

    ContinuousSegment(int startPos) {
      final int num = toggleStrings.size();

      this.startIndex = startPos * 2 + 1;
      this.endIndex = num;

      for (int i = startIndex + 1; (i + 1) < num; i += 2) {
        final String toggleString = toggleStrings.get(i);
        if (!"".equals(toggleString)) {
          this.endIndex = i;
          break;
        }
      }
    }

    //note: pos is the word number (ignoring delims); index is the position of a word or delim

    /**
     * Get the position of the word that starts this segment, where the first word
     * in the string is 0.
     */
    public int getStartWordPosition() {
      return (startIndex - 1) / 2;
    }

    // get the input from the beginning of the input through the end of this segment
    public String getInputThrough(String unnormalizedInput) {
      if (endIndex < 2) return "";
      final DelimitedString orig = new DelimitedString(unnormalizedInput, null);
      final DelimitedString thru = new DelimitedString(buildString(0, endIndex, true), null);
      int newEndIndex = thru.toggleStrings.size() + 1;
      if (newEndIndex >= orig.toggleStrings.size()) newEndIndex--;
      return orig.buildString(0, newEndIndex, true).trim();
    }

    // get the input after the end of this segment
    public String getInputBeyond(String unnormalizedInput) {
      final int numToggleStrings = toggleStrings.size();
      if (endIndex >= numToggleStrings - 2) return "";
      final DelimitedString orig = new DelimitedString(unnormalizedInput, null);
      final DelimitedString thru = new DelimitedString(buildString(0, endIndex, true), null);
      return orig.buildString(thru.toggleStrings.size() + 1, orig.toggleStrings.size(), true).trim();
    }

    public String getString() {
      return buildString(startIndex, endIndex, false);
    }

    public String getString(boolean keepDelims) {
      return buildString(startIndex, endIndex, keepDelims);
    }

    public ContinuousSegment shorten() {
      final int newEndIndex = endIndex - 2;
      return (newEndIndex <= startIndex) ? null : new ContinuousSegment(startIndex, newEndIndex);
    }

    public ContinuousSegment next() {
      final int newStartIndex = endIndex + 1;
      return (newStartIndex <= toggleStrings.size()) ? new ContinuousSegment((newStartIndex - 1) / 2) : null;
    }

    public int length() {
      return (endIndex - startIndex + 1) / 2;
    }

    public String toString() {
      return startIndex + "-" + endIndex + "(" + getString() + ")";
    }
  }
}
