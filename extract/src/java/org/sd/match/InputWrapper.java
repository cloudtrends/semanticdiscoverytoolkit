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
package org.sd.match;


import org.sd.nlp.Normalizer;
import org.sd.nlp.NormalizingTokenizer;
import org.sd.nlp.StringWrapper;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * A wrapper for input to be matched. 
 * <p>
 * @author Spence Koehler
 */
public class InputWrapper {

  private static final InputOrderComparator INPUT_ORDER_COMPARATOR = new InputOrderComparator();

  public static final InputOrderComparator getInputOrderComparator() {
    return INPUT_ORDER_COMPARATOR;
  }

  private Normalizer normalizer;
  private SortableWord[] inputWords;

  public InputWrapper(Normalizer normalizer, List<StringWrapper.SubString> inputWords) {
    this.normalizer = normalizer;

    final TreeSet<SortableWord> sortedWords = new TreeSet<SortableWord>();
    int wordNum = 0;
    for (StringWrapper.SubString inputWord : inputWords) {
      sortedWords.add(new SortableWord(wordNum++, inputWord, normalizer));
    }
    this.inputWords = sortedWords.toArray(new SortableWord[sortedWords.size()]);
  }

  public InputWrapper(Normalizer normalizer, String inputString) {
    this(normalizer, new NormalizingTokenizer(normalizer, inputString).getTokens());
  }

  /**
   * Get the number of input words.
   */
  public int numWords() {
    return inputWords.length;
  }

  /**
   * Get the input word at the given (sorted) index.
   */
  public SortableWord getSortableWord(int index) {
    return inputWords[index];
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    for (int i = 0; i < inputWords.length; ++i) {
      result.append(inputWords[i].word.originalSubString);
      if (i + 1 < inputWords.length) result.append(' ');
    }

    return result.toString();
  }

  public static final int compareChars(char[] chars1, char[] chars2) {
    int result = 0;

    for (int i = 0; result == 0 && i < chars1.length && i < chars2.length; ++i) {
      result = (chars1[i] - chars2[i]);
    }

    if (result == 0) {
      result = (chars1.length - chars2.length);
    }

    return result;
  }

  public static final class SortableWord implements Comparable<SortableWord> {
    public final int wordNum;  // to preserve/reconstruct original order
    public final StringWrapper.SubString word;
    public final char[] wordChars;

    private Normalizer normalizer;
    private Boolean _looksLikeAcronym;

    public SortableWord(int wordNum, StringWrapper.SubString word, Normalizer normalizer) {
      this.wordNum = wordNum;
      this.word = word;
      this.normalizer = normalizer;
      this.wordChars = word.getNormalizedChars(normalizer);
      this._looksLikeAcronym = null;
    }

    public final boolean looksLikeAcronym() {
      if (_looksLikeAcronym == null) {
        _looksLikeAcronym = MatchUtil.looksLikeAcronym(word);
      }
      return _looksLikeAcronym;
    }

    public final String getNormalizedString() {
      return word.getNormalizedString(normalizer);
    }

    public int compareTo(SortableWord other) {
      // sort by chars
      int result = compareChars(this.wordChars, other.wordChars);

      if (result == 0) {
        // secondarily sort by wordNum
        result = (this.wordNum - other.wordNum);
      }

      return result;
    }
  }

  public static final class InputOrderComparator implements Comparator<SortableWord> {
    private InputOrderComparator() {
    }

    public int compare(SortableWord sw1, SortableWord sw2) {
      return sw1.wordNum - sw2.wordNum;
    }
    public boolean equals(Object o) {
      return (this == o) || (o instanceof InputOrderComparator);
    }
  }
}
