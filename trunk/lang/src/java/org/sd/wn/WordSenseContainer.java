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


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility to manage word senses for an input word.
 * <p>
 * @author Spence Koehler
 */
public class WordSenseContainer {

  public static final String SENSE_NUM_PROPERTY = "senseNum";

  private File dictDir;
  private String inputWord;
  private Map<POS, List<WordSenseWrapper>> pos2senses;
  private int numWordSenses;

  private List<WordSenseWrapper> _wrappedWordSenses;

  public WordSenseContainer(File dictDir, String inputWord) throws IOException {
    if (dictDir == null) dictDir = WordNetUtils.getDefaultDictDir();

    this.dictDir = dictDir;
    this.inputWord = inputWord;
    this.pos2senses = new LinkedHashMap<POS, List<WordSenseWrapper>>();
    this.numWordSenses = 0;
    this._wrappedWordSenses = null;

    for (Iterator<WordSenseWrapper> iter = new WordSenseIterator(dictDir, inputWord); iter.hasNext(); ) {
      final WordSenseWrapper wrapper = iter.next();

      final POS pos = wrapper.getWordSense().getPartOfSpeech();
      List<WordSenseWrapper> senses = pos2senses.get(pos);
      if (senses == null) {
        senses = new ArrayList<WordSenseWrapper>();
        pos2senses.put(pos, senses);
      }

      final int senseNum = senses.size();
      wrapper.setProperty(SENSE_NUM_PROPERTY, Integer.toString(senseNum));

      senses.add(wrapper);
      ++numWordSenses;
    }
  }

  /**
   * Get the input word.
   */
  public String getInputWord() {
    return inputWord;
  }

  /**
   * Get the WordNet dictDir.
   */
  public File getDictDir() {
    return dictDir;
  }

  /**
   * Get the number of word senses found for the input word.
   */
  public int getNumWordSenses() {
    return numWordSenses;
  }

  /**
   * Get the number of word senses found for the input word having the given part of speech.
   */
  public int getNumWordSenses(POS partOfSpeech) {
    int result = 0;

    final List<WordSenseWrapper> senses = pos2senses.get(partOfSpeech);
    if (senses != null) result = senses.size();

    return result;
  }

  /**
   * Get the wrapped word senses for the input word.
   */
  public List<WordSenseWrapper> getWrappedWordSenses() {
    if (_wrappedWordSenses == null) {
      _wrappedWordSenses = new ArrayList<WordSenseWrapper>();
      for (List<WordSenseWrapper> senses : pos2senses.values()) {
        _wrappedWordSenses.addAll(senses);
      }
    }
    return _wrappedWordSenses;
  }

  /**
   * Get just those word senses that have the given part of speech.
   */
  public List<WordSenseWrapper> getWrappedWordSenses(POS partOfSpeech) {
    return pos2senses.get(partOfSpeech);
  }

  /**
   * Determine whether the given part of speech is present for the word.
   */
  public boolean hasPartOfSpeech(POS partOfSpeech) {
    return pos2senses.containsKey(partOfSpeech);
  }

  /**
   * Get the parts of speech found for the input word.
   */
  public Set<POS> getPartsOfSpeech() {
    return pos2senses.keySet();
  }

  
//todo: add expansion accessors. call wordSenseWrapper.resetExpanded() before performing an expansion.
//
// use cases: (use WordSenseOperator impls using "expand" in WordSenseWrapper)
//  - Word2Dot,   x Word2DotOperator
//  - Word2Xml,
//  - Word2Text,  x ShowSenseOperator, (see WordSenseIterator.showAll)
//  - getRelationChain(hypernym),   x WordSenseWrapper.getRelationChain(hypernym)
//  - lowestCommonParent(hypernym, otherWordSenseWrapper)

}
