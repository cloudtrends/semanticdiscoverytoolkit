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
package org.sd.extract;

import java.util.regex.Pattern;

/**
 * A regex-based text acceptor.
 * <p>
 * @author Spence Koehler
 */
public class RegexAcceptor implements TextAcceptor {
  
  private Pattern pattern;

  public RegexAcceptor(String patternString) {
    this(Pattern.compile(patternString));
  }

  public RegexAcceptor(Pattern pattern) {
    this.pattern = pattern;
  }

  /**
   * Determine whether the text is acceptable for further processing.
   *
   * @param docText  The candidate doc text to process.
   *
   * @return true if the text is acceptable for further processing; otherwise, false.
   */
  public boolean accept(DocText docText) {
    boolean result = false;
    final String string = docText.getString();
    if (string != null && !"".equals(string)) {
      result =  pattern.matcher(string).matches();
    }
    return result;
  }
}
