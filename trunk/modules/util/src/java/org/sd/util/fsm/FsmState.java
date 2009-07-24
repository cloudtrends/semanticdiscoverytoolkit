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
package org.sd.util.fsm;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Representation of the current machine state while matching to tokens.
 * <p>
 * @author Spence Koehler
 */
public class FsmState <T> {

  private List<FsmSequence<T>> seqs;  // matching sequences
  private int curPos;
  private List<T> tokens;             // accepted tokens
  private int totalSeqs;
  private int numCompleted;
  private int minSize;
  private int maxSize;
  private FsmSequence<T> completedSequence;

  public FsmState(T firstToken, List<FsmSequence<T>> seqs) {
    this.seqs = new LinkedList<FsmSequence<T>>(seqs);  // must copy
    this.curPos = 0;
    this.tokens = new ArrayList<T>();
    this.tokens.add(firstToken);
    this.totalSeqs = seqs.size();
    this.numCompleted = 0;

    this.minSize = 0;
    this.maxSize = 0;
    for (FsmSequence<T> seq : seqs) {
      final int size = seq.size();
      if (minSize == 0 || size < minSize) minSize = size;
      if (size > maxSize) maxSize = size;
    }
  }

  /**
   * Transition this state to the next over the given token.
   *
   * @return true if successfully transitioned; otherwise, false.
   */
  public boolean transition(T token) {
    boolean result = false;

    if (isAtTheEnd()) return result;

    // find which sequences can transition with the token; remove others.
    for (FsmSequence<T> seq : seqs) {
      if (transition(seq, token)) {
        result = true;
      }
    }

    if (result) {
      tokens.add(token);
      ++curPos;
    }

    return result;
  }

  /**
   * Transition the given sequence over the token.
   *
   * @return true if successfully transitions; otherwise, false.
   */
  private final boolean transition(FsmSequence<T> seq, T token) {
    boolean result = false;

    final int nextPos = curPos + 1;
    final int size = seq.size();
    if (size > nextPos) {
      final T nextToken = seq.get(nextPos);
      result = (nextToken.equals(token));
    }

    if (result && size == nextPos + 1) {
      ++numCompleted;
      completedSequence = seq;
    }

    return result;
  }

  /**
   * Determine whether this state is at its start.
   * <p>
   * Note that this could also be at an end if there is a sequence
   * with only one token.
   */
  public boolean isAtStart() {
    return curPos == 0;
  }

  /**
   * Determine whether this state is at an end.
   */
  public boolean isAtAnEnd() {
    return numCompleted > 0;
  }

  /**
   * Determine whether this state is at the end of all possible sequences.
   */
  public boolean isAtTheEnd() {
    return curPos >= maxSize;
  }

  /**
   * Get the accepted tokens.
   */
  public List<T> getTokens() {
    return tokens;
  }

  /**
   * Get the number of accepted tokens.
   */
  public int getNumTokens() {
    return tokens.size();
  }

  /**
   * Get the current token pos.
   */
  public final int getCurPos() {
    return curPos;
  }

  /**
   * Get the minimum number of tokens for any sequence.
   */
  public final int getMinSize() {
    return minSize;
  }

  /**
   * Get the maximum number of tokens for any sequence.
   */
  public final int getMaxSize() {
    return maxSize;
  }

  /**
   * Get the total number of potential sequences.
   */
  public final int getNumSequences() {
    return totalSeqs;
  }

  /**
   * Get the number of completed sequences.
   */
  public final int getNumCompleted() {
    return numCompleted;
  }

  /**
   * Get the last completed sequence.
   */
  public FsmSequence<T> getCompletedSequence() {
    return completedSequence;
  }
}
