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


import org.sd.nlp.Normalizer;
import org.sd.text.WordGramSplitter.Word;
import org.sd.text.WordGramSplitter.WordAcceptor;
import org.sd.util.StatsAccumulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe container for aggretating stats for word N-grams (for one N).
 * <p>
 * @author Spence Koehler
 */
public class WordGramStat {

  private static final boolean DEBUG = false;

  public static final int DEFAULT_STD_DEVS = 10;
  public static final int DEFAULT_NGRAM_LIMIT = 0; //100000;
  public static final long PRUNE_TIME_INTERVAL = 10000;
  public static final boolean COLLAPSE = false;

  /**
   * Interface used for collecting NGrams to determine which to accept.
   */
  public static interface NGramAcceptor {
    /**
     * Determine whether to accept the given NGramFreq.
     */
    public boolean accept(NGramFreq nGramFreq);
  }


  public final int n;
  private WordGramSplitter splitter;
  private Set<String> ngrams;
  private Map<String, NGramFreq> ngram2freq;
  private int ngramLimit;  // max number of ngrams to collect (unlimited if <= 0)
  private boolean collapse;
  private Long timeLimit;  // collapse time limit
  private Long waitMillis;  // collapse wait millis
  private int stdDevs;     // pruning parameter when limit reached

  private long pruneTimeInterval;
  private long lastPruneTime;
  private long freqThreshold;

  private String name;

  /**
   * Collect stats for N-grams using a DefaultWordGramSplitter.
   * <p>
   * Use default automatic pruning parameters.
   */
  public WordGramStat(int n, Normalizer normalizer, WordAcceptor wordAcceptor) {
    this(n, new DefaultWordGramSplitter(n, normalizer, wordAcceptor));
  }

  /**
   * Collect stats for N-grams using the given WordGramSplitter.
   * <p>
   * Use default automatic pruning parameters.
   */
  public WordGramStat(int n, WordGramSplitter splitter) {
    this(n, splitter, DEFAULT_NGRAM_LIMIT, DEFAULT_STD_DEVS, PRUNE_TIME_INTERVAL);
  }

  /**
   * Collect stats for N-grams using the given WordGramSplitter.
   * <p>
   * @param n  The 'n' in word N-gram (number of words)
   * @param splitter  The WordGramSplitter to use
   * @param ngramLimit  The maximum number of ngrams to hold before attempting to prune.
   *                    If 0, then no maximum or automatic pruning will take place.
   * @param stdDevs  Pruning parameter when limit is reached. Only ngrams that have
   *                 a frequency greater N standard deviations from the mean will be
   *                 kept.
   * @param pruneTimeInterval Time interval to wait between attempts to prune.
   */
  public WordGramStat(int n, WordGramSplitter splitter, int ngramLimit, int stdDevs, long pruneTimeInterval) {
    this.n = n;
    this.splitter = splitter;
    this.ngrams = new HashSet<String>();
    this.ngram2freq = new HashMap<String, NGramFreq>();

    this.ngramLimit = ngramLimit;
    this.collapse = COLLAPSE;
    this.timeLimit = null;  // no time limit
    this.waitMillis = null;  // no time limit
    this.stdDevs = stdDevs;
    this.pruneTimeInterval = pruneTimeInterval;
    this.lastPruneTime = 0L;
    this.freqThreshold = 0L;

    this.name = Integer.toString(n);
  }

  /**
   * Construct an instance for reconstruction.
   * <p>
   * NOTE: The add(string) and flush methods will be disabled and only the
   *       add(NGramFreq) and getter methods will work.
   */
  public WordGramStat(int n) {
    this.n = n;
    this.splitter = null;
    this.ngrams = null;

    this.ngram2freq = new HashMap<String, NGramFreq>();
    this.ngramLimit = 0;  // auto pruning disabled for reconstruction instances.
    this.collapse = false;
    this.timeLimit = null;  // no time limit
    this.waitMillis = null;  // no time limit
    this.stdDevs = 0;
    this.pruneTimeInterval = pruneTimeInterval;
    this.lastPruneTime = 0L;
    this.freqThreshold = 0L;

    this.name = Integer.toString(n);
  }

  /**
   * Get this instance's name.
   * <p>
   * Note that the name defaults to "N" but can be overridden through setName.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Set this instance's name, overriding the default of "N".
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the splitter.
   */
  public WordGramSplitter getSplitter() {
    return splitter;
  }

  /**
   * Get the NGrams.
   */
  public Set<String> getNGrams() {
    return ngrams;
  }

  /**
   * Determine whether this instance is in "reconstruction" mode.
   */
  public boolean reconMode() {
    return splitter == null && ngrams == null;
  }

  /**
   * Get the frequency map.
   */
  public Map<String, NGramFreq> getNgram2Freq() {
    return ngram2freq;
  }

