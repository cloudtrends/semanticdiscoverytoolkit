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
import org.sd.nlp.NormalizedString;
import org.sd.nlp.Normalizer;

import java.util.ArrayList;
import java.util.List;

/**
 * A WordGramSplitter for extracting capitalized phrases from text.
 * <p>
 * @author Spence Koehler
 */
public class CapitalizedPhraseSplitter extends AbstractWordGramSplitter {

  public static final int DEFAULT_SKIP = 2;
  public static final boolean DEFAULT_INCLUDE_ALL_CAPS = true;

  private int skip; // number of words that can be uncapitalized
  private boolean includeAllCaps;  // flag indicating whether an all caps word should be considered as capitalized

  /**
   * Construct an instance with the default skip.
   */
  public CapitalizedPhraseSplitter() {
    this(DEFAULT_SKIP, DEFAULT_INCLUDE_ALL_CAPS);
  }

  /**
   * Construct an instance with the default skip.
   */
  public CapitalizedPhraseSplitter(int skip, boolean includeAllCaps) {
    super(GeneralNormalizer.getCaseSensitiveInstance(), null/*wordAcceptor*/);

    this.skip = skip + 1;  // +1 because of how we use it in comparisons
    this.includeAllCaps = includeAllCaps;
  }

  /**
   * Get the number of words that can be uncapitalized between capitalized
   * words to still be acceptable.
   */
  public int getSkip() {
    return skip;
  }

  /**
   * Set the number of words that can be uncapitalized between capitalized
   * words to still be acceptable.
   */
  public void setSkip(int skip) {
    this.skip = skip;
  }

  /**
   * Get whether words that are all caps should be considered to be a
   * capitalized word.
   */
  public boolean getIncludeAllCaps() {
    return includeAllCaps;
  }

  /**
   * Set whether words that are all caps should be considered to be a
   * capitalized word.
   */
  public void setIncludeAllCaps(boolean includeAllCaps) {
    this.includeAllCaps = includeAllCaps;
  }

  /**
   * Get the word grams from the given words.
   */
  public List<String> getWordGrams(Word[] words) {
    final List<String> result = new ArrayList<String>();

    for (int index = firstCapitalized(0, words); index < words.length; ) {
      // special case: only one word and it is capitalized
      if (index == 0 && words.length == 1) {
        result.add(words[0].token);
        break;
      }
      else {
        // need to find the next capitalized word
        int lastCapIndex = index;
        for (int nextIndex = firstCapitalized(index + 1, words);
             nextIndex < words.length;
             nextIndex = firstCapitalized(nextIndex + 1, words)) {
          if (nextIndex - lastCapIndex <= skip) {
            lastCapIndex = nextIndex;
          }
          else break;
        }
        if (lastCapIndex > index ||             // must be more than 1 word
            lastCapIndex == index && index > 0  // unless it isn't the first word
          ) {
          // add an ngram from index (inclusive) to lastCapIndex (inclusive)
          result.add(concat(words, index, lastCapIndex + 1));
        }
        index = firstCapitalized(lastCapIndex + 1, words);
      }
    }

    return result;
  }

  /**
   * Use the instance's non-null normalizer to split the string.
   * <p>
   * This implementation will split on camel case.
   */
  protected String[] splitIntoTokens(Normalizer normalizer, String string) {
    final NormalizedString nString = normalizer.normalize(string);
    nString.setSplitOnCamelCase(true);
    return nString.split();
  }

  private final int firstCapitalized(int startIndex, Word[] words) {
    for (; startIndex < words.length; ++startIndex) {
      final Word word = words[startIndex];
      if ((word.acceptCode == AcceptCode.ACCEPT || word.acceptCode == AcceptCode.ACCEPT_SPLIT) &&
          !"".equals(word.token) &&
          Character.isUpperCase(word.token.codePointAt(0))) {  // found a capitlized word

        // check for all caps
        if (includeAllCaps || !isAllCaps(word.token)) {
          break;
        }
      }
    }
    return startIndex;
  }

  private final boolean isAllCaps(String token) {
    boolean result = true;

    final int len = token.length();
    for (int i = 0; i < len; ++i) {
      if (!Character.isUpperCase(token.codePointAt(i))) {
        result = false;
        break;
      }
    }

    return result;
  }
}
