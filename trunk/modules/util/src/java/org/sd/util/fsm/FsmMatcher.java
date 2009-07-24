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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility to match input against fsm sequences.
 * <p>
 * @author Spence Koehler
 */
public class FsmMatcher <T> {

  public enum Mode {SHORTEST, LONGEST};


  private Mode mode;
  private List<FsmSequence<T>> sequences;
  private FsmState<T> curState;
  private Map<T, List<FsmSequence<T>>> token2seqs;  // map first token to its sequences

  /**
   * Construct with the given sequences, typically accessed using
   * FsmBuilder.getMatcher().
   */
  FsmMatcher(Mode mode, List<FsmSequence<T>> sequences) {
    this.mode = mode;
    this.sequences = sequences;
    this.curState = null;  // at beginning before first add.

    this.token2seqs = new HashMap<T, List<FsmSequence<T>>>();
    for (FsmSequence<T> sequence : sequences) {
      final T firstToken = sequence.get(0);
      List<FsmSequence<T>> seqs = token2seqs.get(firstToken);
      if (seqs == null) {
        seqs = new ArrayList<FsmSequence<T>>();
        token2seqs.put(firstToken, seqs);
      }
      seqs.add(sequence);
    }
  }

  /**
   * Get the state achieved when adding the token to this matcher.
   * <p>
   * @return the state transitioned to by adding the token or null if the
   *         token is unrecognized.
   */
  public FsmState<T> add(T token) {
    FsmState<T> result = null;

    if (curState == null) {
      curState = getStartState(token);
      result = curState;
    }
    else {
      result = curState;

      // transition to the next state (if possible)
      if (!curState.transition(token)) {
        curState = getStartState(token);
        result = curState;

        if (curState != null) {
          if (mode == Mode.SHORTEST && curState.getNumCompleted() > 0) {
            curState = null;  // next time, start a new state
          }
        }
        // else, longest will reset itself.
      }
    }
    return result;
  }

  private final FsmState<T> getStartState(T token) {
    FsmState<T> result = null;

    final List<FsmSequence<T>> seqs = token2seqs.get(token);
    if (seqs != null) {
      result = new FsmState<T>(token, seqs);
    }

    return result;
  }
}
