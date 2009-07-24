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


import org.sd.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility methods for working with WordGramStats.
 * <p>
 * @author Spence Koehler
 */
public class WordGramStatsUtil {

  /**
   * Find the highest N for the given instances.
   */
  public static int getHighN(WordGramStat[] stats) {
    int result = 0;

    for (WordGramStat stat : stats) {
      if (stat.n > result) {
        result = stat.n;
      }
    }

    return result;
  }

  /**
   * Prune lower-N ngrams that are contained (as full words) within
   * higher-N-ngrams at the same frequency.
   * <p>
   * Note that this only prunes those that have already been flushed
   * and that later flushes will need to be "re-pruned".
   * <p>
   * <b>*WARNING*</b> This method should not be called with large datasets!
   */
  public static void pruneOverlap(WordGramStat[] stats) {
    pruneOverlap(stats, getHighN(stats));
  }

  /**
   * Prune lower-N ngrams that are contained (as full words) within
   * higher-N-ngrams at the same frequency.
   * <p>
   * Note that this only prunes those that have already been flushed
   * and that later flushes will need to be "re-pruned".
   * <p>
   * <b>*WARNING*</b> This method should not be called with large datasets!
   */
  public static void pruneOverlap(WordGramStat[] stats, int highN) {

    final List<NGramFreq> all = new LinkedList<NGramFreq>();
    final Map<Long, Map<Integer, List<NGramFreq>>> lookup = new HashMap<Long, Map<Integer, List<NGramFreq>>>();

    for (WordGramStat stat : stats) {
      final Collection<NGramFreq> values = stat.getNgram2Freq().values();

      // add to main list
      all.addAll(values);

      // add to lookup lists
      for (NGramFreq value : values) {
        final long freq = value.getFreq();
        Map<Integer, List<NGramFreq>> freqMap = lookup.get(freq);
        if (freqMap == null) {
          freqMap = new HashMap<Integer, List<NGramFreq>>();
          lookup.put(freq, freqMap);
        }
        final int n = value.getN();
        List<NGramFreq> ngrams = freqMap.get(n);
        if (ngrams == null) {
          ngrams = new LinkedList<NGramFreq>();
          freqMap.put(n, ngrams);
        }
        ngrams.add(value);
      }
    }

    // sort main list
    Collections.sort(all);
    // NOTE: no reason to sort lookup lists (all already have the same frequency)

    // iterate over main list to find ngrams to remove
    for (Iterator<NGramFreq> iter = all.iterator(); iter.hasNext(); ) {
      final NGramFreq nGramFreq1 = iter.next();
      final int n1 = nGramFreq1.getN();
      if (n1 == highN) continue; // won't prune any 'largest'
      if ("".equals(nGramFreq1.getNGram())) continue;
      final long freq1 = nGramFreq1.getFreq();

      boolean remove = false;

      // lookup ngrams that prove the main ngram should be removed by finding
      // an ngram with more words and the same frequency
      final Map<Integer, List<NGramFreq>> freqMap = lookup.get(freq1);  // same freq
      if (freqMap == null) continue;  // none exist, so continue to next

      // for n's that are higher (more words)
      for (int i = n1 + 1; i <= highN && !remove; ++i) {
        final List<NGramFreq> ngrams = freqMap.get(i);
        if (ngrams == null) continue;
        remove = isSubPhrase(nGramFreq1, ngrams, true, false, false);
      }

      if (remove) {
        iter.remove();
        nGramFreq1.removeFromContainer();
      }
    }
  }

