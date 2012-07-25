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


/**
 * Base implementation of a segment pointer finder.
 * <p>
 * @author Spence Koehler
 */
public abstract class BaseSegmentPointerFinder implements SegmentPointerFinder {
  
  protected final String input;
  protected final int len;

  protected BaseSegmentPointerFinder(String input) {
    this.input = input;
    this.len = input.length();
  }

  /** Get the input */
  public String getInput() {
    return input;
  }

  /** Get the input length */
  public int length() {
    return len;
  }

  /**
   * Auxiliary for skipping to the first non-white character at or after
   * fromPtr or to the end of the string.
   */
  protected final int skipToNonWhite(int fromPtr) {
    int result = fromPtr;

    // set startPtr to first non-white char > endPtr
    for (; result < len; ++result) {
      final char c = input.charAt(result);
      if (!isWhitespace(c)) break;
    }
    
    return result;
  }

  /**
   * Auxiliary for skipping to the first white character at or after fromPtr or
   * to the end of the string.
   */
  protected final int skipToWhite(int fromPtr) {
    int result = fromPtr;

    // set startPtr to first non-white char > endPtr
    for (; result < len; ++result) {
      final char c = input.charAt(result);
      if (isWhitespace(c)) break;
    }
    
    return result;
  }

  protected boolean isWhitespace(char c) {
    return Character.isWhitespace(c);
  }
}
