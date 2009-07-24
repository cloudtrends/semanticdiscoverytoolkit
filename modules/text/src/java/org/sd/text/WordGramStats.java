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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Container for aggretating stats for word N-grams (from lowN to highN).
 * <p>
 * @author Spence Koehler
 */
public class WordGramStats {

  /**
   * Set to true to run each WordGramStat add on its own thread.
   * <p>
   * Experimentation showed that threading this part of the process slows
   * things down considerably.
   */
  private static final boolean ASYNC = false;


  public final int lowN;
  public final int highN;
  private WordGramStat[] stats;
  private AsyncAdder[] adders;
  private boolean rFlag;  // reconstruction flag
  private boolean pruned;
  private List<WordGramStat> altStats;

  /**
   * Collect stats for N-grams from lowN (inclusive) to highN (inclusive)
   * using the given WordGramSplitter.
   */
  public WordGramStats(final int lowN, final int highN, final SplitterFactory splitterFactory) {
    this.lowN = lowN;
    this.highN = highN;

    final int num = highN - lowN + 1;
    this.stats = new WordGramStat[num];
    this.adders = ASYNC ? new AsyncAdder[num] : null;
    this.rFlag = false;
    this.pruned = false;
    this.altStats = null;

    int index = 0;
    for (int i = lowN; i <= highN; ++i) {
      this.stats[index] = new WordGramStat(i, splitterFactory.getSplitter(i));
      if (ASYNC) this.adders[index] = new AsyncAdder(this.stats[index]);
      ++index;
    }
  }

  /**
   * Collect stats for N-grams from lowN (inclusive) to highN (inclusive)
   * using a Default WordGramSplitter.
   */
  public WordGramStats(int lowN, int highN, Normalizer normalizer, WordAcceptor wordAcceptor) {
    this.lowN = lowN;
    this.highN = highN;
    this.stats = new WordGramStat[highN - lowN + 1];
    this.rFlag = false;
    this.pruned = false;
    this.altStats = null;

    int index = 0;
    for (int i = lowN; i <= highN; ++i) {
      this.stats[index] = new WordGramStat(i, normalizer, wordAcceptor);
      ++index;
    }
  }

  /**
   * Construct an instance for reconstruction.
   * <p>
   * NOTE: The add(string) and flush methods will be disabled and only the
   *       add(NGramFreq) and getter methods will work.
   */
  public WordGramStats(int n) {
    this.lowN = n;
    this.highN = n;
    this.stats = new WordGramStat[]{new WordGramStat(n)};
    this.rFlag = true;
    this.pruned = false;
    this.altStats = null;
  }

  /**
   * Get lowN.
   */
  public int getLowN() {
    return lowN;
  }

  /**
   * Get highN.
   */
  public int getHighN() {
    return highN;
  }

  /**
   * Set the NGramLimit on each WordGramStat.
   */
  public void setNGramLimit(int ngramLimit) {
    for (WordGramStat stat : stats) {
      stat.setNGramLimit(ngramLimit);
    }
  }

  /**
   * Set the collapse flag on each WordGramStat.
   */
  public void setCollapse(boolean collapse) {
    for (WordGramStat stat : stats) {
      stat.setCollapse(collapse);
    }
  }

  /**
   * Set this instance's collapse flag with time limit.
   */
  public void setCollapse(boolean collapse, long timeLimit, long waitMillis) {
    for (WordGramStat stat : stats) {
      stat.setCollapse(collapse, timeLimit, waitMillis);
    }
  }

  /**
   * Get the WordGramStat instance for the given 'n'.
   *
   * @param n  The 'n' in N-gram.
   *
   * @return the wordGramStat instance or null if 'n' is out of range.
   */
  public WordGramStat getWordGramStat(int n) {
    WordGramStat result = null;

    final int index = n - lowN;
    if (index >= 0 && index < stats.length) {
      result = stats[index];
    }

    return result;
  }

  /**
   * Add an alternate wordGramStat instance to apply in addition
   * to the 'N' instances.
   */
  public void addAlt(WordGramStat altWordGramStat) {
    if (altStats == null) altStats = new ArrayList<WordGramStat>();
    altStats.add(altWordGramStat);
  }

  /**
   * Get the alternate wordGramStat instances.
   *
   * @return the alternate WordGramStat instances, possibly null.
   */
  public List<WordGramStat> getAltStats() {
    return altStats;
  }

  /**
   * Collect n-grams from the given string.
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
    if (rFlag) return;

    String[] tokens = null;

    for (int i = 0; i < stats.length; ++i) {
      if (stats[i].getSplitter() != null) {
        tokens = stats[i].getSplitter().getTokens(string);
        break;
      }
    }

    // as long as one of the stat instances is open for load
    if (tokens != null) {
      if (ASYNC) {
        final Thread[] adderThreads = new Thread[adders.length];

        for (int i = 0; i < adders.length; ++i) {
          adderThreads[i] = adders[i].launchThread(tokens);
        }

        for (int i = 0; i < adderThreads.length; ++i) {
          try {
            adderThreads[i].join();
          }
          catch (InterruptedException eat) {}
        }
      }
      else {
        for (int i = 0; i < stats.length; ++i) {
          final WordGramStat wordGramStat = stats[i];
          final Word[] words = wordGramStat.getWords(tokens);
          wordGramStat.addAll(words);
        }
      }
    }

    if (altStats != null) {
      for (WordGramStat altStat : altStats) {
        altStat.add(string);
      }
    }
  }

  /**
   * Determine whether the underlying stats were pruned while adding.
   */
  public boolean wasPruned() {
    return pruned;
  }

