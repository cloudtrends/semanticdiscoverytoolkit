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
package org.sd.xml;


import java.util.List;
import org.sd.util.SampleCollector;

/**
 * Container for collapsed histogram keys used as the elements in
 * CollapsedHistogram instances.
 * <p>
 * @author Spence Koehler
 */
public class CollapsedKeyContainer <T> implements Comparable<CollapsedKeyContainer<T>> {
  
  private T key;
  private int origCount;
  private int binCount;
  private int maxSamples;
  private SampleCollector<T> sampleCollector;

  /**
   * Construct an instance backed by a bin key.
   */
  public CollapsedKeyContainer(T key, int origCount, int maxSamples) {
    this.key = key;
    this.origCount = origCount;
    this.binCount = 1;
    this.maxSamples = maxSamples;
    this.sampleCollector = (maxSamples > 0) ? new SampleCollector<T>(maxSamples) : null;
  }

  public boolean isCountKey() {
    return key == null;
  }

  public boolean isNormalKey() {
    return key != null;
  }

  public int getOrigCount() {
    return origCount;
  }

  public int getBinCount() {
    return binCount;
  }

  public void setBinCount(int binCount) {
    this.binCount = binCount;
  }

  public void incBinCount() {
    ++binCount;
  }

  public int getTotalCount() {
    return origCount * binCount;
  }

  public T getNormalKey() {
    return key;
  }

  public void considerSample(T element) {
    if (sampleCollector != null) {
      sampleCollector.consider(element);
    }
  }

  public List<T> getSamples() {
    List<T> result = null;

    if (sampleCollector != null) {
      result = sampleCollector.getSamples();
    }

    return result;
  }

  public String buildKeyString() {
    final StringBuilder result = new StringBuilder();

    if (isNormalKey()) {
      result.append(key);
    }
    else {
      // ~"..." (M)
      if (sampleCollector != null && sampleCollector.getNumSamples() > 0) {
        result.
          append("~\"").
          append(sampleCollector.getSamples().get(0)).
          append("\" ");
      }
      result.
        append('(').
        append(origCount).append('*').append(binCount).
        append(')');
    }

    return result.toString();
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(buildKeyString());

    if (isNormalKey()) {
      result.append('(').append(origCount).append(')');
    }

    return result.toString();
  }

  public boolean equals(Object o) {
    boolean result = this == o;

    if (!result && o instanceof CollapsedKeyContainer) {
      final CollapsedKeyContainer other = (CollapsedKeyContainer)o;
      if ((this.key == other.key || (this.key != null && this.key.equals(other.key))) &&
          (this.origCount == other.origCount)) {
        result = true;
      }
    }

    return result;
  }

  public int hashCode() {
    int result = 1;

    if (key != null) {
      result = 17 * result + key.hashCode();
    }

    result = 17 * result + origCount;

    return result;
  }

  public int compareTo(CollapsedKeyContainer<T> other) {
    int result = 0;

    // count keys come after normal keys
    // sort by descending origCount

    if (this != other) {
      if (this.isNormalKey()) {
        if (other.isCountKey()) {
          // this comes before other
          result = -1;
        }
        else {
          // sort by descending frequency
          result = other.origCount - this.origCount;
        }
      }
      else {  // this.isCountKey()
        if (other.isNormalKey()) {
          // this comes after other
          result = 1;
        }
        else {
          // sort by descending count
          result = other.origCount - this.origCount;
        }
      }
    }

    return result;
  }
}
