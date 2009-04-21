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


import org.sd.util.tree.Tree;

import java.util.List;

/**
 * 
 * <p>
 * @author Spence Koehler
 */
public interface State {
  
  /**
   * Get the next FSM state(s) reached by transitioning over the given token.
   *
   * @param token An input token over which to transition states.
   *
   * @return the next FSM state(s) or null if the token is invalid from this state.
   */
  public List<State> getNextStates(Token token);

  /**
   * Get the FSM state that lead to this state.
   *
   * @return the previous FSM state or null if this is the first state.
   */
  public State getPrevState();

  /**
   * Get the FSM state that is a parent to this state.
   */
  public State getParentState();

  /**
   * Get the FSM state that is the last child to this state.
   */
  public State getLastChildState();

  /**
   * Get the input token that lead to this state.
   *
   * @return the input token or null if this is the first state.
   */
  public Token getInputToken();

  /**
   * Get the grammar token that lead to this state (matched the input token or was pushed).
   */
  public Token getMatchedGrammarToken();

  /**
   * Query whether this state is (or can be) terminal.
   * <p>
   * Note that being terminal does not prevent a state from having a next state;
   * only, this state represents a viable exit if warranted by the input.
   */
  public boolean isTerminal();

  /**
   * Get this state's rule(s).
   */
  public Rule getRule();

  /**
   * Build the tree that leads to this state using the default state decoder.
   */
  public Tree<Token> buildTree();

  /**
   * Build the tree that leads to this state using the given state decoder.
   */
  public Tree<Token> buildTree(StateDecoder stateDecoder);

  /**
   * Get the position of the token being considered at this state.
   */
  public int getTokenPos();

  /**
   * Get the position of this state's rule being matched against the current
   * token at this state.
   */
  public int getChainPos();

  /**
   * Get the depth of this state.
   */
  public int getDepth();
}
