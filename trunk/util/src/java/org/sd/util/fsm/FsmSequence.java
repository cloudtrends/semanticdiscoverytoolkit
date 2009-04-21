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
 *
 * <p>
 * @author Spence Koehler
 */
public class FsmSequence <T> {
  private List<T> tokens;
  private int minRepeat;
  private int maxRepeat;
  private int totalRepeat;
  private String _key;
  private String keyDelim;

  /**
   * Default constructor.
   */
  public FsmSequence() {
    this((String)null);
  }

  /**
   * Construct with the given keyDelim.
   */
  public FsmSequence(String keyDelim) {
    this.tokens = new LinkedList<T>();
    this.minRepeat = 0;
    this.maxRepeat = 0;
    this.totalRepeat = 0;
    this._key = null;
    this.keyDelim = keyDelim;
  }

  /**
   * Copy constructor.
   */
  public FsmSequence(FsmSequence<T> other) {
    this.tokens = new LinkedList<T>(other.tokens);  // copy
    this.minRepeat = 0;
    this.maxRepeat = 0;
    this.totalRepeat = 0;
    this._key = other._key;
    this.keyDelim = other.keyDelim;
  }

  /**
   * Construct with the given tokens.
   */
  public FsmSequence(List<T> tokens, String keyDelim) {
    this.tokens = tokens;
    this.minRepeat = 0;
    this.maxRepeat = 0;
    this.totalRepeat = 0;
    this._key = null;
    this.keyDelim = keyDelim;
  }

  public boolean add(T token) {
    return add(token, false);
  }

  public boolean add(T token, boolean force) {
    boolean result = false;

    if (force || !tokens.contains(token)) {
      tokens.add(token);
      this._key = null;
      result = true;
    }

    return result;
  }

  /**
   * If the sequence exists in this sequence beyond the first position,
   * split this sequence up to, but not including, the first token and
   * return the remainder (from the first token onward.)
   * <p>
   * If the given sequence fully matches this sequence, then return
   * the "empty" marker sequence, indicating that the given sequence should
   * not receive more tokens.
   * <p>
   * Otherwise, return null.
   */
  public FsmSequence<T> split(FsmSequence<T> sequence, FsmSequence<T> emptyMarker) {
    FsmSequence<T> result = null;
    final T firstToken = sequence.get(0);
    final int len = sequence.size();
    final int mylen = tokens.size();
    final int pos = tokens.indexOf(firstToken);

    if (mylen > len) {
      // sequence may be found within this instance
      if (pos > 0 && pos + len <= tokens.size()) {
        // found first token. see that others match
        if (matches(sequence, len, pos)) {
          result = new FsmSequence<T>(tokens.subList(pos, tokens.size()), keyDelim);
          tokens = tokens.subList(0, pos);
          this._key = null;
        }
      }
    }
    else if (mylen == len) {
      // this sequence may match the other, in which case, the other needs
      // to be terminated.
      if (pos == 0 && matches(sequence, len, pos)) {
        // communicate this condition by returning the "empty" sequence
        result = emptyMarker;
      }
    }

    return result;
  }

  /**
   * Given that sequence's (of size=len) firstToken matches this instance's
   * token at index=pos and that this instance is long enough to match the
   * sequence from that position, determine whether it does indeed match.
   */
  private final boolean matches(FsmSequence<T> sequence, int len, int pos) {
    boolean matches = true;

    for (int i = 1; i < len; ++i) {
      final T nextToken = sequence.get(i);
      final T myNextToken = tokens.get(pos + i);
      if (!nextToken.equals(myNextToken)) {
        matches = false;
        break;
      }
    }

    return matches;
  }

  public void setRepeat(int repeat) {
    if (repeat < minRepeat || minRepeat == 0) {
      minRepeat = repeat;
    }
    if (repeat > maxRepeat) {
      maxRepeat = repeat;
    }
    totalRepeat += repeat;
  }

  public int getMinRepeat() {
    return minRepeat;
  }

  public int getMaxRepeat() {
    return maxRepeat;
  }

  public int getTotalRepeat() {
    return totalRepeat;
  }

  public int size() {
    return tokens.size();
  }

  public T get(int index) {
    return tokens.get(index);
  }

  public String getKey() {
    if (_key == null) {
      _key = buildKey();
    }
    return _key;
  }

  public List<T> getTokens() {
    return tokens;
  }

  private final String buildKey() {
    final StringBuilder result = new StringBuilder();
 
    for (T token : tokens) {
      if (result.length() > 0 && keyDelim != null) {
        result.append(keyDelim);
      }
      result.append(token.toString());
    }

    return result.toString();
  }

  public String toString() {
    return getKey();
  }
}
