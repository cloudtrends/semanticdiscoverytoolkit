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
package org.sd.extract;


import org.sd.nlp.GeneralNormalizer;
import org.sd.nlp.NormalizedString;
import org.sd.nlp.Normalizer;
import org.sd.text.lucene.LuceneStore;

import java.util.HashSet;
import java.util.Set;

/**
 * A word acceptor that accepts all but stopwords.
 * <p>
 * @author Spence Koehler
 */
public class StopwordsBasedAcceptor extends BaseWordAcceptor {
  
  private static final StopwordsBasedAcceptor DEFAULT_INSTANCE = new StopwordsBasedAcceptor(GeneralNormalizer.getCaseInsensitiveInstance(), LuceneStore.getStopWords());

  public static final StopwordsBasedAcceptor getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }


  private Set<String> stopwords;

  /**
   * Construct with default stopwords and no normalizer.
   * <p>
   * Current defaults are borrowed from lucene.
   */
  public StopwordsBasedAcceptor() {
    this(null, LuceneStore.getStopWords());
  }

  /**
   * Construct with the given normalizer and stopwords.
   */
  public StopwordsBasedAcceptor(Normalizer normalizer, String[] stopwords) {
    super(normalizer);
    this.stopwords = initStopwords(stopwords);
  }

  private final Set<String> initStopwords(String[] stopwords) {
    Set<String> result = null;

    if (stopwords != null && stopwords.length > 0) {
      result = new HashSet<String>();

      for (String stopword : stopwords) {
        final String[] words = split(stopword, false);
        for (String word : words) result.add(word);
      }
    }

    return result;
  }

  /**
   * Test whether the word is acceptable.
   *
   * @return true to accept the word; otherwise, false.
   */
  protected boolean doAccept(NormalizedString normalizedWord) {
    return doAccept(normalizedWord.getNormalized());
  }

  /**
   * Test whether the word is acceptable.
   *
   * @return true to accept the word; otherwise, false.
   */
  protected boolean doAccept(String normalizedWord) {
    return stopwords == null || !stopwords.contains(normalizedWord);
  }
}
