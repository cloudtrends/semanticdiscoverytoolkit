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
package org.sd.util;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Utility and container for generating histogram data.
 * <p>
 * @author Spence Koehler
 */
public class Histogram <T> {
  
  private Map<T, Frequency<T>> map;
  private List<Frequency<T>> _frequencies;
  private Map<Comparator<T>, List<Frequency<T>>> _sortedFrequencies;
  private HistogramDistribution _distribution;

  public Histogram() {
    this.map = new HashMap<T, Frequency<T>>();
  }

  /**
   * Add an element to this histogram.
   * <p>
   * Note that the element's class should have equals and hashCode implemented
   * consistently.
   *
   * @return the added frequency instance.
   */
  public Frequency<T> add(T element) {
    return add(element, 1);
  }

  /**
   * Add an element to this histogram.
   * <p>
   * Note that the element's class should have equals and hashCode implemented
   * consistently.
   *
   * @return the added frequency instance.
   */
  public Frequency<T> add(T element, long freqCount) {
    Frequency<T> freq = map.get(element);
    if (freq == null) {
      freq = new Frequency<T>(element, freqCount);
      map.put(element, freq);
    }
    else {
      freq.inc(freqCount);
    }
    _frequencies = null;
    _distribution = null;
    return freq;
  }

  /**
   * Set the frequency count for the current element.
   */
  public Frequency<T> set(T element, long freqCount) {
    final Frequency<T> freq = new Frequency<T>(element, freqCount);
    map.put(element, freq);
    _frequencies = null;
    _distribution = null;
    return freq;
  }

  /**
   * Add (incorporate) the other histogram's elements to this instance.
   */
  public void add(Histogram<T> other) {
    for (Map.Entry<T, Frequency<T>> otherEntry : other.map.entrySet()) {
      this.add(otherEntry.getKey(), otherEntry.getValue().getFrequency());
    }
  }

  /**
   * Get all of the frequencies in rank order.
   */
  public List<Frequency<T>> getFrequencies() {
    if (_frequencies == null) {
      this._frequencies = new ArrayList<Frequency<T>>(map.values());
      Collections.sort(this._frequencies);
    }
    return _frequencies;
  }
  /**
   * Get all of the frequencies in key order.
   */
  public List<Frequency<T>> getFrequencies(Comparator<T> comparator) {
    if(_sortedFrequencies == null)
      this._sortedFrequencies = new HashMap<Comparator<T>, List<Frequency<T>>>();
    
    List<Frequency<T>> result = this._sortedFrequencies.get(comparator);
    if (result == null) {
      result = new ArrayList<Frequency<T>>();

      TreeSet<T> aggregator = new TreeSet<T>(comparator);
      for(T key : map.keySet())
        aggregator.add(key);

      for(T key : aggregator)
        result.add(map.get(key));

      this._sortedFrequencies.put(comparator, result);
    }
    return result;
  }
  
  /**
   * Get the number of ranks in this histogram.
   */
  public long getNumRanks() {
    return (long)map.size();
  }

  /**
   * Get the total count across all frequencies.
   */
  public long getTotalCount() {
    long result = 0;

    final long numRanks = getNumRanks();
    for (long i = 0; i < numRanks; ++i) {
      result += getFrequencyCount(i);
    }

    return result;
  }

  /**
   * Get the distribution of this histogram where the keys are the histogram's
   * frequency counts and the frequency counts are the  number of buckets with
   * that frequency count.
   */
  public HistogramDistribution getDistribution() {
    if (_distribution == null) {
      _distribution = HistogramDistribution.makeInstance(this);
    }
    return _distribution;
  }

  /**
   * Convenience method to get the maximum frequency count.
   */
  public long getMaxFrequencyCount() {
    return getFrequencyCount(0);
  }

  /**
   * Get the frequency with the given rank, where 0 is the most frequent,
   * 1 is the second most frequent, etc.
   *
   * @return the frequency or null if there is no frequency with the given rank.
   */
  public Frequency<T> getRankFrequency(long rank) {
    Frequency<T> result = null;

    // rank might be larger then the map allows
    if(rank > Integer.MAX_VALUE)
      return result;

    if (rank < map.size()) {
      final List<Frequency<T>> frequencies = getFrequencies();
      result = frequencies.get((int)rank);
    }

    return result;
  }

  /**
   * Get the element with the given rank, where 0 is the most frequent,
   * 1 is the second most frequent, etc.
   *
   * @return the element or null if there is no element with the given rank.
   */
  public T getElement(long rank) {
    T result = null;

    final Frequency<T> freq = getRankFrequency(rank);
    if (freq != null) {
      result = freq.getElement();
    }

    return result;
  }

  /**
   * Get the frequency count of the element with the given rank,
   * where 0 is the most frequent, 1, is the second most frequent, etc.
   *
   * @return the frequency count or -1 if there is no element with the given rank.
   */
  public long getFrequencyCount(long rank) {
    long result = -1;

    final Frequency<T> freq = getRankFrequency(rank);
    if (freq != null) {
      result = freq.getFrequency();
    }

    return result;
  }

