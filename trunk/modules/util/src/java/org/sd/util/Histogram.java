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

  public Histogram() {
    this.map = new HashMap<T, Frequency<T>>();
  }

  /**
   * Add an element to this histogram.
   * <p>
   * Note that the element's class should have equals and hashCode implemented
   * consistently.
   */
  public void add(T element) {
    add(element, 1);
  }

  /**
   * Add an element to this histogram.
   * <p>
   * Note that the element's class should have equals and hashCode implemented
   * consistently.
   */
  public void add(T element, int freqCount) {
    Frequency<T> freq = map.get(element);
    if (freq == null) {
      freq = new Frequency<T>(element, freqCount);
      map.put(element, freq);
    }
    else {
      freq.inc(freqCount);
    }
    _frequencies = null;
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
  public int getNumRanks() {
    return map.size();
  }

  /**
   * Get the total count across all frequencies.
   */
  public int getTotalCount() {
    int result = 0;

    final int numRanks = getNumRanks();
    for (int i = 0; i < numRanks; ++i) {
      result += getFrequencyCount(i);
    }

    return result;
  }

  /**
   * Convenience method to get the maximum frequency count.
   */
  public int getMaxFrequencyCount() {
    return getFrequencyCount(0);
  }

  /**
   * Get the frequency with the given rank, where 0 is the most frequent,
   * 1 is the second most frequent, etc.
   *
   * @return the frequency or null if there is no frequency with the given rank.
   */
  public Frequency<T> getFrequency(int rank) {
    Frequency<T> result = null;

    if (rank < map.size()) {
      final List<Frequency<T>> frequencies = getFrequencies();
      result = frequencies.get(rank);
    }

    return result;
  }

  /**
   * Get the element with the given rank, where 0 is the most frequent,
   * 1 is the second most frequent, etc.
   *
   * @return the element or null if there is no element with the given rank.
   */
  public T getElement(int rank) {
    T result = null;

    final Frequency<T> freq = getFrequency(rank);
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
  public int getFrequencyCount(int rank) {
    int result = -1;

    final Frequency<T> freq = getFrequency(rank);
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
  public int getRank(T element) {
    int result = -1;

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
  public Frequency<T> getFrequency(T element) {
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
      final Frequency<T> freq = getFrequency(element);
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

  public String toString(int maxRanks) {
    final StringBuilder result = new StringBuilder();

    result.append("h(").append(getNumRanks()).append(")");
    for (int i = 0; i < maxRanks && i < getNumRanks(); ++i) {
      result.append("\n  ").append(i).append(": ").append(getFrequency(i));
    }

    return result.toString();
  }


  public class Frequency <T> implements Comparable<Frequency<T>> {
    public final T element;
    private int frequency;

    private Frequency(T element, int frequency) {
      this.element = element;
      this.frequency = frequency;
    }

    void inc(int amount) {
      this.frequency += amount;
    }

    public T getElement() {
      return element;
    }

    public int getFrequency() {
      return frequency;
    }

    /**
     * Natural ordering is from highest to lowest frequency.
     */
    public int compareTo(Frequency<T> other) {
      return other.frequency - this.frequency;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.append(frequency).append(" (").append(element).append(')');

      return result.toString();
    }
  }
}
