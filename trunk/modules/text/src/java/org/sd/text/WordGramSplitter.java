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


import java.util.List;

/**
 * Utility to split a string into N-word grams.
 * <p>
 * @author Spence Koehler
 */
public interface WordGramSplitter {

  public enum AcceptCode {ACCEPT, REJECT_SPLIT, REJECT_IGNORE, ACCEPT_SPLIT};

  /**
   * Interface for accepting a word.
   */
  public interface WordAcceptor {
    /**
     * Determine whether to accept the given word.
     * <p>
     * @return ACCEPT if the word should participate in N-grams;
     *         REJECT_SPLIT if the word should not participate AND
     *                      is considered a break for building N-grams;
     *         REJECT_IGNORE if the word should not participate but
     *                       N-gram construction can continue.
     *         ACCEPT_SPLIT if the word should participate in N-grams
     *                      but only as the start of a new N-gram.
     */
    public AcceptCode accept(String word);
  }

  /**
   * Convenience method to get a string's tokens.
   */
  public String[] getTokens(String string);

  /**
   * Split the string into words suitable for use in getWordGrams.
   */
  public Word[] getWords(String string);

  /**
   * Turn the tokens into words suitable for use in getWordGrams.
   */
  public Word[] getWords(String[] tokens);

  /**
   * Turn the tokens into words suitable for use in getWordGrams.
   */
  public Word[] getWords(String[] tokens, int maxWords);

  /**
   * Get the word grams from the given string.
   */
  public List<String> getWordGrams(String string);

  /**
   * Get the word grams from the given words.
   */
  public List<String> getWordGrams(Word[] words);

  /**
   * Find the (first intance of the normalized) wordGram in the (original) text.
   * <p>
   * Note that if this splitter uses normalizations to compute wordGrams,
   * then the same normalizations will need to be applied to the text in
   * order to find the wordGram within.
   *
   * @return the wordGram's start (inclusive) and end (exclusive) positions in
   * the text or null if not present.
   */
  public int[] findWordGram(String wordGram, String text);


  /**
   * Container to bundle a token with its 'accepted' status.
   */
  public static final class Word {
    public final String token;
    public final AcceptCode acceptCode;

    Word(String token, AcceptCode acceptCode) {
      this.token = token;
      this.acceptCode = acceptCode;
    }
  }
}