  /**
   * Set the frequency map.
   * <p>
   * NOTE: Using this setter will destroy any existing data in the current
   *       instance, restoring it to the state represented by the supplied
   *       ngram2freq data. It will also place this instance into
   *       "reconstruction" mode such that it will not accept string adds.
   */
  public void setNgram2Freq(Map<String, NGramFreq> ngram2freq) {
    this.ngram2freq = ngram2freq;
    this.splitter = null;  // place into "reconstruction" mode.
    this.ngrams = null;
  }

  /**
   * Collect n-grams from the given string. (Thread safe.)
   * <p>
   * The idea is to add strings that are split into N-grams that are collected
   * but not analyzed for statistics. Once all strings have been added, then
   * a "flush" will add the collected N-grams to the statistical tallies.
   * <p>
   * The consumer can manually flush after every add for tallies on every
   * string or flush after certain groups of strings have been added so that
   * within the group, duplicates are not tallied, but across groups they are.
   */
  public void add(String string) {
    if (splitter == null || string == null) return;

    final String[] tokens = splitter.getTokens(string);

    final Word[] words = splitter.getWords(tokens);
    final List<String> wordGrams = splitter.getWordGrams(words);
    synchronized (ngrams) {
      ngrams.addAll(wordGrams);
    }
  }

  String[] getTokens(String string) {
    String[] result = null;

    if (splitter != null && string != null) {
      return getTokens(string);
    }

    return result;
  }

  Word[] getWords(String[] tokens) {
    Word[] result = null;

    if (splitter != null && tokens != null) {
      result = splitter.getWords(tokens);
    }

    return result;
  }

  /**
   * Add all words.
   */
  void addAll(Word[] words) {
    if (splitter != null && ngrams != null && words != null) {
      final List<String> wordGrams = splitter.getWordGrams(words);
      synchronized (ngrams) {
        ngrams.addAll(wordGrams);
      }
    }
  }

  /**
   * Flush added N-grams through stat aggregation since the last flush,
   * identifying the source (ok if null).
   * 
   * @return true if pruning occurred while flushing; otherwise, false.
   */
  public boolean flush(String source) {
    boolean result = false;

    if (ngrams == null) return result;

    synchronized (ngrams) {
      for (String ngram : ngrams) {
        NGramFreq freq = ngram2freq.get(ngram);
        if (freq == null) {
          freq = new NGramFreq(ngram, source, ngram2freq, n);
          synchronized (ngram2freq) {
            ngram2freq.put(ngram, freq);
          }
        }
        else {
          freq.inc(source);
        }
      }

      // zero out ngrams for next add(s).
      ngrams.clear();

      if (ngramLimit > 0 && ngram2freq.size() > ngramLimit) {
        // statistically prune the "fluff"
        result = pruneNGrams();
      }
    }

    return result;
  }

  /**
   * Set this instance's ngramLimit.
   */
  public void setNGramLimit(int ngramLimit) {
    this.ngramLimit = ngramLimit;
  }

  /**
   * Set this instance's collapse flag.
   */
  public void setCollapse(boolean collapse) {
    this.collapse = collapse;
  }

  /**
   * Set this instance's collapse flag with time limit.
   */
  public void setCollapse(boolean collapse, long timeLimit, long waitMillis) {
    this.collapse = collapse;
    this.timeLimit = timeLimit;
    this.waitMillis = waitMillis;
  }

  /**
   * Add the nGramFreq to a reconstruction instance.
   */
  public void add(NGramFreq nGramFreq) {
    if (splitter != null) return;

    final String ngram = nGramFreq.getNGram();
    final NGramFreq existing = ngram2freq.get(ngram);
    if (existing != null) {
      existing.inc(nGramFreq.getFreq(), nGramFreq.getSources());
    }
    else {
      synchronized (ngram2freq) {
        ngram2freq.put(ngram, nGramFreq);
      }
    }
  }

  /**
   * Get the top 'num' n-grams.
   *
   * @param num  Is the (maximum) number of n-grams to collect.
   * @param die  Flag to monitor for a signal to terminate early (ok if null).
   */
  public NGramFreq[] getTopNGramsLimitedByCount(int num, AtomicBoolean die) {
    return getTopNGramsLimitedByCount(num, null, die);
  }

  /**
   * Get the top 'num' n-grams.
   *
   * @param num  Is the (maximum) number of n-grams to collect.
   * @param acceptor  (ok if null) NGramAcceptor to determine which ngrams to
   *                  keep or reject while collecting.
   * @param die  Flag to monitor for a signal to terminate early (ok if null).
   *
   * @return up to num of the accepted top nGrams.
   */
  public NGramFreq[] getTopNGramsLimitedByCount(int num, NGramAcceptor acceptor, AtomicBoolean die) {
    return getTopNGrams(num, 0, acceptor, die);
  }

  /**
   * Get the top n-grams w/freq greater than or equal to minFreq.
   *
   * @param minFreq  Is the (minimum) frequency of an n-gram to collect.
   * @param die  Flag to monitor for a signal to terminate early (ok if null).
   */
  public NGramFreq[] getTopNGramsLimitedByFreq(int minFreq, AtomicBoolean die) {
    return getTopNGramsLimitedByFreq(minFreq, null, die);
  }

