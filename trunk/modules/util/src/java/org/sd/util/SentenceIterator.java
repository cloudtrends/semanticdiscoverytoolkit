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


import java.text.BreakIterator;
import java.util.Locale;

/**
 * A naive iterator over the sentences in a string based on
 * java.text.BreakIterator.
 * <p>
 * @author Spence Koehler
 */
public class SentenceIterator extends BaseTextIterator {
  
  private boolean detectAbbrev;

  /**
   * Construct with the string whose words are to be iterated over
   * using the default locale.
   */
  public SentenceIterator(String string) {
    this(string, false);
  }

  /**
   * Construct with the string whose words are to be iterated over
   * using the default locale.
   */
  public SentenceIterator(String string, boolean detectAbbrev) {
    super(BreakIterator.getSentenceInstance(), false);
    this.detectAbbrev = detectAbbrev;
    setText(string);
  }

  /**
   * Construt with the string whose words are to be iterated over
   * using the given local.
   */
  public SentenceIterator(String string, Locale locale) {
    super(BreakIterator.getSentenceInstance(locale), false);
    setText(string);
  }

  /**
   * Construt with the string whose words are to be iterated over
   * using the given local.
   */
  public SentenceIterator(String string, Locale locale, boolean detectAbbrev) {
    super(BreakIterator.getSentenceInstance(locale), false);
    this.detectAbbrev = detectAbbrev;
    setText(string);
  }

  /**
   * Get the current detectAbbrev flag.
   */
  public boolean detectAbbrev() {
    return detectAbbrev;
  }

  /**
   * Determine whether the the substring should be accepted as text to return.
   * <p>
   * This default implementation accepts any text, unless detectAbbrev is true,
   * in which case capitalized abbreviations are rejected as sentence ending
   * boundaries. See StringUtil.isLikelyAbbreviation.
   */
  protected boolean accept(String text, int start, int end) {
    boolean result = true;

    if (detectAbbrev) {
      // scan backwards to first letter or digit
      for (--end; end >= start; --end) {
        final char curC = text.charAt(end);
        if (Character.isLetterOrDigit(curC)) break;
      }

      if (end > 0) {
        // scan backwards to whitespace
        int lastWordStart = end;

        for (; lastWordStart > start; --lastWordStart) {
          final char curC = text.charAt(lastWordStart - 1);
          if (Character.isWhitespace(curC)) break;
        }

        // reject capitalized abbreviations as a sentence boundary
        if (Character.isUpperCase(text.charAt(lastWordStart)) && StringUtil.isLikelyAbbreviation(text.substring(lastWordStart, end))) {
          result = false;
        }
      }
    }

    return result;
  }


  /**
   * Show segmentation of each argument.
   */
  public static final void main(String[] args) {
    for (String arg : args) {
      System.out.println(arg + " --> ");
      for (SentenceIterator iter = new SentenceIterator(arg, true); iter.hasNext(); ) {
        System.out.println("\t" + iter.next());
      }
    }
  }
}
