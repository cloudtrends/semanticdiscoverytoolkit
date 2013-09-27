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
import org.sd.util.range.LongRange;

/**
 * Container for a class representing the distribution of a histogram.
 * <p>
 * The bucket keys are the frequency counts of the represented histogram with 
 * frequency counts for the occurrence of each count.
 * 
 * @author Spence Koehler
 */
public class HistogramDistribution extends Histogram<Long> {
  
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

  private Long _switchRank;
  private Long _numOriginalRanks;
  private Long _numOriginalInstances;
  private Map<Long, Long> _count2rank;

  public HistogramDistribution() {
    super();
    this._switchRank = null;
    this._numOriginalRanks = null;
    this._numOriginalInstances = null;
    this._count2rank = null;
  }

  /**
   * Get the number of original bins with the given original count.
   */
  public long getNumBins(long origCount) {
    long result = 0;

    final Frequency<Long> freq = getElementFrequency(origCount);
    if (freq != null) {
      result = freq.getFrequency();
    }
    
    return result;
  }

  /**
   * Get the number of ranks in the *original* histogram.
   */
  public long getNumOriginalRanks() {
    if (_numOriginalRanks == null) {
      _numOriginalRanks = computeNumOriginalRanks();
    }
    return _numOriginalRanks;
  }

  private final long computeNumOriginalRanks() {
    long result = 0;

    for (Frequency<Long> freq : getFrequencies()) {
      final long numBuckets = freq.getFrequency();
      result += numBuckets;
    }

    return result;
  }

  /**
   * Get the number of instances (totalCount) in the *original* histogram.
   */
  public long getNumOriginalInstances() {
    if (_numOriginalInstances == null) {
      _numOriginalInstances = computeNumOriginalInstances();
    }
    return _numOriginalInstances;
  }

  private final long computeNumOriginalInstances() {
    long result = 0;

    for (Frequency<Long> freq : getFrequencies()) {
      final long curCount = freq.getElement();
      final long numBuckets = freq.getFrequency();
      result += (curCount * numBuckets);
    }

    return result;
  }

  /**
   * Get the *original* histogram rank(s) having the given count.
   */
  public LongRange getOriginalRank(long count) {
    LongRange result = null;

    final Map<Long, Long> count2rank = getCount2Rank();
    final Long startRank = count2rank.get(count);
    if (startRank != null) {
      final Frequency<Long> freq = getElementFrequency(count);
      final long numBuckets = freq.getFrequency();

      final long low = startRank;
      final long high = startRank + numBuckets - 1;
      result = new LongRange(low, high);
    }

    return result;
  }

  /**
   * Get the number of *original* histogram instances in the original bucket(s)
   * having the given count.
   */
  public Long getNumOriginalInstances(long count) {
    Long result = null;

    final Frequency<Long> freq = getElementFrequency(count);
    if (freq != null) {
      final long numBuckets = freq.getFrequency();
      result = count * numBuckets;
    }

    return result;
  }

  /**
   * Get the percentage of *original* histogram instances with the given count.
   */
  public Double getCountPercentage(long count) {
    Double result = null;

    final Long numInstances = getNumOriginalInstances(count);
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
  public long getSwitchRank() {
    if (_switchRank == null) {
      _switchRank = computeSwitchRank(0);
    }
    return _switchRank;
  }

  public long computeSwitchRank(long maxNumBuckets) {
    long result = getNumOriginalRanks();

    // rank order is distribution's (long) keys sorted from max to min
    for (Frequency<Long> freq : getFrequencies()) {
      final long curCount = freq.getElement();
      final long numBuckets = freq.getFrequency();

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
  public Map<Long, Long> getCount2Rank() {
    if (_count2rank == null) {
      _count2rank = computeCount2Rank();
    }
    return _count2rank;
  }

  private final Map<Long, Long> computeCount2Rank() {
    final Map<Long, Long> result = new HashMap<Long, Long>();

    long rank = 0;
    final Set<Frequency<Long>> freqs = new TreeSet<Frequency<Long>>(ORIG_FREQ_ORDER);
    freqs.addAll(getFrequencies());
    for (Frequency<Long> freq : freqs) {
      final long curCount = freq.getElement();
      final long numBuckets = freq.getFrequency();

      result.put(curCount, rank);

      rank += numBuckets;
    }

    return result;
  }

  private static final class OriginalFreqOrder 
    implements Comparator<Histogram<Long>.Frequency<Long>> 
  {
    public int compare(Histogram<Long>.Frequency<Long> freq1, 
                       Histogram<Long>.Frequency<Long> freq2) 
    {
      long v = freq2.getElement() - freq1.getElement();
      if(v < Integer.MIN_VALUE)
        return Integer.MIN_VALUE;
      else if(v > Integer.MAX_VALUE)
        return Integer.MAX_VALUE;
      else
        return (int)v;
    }
  }
}
