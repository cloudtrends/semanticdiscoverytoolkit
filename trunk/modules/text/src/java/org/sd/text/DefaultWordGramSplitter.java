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


import org.sd.nlp.GeneralNormalizer;
import org.sd.nlp.Normalizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility to split a string into N-word grams.
 * <p>
 * @author Spence Koehler
 */
public class DefaultWordGramSplitter extends AbstractWordGramSplitter {

  private int numWords;

  /**
   * Construct an instance that accepts all words.
   */
  public DefaultWordGramSplitter(int numWords, Normalizer normalizer) {
    this(numWords, normalizer, null);
  }

  /**
   * Construct an instance with the given params.
   */
  public DefaultWordGramSplitter(int numWords, Normalizer normalizer, WordAcceptor wordAcceptor) {
    super(normalizer, wordAcceptor);
    this.numWords = numWords;
  }

  /**
   * Get the word grams from the given words.
   */
  public List<String> getWordGrams(Word[] words) {
    final List<String> result = new ArrayList<String>();

    for (int i = 0; i <= words.length - numWords; ++i) {
      if (isValidNGram(words, i)) {
        result.add(concat(words, i));
      }
    }

    return result;
  }


  /**
   * Determine whether we have a valid ngram from words starting at fromPos.
   */
  private boolean isValidNGram(Word[] words, int fromPos) {
    int numWords = this.numWords;  // shadow
    if (fromPos + numWords > words.length) return false;

    boolean isValid = true;
    for (int i = fromPos; i < fromPos + numWords; ++i) {
      switch (words[i].acceptCode) {
        case ACCEPT :
          break;
        case REJECT_SPLIT :
          isValid = false;
          break;
        case REJECT_IGNORE :
          if (fromPos + numWords + 1 > words.length) {
            isValid = false;
          }
          else {
            ++numWords;
          }
          break;
      }

      if (!isValid) break;
    }

    return isValid;
  }

  /**
   * Concatenate word tokens starting at fromPos.
   */
  private String concat(Word[] words, int fromPos) {
    return super.concatN(words, fromPos, this.numWords);
  }


  public static void main(String[] args) {
    //args0: N
    //args1+: strings to split

    final DefaultWordGramSplitter splitter = new DefaultWordGramSplitter(Integer.parseInt(args[0]), GeneralNormalizer.getCaseInsensitiveInstance(), null);

    for (int i = 1; i < args.length; ++i) {
      final String string = args[i];
      final List<String> wordGrams = splitter.getWordGrams(string);
      System.out.println(string + " -> " + wordGrams);
    }
  }
}
