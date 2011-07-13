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
package org.sd.token;


/**
 * Container for bundling a token with its category in a parse.
 *
 * Note that the same token may have a different category in a different parse.
 * <p>
 * @author Spence Koehler
 */
public class CategorizedToken {
  
  public final Token token;
  public final String category;

  public CategorizedToken(Token token, String category) {
    this.token = token;
    this.category = category;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    // overall format is (cat)token, or:
    //   (cat)'text'[startIdx,endIdx{wordCount}](seqNum.revisNum)
    result.append("(").append(category).append(')').append(token.toString());

    return result.toString();
  }
}
