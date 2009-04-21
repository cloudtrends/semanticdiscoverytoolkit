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
package org.sd.text.align;


import java.util.LinkedList;
import java.util.Map;

/**
 * Abstract alignment class.
 * <p>
 * Note that alignment is based on the "equals" and "hashCode" methods
 * of T.
 *
 * @author Spence Koehler
 */
public abstract class AbstractAligner <T> {

  protected abstract void markSkipped(T e2, int pos2);
  protected abstract void markMissed(T e1, int pos1);
  protected abstract int getMissedPenalty(T e1, int pos1);
  protected abstract int getSkippedPenalty(T e2, int pos2);


  private T[] seq1;
  private T[] seq2;

  private int numAligned;
  private int numSkipped;
  private int numMissing;
  private int numReclaimed;
  private int skippedPenalty;
  private int missedPenalty;

  public AbstractAligner(T[] seq1, T[] seq2) {
    // swap so seq1 is the shorter sequence
    if (seq2.length < seq1.length) {
      this.seq1 = seq2;
      this.seq2 = seq1;
    }
    else {
      this.seq1 = seq1;
      this.seq2 = seq2;
    }

    this.numAligned = 0;
    this.numSkipped = 0;
    this.numMissing = 0;
    this.numReclaimed = 0;
    this.skippedPenalty = 0;
    this.missedPenalty = 0;

    align();
  }

  public int getMatchScore() {
    return seq1.length - numAligned - missedPenalty;
  }

  public int getUnmatchedCount() {
    return seq1.length - numAligned - numReclaimed;
  }

  public int getExtraCount() {
    return seq2.length - numAligned - numReclaimed;
  }

  public boolean isExactMatch() {
    return seq1.length == seq2.length && numAligned == seq1.length;
  }

  public boolean isFuzzyMatch() {
    return getUnmatchedCount() == 0;
  }

  private final void align() {
    int pos2 = 0;
    for (int pos1 = 0; pos1 < seq1.length; ++pos1) {
      final T e1 = seq1[pos1];
      final int npos2 = findIndexOf(seq2, e1, pos2);
      if (npos2 >= pos2) {  // found a matching element
        ++numAligned;

        // add a penalty for skipped element(s)
        if (npos2 > pos2) {  // skipped some
          for (int i = pos2; i <= npos2; ++i) {
            final T e2 = seq2[i];
            addSkipped(e2, i, pos1);
          }
        }

        // set for next go-around
        pos2 = npos2 + 1;
      }
      else {  // missing the current (seq1) element (from seq2)
        // add a penalty for missing element
        // check for possibility of "reclaimed" (skipped) element and factor in its distance
        addMissed(e1, pos1, pos2);
      }
    }
  }

  private final int findIndexOf(T[] seq, T elt, int startPos) {
    int result = -1;

    for (int i = startPos; i < seq.length; ++i) {
      if (elt.equals(seq[i])) {
        result = i;
        break;
      }
    }

    return result;
  }

  private final void addSkipped(T e2, int pos2, int pos1) {
    // check for match against a missed element
    final int missedPenalty = getMissedPenalty(e2, pos1);
    if (missedPenalty > 0) {
      --numMissing;
      this.missedPenalty += missedPenalty;
      ++numReclaimed;
    }
    else {
      markSkipped(e2, pos2);
      ++numSkipped;
    }
  }

  private final void addMissed(T e1, int pos1, int pos2) {
    // check for match against a missed char
    final int skippedPenalty = getSkippedPenalty(e1, pos2);
    if (skippedPenalty > 0) {
      --numSkipped;
      this.skippedPenalty += skippedPenalty;
      ++numReclaimed;
    }
    else {
      markMissed(e1, pos1);
      ++numMissing;
    }
  }

  protected final void mark(Map<T, LinkedList<Integer>> elt2poss, T e, int pos) {
    LinkedList<Integer> poss = elt2poss.get(e);
    if (poss == null) {
      poss = new LinkedList<Integer>();
      elt2poss.put(e, poss);
    }
    poss.addFirst(pos);
  }
}
