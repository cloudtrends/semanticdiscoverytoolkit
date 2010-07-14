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
package org.sd.atn.extract;


import org.sd.util.StringInputContext;

/**
 * An extraction based on input from a string.
 * <p>
 * @author Spence Koehler
 */
public class StringExtraction extends Extraction {
  
  private StringInputContext stringContext;
  private int startPos;
  private int endPos;

  public StringExtraction(String type, StringInputContext stringContext, int startPos, int endPos) {
    super(type, stringContext.getText());

    this.stringContext = stringContext;
    this.startPos = startPos;
    this.endPos = endPos;
  }


  public StringInputContext getStringContext() {
    return stringContext;
  }

  public int getStartPos() {
    return startPos;
  }

  public int getEndPos() {
    return endPos;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append("s[").
      append(stringContext.getId()).
      append(',').
      append(stringContext.getText().length()).
      append("].").
      append(startPos).
      append('-').
      append(endPos).
      append('=').
      append(getText());

    return result.toString();
  }
}