  /**
   * Get the top n-grams w/freq greater than or equal to minFreq.
   *
   * @param minFreq  Is the (minimum) frequency of an n-gram to collect.
   * @param die  Flag to monitor for a signal to terminate early (ok if null).
   */
  public NGramFreq[] getTopNGramsLimitedByFreq(int minFreq, NGramAcceptor acceptor, AtomicBoolean die) {
    return getTopNGrams(0, minFreq, acceptor, die);
  }

  /**
   * Get the stats (min, max, ave, etc.) of frequencies for all collected NGrams.
   * <p>
   * The returned stats have the following meaning:
   * <ul>
   * <li>stats.getN() -- the total number of unique ngrams collected</li>
   * <li>stats.getSum() -- the total number of ngrams collected</li>
   * <li>stats.getMean() -- the average frequency of ngrams collected</li>
   * <li>stats.getMin() -- the minimum frequency of ngrams collected</li>
   * <li>stats.getMax() -- the maximum frequency of ngrams collected</li>
   * </ul>
   * <p>
   * The probability of any ngram occurring is: p(ngram) = ngram.getFreq() / stats.getSum();
   * <p>
   */
  public StatsAccumulator getFreqStats() {
    final StatsAccumulator freqStats = new StatsAccumulator(n + "-gramFreqStats");

    synchronized (ngram2freq) {
      for (NGramFreq ngramFreq : ngram2freq.values()) {
        freqStats.add(ngramFreq.getFreq());
      }
    }

    return freqStats;
  }

  /**
   * Get the top n-grams.
   * <p>
   * <b>NOTE:</b>
   * If a timeLimit is set for this instance, then the timeLimit will be
   * applied while collapsing the NGrams and the 'die' parameter will be
   * ignored.
   *
   * @param countLimit  The largest number to collect (unlimited if &lt;= 0).
   * @param minFreq     The minimum frequency to collect (no minimum if &lt;= 0).
   * @param acceptor    A function to test each ngram for inclusion.
   * @param die         Flag to monitor for a signal to terminate early (ok if null).
   */
  public final NGramFreq[] getTopNGrams(int countLimit, int minFreq, NGramAcceptor acceptor,
                                        AtomicBoolean die) {
    NGramFreq[] result = null;

    if (ngram2freq != null) {
      synchronized (ngram2freq) {
        result =
          WordGramStatsUtil.getTopNGrams(ngram2freq.values(), countLimit,
                                         minFreq, acceptor, die, false,
                                         collapse, timeLimit, waitMillis);
      }
    }

    return result;
  }

  /**
   * Discard ngrams to make way for those that are relevant.
   * <p>
   * The idea here is that it is the "statistically improbable" N-Grams that
   * are of interest when we're looking for the most frequent. So, we'll
   * prune away those N-grams that are less than N standard deviations
   * from the mean.
   * 
   * @return true if pruning occurred; otherwise, false.
   */
  private final boolean pruneNGrams() {
    boolean result = false;

    final long curtime = System.currentTimeMillis();
    if (curtime - lastPruneTime < pruneTimeInterval) {
      return result;
    }

    if (freqThreshold == 0) {
      final StatsAccumulator freqStats = getFreqStats();
      freqThreshold = Math.round(freqStats.getMean() + stdDevs * freqStats.getStandardDeviation());

      if (DEBUG) {
        System.out.println(new Date() + ": WordGramStat.pruneNGrams(" + n + ") size=" +
                           ngram2freq.size() + ", threshold=" +
                           freqThreshold + ", stats=" + freqStats.getLabel() +
                           " [n=" + freqStats.getN() +
                           ", min=" + freqStats.getMin() +
                           ", max=" + freqStats.getMax() +
                           ", ave=" + freqStats.getMean() +
                           ", stdev=" + freqStats.getStandardDeviation() +
                           "]");
      }

      if (freqThreshold >= (freqStats.getMax() - freqThreshold)) {
        if (DEBUG) {
          System.out.println(new Date() + ": WordGramStat.pruneNGrams(" + n + ") range to small to prune.");
        }

        freqThreshold = 0L;
        lastPruneTime = System.currentTimeMillis();
        return result;
      }
    }

    if (DEBUG) {
      System.out.println(new Date() + ": WordGramStat.pruneNGrams(" + n + ", start) size=" + ngram2freq.size());
    }

    synchronized (ngram2freq) {
      for (Iterator<Map.Entry<String, NGramFreq>> iter = ngram2freq.entrySet().iterator(); iter.hasNext(); ) {
        final Map.Entry<String, NGramFreq> elt = iter.next();
        final NGramFreq ngramFreq = elt.getValue();
        if (ngramFreq.getFreq() <= freqThreshold) {
          // prune this entry
          iter.remove();
          if (!result) result = true;
        }
      }
    }

    if (DEBUG) {
      System.out.println(new Date() + ": WordGramStat.pruneNGrams(" + n + ", end) size=" + ngram2freq.size());
    }

    lastPruneTime = System.currentTimeMillis();

    return result;
  }
}
