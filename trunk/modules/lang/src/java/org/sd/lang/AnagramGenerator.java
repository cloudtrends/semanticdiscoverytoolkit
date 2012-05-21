/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.lang;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sd.util.GeneralUtil;


/**
 * Utility to generate varying length anagrams for a word.
 * <p>
 * @author Spence Koehler
 */
public class AnagramGenerator {

  public static interface WordValidator {
    public boolean isValid(String word);
  }


  private WordValidator wordValidator;

  public AnagramGenerator() {
    this(null);
  }

  public AnagramGenerator(WordValidator wordValidator) {
    this.wordValidator = wordValidator;
  }


  public WordValidator getWordValidator() {
    return wordValidator;
  }

  public void setWordValidator(WordValidator wordValidator) {
    this.wordValidator = wordValidator;
  }


  /**
   * Get the anagrams of the word of all lengths.
   */
  public final Map<Integer, Set<String>> getAnagrams(String word) {
    return getAnagrams(word, 1, word.length());
  }

  /**
   * Get the anagrams of the word of all lengths from minLen to wordLen.
   */
  public final Map<Integer, Set<String>> getAnagrams(String word, int minLen) {
    return getAnagrams(word, minLen, word.length());
  }

  /**
   * Get the anagrams of the word of all lengths from minLen to maxLen.
   */
  public final Map<Integer, Set<String>> getAnagrams(String word, int minLen, int maxLen) {
    final Map<Integer, Set<String>> result = new TreeMap<Integer, Set<String>>();

    final char[] letters = word.toCharArray();

    for (int numLetters = minLen; numLetters <= maxLen; ++numLetters) {
      final Set<String> anagrams = new TreeSet<String>();
      addPermutations(anagrams, letters, numLetters);
      result.put(numLetters, anagrams);
    }

    return result;
  }


  private final void addPermutations(Set<String> result, char[] letters, int numLetters) {
    final List<Character> chars = new ArrayList<Character>();
    for (char letter : letters) chars.add(letter);

    final List<LinkedList<Character>> combos = GeneralUtil.getCombinations(chars, numLetters);
    for (LinkedList<Character> combo : combos) {
      final List<List<Character>> permutations = GeneralUtil.permute(combo);
      for (List<Character> permutation : permutations) {
        final String word = buildString(permutation);
        if (wordValidator == null || wordValidator.isValid(word)) {
          result.add(word);
        }
      }
    }
  }

  private final String buildString(List<Character> chars) {
    final StringBuilder result = new StringBuilder();
    for (Character c : chars) {
      result.append(c);
    }
    return result.toString();
  }


  public void doMain(String[] args, int startIdx) {
    for (int i = startIdx; i < args.length; ++i) {
      final String arg = args[i];
      final Map<Integer, Set<String>> allAnagrams = getAnagrams(arg, Math.min(arg.length(), 3));

      System.out.println("\nAnagrams for '" + arg + "'");

      for (Map.Entry<Integer, Set<String>> entry : allAnagrams.entrySet()) {
        final int numLetters = entry.getKey();
        final Set<String> anagrams = entry.getValue();

        System.out.println("\n\tFound " + anagrams.size() + " with " + numLetters + " letters:");
        for (String anagram : anagrams) {
          System.out.println("\t\t" + anagram);
        }
      }
    }
  }

  public static void main(String[] args) {
    final AnagramGenerator agen = new AnagramGenerator();
    agen.doMain(args, 0);
  }
}
