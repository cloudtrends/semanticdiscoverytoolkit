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


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Base implementation of an FsmBuilder.
 * <p>
 * @author Spence Koehler
 */
public abstract class BaseFsmBuilder <T> implements FsmBuilder <T> {
  
  /**
   * Get all of the sequences for collapsing.
   */
  protected abstract List<FsmSequence<T>> getSequences();

  /**
   * Do the work of adding a token.
   */
  protected abstract void doAdd(T token);


  private List<FsmSequence<T>> _uniqueCollapsedSequences;
  private List<FsmSequence<T>> _allCollapsedSequences;
  private FsmSequence<T> emptyMarker;
  private String keyDelim;

  protected BaseFsmBuilder() {
    this(null);
  }

  protected BaseFsmBuilder(String keyDelim) {
    this._allCollapsedSequences = null;
    this._uniqueCollapsedSequences = null;
    this.emptyMarker = new FsmSequence<T>((List<T>)null, null);
    this.keyDelim = keyDelim;
  }

  public final void add(T token) {
    this._allCollapsedSequences = null;
    this._uniqueCollapsedSequences = null;

    doAdd(token);
  }

  /**
   * Get the (collapsed) sequences.
   */
  public final List<FsmSequence<T>> getSequences(boolean keepAll) {
    List<FsmSequence<T>> result = null;

    if (keepAll) {
      if (_allCollapsedSequences == null) {
        _allCollapsedSequences = collapse(true);
      }
      result = _allCollapsedSequences;
    }
    else {
      if (_uniqueCollapsedSequences == null) {
        _uniqueCollapsedSequences = collapse(false);
      }
      result = _uniqueCollapsedSequences;
    }

    return result;
  }

  /**
   * Get a matcher for this builder's (current) sequences.
   * <p>
   * NOTE: A new instance of a matcher will be built on each invocation and
   * the returned matcher will always be based on the current state of this
   * builder.
   */
  public FsmMatcher<T> getMatcher(FsmMatcher.Mode mode) {
    return new FsmMatcher<T>(mode, getSequences(false));
  }

  /**
   * Get the "empty" marker for this instance.
   */
  protected final FsmSequence<T> getEmptyMarker() {
    return emptyMarker;
  }

  /**
   * Collapse the sequences.
   */
  protected List<FsmSequence<T>> collapse(boolean keepAll) {
    final List<FsmSequence<T>> result = new LinkedList<FsmSequence<T>>();

    FsmSequence<T> lastSequence = null;
    FsmSequence<T> finalSequence = null;
    int sequenceCount = 1;
    final Map<String, FsmSequence<T>> key2sequence = new HashMap<String, FsmSequence<T>>();

    // Find unique sequences and count repeats
    for (FsmSequence<T> sequence : getSequences()) {
      final String key = sequence.getKey();
      finalSequence = key2sequence.get(key);
      if (finalSequence == null) {
        finalSequence = new FsmSequence<T>(sequence);
        key2sequence.put(key, finalSequence);
        if (!keepAll) result.add(finalSequence);
      }

      // if this isn't the same as the last sequence, it needs to be added
      if (finalSequence != lastSequence) {
        if (lastSequence != null) {
          lastSequence.setRepeat(sequenceCount);
        }

        if (keepAll) result.add(finalSequence);
        sequenceCount = 1;
      }
      else {
        ++sequenceCount;
      }

      lastSequence = finalSequence;
    }
    if (lastSequence != null) {
      lastSequence.setRepeat(sequenceCount);
    }

    return result;
  }

  protected final FsmSequence<T> buildFsmSequence() {
    return new FsmSequence<T>(keyDelim);
  }
}
