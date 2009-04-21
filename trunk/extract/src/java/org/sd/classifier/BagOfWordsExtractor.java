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
package org.sd.classifier;


import org.sd.extract.Extraction;
import org.sd.extract.Extractor;
import org.sd.extract.StopwordsBasedAcceptor;
import org.sd.extract.WordAcceptor;
import org.sd.extract.WordsExtractor;
import org.sd.nlp.GeneralNormalizer;
import org.sd.nlp.Normalizer;
import org.sd.text.lucene.LuceneStore;

import java.util.HashSet;
import java.util.Set;

/**
 * A primary feature extractor for extracting a bag of words.
 * <p>
 * @author Spence Koehler
 */
public class BagOfWordsExtractor extends PrimaryFeatureExtractor {

  /**
   * Factory method to get an instance with a words extractor.
   */
  public static final BagOfWordsExtractor getWordsInstance(String bagName, Integer maxWords) {
    final BagOfWordsExtractor result = new BagOfWordsExtractor(bagName, maxWords);
    result.addWordsExtractor();
    return result;
  }

  /**
   * Factory method to get an instance with a words extractor and the given params.
   */
  public static final BagOfWordsExtractor getWordsInstance(String bagName, Integer maxWords, WordAcceptor wordAcceptor) {
    final BagOfWordsExtractor result = new BagOfWordsExtractor(bagName, maxWords);
    result.addWordsExtractor(wordAcceptor);
    return result;
  }

  /**
   * Factory method to get an instance with an extractor.
   */
  public static final BagOfWordsExtractor getInstance(String bagName, Integer maxWords, Extractor extractor) {
    final BagOfWordsExtractor result = new BagOfWordsExtractor(bagName, maxWords);
    result.addExtractor(extractor);
    return result;
  }

  /**
   * Factory method to get an instance with an extractor.
   */
  public static final BagOfWordsExtractor getInstance(String bagName, Integer n, Integer maxWords, Extractor extractor) {
    final BagOfWordsExtractor result = new BagOfWordsExtractor(bagName, n, maxWords);
    result.addExtractor(extractor);
    return result;
  }

  /**
   * Factory method to get an instance with an extractor and the given params.
   */
  public static final BagOfWordsExtractor getInstance(String bagName, Integer maxWords, Extractor extractor, WordAcceptor wordAcceptor) {
    final BagOfWordsExtractor result = new BagOfWordsExtractor(bagName, maxWords);
    result.addExtractor(extractor, wordAcceptor);
    return result;
  }

  /**
   * Factory method to get an instance with an extractor and the given params.
   */
  public static final BagOfWordsExtractor getInstance(String bagName, int n, Integer maxWords, Extractor extractor, WordAcceptor wordAcceptor) {
    final BagOfWordsExtractor result = new BagOfWordsExtractor(bagName, n, maxWords);
    result.addExtractor(extractor, wordAcceptor);
    return result;
  }


  private String bagName;
  private Integer maxWords;
  private Integer n;
  private Set<String> words;
  private String bagPrefix;

  public BagOfWordsExtractor(String bagName) {
    this(bagName, null);
  }

  public BagOfWordsExtractor(String bagName, Integer maxWords) {
    super();

    this.bagName = bagName;
    this.maxWords = maxWords;
    this.words = maxWords != null ? new HashSet<String>() : null;
    this.bagPrefix = "_" + bagName + "_";
    this.n = null;
  }

  public BagOfWordsExtractor(String bagName, int n, Integer maxWords) {
    this(bagName, maxWords);
    this.n = n;
  }

  /**
   * Add a default words extractor with a GeneralNormalizer and LuceneStore.stopWords.
   */
  public final void addWordsExtractor() {
    addWordsExtractor(StopwordsBasedAcceptor.getDefaultInstance());
  }

  /**
   * Add a words extractor with the given normalizer and stopwords.
   */
  public final void addWordsExtractor(WordAcceptor wordAcceptor) {
    super.addExtractor(new WordsExtractor(bagName, wordAcceptor, n), null);
  }

  /**
   * Add the given extractor.
   * <p>
   * Note that its extracted strings will be split into words for the bag
   * using the default normalizer (case-insensitive GeneralNormalizer) and
   * stopwords (lucene store's stopwords).
   */
  public final void addExtractor(Extractor extractor) {
    addExtractor(extractor, StopwordsBasedAcceptor.getDefaultInstance());
  }

  /**
   * Add the given extractor.
   */
  public final void addExtractor(Extractor extractor, WordAcceptor wordAcceptor) {
    super.addExtractor(new WordsExtractor(extractor, wordAcceptor, n), null);
  }

//todo: make properties-based constructor.

//todo: set a limit on the maximum number of words; apply stopwords?
//todo: only keep words that appear on more than one site. (Use BagTrimmer isa FeatureSelector utility over an arff)

  /**
   * Override to set append the bagName to each extracted string.
   */
  protected boolean setAttributeValue(FeatureVector result, Extraction extraction, FeatureDictionary featureDictionary) {
    boolean rv = true;

    final String word = extraction.asString();
    
    if (isValidWord(word)) {
      final String attributeName = bagPrefix + extraction.asString();

/*
  final NominalFeatureAttribute attribute = featureDictionary.getNominalFeatureAttribute(attributeName, attributeName);
  if (attribute != null) {
  // add the "*NotPresent*" value to the attribute.
  featureDictionary.getNominalFeatureAttribute(attributeName, NOT_PRESENT_VALUE);

  // set the "default value" to "not present".
  attribute.setDefaultValue(1.0);

  rv = result.setValue(attribute, attributeName);
  }
*/
      final IntegerFeatureAttribute attribute = featureDictionary.getIntegerFeatureAttribute(attributeName);
      if (attribute != null) {
        attribute.setDefaultValue(0.0);
        rv = result.setValue(attribute, 1.0);
      }
    }

    return true;  //rv
  }

  /**
   * Default valid words are 2 or more chars.
   */
  protected boolean isValidWord(String word) {
    boolean result = (word.length() > 1);  //note: stopwords are filtered in the WordsExtractor

    if (result && words != null && !words.contains(word)) {
      if (words.size() >= maxWords) {
        result = false;
      }
      else {
        words.add(word);
      }
    }

    return result;
  }
}
