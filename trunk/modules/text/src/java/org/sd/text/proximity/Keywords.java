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
package org.sd.text.proximity;


import org.sd.io.FileUtil;
import org.sd.nlp.GeneralNormalizer;
import org.sd.nlp.Normalizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to manage a ranked list of keywords.
 * <p>
 * @author Spence Koehler
 */
public class Keywords {
  
  private static final Map<File, Keywords> INSTANCES = new HashMap<File, Keywords>();

  /**
   * Get a keywords instance using the given resource.
   * <p>
   * The resource must have lines of the form <count>|<keyword>. Blank lines
   * and comment lines (beginning with a '#' sign) are ignored. The counts
   * must be integers sorted from HIGHEST to LOWEST. Currently, keywords are
   * assumed to be lowercase sequences of letters only.
   */
  public static final Keywords getInstance(File resource) {
    Keywords result = null;

    try {
      result = INSTANCES.get(resource);

      if (result == null) {
        result = new Keywords(resource);
        INSTANCES.put(resource, result);
      }
    }
    catch (IOException e) {
      // indicate failure by null result.
    }

    return result;
  }


  private File resource;
  private int totalTerms;
  private Map<Integer, Integer> rankNum2termCount;  // numRanks = .size; 
  private Map<Integer, Double> rank2power;          // percentage of source terms having rank's count or less
  private Map<String, Integer> keyword2rank;
  private Map<String, Keyword> word2keyword;
  private Normalizer normalizer;

  private Keywords(File resource) throws IOException {
    this.resource = resource;
    this.totalTerms = 0;
    this.rankNum2termCount = new HashMap<Integer, Integer>();
    this.rank2power = new HashMap<Integer, Double>();
    this.keyword2rank = new HashMap<String, Integer>();
    this.word2keyword = new HashMap<String, Keyword>();

    loadResource();
    computePowers();

    this.normalizer = GeneralNormalizer.getCaseInsensitiveInstance();
  }

  private final void loadResource() throws IOException {

    final BufferedReader reader = FileUtil.getReader(resource);
    String line = null;
    int lastCount = Integer.MAX_VALUE;
    int curRank = -1;

    while ((line = reader.readLine()) != null) {
      if (line.length() == 0 || line.charAt(0) == '#') continue;

      final String[] pieces = line.split("\\s*\\|\\s*");
      final Integer count = new Integer(pieces[0]);
      final String keyword = pieces[1];

      totalTerms += count;

      if (count < lastCount) {
        ++curRank;
      }
      else if (count > lastCount) {
        throw new IllegalArgumentException("Resource '" + resource + "' must be sorted from HIGHEST to LOWEST!");
      }

      final Integer rankCount = rankNum2termCount.get(curRank);
      rankNum2termCount.put(curRank, rankCount == null ? count : rankCount + count);

      keyword2rank.put(keyword, curRank);

      lastCount = count;
    }

    reader.close();
  }

  private final void computePowers() {
    final int numRanks = rankNum2termCount.size();

    int ranksSum = 0;
    for (int rank = numRanks - 1; rank >= 0; --rank) {
      final Integer rankCount = rankNum2termCount.get(rank);
      ranksSum += rankCount;
      final double rankPower = ((double)ranksSum) / ((double)totalTerms);
      rank2power.put(rank, rankPower);

//      System.out.println("rank " + rank + " power=" + rankPower + " (rankCount=" + rankCount + ", rankSum=" + ranksSum + ")");
    }
  }

  /**
   * Find the keyword in the input.
   *
   * @param input       The input to scan for the keyword.
   * @param normalized  true if the input has already been normalized.
   */
  public Keyword findKeyword(String input, boolean normalized) {
    Keyword result = null;
    final String keywordString = findKeywordString(input, normalized);

    if (keywordString != null) {
      result = word2keyword.get(keywordString);
      if (result == null) {
        final Integer rank = keyword2rank.get(keywordString);
        final Double power = rank2power.get(rank);
        result = new Keyword(keywordString, this, rank, power);
        word2keyword.put(keywordString, result);
      }
    }

    return result;
  }

  /**
   * Determine whether the input contains a keyword.
   *
   * @param input       The input to scan for the keyword.
   * @param normalized  true if the input has already been normalized.
   */
  public boolean hasKeyword(String input, boolean normalized) {
    return findKeywordString(input, normalized) != null;
  }
  
  /**
   * Normalize the string using this instance's normalizer.
   */
  public final String normalize(String string) {
    return normalizer.normalize(string).getNormalized();
  }

  /**
   * Report whether the given (normalized) word is a keyword.
   */
  public final boolean isKeyword(String word) {
    return keyword2rank.containsKey(word);
  }

  /**
   * Report whether the given word is a keyword.
   */
  public final boolean isKeyword(String word, boolean normalized) {
    if (!normalized) word = normalize(word);
    return keyword2rank.containsKey(word);
  }

  private final String findKeywordString(String input, boolean normalized) {
    String result = null;

    final String[] words = splitInput(input, normalized);
    for (String word : words) {
      if (isKeyword(word)) {
        result = word;
        break;
      }
    }
    
    return result;
  }

  /**
   * Split input into words, normalizing if needed.
   */
  private final String[] splitInput(String input, boolean normalized) {
    if (!normalized) input = normalize(input);
    return input.split("[^A-Za-z]+");
  }
}
