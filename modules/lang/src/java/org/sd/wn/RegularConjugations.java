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
package org.sd.wn;


import java.util.ArrayList;
import java.util.List;

/**
 * Utility for regular English conjugations.
 * <p>
 * @author Spence Koehler
 */
public class RegularConjugations {
  
  private static final RegularConjugations INSTANCE = new RegularConjugations();

  public static final RegularConjugations getInstance() {
    return INSTANCE;
  }


  private RegularConjugations() {
  }


  /**
   * Get the stem of a(n English plural) word (noun).
   * <p>
   * Transform:
   *    x-ies to x,
   *    x-['e]s to x,
   *    x-[s'] to x,
   *    x-s to x, (as long as x doesn't end with 's')
   *    x-[ey] to x
   *    x to x.
   */
  public String getStem(String word) {
    String result = word;

    final int len = word.length();
    final char last = word.charAt(len - 1);
    if (last == 's') {
      if (len > 4 && word.endsWith("ies")) {
        result = word.substring(0, len - 3);
      }
      else if (len > 3) {
        final char penu = word.charAt(len - 2);
        if (penu == 'e' || penu == '\'') {
          result = word.substring(0, len - 2);
        }
        else if (penu != 's') {
          result = word.substring(0, len - 1);
        }
      }
    }
    else if (last == 'e' || last == 'y' || last == '\'') {
      result = word.substring(0, len - 1);
    }

    return result;
  }

  public List<Word> getPotentialWords(String word) {
    //NOTE: since we're dealing with English here, we're not worrying about utf-8 code points.
    return getPotentialWords(word, word.toCharArray());
  }

  public List<Word> getPotentialWords(String word, char[] wordChars) {
    //NOTE: since we're dealing with English here, we're not worrying about utf-8 code points.
    List<Word> result = null;

    if (wordChars == null || wordChars.length <= 2) return result;
    final int len = wordChars.length;

    final char c0 = wordChars[len - 1];
    if (c0 == 's') {
      final char c1 = wordChars[len - 2];

      if (result == null) result = new ArrayList<Word>();

      // -es or -'s (regular noun, strip 's or es)
      if (c1 == 'e' || c1 == '\'') {
        addWord(result, word, wordChars, len, len - 2, POS.NOUN, POS.NOUN);
        if (c1 == 'e') {
          addWord(result, word, wordChars, len, len - 1, POS.NOUN, POS.NOUN);
        }
      }
      // -s (regular verb or noun inflection)
      else {
        addWord(result, word, wordChars, len, len - 1, POS.NOUN, POS.NOUN);
        addWord(result, word, wordChars, len, len - 1, POS.VERB, POS.NOUN);
      }
    }
    else if (c0 == 'd') {
      // -ed (regular verb inflection)
      if (len > 2) {
        final char c1 = wordChars[len - 2];
        if (c1 == 'e') {
          if (result == null) result = new ArrayList<Word>();
          addWord(result, word, wordChars, len, len - 2, POS.VERB, POS.VERB);
        }
      }
    }
    else if (c0 == 'g') {
      // -ing (regular verb inflection)
      if (len > 3) {
        final char c1 = wordChars[len - 2];
        if (c1 == 'n') {
          final char c2 = wordChars[len - 3];
          if (c2 == 'i') {
            if (result == null) result = new ArrayList<Word>();
            addWord(result, word, wordChars, len, len - 3, POS.VERB, POS.VERB);
          }
        }
      }
    }
    else if (c0 == 'r') {
      // -er (regular verb or adjective/adverb inflection)
      if (len > 2) {
        final char c1 = wordChars[len - 2];
        if (c1 == 'e') {
          if (result == null) result = new ArrayList<Word>();
          addWord(result, word, wordChars, len, len - 2, POS.NOUN, POS.VERB);
          addWord(result, word, wordChars, len, len - 2, POS.ADJ, POS.VERB);
        }
      }
    }
    else if (c0 == 't') {
      // -est (regular adjective/adverb inflection)
      if (len > 3) {
        final char c1 = wordChars[len - 2];
        if (c1 == 's') {
          final char c2 = wordChars[len - 3];
          if (c2 == 'e') {
            if (result == null) result = new ArrayList<Word>();
            addWord(result, word, wordChars, len, len - 3, POS.ADJ, POS.ADJ);
          }
        }
      }
    }
    else if (c0 == 'y') {
      // -ly (regular adverb from an adjective)
      if (len > 2) {
        final char c1 = wordChars[len - 2];
        if (c1 == 'l') {
          if (result == null) result = new ArrayList<Word>();
          addWord(result, word, wordChars, len, len - 2, POS.ADV, POS.ADJ);
        }
      }
    }

    return result;
  }

  private static final String substring(char[] chars, int startPos, int endPos) {
    final StringBuilder result = new StringBuilder();

    for (int i = startPos; i < endPos; ++i) {
      result.append(chars[i]);
    }

    return result.toString();
  }

  private static final void addWord(List<Word> result, String word, char[] wordChars, int len, int endIndex, POS wordPos, POS basePos) {
    String stem = null;
    String base = null;

    // check for 'i' before an 'e' at endIndex (to change i to y)
    // or for ending in a double letter (to offer forms w/double or single letter end)

    boolean i2y = false;  // convert i to y
    boolean dbl = false;  // ends in double

    if (endIndex < len && endIndex - 1 > 0) {
      // check for 'i' before an 'e' at endIndex
      if (wordChars[endIndex] == 'e' && wordChars[endIndex - 1] == 'i') {
        i2y = true;
      }
      // check for ending in double letter
      else if (endIndex - 2 > 0 && wordChars[endIndex - 1] == wordChars[endIndex - 2]) {
        dbl = true;
      }
    }

    if (i2y) {
      // change i to y
      stem = substring(wordChars, 0, endIndex - 1);
      base = stem + 'y';
    }
    else if (dbl) {
      // ends in double letter
      stem = substring(wordChars, 0, endIndex - 1);
      base = substring(wordChars, 0, endIndex);
      // keep the double-letter version (here) and the single-letter version (later)
      result.add(new Word(word, stem, base, wordPos, basePos, wordChars));

      base = stem;
    }
    else {
      stem = substring(wordChars, 0, endIndex);
      base = stem;
    }

    result.add(new Word(word, stem, base, wordPos, basePos, wordChars));
  }


  public static final class Word {
    public final String input;
    public final String stem;
    public final String base;
    public final POS wordPos;
    public final POS basePos;
    public final char[] chars;

    public Word(String input, String stem, String base, POS wordPos, POS basePos, char[] chars) {
      this.input = input;
      this.stem = stem;
      this.base = base;
      this.wordPos = wordPos;
      this.basePos = basePos;
      this.chars = chars;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.
        append(input).
        append('(').append(wordPos).append(')').
        append(" <-- ").
        append(base).
        append('(').append(basePos).append(')');

      return result.toString();
    }
  }


  public static final void main(String[] args) {
    final RegularConjugations rc = RegularConjugations.getInstance();

    for (String arg : args) {
      final List<Word> words = rc.getPotentialWords(arg);
      if (words == null) {
        System.out.println(arg);
      }
      else {
        for (Word word : words) {
          System.out.println(word);
        }
      }
    }
  }
}
