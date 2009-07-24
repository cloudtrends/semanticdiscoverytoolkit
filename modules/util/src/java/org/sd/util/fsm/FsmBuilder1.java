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


import java.util.LinkedList;
import java.util.List;

/**
 * Utility class to build an FSM grammar from input.
 * <p>
 * @author Spence Koehler
 */
public class FsmBuilder1 <T> extends BaseFsmBuilder <T> {

  private final List<FsmSequence<T>> sequences;                // list of sequences
  private FsmSequence<T> curSequence;
  private boolean terminate;

  public FsmBuilder1() {
    this(null);
  }

  public FsmBuilder1(String keyDelim) {
    super(keyDelim);

    this.sequences = new LinkedList<FsmSequence<T>>();
    this.curSequence = null;
    this.terminate = true;
  }

  protected final void doAdd(T token) {

    if (terminate) {
      this.curSequence = buildFsmSequence();
      sequences.add(curSequence);
    }

    if (!curSequence.add(token)) {
      curSequence = buildFsmSequence();
      curSequence.add(token);
      sequences.add(curSequence);
    }

    // split sequences that contain the current sequence
    int numSequences = sequences.size() - 1;  // don't visit curSequence

    this.terminate = false;
    for (int i = 0; i < numSequences; ++i) {
      final FsmSequence<T> sequence = sequences.get(i);
      final FsmSequence<T> insert = sequence.split(curSequence, getEmptyMarker());
      if (insert != null) {
        if (insert != getEmptyMarker()) {
          // insert the sequence after the current
          sequences.add(++i, insert);
          ++numSequences;
        }
        else {  // received "empty" marker
          // terminate the current sequence
          this.terminate = true;
        }
      }
    }
  }

  /**
   * Get all of the sequences for collapsing.
   */
  protected final List<FsmSequence<T>> getSequences() {
    return sequences;
  }
}
