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
package org.sd.text.segment;


/**
 * A split function that splits text based on the presense of delimiters.
 * This function will set the CommonFeatures.DELIM feature to true on all
 * delimiter segments and to false on all other segments created.
 *
 * <p>
 * @author Spence Koehler
 */
public class SingleDelimSplitFunction extends BaseDelimSplitFunction {

  public static final char[] DEFAULT_CHARS_TO_EXCLUDE = new char[] {
    '.', '(', ')', '/', '\\', '&', '\'', '"', '%', '#', '$', '{', '}', '[', ']',
  };

  private static final SingleDelimSplitFunction INSTANCE = new SingleDelimSplitFunction();

  public static final SingleDelimSplitFunction getDefaultInstance() {
    return INSTANCE;
  }

  protected SingleDelimSplitFunction() {
    super(1, DEFAULT_CHARS_TO_EXCLUDE);
  }

  /**
   * Construct an instance that will treat all non-letter/digit/white and
   * the given chars as delimiters for splitting.
   */
  public SingleDelimSplitFunction(char[] charsToExclude) {
    super(1, charsToExclude);
  }
}
