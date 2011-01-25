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
 * A naive iterator over the words in a string based on java.text.BreakIterator.
 * <p>
 * @author Spence Koehler
 */
public class WordIterator extends BaseTextIterator {
  
  /**
   * Construct with the string whose words are to be iterated over
   * using the default locale.
   */
  public WordIterator(String string) {
    super(BreakIterator.getWordInstance(), true);
    setText(string);
  }

  /**
   * Construct with the string whose words are to be iterated over
   * using the given locale.
   */
  public WordIterator(String string, Locale locale) {
    super(BreakIterator.getWordInstance(locale), true);
    setText(string);
  }


  /**
   * Show segmentation of each argument.
   */
  public static final void main(String[] args) {
    for (String arg : args) {
      System.out.println(arg + " --> ");
      for (WordIterator iter = new WordIterator(arg); iter.hasNext(); ) {
        System.out.println("\t" + iter.next());
      }
    }
  }
}
