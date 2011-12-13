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
package org.sd.util;


import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.sd.util.range.IntegerRange;

/**
 * Container for a class representing the distribution of a histogram.
 * <p>
 * The bucket keys are the frequency counts of the represented histogram with 
 * frequency counts for the occurrence of each count.
 * 
 * @author Spence Koehler
 */
public class HistogramDistribution extends Histogram<Integer> {
  
  /**
   * Create a HistogramDistribution instance from the given histogram.
   */
  public static final <T> HistogramDistribution makeInstance(Histogram<T> histogram) {
    final HistogramDistribution result = new HistogramDistribution();

    for (Histogram<T>.Frequency<T> freq : histogram.getFrequencies()) {
      result.add(freq.getFrequency());
    }

    return result;
  }


  private static final OriginalFreqOrder ORIG_FREQ_ORDER = new OriginalFreqOrder();

  private Integer _switchRank;
  private Integer _numOriginalRanks;
  private Integer _numOriginalInstances;
  private Map<Integer, Integer> _count2rank;

  public HistogramDistribution() {
    super();
    this._switchRank = null;
    this._numOriginalRanks = null;
    this._numOriginalInstances = null;
    this._count2rank = null;
  }

  /**
   * Get the number of ranks in the *original* histogram.
   */
  public int getNumOriginalRanks() {
    if (_numOriginalRanks == null) {
      _numOriginalRanks = computeNumOriginalRanks();
    }
    return _numOriginalRanks;
  }

  private final int computeNumOriginalRanks() {
    int result = 0;

    for (Frequency<Integer> freq : getFrequencies()) {
      final int numBuckets = freq.getFrequency();
      result += numBuckets;
    }

    return result;
  }

  /**
   * Get the number of instances (totalCount) in the *original* histogram.
   */
  public int getNumOriginalInstances() {
    if (_numOriginalInstances == null) {
      _numOriginalInstances = computeNumOriginalInstances();
    }
    return _numOriginalInstances;
  }

  private final int computeNumOriginalInstances() {
    int result = 0;

    for (Frequency<Integer> freq : getFrequencies()) {
      final int curCount = freq.getElement();
      final int numBuckets = freq.getFrequency();
      result += (curCount * numBuckets);
    }

    return result;
  }

  /**
   * Get the *original* histogram rank(s) having the given count.
   */
  public IntegerRange getOriginalRank(int count) {
    IntegerRange result = null;

    final Map<Integer, Integer> count2rank = getCount2Rank();
    final Integer startRank = count2rank.get(count);
    if (startRank != null) {
      final Frequency<Integer> freq = getElementFrequency(count);
      final int numBuckets = freq.getFrequency();

      final int low = startRank;
      final int high = startRank + numBuckets - 1;
      result = new IntegerRange(low, high);
    }

    return result;
  }

  /**
   * Get the number of *original* histogram instances in the original bucket(s)
   * having the given count.
   */
  public Integer getNumOriginalInstances(int count) {
    Integer result = null;

    final Frequency<Integer> freq = getElementFrequency(count);
    if (freq != null) {
      final int numBuckets = freq.getFrequency();
      result = count * numBuckets;
    }

    return result;
  }

  /**
   * Get the percentage of *original* histogram instances with the given count.
   */
  public Double getCountPercentage(int count) {
    Double result = null;

    final Integer numInstances = getNumOriginalInstances(count);
    if (numInstances != null) {
      result = (double)numInstances / (double)getNumOriginalInstances();
    }

    return result;
  }

  /**
   * Get the rank of the *original* histogram at which the frequency counts for
   * bins become less than the number of bins with a certain frequency count.
   * <p>
   * This rank can be used as a cutoff value for displaying histograms that
   * are too large (have too many keys) in its tail.
   * <p>
   * See org.sd.xml.CollapsedHistogram for an example of its use.
   */
  public int getSwitchRank() {
    if (_switchRank == null) {
      _switchRank = computeSwitchRank(0);
    }
    return _switchRank;
  }

  public int computeSwitchRank(int maxNumBuckets) {
    int result = getNumOriginalRanks();

    // rank order is distribution's (integer) keys sorted from max to min
    for (Frequency<Integer> freq : getFrequencies()) {
      final int curCount = freq.getElement();
      final int numBuckets = freq.getFrequency();

      // original histogram's rank increases by the product
      result -= numBuckets;

      if (curCount > numBuckets || (maxNumBuckets > 0 && numBuckets > maxNumBuckets)) break;
    }

    return result;
  }

  /**
   * Get the mapping from an original histogram count to its (first) original
   * rank.
   */
  public Map<Integer, Integer> getCount2Rank() {
    if (_count2rank == null) {
      _count2rank = computeCount2Rank();
    }
    return _count2rank;
  }

  private final Map<Integer, Integer> computeCount2Rank() {
    final Map<Integer, Integer> result = new HashMap<Integer, Integer>();

    int rank = 0;
    final Set<Frequency<Integer>> freqs = new TreeSet<Frequency<Integer>>(ORIG_FREQ_ORDER);
    freqs.addAll(getFrequencies());
    for (Frequency<Integer> freq : freqs) {
      final int curCount = freq.getElement();
      final int numBuckets = freq.getFrequency();

      result.put(curCount, rank);

      rank += numBuckets;
    }

    return result;
  }

  private static final class OriginalFreqOrder implements Comparator<Histogram<Integer>.Frequency<Integer>> {
    public int compare(Histogram<Integer>.Frequency<Integer> freq1, Histogram<Integer>.Frequency<Integer> freq2) {
      return freq2.getElement() - freq1.getElement();
    }
  }
}
