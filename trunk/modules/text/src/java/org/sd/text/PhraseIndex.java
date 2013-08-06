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
  private Set<String> mandatoryWords;

  public PhraseIndex() {
    this.word2items = new HashMap<String, Set<T>>();
    this.item2words = new HashMap<T, List<String[]>>();
    this.mandatoryWords = new HashSet<String>();
  }

  public Map<String, Set<T>> getWord2Items() {
    return word2items;
  }

  public Map<T, List<String[]>> getItem2Words() {
    return item2words;
  }

  public Set<String> getMandatoryWords() {
    return mandatoryWords;
  }

  public void clear() {
    this.word2items.clear();
    this.item2words.clear();
    this.mandatoryWords.clear();
  }

  /**
   * Add words that when present in input must match a retrieved value.
   */
  public void addMandatoryWords(String[] mandatoryWords) {
    for (String mandatoryWord : mandatoryWords) {
      this.mandatoryWords.add(mandatoryWord);
    }
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
   *
   * @param inputString  Original input string (for reference).
   * @param words  Normalized words (for lookup).
   *
   * @return retrieval results.
   */
  public List<RetrievalResult<T>> get(String inputString, String[] words) {
    List<RetrievalResult<T>> result = null;

    // Algorithm:
    // - Looking up each word, find values that are common across words
    //   - Consider only the values that appeared most frequently across all words
    //   - Consider only phrases including words with the lowest cardinality lookups
    //   - If a "mandatory" word is in the input, then it must be present in
    //     a phrase in the index to match.
    //     - this has the effect that words that tend to mark a split between phrases
    //       when present in the input, can still have that effect while still allowing
    //       stored phrases to contain these words. Thus, these words need not be
    //       filtered out (or otherwise managed) during normalization, etc.
    //   - Consider each stored phrase of words against the input words as if though
    //     each unique word is a unique character and perform a string edit distance
    //     between these "phrase words" to compute an alignment score (the lower the
    //     better.)
    //     - The overridable default EditDistance uses the Levenshtein algorithm.
    //     - See "getEditDistance".
    //     - Keep the best distance score across all phrases for an item.
    //     - This edit distance score, e, is used to compute the retrieval score, r, as:
    //       - r = m - e,
    //         - where m is the maximum number of word intersections between the input
    //           and the phrase
    //         - note that this inverts scoring such that higher scores correspond to
    //           better matches.
    //       - for strict applications, retrieval scores <= 0 would be considerd to be
    //         poor matches.
    //   - Return matching results in sorted order from best (highest) to worst (lowest).

    Map<T, Integer> values = null;  // values w/inst freq across words
    int maxIxCount = 0;
    int minCardinality = Integer.MAX_VALUE;

    // map each input word to the number of phrases it participates in in the index
    final Map<String, Integer> word2cardinality = new HashMap<String, Integer>();
    for (String word : words) {
      final Set<T> items = word2items.get(word);
      if (items != null) {
        final int numItems = items.size();
        word2cardinality.put(word, numItems);

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
      result = new ArrayList<RetrievalResult<T>>();

      // hook to bypass default matching for injecting special treatment
      if (!bypassDefaultMatching(result, inputString, words, word2cardinality, values, maxIxCount)) {

        final CharDictionary dictionary = new CharDictionary();
        final char[] inputChars = dictionary.lookup(words);

        // Identify words that must be present in retrieved phrase
        final Set<String> mustHaveWords = new HashSet<String>();
        for (String word : words) {
          final Integer cardinality = word2cardinality.get(word);
          if (mandatoryWords.contains(word) || (cardinality != null && cardinality == minCardinality)) {
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
                  // currently: true if *any* (not necessarily *all*) are present.
                  // note that this allows for loose alternatives, probably with
                  // lower scores, to be considered.
                  hasMustHave = true;
                }
              }
              if (!hasMustHave) continue;


              final int dist = getEditDistance(item, storedChars, inputChars);
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
    }

    if (result != null) Collections.sort(result);
    return result;
  }

  /**
   * Hook for extenders to override default matching given lookup info.
   *
   * @param result  Output collector for results.
   * @param inputString  Original input string.
   * @param words  Normalized input words.
   * @param word2cardinality  Cardinality of retrieved values for each word.
   * @param values  Values retrieved w/inst freq across words.
   * @param maxIxCount  Maximum intersection count (freq) of values across words
   *                    (note: this is the highest value in values for any key)
   *
   * @return true if matching can be bypassed or false to continue with default matching.
   */
  protected boolean bypassDefaultMatching(List<RetrievalResult<T>> result,
                                          String inputString, String[] words,
                                          Map<String, Integer> word2cardinality,
                                          Map<T, Integer> values, int maxIxCount) {
    return false;
  }

  protected int getEditDistance(T storedItem, char[] storedChars, char[] inputChars) {
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

    public void setScore(int score) {
      this.score = score;
    }

    public String[] getMatchedWords() {
      return matchedWords;
    }

    public String[] getInputWords() {
      return inputWords;
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
