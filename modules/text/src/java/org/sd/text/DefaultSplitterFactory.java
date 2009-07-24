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


import java.io.File;
import java.util.Properties;

import org.sd.nlp.Normalizer;
import org.sd.text.WordGramSplitter.WordAcceptor;
import org.sd.util.ReflectUtil;

/**
 * Default implementation of the SplitterFactory interface to generate
 * DefaultWordGramSplitter instances.
 * <p>
 * @author Spence Koehler
 */
public class DefaultSplitterFactory implements SplitterFactory {

  protected Normalizer normalizer;
  protected WordAcceptor wordAcceptor;

  /**
   * Default constructor.
   * <p>
   * Sets normalizer to an IndexingNormalizer with a null wordAcceptor.
   */
  public DefaultSplitterFactory() {
    this.normalizer = IndexingNormalizer.getInstance(IndexingNormalizer.DEFAULT_INDEXING_OPTIONS);
    this.wordAcceptor = null;
  }

  /**
   * Properties based constructor.
   * <p>
   * Properties:
   * <ul>
   * <li>normalizer -- (optional, default=IndexingNormalizer) specifies the
   *                   normalizer to use in a format suitable for ReflectUtil
   *                   to build.</li>
   * <li>acceptor -- (optional, default=null) specifies the wordAcceptor to use
   *                 in a format suitable for ReflectUtil to build.</li>
   * </ul>
   */
  public DefaultSplitterFactory(Properties properties) {
    final String normalizerString = properties.getProperty("normalizer");
    final String wordAcceptorString = properties.getProperty("acceptor");

    if (normalizerString != null) {
      this.normalizer = (Normalizer)ReflectUtil.buildInstance(normalizerString, properties);
    }
    else {
      this.normalizer = IndexingNormalizer.getInstance(IndexingNormalizer.DEFAULT_INDEXING_OPTIONS);
    }

    if (wordAcceptorString != null) {
      this.wordAcceptor = (WordAcceptor)ReflectUtil.buildInstance(wordAcceptorString, properties);
    }
    else {
      this.wordAcceptor = null;
    }
  }

  /**
   * Create a WordGramSplitter instance for WordGrams with the given number of
   * words.
   *
   * @return a WordGramSplitter instance for the given number of words.
   */
  public WordGramSplitter getSplitter(int numWords) {
    return new DefaultWordGramSplitter(numWords, normalizer, wordAcceptor);
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
}
