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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper around a word sense for relationship traversal and context preservation.
 * <p>
 * @author Spence Koehler
 */
public class WordSenseWrapper {
  
  // where we are
  private WordId wordId;
  private WordSense wordSense;
  private Map<String, String> properties;
  private String[] _synonyms;

  // where we came from
  private WordSenseWrapper source;
  private int sourcePointerIndex;
  private Set<WordId> expanded;

  // where we can go: getNext()

  public WordSenseWrapper(WordSense wordSense) {
    this.wordId = new WordId(wordSense.getPartOfSpeech(), wordSense.getSynsetOffset(), 0);
    this.wordSense = wordSense;
    this.properties = null;
    this._synonyms = null;
    this.source = null;
    this.sourcePointerIndex = 0;
    this.expanded = new HashSet<WordId>();
  }

  private WordSenseWrapper(WordId wordId, WordSense wordSense, WordSenseWrapper source, int sourcePointerIndex, Set<WordId> expanded) {
    this.wordId = wordId;
    this.wordSense = wordSense;
    this.properties = null;
    this._synonyms = null;
    this.source = source;
    this.sourcePointerIndex = sourcePointerIndex;
    this.expanded = expanded;
  }

  /**
   * Get this wrapper's word identifier.
   */
  public WordId getWordId() {
    return wordId;
  }

  /**
   * Get this wrapper's word sense.
   */
  public WordSense getWordSense() {
    return wordSense;
  }

  /**
   * Get this word sense's synonyms.
   */
  public String[] getSynonyms() {
    if (_synonyms != null) {
      final WordNetFile.Entry entry = wordSense.getFileEntry();
      _synonyms = new String[entry.words.length];

      int index = 0;
      for (WordNetFile.Word word : entry.words) {
        _synonyms[index++] = word.word;
      }
    }
    return _synonyms;
  }

  public String getLexName() {
    return LexName.getLexName(wordSense.getFileEntry().lexFilenum).fileName;
  }

  /**
   * Get this wrapper's sense's identified word.
   * <p>
   * If this id represents the entire synset, get the first word in the synset.
   */
  public String getWord() {
    final int wordIndex = (wordId.wordNum == 0) ? 0 : wordId.wordNum - 1;
    final WordNetFile.Entry entry = wordSense.getFileEntry();
    return entry.words[wordIndex].word;
  }

  /**
   * Set a property on this instance.
   */
  public void setProperty(String attribute, String value) {
    if (properties == null) properties = new HashMap<String, String>();
    properties.put(attribute, value);
  }

  /**
   * Get this instance's property.
   */
  public String getProperty(String attribute) {
    String result = null;

    if (properties != null) {
      result = properties.get(attribute);
    }

    return result;
  }

  /**
   * Get the relation that brought us here from its source.
   *
   * @return the pointer symbol or null if this is the first visited sense after lookup.
   */
  public PointerSymbol getRelationFromSource() {
    PointerSymbol result = null;

    if (source != null) {
      final WordNetFile.Entry entry = source.getWordSense().getFileEntry();
      final WordNetFile.Pointer pointer = entry.pointers[sourcePointerIndex];
      result = pointer.pointerSymbol;
    }

    return result;
  }

  /**
   * Reset the expanded cache.
   */
  public void resetExpanded() {
    expanded.clear();
  }

