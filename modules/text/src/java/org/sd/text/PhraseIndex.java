/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility for storing/retrieving multi-word (phrase) sequences associated
¤ * with one or more values (e.g., ambiguity allowed).
 * <p>
 * @author Spence Koehler
 */
public class PhraseIndex <T> {

  private Map<String, Set<T>> word2items;
  private Map<T, List<String[]>> item2words;

  public PhraseIndex() {
    this.word2items = new HashMap<String, Set<T>>();
    this.item2words = new HashMap<T, List<String[]>>();
  }

  public void clear() {
    this.word2items.clear();
    this.item2words.clear();
  }

  /**
   * Associate the (normalized) word sequence with the value.
   */
  public void put(String[] words, T value) {
    // update item2words
    List<String[]> wordLists = item2words.get(value);
    if (wordLists == null) {
      wordLists = new ArrayList<String[]>();
      item2words.put(value, wordLists);
    }
    wordLists.add(words);

    // update word2items
    for (String word : words) {
      Set<T> items = word2items.get(word);
      if (items == null) {
        items = new HashSet<T>();
        word2items.put(word, items);
      }
      items.add(value);
    }
  }

  /**
   * Get the ranked (from lowest to highest distance) retrieval results for the
   * given string.
   */
  public List<RetrievalResult<T>> get(String[] words) {
    List<RetrievalResult<T>> result = null;

    // Algorithm:
    // - Looking up each word, find values that are common across words
    //   - Consider only the values that appeared most frequently across all words
    //   - Consider only phrases including words with the lowest cardinality lookups

    Map<T, Integer> values = null;  // values w/inst freq across words
    int maxIxCount = 0;
    int minCardinality = Integer.MAX_VALUE;

    final Map<String, WordWrapper> wordWrappers = new HashMap<String, WordWrapper>();
    for (String word : words) {
      final Set<T> items = word2items.get(word);
      wordWrappers.put(word, new WordWrapper<T>(word, items));
      if (items != null) {
        final int numItems = items.size();
        if (numItems < minCardinality) {
          minCardinality = numItems;
        }
        if (values == null) {
          values = new HashMap<T, Integer>();
        }
        for (T item : items) {
          final Integer count = values.get(item);
          final int ucount = (count == null) ? 1 : count + 1;
          values.put(item, ucount);
          if (ucount > maxIxCount) {
            maxIxCount = ucount;
          }
        }
      }
    }

    if (maxIxCount > 0) {
      final CharDictionary dictionary = new CharDictionary();
      final char[] inputChars = dictionary.lookup(words);
      result = new ArrayList<RetrievalResult<T>>();

      // Words that must be present in retrieved phrase
      final Set<String> mustHaveWords = new HashSet<String>();
      for (Map.Entry<String, WordWrapper> entry : wordWrappers.entrySet()) {
        final String word = entry.getKey();
        final WordWrapper wordWrapper = entry.getValue();
        if (wordWrapper.cardinality() == minCardinality) {
          mustHaveWords.add(word);
        }
      }
      final Set<Character> mustHaveWordChars = new HashSet<Character>();
      for (String mustHaveWord : mustHaveWords) {
        mustHaveWordChars.add(dictionary.lookup(mustHaveWord));
      }

      for (Map.Entry<T, Integer> entry : values.entrySet()) {
        final T item = entry.getKey();
        final Integer count = entry.getValue();
        if (count < maxIxCount) continue;  // consider only most frequent values

        final List<String[]> wordLists = item2words.get(item);
        if (wordLists != null) {
          String[] bestWords = null;
          int bestDist = Integer.MAX_VALUE;
          for (String[] wordList : wordLists) {
            //final char[] storedChars = dictionary.lookup(wordList);

            boolean hasMustHave = false;
            final char[] storedChars = new char[wordList.length];
            for (int i = 0; i < wordList.length; ++i) {
              final String word = wordList[i];
              final char wordChar = dictionary.lookup(word);
              storedChars[i] = wordChar;
              if (mustHaveWordChars.contains(wordChar)) {
                hasMustHave = true;
              }
            }
            if (!hasMustHave) continue;


            final int dist = getEditDistance(storedChars, inputChars);
            if (dist < bestDist) {
              bestDist = dist;
              bestWords = wordList;

              if (dist == 0) break;
            }
          }
          if (bestDist < Integer.MAX_VALUE) {
            result.add(new RetrievalResult<T>(item, maxIxCount - bestDist, bestWords, words));
          }
        }
      }
    }

    if (result != null) Collections.sort(result);
    return result;
  }

  protected int getEditDistance(char[] storedChars, char[] inputChars) {
    return EditDistance.lev(storedChars, inputChars);
  }


  public static final class RetrievalResult <T> implements Comparable<RetrievalResult<T>> {
    private T value;
    private int score;
    private String[] matchedWords;
    private String[] inputWords;

    public RetrievalResult(T value, int score, String[] matchedWords, String[] inputWords) {
      this.value = value;
      this.score = score;
      this.matchedWords = matchedWords;
      this.inputWords = inputWords;
    }

    /** Support natural ordering from highest to lowest score. */
    public int compareTo(RetrievalResult<T> other) {
      return other.score - this.score;
    }

    public T getValue() {
      return value;
    }

    public int getScore() {
      return score;
    }

    public String[] getMatchedWords() {
      return matchedWords;
    }

    public String[] getInputWords() {
      return inputWords;
    }
  }

  private static final class WordWrapper <T> {
    public final String word;
    public final Set<T> items;

    public WordWrapper(String word, Set<T> items) {
      this.word = word;
      this.items = items;
    }

    public boolean hasItems() {
      return items != null && items.size() > 0;
    }

    public int cardinality() {
      return (items == null) ? 0 : items.size();
    }
  }

  private static final class CharDictionary {
    private char nextC;
    private Map<String, Character> map;
    public CharDictionary() {
      this.map = new HashMap<String, Character>();
      this.nextC = 'A';
    }
    public char[] lookup(String[] words) {
      final int n = words.length;
      char[] result = new char[n];

      for (int i = 0; i < n; ++i) {
        result[i] = lookup(words[i]);
      }

      return result;
    }
    public char lookup(String word) {
      Character result = map.get(word);
      if (result == null) {
        result = nextC++;
        map.put(word, result);
      }
      return result;
    }
  }
}
