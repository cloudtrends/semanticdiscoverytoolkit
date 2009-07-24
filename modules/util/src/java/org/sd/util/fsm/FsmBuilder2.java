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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Utility class to build an FSM grammar from input.
 * <p>
 * @author Spence Koehler
 */
public class FsmBuilder2 <T> extends BaseFsmBuilder <T> {

  private Map<T, Map<T, Integer>> token2nexts;
  private LinkedList<T> inputTokens;

  private FsmSequence<T> emptyMarker;
  private List<FsmSequence<T>> _sequences;
  private List<FsmSequence<T>> _uniqueCollapsedSequences;
  private List<FsmSequence<T>> _allCollapsedSequences;

  public FsmBuilder2() {
    this(null);
  }

  public FsmBuilder2(String keyDelim) {
    super(keyDelim);

    this.token2nexts = new HashMap<T, Map<T, Integer>>();
    this.inputTokens = new LinkedList<T>();
    this._sequences = null;
  }

//
// For each token, count the frequency of occurrences of following tokens.
// Keep in sequences those that occur the most.
//

  protected final void doAdd(T token) {
    this._sequences = null;

    if (inputTokens.size() > 0) {
      final T lastToken = inputTokens.getLast();

      Map<T, Integer> nexts = token2nexts.get(lastToken);
      if (nexts == null) {
        nexts = new HashMap<T, Integer>();
        token2nexts.put(lastToken, nexts);
      }
      final Integer count = nexts.get(token);
      nexts.put(token, (count == null) ? 1 : count + 1);
    }
    
    inputTokens.add(token);
  }

  /**
   * Get all of the sequences for collapsing.
   */
  protected final List<FsmSequence<T>> getSequences() {
    if (_sequences == null) {
      this._sequences = buildSequences();
    }
    return _sequences;
  }

  private final List<FsmSequence<T>> buildSequences() {
    final List<FsmSequence<T>> result = new ArrayList<FsmSequence<T>>();
    final int numTokens = inputTokens.size();
    if (numTokens == 0) return result;

    FsmSequence<T> curSequence = buildFsmSequence();
    curSequence.add(inputTokens.get(0));
    result.add(curSequence);

    for (int i = 1; i < numTokens; ++i) {

      if (i >= 11) {
        final boolean stopHere = true;
      }

      // make sequence boundary at b when freq(a -> b) < freq(b -> c)
      // or at c when freq(a -> b) > freq(b -> c)
      // if only 2 tokens, don't make sequence boundary
      if ((i + 1 < numTokens && getFrequency(i - 1, i) < getFrequency(i, i + 1)) ||
          (i - 2 >= 0 && getFrequency(i - 2, i - 1) > getFrequency(i - 1, i))) {
        curSequence = buildFsmSequence();
        result.add(curSequence);
      }
      curSequence.add(inputTokens.get(i), true);
    }

    return result;
  }

  private final int getFrequency(int a, int b) {
    final Map<T, Integer> amap = token2nexts.get(inputTokens.get(a));
    return amap.get(inputTokens.get(b));
  }
}
