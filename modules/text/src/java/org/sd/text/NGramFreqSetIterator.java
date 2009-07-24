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


import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * An iterator over sets of NGrams with the same frequency from most
 * to least frequent.
 * <p>
 * @author Spence Koehler
 */
public class NGramFreqSetIterator implements Iterator<NGramFreqSet> {
  
  private List<NGramFreq> ngrams;
  private boolean collapsible;

  private Iterator<NGramFreq> iter;
  private NGramFreqSet nextNGramSet;
  private NGramFreq lastNGram;

  /**
   * Create an iterator over the list of ngrams.
   * <p>
   * This default constructor assumes that the ngrams are not sorted;
   * also, the generated sets <b>will</b> be collapsible.
   *
   * @param ngrams  The list of ngrams over which to iterate.
   */
  public NGramFreqSetIterator(List<NGramFreq> ngrams) {
    this(ngrams, false, true);
  }

  /**
   * Create an iterator over the list of ngrams.
   * <p>
   * @param ngrams  The list of ngrams over which to iterate.
   * @param isSorted  If true, then the ngrams will not be (re)sorted here.
   *                  If false, the underlying input collection will not be
   *                  changed when sorted here.
   * @param collapsible  If true, then the returned sets will be collapsible.
   *                     Note that collapsible sets are more costly to construct.
   */
  public NGramFreqSetIterator(Collection<NGramFreq> ngrams, boolean isSorted, boolean collapsible) {
    this.ngrams = new LinkedList<NGramFreq>(ngrams);
    this.collapsible = collapsible;

    // sort if needed
    if (!isSorted) Collections.sort(this.ngrams);

    this.iter = this.ngrams.iterator();
    this.lastNGram = null;

    inc();
  }

  /**
   * Determine whether there is a next set.
   */
  public boolean hasNext() {
    return nextNGramSet != null;
  }

  /**
   * Get the next set.
   */
  public NGramFreqSet next() {
    final NGramFreqSet result = nextNGramSet;
    inc();
    return result;
  }

  /**
   * Remove the last text returned by 'next'.
   * <p>
   * Not implemented!
   *
   * @throws UnsupportedOperationException
   */
  public void remove() {
    throw new UnsupportedOperationException("Not implemented!");
  }

  /**
   * Increment to the next set.
   */
  private final void inc() {
    if (!iter.hasNext()) {
      nextNGramSet = null;
    }
    else {
      if (lastNGram == null) lastNGram = iter.next();
      nextNGramSet = new NGramFreqSet(lastNGram, collapsible);
      lastNGram = null;  // clear what we just added

      while (iter.hasNext()) {
        lastNGram = iter.next();
        if (!nextNGramSet.add(lastNGram)) break;
        lastNGram = null;  // clear what we just added
      }
    }
  }
}
