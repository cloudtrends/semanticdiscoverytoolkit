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
 * Utility to build vertical-bar delimited output lines.
 * <p>
 * Note that every appended string will be treated as a separate
 * field, automatically following a vertical-bar delimiter.
 *
 * @author Spence Koehler
 */
public class LineBuilder {

  private StringBuilder line;
  private int fieldNum;

  /**
   * Construct a new instance.
   */
  public LineBuilder() {
    this.line = new StringBuilder();
    this.fieldNum = 0;
  }

  /**
   * Append the given string, replacing any vertical bars with their
   * url escape sequence.
   */
  public LineBuilder appendUrl(String string) {
    return doAppend(fixString(string, "%7c"));
  }

  /**
   * Append the given string, replacing any vertical bars with their
   * html escape sequence.
   */
  public LineBuilder appendHtml(String string) {
    return doAppend(fixString(string, "&#124;"));
  }

  /**
   * Append the given string that was built through a line builder.
   * <p>
   * In other words, any necessary escaping has already been performed.
   */
  public LineBuilder appendBuilt(String string) {
    return doAppend(string);
  }

  /**
   * Append the given string, replacing any vertical bars with a comma.
   */
  public LineBuilder append(String string) {
    return doAppend(fixString(string, ","));
  }

  /**
   * Append the given strings, replacing any vertical bars with a comma.
   */
  public LineBuilder append(String[] strings) {
    for (String string : strings) {
      doAppend(fixString(string, ","));
    }
    return this;
  }

  /**
   * Append the given string without replacing any vertical bars in the input. This
   * should only be used when any possible vertical bars in the input are intended
   * to be field separators.
   */
  public LineBuilder appendNoFix(String string) {
    return doAppend(string);
  }

  /**
   * Append the given string without replacing any vertical bars in the input. This
   * should only be used when any possible vertical bars in the input are intended
   * to be field separators.
   */
  public LineBuilder appendNoFix(String[] strings) {
    for (String string : strings) {
      doAppend(string);
    }
    return this;
  }

  /**
   * Append the given integer.
   */
  public LineBuilder append(int value) {
    if (fieldNum > 0) line.append('|');
    line.append(value);
    ++fieldNum;
    return this;
  }

  /**
   * Append the given long.
   */
  public LineBuilder append(long value) {
    if (fieldNum > 0) line.append('|');
    line.append(value);
    ++fieldNum;
    return this;
  }

  /**
   * Append the given value as a string with the given number of decimal places.
   */
  public LineBuilder append(double value, int places) {
    return doAppend(MathUtil.doubleString(value, places));
  }

  /**
   * Append the given boolean.
   */
  public LineBuilder append(boolean value) {
    if (fieldNum > 0) line.append('|');
    line.append(value);
    ++fieldNum;
    return this;
  }

  /**
   * Append empty fields to the end of this line.
   */
  public LineBuilder appendEmptyFields(int numFields) {
    for (int i = 0; i < numFields; ++i) append("");
    return this;
  }

  /**
   * Clear the contents of this instance.
   */
  public void reset() {
    line.setLength(0);
    fieldNum = 0;
  }

  /**
   * Get the built string.
   */
  public String toString() {
    return line.toString();
  }

  /**
   * Add a field delimiter and the already escaped string.
   */
  private final LineBuilder doAppend(String string) {
    if (fieldNum > 0) line.append('|');
    if (string != null) line.append(string);
    ++fieldNum;
    return this;
  }

  /**
   * Replace vertical bars and squash non-printing chars.
   */
  final static String fixString(String string, String vbReplacement) {
    if (string == null) return null;

    final StringBuilder result = new StringBuilder();

    for (StringUtil.StringIterator iter = new StringUtil.StringIterator(string); iter.hasNext(); ) {
      final StringUtil.StringPointer pointer = iter.next();
      int codePoint = pointer.codePoint;
      if (codePoint < 32 || codePoint == 127) {
        // do nothing.
      }
      else if (vbReplacement != null && codePoint == '|') {
        result.append(vbReplacement);
      }
      else {
        result.appendCodePoint(codePoint);
      }
    }

    return result.toString();
  }
}
