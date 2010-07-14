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
 * 
 * <p>
 * @author Spence Koehler
 */
public class StandardNormalizer implements Normalizer {
  
  public static String TOKEN_FEATURE = "StandardNormalizer";

  StandardNormalizerOptions options;

  public StandardNormalizer(StandardNormalizerOptions options) {
    this.options = options;
  }


  public String normalize(String text) {
    final StringBuilder result = new StringBuilder();

    boolean didNonWhite = false;
    boolean needWhite = false;
    final int len = text.length();
    for (int pos = 0; pos < len; ++pos) {
      boolean addChar = true;
      char curChar = text.charAt(pos);

      // apply CommonCase option
      if (options.getCommonCase() && Character.isUpperCase(curChar)) curChar = Character.toLowerCase(curChar);

      // apply ReplaceSymbolsWithWhite option
      if (options.getReplaceSymbolsWithWhite() && !Character.isLetterOrDigit(curChar)) curChar = ' ';

      // apply CompactWhite option
      if (options.getCompactWhite()) {
        if (Character.isWhitespace(curChar)) {
          addChar = false;

          if (didNonWhite) {
            needWhite = true;
          }
        }
        else didNonWhite = true;
      }

      if (addChar) {
        if (needWhite && didNonWhite) {
          result.append(' ');
          needWhite = false;
        }
        result.append(curChar);
      }
    }

    return result.toString();
  }

  public String getTokenFeature() {
    return TOKEN_FEATURE;
  }
}
