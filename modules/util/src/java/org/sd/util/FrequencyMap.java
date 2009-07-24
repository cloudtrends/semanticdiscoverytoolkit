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
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Map from a key to multiple values with frequencies for each value.
 * <p>
 * @author Spence Koehler
 */
public class FrequencyMap<K, V> {

  private Map<K, FValues<V>> map;

  public FrequencyMap() {
    this.map = new HashMap<K, FValues<V>>();
  }

  public void put(K key, V value) {
    FValues<V> fvals = map.get(key);
    if (fvals == null) {
      fvals = new FValues<V>();
      map.put(key, fvals);
    }
    fvals.add(value);
  }

  /**
   * Get the values for the key from the most to least frequent.
   */
  public List<FValue<V>> getValues(K key) {
    List<FValue<V>> result = null;

    final FValues<V> fvals = map.get(key);
    if (fvals != null) {
      result = new ArrayList<FValue<V>>(fvals.getFValues());
      Collections.sort(result);
    }

    return result;
  }

  /**
   * Get the most frequent value for the key.
   */
  public V getMostFrequentValue(K key) {
    V result = null;

    final List<FValue<V>> values = getValues(key);
    if (values != null && values.size() > 0) {
      result = values.get(0).getValue();
    }

    return result;
  }

  public Map<K, V> getMostFrequentMappings() {
    final Map<K, V> result = new HashMap<K, V>();

    for (K key : map.keySet()) {
      final V value = getMostFrequentValue(key);
      result.put(key, value);
    }

    return result;
  }


  private static final class FValues<V> {
    private Map<V, FValue<V>> val2freq;

    FValues() {
      val2freq = new HashMap<V, FValue<V>>();
    }

    public void add(V value) {
      FValue fval = val2freq.get(value);
      if (fval == null) {
        val2freq.put(value, new FValue<V>(value));
      }
      else {
        fval.inc();
      }
    }

    public Collection<FValue<V>> getFValues() {
      return val2freq.values();
    }
  }

  public static final class FValue<V> implements Comparable<FValue<V>> {
    private V value;
    private int freq;

    FValue(V value) {
      this.value = value;
      this.freq = 1;
    }

    public final int inc() {
      return inc(1);
    }

    public final int inc(int n) {
      this.freq += n;
      return freq;
    }

    public V getValue() {
      return value;
    }

    public int getFreq() {
      return freq;
    }

    /**
     * Compare such that greater frequencies sort first.
     */
    public int compareTo(FValue<V> other) {
      return other.freq - this.freq;
    }
  }
}
