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


import org.sd.fsm.Rule;
import org.sd.fsm.State;
import org.sd.fsm.Token;

import java.util.LinkedList;
import java.util.List;

/**
 * Utilities for working with states.
 * <p>
 * @author Spence Koehler
 */
public class StateUtil {
  
  public static final State getTop(State state) {
    State parent = state.getParentState();
    while (parent != null) {
      state = parent;
      parent = state.getParentState();
    }
    return state;
  }

  public static final List<State> getStateChain(State state) {
    final LinkedList<State> result = new LinkedList<State>();
    for (State curState = state; curState != null; curState = curState.getPrevState()) {
      result.addFirst(curState);
    }
    return result;
  }

  /**
   * Determine whether a later state subsumes a previous state. If true, this
   * means that the previous state lead to more states, which became the later
   * state.
   */
  public static final boolean subsumes(State laterState, State prevState) {
    // searching for prevState in laterState's history.
    if (prevState == null) return false;

    final State matchingPosState = subsumesAux(laterState, prevState);
    if (matchingPosState == null) return false;

    return true;
  }

  private static final State subsumesAux(State laterState, State prevState) {
    State matchingRuleState = searchForMatchingRule(laterState, prevState, true);
    while (matchingRuleState != null) {
      final State matchingPosState = searchForMatchingPos(matchingRuleState, prevState);
      if (matchingPosState != null) return matchingPosState;
      matchingRuleState = searchForMatchingRule(matchingRuleState, prevState, false);
    }

    return null;
  }

  /**
   * Assuming the rule matches, spin back through previous states looking for
   * token and chain posiition matches.
   */
  private static final State searchForMatchingPos(State laterState, State prevState) {
    State result = null;

    final int tokenPos = prevState.getTokenPos();
    final int chainPos = prevState.getChainPos();

    while (laterState != null) {
      final int laterTokenPos = laterState.getTokenPos();
      final int laterChainPos = laterState.getChainPos();

      if (laterTokenPos == tokenPos && laterChainPos == chainPos) {
        if (verifyTokensMatch(laterState, prevState)) {
          result = laterState;
        }
        break;
      }
      else if (laterTokenPos < tokenPos || laterChainPos < chainPos) {
        // we're beyond where we could match.
        break;  // exit, no match.
      }
      laterState = laterState.getPrevState();
    }

    return result;
  }

  /**
   * Assuming that the rule, tokenPos, and chainPos match, verify the tokens
   * match in all of the deep chains.
   */
  private static final boolean verifyTokensMatch(State laterState, State prevState) {
    while (laterState != null && prevState != null) {
      if (!verifyLocalTokensMatch(laterState, prevState)) return false;

      laterState = incrementForTokenSearch(laterState);
      prevState = incrementForTokenSearch(prevState);
    }
    return true;
  }

  /**
   * Assuming that the rule, tokenPos, and chainPos match, verify the tokens
   * match in the chain.
   */
  private static final boolean verifyLocalTokensMatch(State laterState, State prevState) {
    while (laterState != null && prevState != null) {
      final Token prevToken = prevState.getMatchedGrammarToken();
      final Token laterToken = laterState.getMatchedGrammarToken();

      if (prevToken != laterToken) return false;

      laterState = laterState.getPrevState();
      prevState = prevState.getPrevState();
    }

    return true;
  }

  /**
   * Find a rule in laterState's history that matches that of prevState.
   */
  private static final State searchForMatchingRule(State laterState, State prevState, boolean includeSelf) {
    final Rule targetRule = prevState.getRule();

    if (!includeSelf) {
      laterState = incrementForRuleSearch(laterState, targetRule);
    }

    while (laterState != null) {
      if (laterState.getRule() == targetRule) break;
      laterState = incrementForRuleSearch(laterState, targetRule);
    }

    return laterState;
  }

  /**
   * Increment the state for searching through its history for a rule.
   */
  private static State incrementForRuleSearch(State state, Rule targetRule) {
    // go up to the first parent that has a prevState and get that prevState's (deepest) last child.
    state = state.getParentState();

    while (state != null) {
      if (state.getRule() == targetRule) return state;
      if (state.getPrevState() != null) {
        state = state.getPrevState();
        while (state.getLastChildState() != null) {
          state = state.getLastChildState();
          if (state.getRule() == targetRule) return state;
        }
        break;
      }
      else {
        state = state.getParentState();
      }
    }

    return state;
  }

  private static State incrementForTokenSearch(State state) {
    if (state.getPrevState() != null) {
      state = state.getPrevState();

      if (state != null) {  // dive
        while (state.getLastChildState() != null) state = state.getLastChildState();
      }
    }
    else {  // climb
      state = state.getParentState();
    }

    return state;
  }
}