  /**
   * Get the rank of the given element.
   *
   * @return the rank or -1 if the element is not found.
   */
  public long getRank(T element) {
    long result = -1;

    final Frequency<T> freq = map.get(element);
    if (freq != null) {
      final List<Frequency<T>> frequencies = getFrequencies();
      result = frequencies.indexOf(freq);
    }

    return result;
  }

  /**
   * Get the frequency instance for the given element.
   */
  public Frequency<T> getElementFrequency(T element) {
    return map.get(element);
  }

  /**
   * Given a collection of elements, sort them in order of frequency
   * according to this histogram. If an element isn't in this histogram,
   * do not include it in the result.
   *
   * @return the ranked elements that exist in this histogram, or null
   *         if none exist.
   */
  public List<Frequency<T>> getFrequencies(Collection<T> elements) {
    List<Frequency<T>> result = null;

    for (T element : elements) {
      final Frequency<T> freq = getElementFrequency(element);
      if (freq != null) {
        if (result == null) result = new ArrayList<Frequency<T>>();
        result.add(freq);
      }
    }

    if (result != null) {
      Collections.sort(result);
    }

    return result;
  }

  public String toString() {
    return toString(20);
  }

  public String toString(long maxRanks) {
    final StringBuilder result = new StringBuilder();

    final long totalCount = getTotalCount();
    long cumulativeCount = 0;

    long numRanks = getNumRanks();
    if (maxRanks > 0 && maxRanks < numRanks) numRanks = maxRanks;
    final int maxRankDigits = (int)Math.round(MathUtil.log10(numRanks) + 0.5);

    final long maxFreq = getMaxFrequencyCount();
    final int maxFreqDigits = (int)Math.round(MathUtil.log10(maxFreq) + 0.5);

    // rank  freq  cumulativePct  pct  label
    // %<maxRankDigits>d  %<maxFreqDigits>d  %6.2f  %6.2f  %40s
    final StringBuilder formatString = new StringBuilder();
    formatString.
      append("%").
      append(Math.max(1, maxRankDigits)).
      append("d  %").
      append(Math.max(1, maxFreqDigits)).
      append("d  %6.2f  %6.2f  %-40s");

    result.append("h(").append(getTotalCount()).append('/').append(getNumRanks()).append(")");
    for (long i = 0; i < numRanks; ++i) {

      //result.append("\n  ").append(i).append(": ").append(getRankFrequency(i));

      final Frequency<T> freq = getRankFrequency(i);
      cumulativeCount += freq.getFrequency();
      final double cumPct = 100.0 * ((double)cumulativeCount / (double)totalCount);
      final double pct = 100.0 * ((double)freq.getFrequency() / (double)totalCount);
      result.
        append("\n  ").
        append(String.format(formatString.toString(),
                             i,                    // rank
                             freq.getFrequency(),  // freq
                             cumPct,               // cumPct
                             pct,                  // pct
                             freq.getElement().toString()));
    }

    return result.toString();
  }


  public class Frequency <T> implements Comparable<Frequency<T>> {
    public final T element;
    private long frequency;
    private Map<String, String> attributes;

    private Frequency(T element, long frequency) {
      this.element = element;
      this.frequency = frequency;
      this.attributes = null;
    }

    void inc(long amount) {
      this.frequency += amount;
    }

    public T getElement() {
      return element;
    }

    public long getFrequency() {
      return frequency;
    }

    public boolean hasAttributes() {
      return attributes != null && attributes.size() > 0;
    }

    /**
     * Get the (possibly null) attributes.
     */
    public Map<String, String> getAttributes() {
      return attributes;
    }

    /**
     * Set this instance's attributes to the given map instance.
     *
     * @return the previous attributes map.
     */
    public Map<String, String> setAttributes(Map<String, String> attributes) {
      final Map<String, String> result = this.attributes;
      this.attributes = attributes;
      return result;
    }

    public void setAttribute(String att, String val) {
      if (attributes == null) attributes = new HashMap<String, String>();
      attributes.put(att, val);
    }

    /**
     * Get the (possibly null) value for the given attribute.
     */
    public String getAttributeValue(String att) {
      String result = null;

      if (attributes != null) {
        result = attributes.get(att);
      }

      return result;
    }

    /**
     * Natural ordering is from highest to lowest frequency.
     */
    public int compareTo(Frequency<T> other) {
      long v = other.frequency - this.frequency;
      if(v < Integer.MIN_VALUE)
        return Integer.MIN_VALUE;
      else if(v > Integer.MAX_VALUE)
        return Integer.MAX_VALUE;
      else
        return (int)v;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.append(frequency).append(" (").append(element).append(')');

      return result.toString();
    }
  }
}