  /**
   * Flush added N-grams through stat aggregation since the last flush,
   * identifying the source (ok if null).
   */
  public void flush(String source) {
    if (rFlag) return;

    for (WordGramStat stat : stats) {
      if (stat.flush(source)) {
        pruned = true;
      }
    }
    if (altStats != null) {
      for (WordGramStat altStat : altStats) {
        if (altStat.flush(source)) {
          pruned = true;
        }
      }
    }
  }

  /**
   * Add the nGramFreq to a reconstruction instance.
   */
  public void add(NGramFreq nGramFreq) {
    if (!rFlag) return;

    stats[0].add(nGramFreq);
    if (altStats != null) {
      for (WordGramStat altStat : altStats) {
        altStat.add(nGramFreq);
      }
    }
  }

  /**
   * Prune lower-N ngrams that are contained (as full words) within
   * higher-N-ngrams at the same frequency.
   * <p>
   * Note that this only prunes those that have already been flushed
   * and that later flushes will need to be "re-pruned".
   */
  public void pruneOverlap() {
    if (rFlag) return;
    WordGramStatsUtil.pruneOverlap(stats, highN);
  }

  /**
   * Find the (first intance of the normalized) wordGram in the (original) text.
   *
   * @param wordGram  The word N-gram text to find.
   * @param n         The N-gram's 'N' (number of words in wordGram).
   * @param text      The original text in which to locate the wordGram.
   *
   * @return the first [start, end) position of the word N-gram in text, or
   *         null if not found.
   */
  public int[] findWordGram(String wordGram, int n, String text) {
    int[] result = null;

    final int index = n - lowN;

    if (index >= 0 && index < stats.length) {
      result = stats[index].getSplitter().findWordGram(wordGram, text);
    }

    return result;
  }

  /**
   * Get the raw n-gram to frequency map.
   *
   * @param n  Is the "n" in n-gram in the range of lowN to highN.
   */
  public Map<String, NGramFreq> getNgram2Freq(int n) {
    Map<String, NGramFreq> result = null;

    final int index = n - lowN;
    if (index >= 0 && index < stats.length) {
      result = stats[index].getNgram2Freq();
    }

    return result;
  }

  /**
   * Get the top 'num' n-grams.
   *
   * @param n  Is the "n" in n-gram in the range of lowN to highN.
   * @param num  Is the (maximum) number of n-grams to collect.
   * @param die  Flag to monitor for a signal to terminate early.
   */
  public NGramFreq[] getTopNGramsLimitedByCount(int n, int num, AtomicBoolean die) {
    NGramFreq[] result = null;

    final int index = n - lowN;
    if (index >= 0 && index < stats.length) {
      result = stats[index].getTopNGramsLimitedByCount(num, die);
    }

    return result;
  }

  /**
   * Get the top n-grams w/freq greater than or equal to minFreq.
   *
   * @param n  Is the "n" in n-gram in the range of lowN to highN.
   * @param minFreq  Is the (minimum) frequency of an n-gram to collect.
   * @param die  Flag to monitor for a signal to terminate early.
   */
  public NGramFreq[] getTopNGramsLimitedByFreq(int n, int minFreq, AtomicBoolean die) {
    NGramFreq[] result = null;

    final int index = n - lowN;
    if (index >= 0 && index < stats.length) {
      result = stats[index].getTopNGramsLimitedByFreq(minFreq, die);
    }

    return result;
  }

  /**
   * Get the top 'num' n-grams.
   *
   * @param n  Is the "n" in n-gram in the range of lowN to highN.
   * @param num  Is the (maximum) number of n-grams to collect.
   * @param minFreq  Is the (minimum) frequency of an n-gram to collect.
   * @param die  Flag to monitor for a signal to terminate early.
   */
  public NGramFreq[] getTopNGrams(int n, int num, int minFreq, AtomicBoolean die) {
    NGramFreq[] result = null;

    final int index = n - lowN;
    if (index >= 0 && index < stats.length) {
      result = stats[index].getTopNGrams(num, minFreq, null, die);
    }

    return result;
  }

  private static final class AsyncAdder implements Runnable {

    final WordGramStat wordGramStat;
    private String[] tokens;

    AsyncAdder(WordGramStat wordGramStat) {
      this.wordGramStat = wordGramStat;
    }

    public void run() {
      final Word[] words = wordGramStat.getWords(tokens);
      wordGramStat.addAll(words);
    }

    public Thread launchThread(String[] tokens) {
      this.tokens = tokens;
      final Thread result = new Thread(this, "WordGramStats-AdderThread-" + wordGramStat.n);
      result.start();
      return result;
    }
  }
}
