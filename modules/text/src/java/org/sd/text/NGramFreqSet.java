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



/**
 * Container for a set of NGrams sharing the same frequency.
 * <p>
 * @author Spence Koehler
 */
public class NGramFreqSet extends NGramSet {
  
  private long freq;

  /**
   * Construct a new (empty) set. The first added ngram will determine the
   * constrained frequency.
   * 
   * @param collapsible  True if this set is to be collapsible. This should
   *                     only be set when needed as it adds a measure of
   *                     computational complexity incurred during construction.
   */
  public NGramFreqSet(boolean collapsible) {
    this(null, collapsible);
  }

  /**
   * Construct a new set starting with the given ngram.
   * 
   * @param ngram  This set's first ngram.
   * @param collapsible  True if this set is to be collapsible. This should
   *                     only be set when needed as it adds a measure of
   *                     computational complexity incurred during construction.
   */
  public NGramFreqSet(NGramFreq ngram, boolean collapsible) {
    super(ngram, collapsible);

    if (ngram != null) {
      this.freq = ngram.getFreq();
    }
  }

  /**
   * Add the ngram iff it has the same frequency as others in this set.
   *
   * @return true if added; otherwise, false.
   */
  public boolean add(NGramFreq ngram) {
    boolean result = false;

    if (size() == 0) {
      result = super.add(ngram);
      this.freq = ngram.getFreq();
    }
    else if (ngram.getFreq() == freq) {
      result = super.add(ngram);
    }

    return result;
  }

  /**
   * Get the frequency of all of the ngrams in this set.
   */
  public long getFreq() {
    return freq;
  }
}
