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
package org.sd.fsm;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A validator using an fsm for token sequences.
 * <p>
 * NOTE: This class is not thread safe, but can be reused. Only one instance
 *       should be used per thread.
 *
 * @author Spence Koehler
 */
public class Validator {

  private FSM fsm;

  /**
   * Construct a validator with the given FSM.
   */
  public Validator(FSM fsm) {
    this.fsm = fsm;
  }

  /**
   * Reset the fsm and get the states that prove the sequence valid.
   *
   * @param sequence  The sequence of tokens.
   * @param strict    If false, invalid tokens will be skipped and a sequence
   *                  that ends as expected will still be valid.
   *
   * @return the states or null if invalid.
   */
  public synchronized List<State> validate(Iterator<Token> sequence, boolean strict) {
    List<State> result = null;

    while (sequence.hasNext()) {
      final Token token = sequence.next();
      final List<State> backup = result;

      if (result == null) {
        result = fsm.accept(token);
      }
      else {
        result = fsm.accept(token, result);
      }

      if (result == null && !strict) result = backup;
      if (result == null) break;
    }
    
    return getFinalStates(result);
  }
  
  private final List<State> getFinalStates(List<State> states) {
    List<State> result = null;
    if (states != null) {
      for (State state: states) {
        if (state.isTerminal()) {
          if (result == null) result = new ArrayList<State>();
          result.add(state);
        }
      }
    }
    return result;
  }
}
