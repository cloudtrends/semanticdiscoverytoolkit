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


import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sd.util.thread.BaseGovernable;
import org.sd.util.thread.GovernableThread;
import org.sd.util.StringUtil;

/**
 * Container for a set of NGrams.
 * <p>
 * @author Spence Koehler
 */
public class NGramSet {
  
  private List<NGramFreq> ngrams;
  private boolean collapsible;
  private boolean collapsed;
  private Boolean _hasOverlap;

  /**
   * Construct a new (empty) set.
   * 
   * @param collapsible  True if this set is to be collapsible. This should
   *                     only be set when needed as it adds a measure of
   *                     computational complexity incurred during construction.
   */
  public NGramSet(boolean collapsible) {
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
  public NGramSet(NGramFreq ngram, boolean collapsible) {
    this.ngrams = new LinkedList<NGramFreq>();
    if (ngram != null) {
      ngrams.add(ngram);
    }
    this.collapsible = collapsible;
    this.collapsed = false;
    this._hasOverlap = collapsible ? false : null;
  }

  /**
   * Add the ngram.
   *
   * @return true.
   */
  public boolean add(NGramFreq ngram) {
    if (ngrams.size() == 0) {
      ngrams.add(ngram);
    }
    else {
      if (isCollapsible()) {
        insert(ngram);
      }
      else {
        ngrams.add(ngram);
      }
      collapsed = false;
    }

    return true;
  }

  /**
   * Get this set's NGrams.
   * <p>
   * If this set has been collapsed, then the collapsed NGrams will be returned.
   */
  public List<NGramFreq> getNGrams() {
    return ngrams;
  }

  /**
   * Get the size of this set.
   */
  public int size() {
    return ngrams.size();
  }

  /**
   * Collapse and return this set's ngrams.
   */
  public List<NGramFreq> getCollapsedNGrams() {
    return getCollapsedNGrams(null);
  }

  /**
   * Collapse and return this set's ngrams, cutting off computation if/when
   * the time limit is reached.
   *
   * @param timeLimit  The number of millis at which to halt computation.
   * @param waitMillis  The number of millis to wait for the process to die
   *                    after timeLimit has expired.
   *
   * @return the (possibly partially) collapsed ngrams.
   */
  public List<NGramFreq> getCollapsedNGrams(long timeLimit, long waitMillis) {
    if (timeLimit > 0) {
      collapse(timeLimit, waitMillis);
    }
    return ngrams;
  }

  /**
   * Collapse and return this set's ngrams.
   *
   * @param die  Flag to monitor for a signal to terminate early (ok if null).
   */
  public List<NGramFreq> getCollapsedNGrams(AtomicBoolean die) {
    collapse(die);
    return ngrams;
  }

  /**
   * Determine whether this set has been collapsed.
   */
  public boolean isCollapsed() {
    return collapsed;
  }

  /**
   * Determine whether this set is collapsible.
   */
  public boolean isCollapsible() {
    return collapsible;
  }

  /**
   * Collapse this set if possible.
   *
   * @return true if collapsed (now or previously); otherwise false.
   */
  public boolean collapse() {
    return collapse(null);
  }

  /**
   * Collapse this set if possible, cutting off computation if/when the time
   * limit is reached.
   *
   * @param timeLimit  The number of millis at which to halt computation.
   * @param waitMillis  The number of millis to wait for the process to die
   *                    after timeLimit has expired.
   *
   * @return true if collapsed (now or previously); otherwise false.
   */
  public boolean collapse(long timeLimit, long waitMillis) {
    if (collapsible) {
      GovernableThread.newGovernableThread(new BaseGovernable() {
          protected boolean doOperation(long workUnit, AtomicBoolean die) {
            collapse(die);
            return true;
          }
        }, true).runFor(timeLimit, waitMillis, true);
    }
    return collapsed;
  }

  /**
   * Collapse this set if possible.
   *
   * @param die  Flag to monitor for a signal to terminate early (ok if null).
   *
   * @return true if collapsed (now or previously); otherwise false.
   */
  public boolean collapse(AtomicBoolean die) {
    if (collapsible) {
      while (hasOverlap() && (die == null || !die.get())) {
        final int sizeBeforeCollapse = size();
        ngrams = doCollapse(die);
        final int sizeAfterCollapse = size();

        if (sizeBeforeCollapse == sizeAfterCollapse) {
          this._hasOverlap = false;
        }
        else {
          this._hasOverlap = null;  // don't know anymore, we'll have to recompute.
        }
      }
      collapsed = true;
    }

    return collapsed;
  }

  /**
   * Insert the ngram into this set such ngrams end up in an appropriate order
   * for collapsing.
   */
  protected final void insert(NGramFreq ngram) {
    // find the ngram with the highest overlap.
    OverlapInfo maxOverlap = null;

    int index = 0;
    for (NGramFreq existing : ngrams) {
      final OverlapInfo curOverlap = new OverlapInfo(ngram, index, existing);
      if (maxOverlap == null || maxOverlap.compareTo(curOverlap) > 0 && curOverlap.proper) {  // found one with higher overlap
        maxOverlap = curOverlap;
      }
      ++index;
    }
    
    // the one with the greatest starting position comes first
    if (maxOverlap == null || maxOverlap.length == 0) {  // no overlap, just add new to end
      ngrams.add(ngram);
    }
    else {
      this._hasOverlap = true;

      final int startPos = ngram.getNGram().indexOf(maxOverlap.overlap);
      if (startPos < maxOverlap.cpos) {
        // insert after
        if (maxOverlap.index == ngrams.size()) {
          // tack onto end
          ngrams.add(ngram);
        }
        else {
          // insert
          ngrams.add(maxOverlap.index + 1, ngram);
        }
      }
      else {
        // insert before
        ngrams.add(maxOverlap.index, ngram);
      }
    }
  }

  private final boolean hasOverlap() {
    if (_hasOverlap == null && collapsible) {
      // compute by re-inserting all of the ngrams.
      final List<NGramFreq> curNGrams = new ArrayList<NGramFreq>(ngrams);
      ngrams.clear();
      collapsed = false;
      this._hasOverlap = false;
      for (NGramFreq ngram : curNGrams) {
        insert(ngram);
      }
    }

    return _hasOverlap;
  }

  /**
   * Do the work of collapsing the current list of ngrams.
   * <p>
   * @param die  Flag to monitor for a signal to terminate early (ok if null).
   *
   * @return the (fully or partially) collapsed N-grams.
   */
  private final List<NGramFreq> doCollapse(AtomicBoolean die) {
    final List<NGramFreq> result = new LinkedList<NGramFreq>();

    NGramFreq lastNGramFreq = null;

    for (NGramFreq ngramFreq : ngrams) {
      if (die != null && die.get()) {
        // if asked to die, just spin through without collapsing
        if (lastNGramFreq != null) {
          result.add(lastNGramFreq);
          lastNGramFreq = null;
        }
        result.add(ngramFreq);
        continue;
      }

      boolean combined = false;

      if (lastNGramFreq != null) {
        final OverlapInfo overlapInfo = new OverlapInfo(ngramFreq, lastNGramFreq);
        String combinedNGram = null;
        if (overlapInfo.proper) {
          combinedNGram = overlapInfo.getCombined();
        }

        if (combinedNGram != null) {
          final long comboFreq = Math.min(ngramFreq.getFreq(), lastNGramFreq.getFreq());
          final int comboN = Math.max(ngramFreq.getN(), lastNGramFreq.getN());
          final NGramFreq combo = new NGramFreq(combinedNGram, comboFreq, comboN);
          combo.addAllSources(lastNGramFreq);
          combo.addAllSources(ngramFreq);
          lastNGramFreq = combo;
          combined = true;
        }
      }

      if (!combined) {
        if (lastNGramFreq != null) {
          result.add(lastNGramFreq);
          lastNGramFreq = null;
        }

        lastNGramFreq = ngramFreq;
      }
    }

    if (lastNGramFreq != null) {
      result.add(lastNGramFreq);
    }

    return result;
  }


  /**
   * Temporary data structure for finding insert position for a
   * target among candidates.
   */
  private static final class OverlapInfo implements Comparable<OverlapInfo> {
    final int index;       // index in list of the candidate ngram
    final int length;      // length of overlap
    final int cpos;        // character position in candidate ngram for start of overlap
    final int tpos;        // character position in target ngram for start of overlap
    final String overlap;  // the overlap string
    final boolean proper;  // whether the overlap is "proper" defined as being
                           // at the start of the target and to the end of the candidate.

    private String combined;  // combined string -- only computed when index<0

    /**
     * Construct such that "combined" is computed if "proper" is true.
     */
    OverlapInfo(NGramFreq target, NGramFreq candidate) {
      this(target, -1, candidate);
    }

    /**
     * Construct with the given info.
     * <p>
     * If index &lt; 0 then compute combined if "proper" is true.
     */
    OverlapInfo(NGramFreq target, int index, NGramFreq candidate) {
      this.index = index;

      final StringBuilder intersection = new StringBuilder();
      final String tgram = target.getNGram();
      final String cgram = candidate.getNGram();
      final int[] overlap = StringUtil.findOverlap(cgram, tgram, intersection);

      if (overlap == null || overlap[0] <= 0) {
        this.length = 0;
        this.cpos = -1;
        this.tpos = -1;
        this.proper = false;
        this.combined = null;
      }
      else {
        this.length = overlap[0];
        this.cpos = overlap[1];

        this.tpos = tgram.indexOf(intersection.toString());
        this.proper = (tpos == 0 && ((cpos + length) == cgram.length()));

        if (proper) {
          final StringBuilder combinedBuilder = new StringBuilder();
          if (cpos > 0) combinedBuilder.append(cgram.substring(0, cpos));
          combinedBuilder.append(tgram);
          this.combined = combinedBuilder.toString();
        }
        else {
          this.combined = null;
        }
      }

      this.overlap = intersection.toString();
    }

    /**
     * Compare such that we sort from most to least overlap.
     */
    public int compareTo(OverlapInfo other) {
      return other.length - this.length;
    }

    /**
     * Get the combined ngram, which is only non-null when index &lt; 0
     * and proper is true.
     */
    String getCombined() {
      return combined;
    }
  }
}