  /**
   * Get the next word senses pointed to from this one, excluding the source word sense
   * and any others not accepted through the filter strategy.
   */
  public List<WordSenseWrapper> getNext(PointerFilter filterStrategy) {
    List<WordSenseWrapper> result = null;

    final WordNetFile.Entry entry = wordSense.getFileEntry();
    final WordId sourceId = (source == null) ? null : source.getWordId();

    if (expanded.contains(wordId)) return null;
    expanded.add(wordId);

    int pointerIndex = 0;
    for (WordNetFile.Pointer pointer : entry.pointers) {
      if (pointer.sourceOffset == 0 || pointer.sourceOffset == wordId.wordNum) {
        if (filterStrategy.accept(pointer, this)) {
          final WordId nextId = new WordId(pointer.partOfSpeech, pointer.synsetOffset, pointer.targetOffset);
          if (source == null || !nextId.equals(source.getWordId())) {  // don't ever go straight back
            final WordSense nextWordSense = wordSense.getInstance(nextId.partOfSpeech, nextId.synsetOffset);
            final WordSenseWrapper nextWrapper = new WordSenseWrapper(nextId, nextWordSense, this, pointerIndex, expanded);
            if (filterStrategy.accept(nextWrapper, this)) {
              if (result == null) result = new ArrayList<WordSenseWrapper>();
              result.add(nextWrapper);
            }
          }
        }
      }
      ++pointerIndex;
    }

    return result;
  }

  /**
   * Expand this word sense according to the filter strategy, applying the
   * operator to each include this. Before expanding, 'resetExpanded' will
   * be called.
   *
   * @return the word sense wrapper identified by the operator's thrown
   *         UnwindOperatorException or null.
   */
  public UnwindOperatorException expand(WordSenseOperator operator, PointerFilter filterStrategy) {
    UnwindOperatorException result = null;

    resetExpanded();

    try {
      doExpand(operator, filterStrategy, this, null);
    }
    catch (UnwindOperatorException e) {
      operator.handleUnwind(e);
      result = e;
    }

    return result;
  }

  /**
   * Recursive auxiliary for expand.
   */
  private final void doExpand(WordSenseOperator operator, PointerFilter filterStrategy,
                              WordSenseWrapper childWrapper, WordSenseWrapper parentWrapper) throws UnwindOperatorException {
    operator.operate(childWrapper, parentWrapper);

    final List<WordSenseWrapper> next = childWrapper.getNext(filterStrategy);  // expand the child

    if (next != null) {
      for (WordSenseWrapper w : next) {
        doExpand(operator, filterStrategy, w, childWrapper);  // child becomes the parent
      }
    }
  }

  /**
   * Get the instance that brought us here, or null if this is the first.
   */
  public WordSenseWrapper getSource() {
    return source;
  }

  /**
   * Get the depth of this word sense from where traversal started.
   */
  public int depth() {
    int result = 0;

    for (WordSenseWrapper prev = source; prev != null; prev = prev.getSource()) {
      ++result;
    }

    return result;
  }

  /**
   * Get the chain of senses found by following the given relation from this
   * sense.
   */
  public List<WordSenseWrapper> getRelationChain(PointerSymbol relation) {
    final CollectSensesOperator operator = new CollectSensesOperator();
    final PointerFilter filter = SingleRelationFilter.getInstance(relation);
    expand(operator, filter);
    return operator.getSenses();
  }

  public int hashCode() {
    return wordSense.hashCode() * 17 + wordId.wordNum;
  }

  public boolean equals(Object o) {
    boolean result = (this == o);

    if (!result && o instanceof WordSenseWrapper) {
      final WordSenseWrapper other = (WordSenseWrapper)o;

      result =
        this.wordSense.equals(other.wordSense) &&
        this.wordId.wordNum == other.wordId.wordNum;
    }

    return result;
  }

  /**
   * Get a string representation of this instance.
   */
  public String toString() {
    final StringBuilder result = new StringBuilder();

    if (source != null) {
      final String sourceWord = (source.getWordId().wordNum == 0) ? "*" : source.getWord();
      result.
        append(sourceWord).append(" --").
        append(getRelationFromSource().name().toLowerCase()).
        append("--> ");
    }

    result.append(wordId).append(' ');

    final WordNetFile.Entry entry = wordSense.getFileEntry();
    if (wordId.wordNum == 0) {
      result.append(entry);
    }
    else {
      result.append(getWord());
    }

    if (source == null) {
      result.append(" -- " + entry.gloss);
    }

    return result.toString();
  }
}
