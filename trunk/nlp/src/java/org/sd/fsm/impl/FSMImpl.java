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
package org.sd.fsm.impl;


import org.sd.fsm.FSM;
import org.sd.fsm.Grammar;
import org.sd.fsm.State;
import org.sd.fsm.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * Default FSM implementation.
 * <p>
 * @author Spence Koehler
 */
public class FSMImpl implements FSM {

  private Grammar grammar;

  public FSMImpl(Grammar grammar) {
    this.grammar = grammar;
  }

  /**
   * Accept the token from an initial state.
   *
   * @param token  The next token for the machine to accept.
   *
   * @return valid transition states over the token, or null.
   */
  public List<State> accept(Token token) {
    return accept(token, grammar.getFirstState());
  }

  /**
   * Accept the token from the given state.
   *
   * @param token  The next token for the machine to accept.
   * @param state  The state from which to transition over the token.
   *
   * @return valid transition states over the token, or null.
   */
  public List<State> accept(Token token, State state) {
    return state.getNextStates(token);
  }

  /**
   * Accept the token from each of the given states.
   *
   * @param token  The next token for the machine to accept.
   * @param states The states from which to transition over the token.
   *
   * @return valid transition states over the token, or null.
   */
  public List<State> accept(Token token, List<State> states) {
    List<State> result = null;

    for (State state : states) {
      final List<State> curResult = state.getNextStates(token);
      if (curResult != null) {
        if (result == null) {
          result = new ArrayList<State>();
        }
        result.addAll(curResult);
      }
    }

    return result;
  }
}
