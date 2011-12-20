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


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility for collecting random samples.
 * <p>
 * @author Spence Koehler
 */
public class SampleCollector<T> {
  
  private Random random;
  private int maxSamples;
  private List<T> samples;
  private int totalCount;

  /**
   * Construct to collect a maximum of maxSamples samples.
   */
  public SampleCollector(int maxSamples) {
    this(new Random(), maxSamples);
  }

  /**
   * Construct to collect a mximum of maxSamples samples using the given
   * random generator.
   */
  public SampleCollector(Random random, int maxSamples) {
    this.random = random;
    this.maxSamples = maxSamples;
    this.samples = new ArrayList<T>();
    this.totalCount = 0;
  }

  /**
   * Consider a value for collection.
   *
   * @return true if the value was selected as a sample (for now).
   */
  public boolean consider(T value) {
    boolean result = false;
    final int curCount = samples.size();

    if (curCount < maxSamples) {
      // fill samples with first encountered values
      samples.add(value);
      result = true;
    }
    else {
      // randomly replace a sample with this at rate of numSamples/totalCount
      // so every instance has an equal chance of being selected.
      final int sampleIdx = random.nextInt(totalCount);
      if (sampleIdx < maxSamples) {
        samples.set(sampleIdx, value);
        result = true;
      }
    }

    ++totalCount;

    return result;
  }

  /**
   * Add the value as a sample unless no more will fit.
   */
  public void add(T value) {
    if (maxSamples == 0 || samples.size() < maxSamples) {
      samples.add(value);
    }
  }

  /**
   * Get the maximum number of samples to collect.
   */
  public int getMaxSamples() {
    return maxSamples;
  }

  /**
   * Set the maximum number of samples to collect.
   */
  public void setMaxSamples(int maxSamples) {
    this.maxSamples = maxSamples;
  }

  /**
   * Get the random number generator.
   */
  public Random getRandom() {
    return random;
  }

  /**
   * Set the random number generator.
   */
  public void setRandom(Random random) {
    this.random = random;
  }

  /**
   * Get the actual number of samples collected.
   */
  public int getNumSamples() {
    return samples == null ? 0 : samples.size();
  }

  /**
   * Get the collected samples.
   */
  public List<T> getSamples() {
    return samples;
  }

  /**
   * Get the total number of samples considered.
   */
  public int getTotalCount() {
    return totalCount;
  }
}
