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


import org.sd.nlp.NormalizedString;
import org.sd.nlp.Normalizer;

import java.util.List;

/**
 * Abstract implementation of the WordGramSplitter interface.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractWordGramSplitter implements WordGramSplitter {
  
  public static final char DEFAULT_CONCAT_DELIM = ' ';

  private Normalizer normalizer;
  private WordAcceptor wordAcceptor;
  private char concatDelim = DEFAULT_CONCAT_DELIM;

  /**
   * Default constructor creates an instance with null normalizer and
   * wordAcceptor.
   */
  protected AbstractWordGramSplitter() {
    this(null, null);
  }

  /**
   * Construct with the given normalizer and wordAcceptor.
   * <p>
   * NOTE: The normalizer and/or wordAcceptor can be null.
   */
  protected AbstractWordGramSplitter(Normalizer normalizer, WordAcceptor wordAcceptor) {
    this.normalizer = normalizer;
    this.wordAcceptor = wordAcceptor;
  }

  /**
   * Set this instance's normalizer.
   */
  protected void setNormalizer(Normalizer normalizer) {
    this.normalizer = normalizer;
  }

  /**
   * Set this instance's wordAcceptor.
   */
  protected void setWordAcceptor(WordAcceptor wordAcceptor) {
    this.wordAcceptor = wordAcceptor;
  }

  /**
   * Set the concatDelim.
   * <p>
   * Note that if never set, the concatDelim defaults to a single space.
   * To set the concatDelim as "nothing", use (char)0. This would be useful
   * for asian character strings, for example.
   */
  public void setConcatDelim(char concatDelim) {
    this.concatDelim = concatDelim;
  }

  /**
   * Get this instance's normalizer.
   */
  public Normalizer getNormalizer() {
    return normalizer;
  }

  /**
   * Get this instance's wordAcceptor.
   */
  public WordAcceptor getWordAcceptor() {
    return wordAcceptor;
  }

  /**
   * Convenience method to get a string's tokens.
   */
  public String[] getTokens(String string) {
    String[] result = null;

    if (string == null) {
      result = new String[0];
    }
    else if (normalizer == null) {
      result = string.split("\\s+");
    }
    else {
      result = splitIntoTokens(normalizer, string);
    }

    return result;
  }

  /**
   * Use the instance's non-null normalizer to split the string.
   * <p>
   * By default, this does NOT split on camel case.
   */
  protected String[] splitIntoTokens(Normalizer normalizer, String string) {
    final NormalizedString nString = normalizer.normalize(string);
    nString.setSplitOnCamelCase(false);
    return nString.split();
  }

  /**
   * Split the string into words suitable for use in getWordGrams.
   */
  public Word[] getWords(String string) {
    return getWords(getTokens(string), 0);
  }

  /**
   * Turn the tokens into words suitable for use in getWordGrams.
   */
  public Word[] getWords(String[] tokens) {
    return getWords(tokens, 0);
  }

  /**
   * Turn the tokens into words suitable for use in getWordGrams.
   */
  public Word[] getWords(String[] tokens, int maxWords) {
    final int numTokens = maxWords == 0 ? tokens.length : Math.min(tokens.length, maxWords);
    final Word[] result = new Word[numTokens];

    for (int i = 0; i < numTokens; ++i) {
      final String token = tokens[i];
      result[i] = new Word(token, wordAcceptor == null ? AcceptCode.ACCEPT : wordAcceptor.accept(token));
    }

    return result;
  }

  /**
   * Get the word grams from the given string.
   */
  public List<String> getWordGrams(String string) {
    return getWordGrams(getWords(string));
  }

  /**
   * Find the (first instance of the) wordGram in the text.
   * <p>
   * Note that if this splitter uses normalizations to compute wordGrams,
   * then the same normalizations will need to be applied to the text in
   * order to find the wordGram within.
   *
   * @return the wordGram's start and end positions in the text or null if not present.
   */
  public int[] findWordGram(String wordGram, String text) {
    int[] result = null;

    if (normalizer == null) {
      final int pos = text.indexOf(wordGram);
      if (pos >= 0) {
        result = new int[] {pos, pos + wordGram.length()};
      }
    }
    else {
      final NormalizedString nString = normalizer.normalize(text);
      final String normalized = nString.getNormalized();
      final int nPos = normalized.indexOf(wordGram);
      if (nPos >= 0) {
        result = new int[] {
          nString.getOriginalIndex(nPos),
          nString.getOriginalIndex(nPos + wordGram.length())
        };
      }
    }

    return result;
  }

  /**
   * Concatenate word tokens starting at fromPos (inclusive) until toPos (exclusive).
   */
  protected String concat(Word[] words, int fromPos, int toPos) {
    final StringBuilder result = new StringBuilder();

    for (int i = fromPos; i < toPos; ++i) {
      final Word word = words[i];
      if (word.acceptCode == AcceptCode.REJECT_IGNORE) {
        continue;
      }

      final String token = word.token;
      if (result.length() > 0) concatDelim(result);
      result.append(token);
    }

    return result.toString();
  }

  /**
   * Concatenate N (non-ignored) word tokens starting at fromPos (inclusive).
   */
  protected String concatN(Word[] words, int fromPos, int n) {
    final StringBuilder result = new StringBuilder();

    int numWords = n;
    for (int i = fromPos; i < fromPos + numWords; ++i) {
      final Word word = words[i];
      if (word.acceptCode == AcceptCode.REJECT_IGNORE) {
        ++numWords;
        continue;
      }

      final String token = word.token;
      if (result.length() > 0) concatDelim(result);
      result.append(token);
    }

    return result.toString();
  }

  /**
   * Concatenate a word separating delimiter onto the end of the builder.
   * <p>
   * This implementation adds a single space char.
   */
  protected void concatDelim(StringBuilder builder) {
    if (concatDelim > 0) builder.append(concatDelim);
  }
}
