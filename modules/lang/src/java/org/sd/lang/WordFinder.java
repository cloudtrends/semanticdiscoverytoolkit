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
package org.sd.lang;


import org.sd.text.TermFinder;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Wrapper around a term finder for searching sentences for words.
 * <p>
 * @author Spence Koehler
 */
public class WordFinder {

  private TermFinder wordFinder;

  /**
   * Construct with a list of words in the given file.
   * <p>
   * Words to find are added from the file, one "word" per line. A word
   * can be a compound word (i.e. can include spaces.)
   * <p>
   * Empty lines and comment lines (starting with '#') are ignored.
   * <p>
   * Each word will be normalized while loading.
   */
  public WordFinder(String label, File wordListFile, boolean caseSensitive) throws IOException {
    this.wordFinder = new TermFinder(label, caseSensitive);
    wordFinder.loadTerms(wordListFile);
  }

  /**
   * Construct with a list of words.
   * <p>
   * A word can be a compound word (i.e. can include spaces.)
   * <p>
   * Each word will be normalized while loading.
   */
  public WordFinder(String label, String[] terms, boolean caseSensitive) {
    this.wordFinder = new TermFinder(label, caseSensitive);
    wordFinder.loadTerms(terms);
  }

  /**
   * Find the position and length of the first occurence of a word
   * in the string.
   */
  public int[] findFirst(String input) {
    return wordFinder.findPatternPos(input, TermFinder.FULL_WORD);
  }

  /**
   * Truncate the input before the first occurence of a found word.
   * <p>
   * If a word is not found, return the full input.
   */
  public String truncateBeforeFirst(String input) {
    String result = input;

    final int[] pos = findFirst(input);
    if (pos != null) {
      result = input.substring(0, pos[0]).trim();
    }

    return result;
  }

  public Set<String> getWords() {
    return wordFinder.getTerms();
  }
}