  /**
   * Determine whether the given nGramFreq is a sub-phrase of any of the given
   * ngrams.
   * <p>
   * To be a sub-phrase of another nGram, the two nGrams must have the same
   * frequency and the given nGram must have fewer words than that of which
   * it is a sub-phrase. Note that a sub-phrase match requires the nGram
   * to be found with word boundaries on each of its ends.
   * <p>
   * If the ngrams list is not sorted, it will be sorted.
   *
   * @param nGramFreq  the nGram to find as a subPhrase.
   * @param ngrams     the nGrams of which the nGram may be a subphrase.
   * @param isSorted   flag indicating whether ngrams have been sorted.
   * @param allowSort  flag indicating whether ngrams can be sorted by this method.
   * @param ignoreFreq flag indiciating whether to ignore freq
   *
   * @return true if the nGramFreq is a sub-phrase of any of the ngrams.
   */
  public static final boolean isSubPhrase(NGramFreq nGramFreq, List<NGramFreq> ngrams,
                                          boolean isSorted, boolean allowSort, boolean ignoreFreq) {
    boolean result = false;

    final int n1 = nGramFreq.getN();
    final long freq1 = nGramFreq.getFreq();
    final String string1 = nGramFreq.getNGram();
    final int len1 = string1.length();

    if (!isSorted && allowSort) {
      Collections.sort(ngrams);  // sorts from most to least frequent
      isSorted = true;
    }

    for (NGramFreq nGramFreq2 : ngrams) {
      if (!ignoreFreq) {
        // NOTE: test only those with more words and the same frequency,
        if (n1 >= nGramFreq2.getN()) continue;  // skip this, it's too small
      
        final long freq2 = nGramFreq2.getFreq();
        if (isSorted) {
          if (freq2 > freq1) continue;  // haven't reached those w/same freq yet
          if (freq2 < freq1) break;     // passed those w/same freq
        }
        else if (freq1 != freq2) continue;  // only compare those w/same freq
      }

      // if string2 has string1 as a substring
      final String string2 = nGramFreq2.getNGram();
      final int pos = string2.indexOf(string1);
      if (pos >= 0) {
        // and the substring has word boundaries
        if (hasWordBoundaries(string2, pos, len1)) {
          // then string1 is a sub-phrase
          result = true;
          break;
        }
      }
    }

    return result;
  }

  /**
   * Determine whether the substring in string at startpos and having the
   * given length has word boundaries on both sides.
   */
  private static final boolean hasWordBoundaries(String string, int startpos, int len) {
    return
      hasLeftWordBoundary(string, startpos) &&
      hasRightWordBoundary(string, startpos + len);
  }

  private static final boolean hasLeftWordBoundary(String string, int firstCharPos) {
    boolean result = (firstCharPos == 0);

    if (!result) {
      final char c = string.charAt(firstCharPos - 1);
      result = isBoundaryChar(c);
    }

    return result;
  }

  private static final boolean hasRightWordBoundary(String string, int lastCharPos) {
    boolean result = (lastCharPos == string.length());

    if (!result) {
      final char c = string.charAt(lastCharPos);
      result = isBoundaryChar(c);
    }

    return result;
  }

  /**
   * Determine whether the character is considered a boundary
   * (for pruneOverlap).
   */
  protected static boolean isBoundaryChar(char c) {
    return (c == ' ');  // todo: should we check for non-char instead of a space?
  }


  public static final NGramFreq[] getTopNGrams(Collection<NGramFreq> ngrams, int countLimit, int minFreq,
                                               WordGramStat.NGramAcceptor acceptor, AtomicBoolean die,
                                               boolean isSorted, boolean collapse,
                                               Long timeLimit, Long waitMillis) {
    List<NGramFreq> result = null;

    if (ngrams != null) {
      result = new ArrayList<NGramFreq>();

      int index = 0;
      final long startTime = System.currentTimeMillis();
      for (Iterator<NGramFreqSet> iter = new NGramFreqSetIterator(ngrams, isSorted, collapse/*collapsible*/); iter.hasNext(); ) {
        final NGramFreqSet ngramSet = iter.next();
        if (minFreq > 0 && ngramSet.getFreq() < minFreq) break;

        final List<NGramFreq> freqs = collapse ?
          (timeLimit == null ? ngramSet.getCollapsedNGrams(die) : ngramSet.getCollapsedNGrams(timeLimit, waitMillis))
          : ngramSet.getNGrams();

        for (NGramFreq freq : freqs) {
          if (acceptor == null || acceptor.accept(freq)) {
            result.add(freq);
            ++index;
            if (countLimit > 0 && index >= countLimit) break;
          }
        }
        
        if (countLimit > 0 && index >= countLimit) break;
        if (minFreq > 0 && ngramSet.getFreq() == minFreq) break;
        
        if (timeIsUp(die, startTime, timeLimit)) {
          collapse = false;  // don't collapse any more
        }
      }
    }

    return result == null ? null : result.toArray(new NGramFreq[result.size()]);
  }

  private static final boolean timeIsUp(AtomicBoolean die, long startTime, Long timeLimit) {
    boolean result = false;

    if ((die != null && die.get())) {
      result = true;
    }
    else if (timeLimit != null && (System.currentTimeMillis() - startTime) > timeLimit) {
      if (die != null) die.set(true);
      result = true;
    }

    return result;
  }
}
