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
  private boolean greedy;

  /**
   * Construct with the string whose words are to be iterated over
   * using the default locale.
   */
  public SentenceIterator(String string) {
    this(string, false, true);
  }

  /**
   * Construct with the string whose words are to be iterated over
   * using the default locale.
   */
  public SentenceIterator(String string, boolean detectAbbrev) {
    this(string, detectAbbrev, true);
  }

  public SentenceIterator(String string, boolean detectAbbrev, boolean greedy) {
    super(BreakIterator.getSentenceInstance(), false);
    this.detectAbbrev = detectAbbrev;
    this.greedy = greedy;
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

  public SentenceIterator setDetectAbbrev(boolean detectAbbrev) {
    this.detectAbbrev = detectAbbrev;
    return this;
  }

  public boolean isGreedy() {
    return greedy;
  }

  public SentenceIterator setGreedy(boolean greedy) {
    this.greedy = greedy;
    return this;
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
      final int origEnd = end;

      // scan backwards to first letter or digit
      for (--end; end >= start; --end) {
        final char curC = text.charAt(end);
        if (Character.isLetterOrDigit(curC)) break;
      }

      if (end > 0) {
        // scan backwards to whitespace
        int lastWordStart = end;
        boolean hasEmbeddedDot = false;

        for (; lastWordStart > start; --lastWordStart) {
          final char curC = text.charAt(lastWordStart - 1);
          if (Character.isWhitespace(curC)) break;
          if (curC == '.') hasEmbeddedDot = true;
        }

        // reject capitalized abbreviations as a sentence boundary
        if ((hasEmbeddedDot || Character.isUpperCase(text.charAt(lastWordStart))) &&
            StringUtil.isLikelyAbbreviation(text.substring(lastWordStart, end))) {
          result = false;
        }
      }

      // reject in favor of greedy collection if next word ends in non-white
      if (result) {
        final int len = text.length();
        if (origEnd < len) {
          int ptr = origEnd;

          // skip forward over white to Letter, failing if encounter non-White/Letter
          for (; ptr < len; ++ptr) {
            final char curC = text.charAt(ptr);
            if (!Character.isWhitespace(curC)) {
              if (!Character.isLetter(curC)) {
                result = false; //fail
              }
              break;
            }
          }

          // skip forward over letters, failing if first non-letter isn't whitespace
          if (result && greedy) {
            final int startPtr = ptr;
            for (ptr = ptr + 1; ptr < len; ++ptr) {
              final char curC = text.charAt(ptr);
              if (!Character.isLetter(curC) && curC != '-') {
                if (!Character.isWhitespace(curC)) {
                  if (greedy /*||
                      (curC == '.' &&
                       Character.isUpperCase(text.charAt(startPtr)) &&
                       StringUtil.isLikelyAbbreviation(text.substring(startPtr, ptr)))*/) {
                    result = false;
                  }
                }
                break;
              }
            }
          }
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
