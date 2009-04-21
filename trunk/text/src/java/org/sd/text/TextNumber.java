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


import java.util.HashSet;
import java.util.Set;

/**
 * Utility to recognize and map textual numbers.
 * <p>
 * @author Spence Koehler
 */
public class TextNumber {
  
  public static final String[] NUMBER_STRING_VALUES = new String[] {
    "one", "first", //"1st",
    "two", "second", //"2nd",
    "three", "third", //"3rd",
    "four", "fourth", //"4th",
    "five", "fifth", //"5th",
    "six", "sixth", //"6th",
    "seven", "seventh", //"7th",
    "eight", "eighth", //"8th",
    "nine", "ninth", //"9th",
    "ten", "tenth", //"10th",
    "eleven", "eleventh", //"11th",
    "twelve", "twelfth", //"12th",
    "thirteen", "thirteenth", //"13th",
    "fourteen", "fourteenth", //"14th",
    "fifteen", "fifteenth", //"15th",
    "sixteen", "sixteenth", //"16th",
    "seventeen", "seventeenth", //"17th",
    "eighteen", "eighteenth", //"18th",
    "nineteen", "nineteenth", //"19th",
    "twenty", "twentieth", //"20th",
    "thirty", "thirtieth", //"30th",
    "forty", "fortieth", //"40th",
    "fifty", "fiftieth", //"50th",
    "sixty", "sixtieth", //"60th",
    "seventy", "seventieth", //"70th",
    "eighty", "eightieth", //"80th",
    "ninety", "ninetieth", //"90th",
    "hundred", "hundredth",
    "thousand", "thousandth",
    "million", "millionth",
    "billion", "billionth",
    "trillion", "trillionth",
    "quadrillion", "quadrillionth",
    "quintillion", "quintillionth",
    "sextillion", "sextillionth",
    "septillion", "septillionth",
    "octillion", "octillionth",
  };
  public static final Set<String> NUMBER_STRINGS = new HashSet<String>();
  static {
    for (String string : NUMBER_STRING_VALUES) {
      NUMBER_STRINGS.add(string);
    }
  }

  public static final String[] NUMBER_ENDING_STRINGS = new String[]{"st", "nd", "rd", "th"};
  public static final Set<String> NUMBER_ENDINGS = new HashSet<String>();
  static {
    for (String string : NUMBER_ENDING_STRINGS) {
      NUMBER_ENDINGS.add(string);
    }
  }

  /**
   * Build a text number finder.
   */
  public static final TermFinder buildTextNumberFinder() {
    return new TermFinder("TextNumber", false, NUMBER_STRING_VALUES);
  }

  /**
   * Determine whether the (lowercased) token is a text number.
   */
  public static final boolean isNumber(String token) {
    return NUMBER_STRINGS.contains(token);
  }

  /**
   * Determine whether the (lowercased) token is a text number ending (like "st", "nd", "rd", or "th").
   */
  public static final boolean isNumberEnding(String token) {
    return NUMBER_ENDINGS.contains(token);
  }

//  public static final char[] ROMAN_NUMERAL_CHARS = new char[]{'C', 'D', 'I', 'L', 'M', 'V', 'X'};
//just 'low' numbers
//  public static final char[] ROMAN_NUMERAL_CHARS = new char[]{'I', 'V', 'X'};

  /**
   * Determine whether the token is a roman numeral.
   * <p>
   * This impl just looks for only having the chars: CDILMVX.
   */
  public static final boolean isRomanNumeral(String token, boolean justLowNumbers) {
    if (token == null || "".equals(token)) return false;
    final int len = token.length();
    for (int i = 0; i < len; ++i) {
      final char c = token.charAt(i);
      if (justLowNumbers) {
        if (!((c == 'I' || c == 'V' || c == 'X'))) {
          return false;
        }
      }
      else {
        if (!((c == 'I' || c == 'V' || c == 'X' || c == 'L' || c == 'C' || c == 'D' || c == 'M'))) {
          return false;
        }
      }
    }
    return true;
  }
}
