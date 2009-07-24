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
import org.sd.fsm.StateDecoder;
import org.sd.fsm.Token;
import org.sd.util.tree.Tree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * <p>
 * @author Spence Koehler
 */
class StateImpl implements State {
  
  private GrammarImpl grammar;  // grammar
  private RuleImpl rule;        // current rule
  private AbstractToken token;  // inputToken for transition from prevState to this
  private StateImpl prevState;  // prevState
  private int curPos;           // current RHS position to match for transition
  private StateImpl parent;     // parent
  private StateImpl lastChild;  // lastChild

  /**
   * Constructor for an initial state. Used by GrammarImpl.
   */
  StateImpl(GrammarImpl grammar) {
    this.grammar = grammar;
    this.rule = null;
    this.token = null;
    this.prevState = null;
    this.curPos = 0;
    this.parent = null;
    this.lastChild = null;
  }

  /**
   * Copy constructor.
   */
  private StateImpl(StateImpl other, boolean keepLastChild) {
    this.grammar = other.grammar;
    this.rule = other.rule;
    this.token = other.token;
    this.prevState = other.prevState;
    this.curPos = other.curPos;
    this.parent = other.parent;
    this.lastChild = keepLastChild ? other.lastChild : null;
    if (keepLastChild && lastChild != null) lastChild.parent = this;
  }

  /**
   * Constructor for a push state.
   */
  private StateImpl(GrammarImpl grammar, GrammarImpl.RulePos rulePos, StateImpl parent, AbstractToken inputToken) {
    this.grammar = grammar;
    this.rule = rulePos.rule;
    this.token = inputToken;
    this.prevState = null;
    this.curPos = rulePos.pos + 1;  // +1 because we increment when we push, not when we pop.
    this.parent = parent;
    this.lastChild = null;
    if (parent != null) parent.lastChild = this;
  }

  /**
   * Duplicate this state and all of its parents.
   */
  private StateImpl duplicateUp(boolean keepLastChild) {
    StateImpl result = new StateImpl(this, keepLastChild);
    if (parent != null) result.setParent(parent.duplicateUp(keepLastChild));
    return result;
  }

  /**
   * Create a state to follow this state safely such that other states which
   * may be generated to follow this state will not interfere.
   */
  private StateImpl createNextState(AbstractToken inputToken, int newPosition) {
    StateImpl result = this.duplicateUp(false);
    result.token = inputToken;
    result.curPos = newPosition;
    result.prevState = this;
    return result;
  }

  /**
   * Get the next FSM state(s) reached by transitioning over the given token.
   *
   * @param t  An input token over which to transition states.
   *
   * @return the next FSM state(s) or null if the token is invalid from this state.
   */
  public List<State> getNextStates(Token t) {
    return getNextStates(t, false);
  }

  private final List<State> getNextStates(Token t, boolean recursing) {
    List<State> result = null;
    final AbstractToken token = (AbstractToken)t;

    if (rule == null) {
      // at first state, need to find applicable rules through the grammar.
      final List<State> pushes = collectDeepPushStates(token);
      if (pushes != null) {
        result = addStates(result, pushes);
      }
    }
    else {
      // find whether rule applies at the current position.
      final List<Integer> nextPos = computeNextPos(token);

      if (nextPos != null) {
        result = new ArrayList<State>();
        for (int pos : nextPos) {
          result = addState(result, createNextState(token, pos));
        }
      }

      // check for repeat pushes (rule substitutions at "special" token)
      final GrammarToken specialToken = specialSubstitutionToken();
      boolean didPush = false;
      if (specialToken != null && specialToken != GrammarToken.OPTIONAL) {
        final GrammarToken filter = getPrevGrammarToken();
        final List<StateImpl> nextSubStates = nextPushStates(token, filter);
        if (nextSubStates != null) {
          result = addStates(result, nextSubStates);
          didPush = true;
        }
      }

      // check for pops
      if (atEndOfRule()) {
        final List<State> nextPopStates = nextPopStates(token, recursing);
        if (nextPopStates != null) {
          result = addStates(result, nextPopStates);
        }
      }

      // check for new pushes, adding states for plausible substitutions (child states)
      if (!didPush) {
        final GrammarToken curToken = getGrammarToken();
        if (curToken != null) {
          StateImpl stateToPushFrom = this;
          if (curToken.isSpecial()) {
            stateToPushFrom = null;
            final int nextTokenPos = rule.getNextTokenPosition(curPos);
            if (nextTokenPos >= 0) {
              stateToPushFrom = createNextState(null, nextTokenPos);
            }
          }
          if (stateToPushFrom != null) {
            final List<StateImpl> pushStates = stateToPushFrom.nextPushStates(token, null);
            if (pushStates != null) {
              result = addStates(result, pushStates);
            }
          }
        }
      }
    }

    return result;
  }

  private final List<State> addState(List<State> result, State state) {
    if (result == null) result = new ArrayList<State>();
    result.add(state);
    return result;
  }

  private final List<State> addStates(List<State> result, List<? extends State> moreStates) {
    if (result == null) result = new ArrayList<State>();
    result.addAll(moreStates);
    return result;
  }

  public boolean equals(Object o) {
    boolean result = (this == o);
    if (!result && o instanceof StateImpl) {
      final StateImpl other = (StateImpl)o;
      result =
        rule == other.rule &&
        curPos == other.curPos &&
        token == other.token &&
        chainPos() == other.chainPos() &&
        ((parent != null) == (other.parent != null)) &&
        ((lastChild != null) == (other.lastChild != null)) &&
        depth() == other.depth();
    }
    return result;
  }

  public int hashCode() {
    int result = 7;

    result = result * 31 + rule.hashCode();
    result = result * 31 + curPos;
    result = result * 31 + token.hashCode();
    result = result * 31 + chainPos();
    if (parent != null) result = result * 31 + 1;
    if (lastChild != null) result = result * 31 + 1;
    result = result * 31 + depth();

    return result;
  }

  private final GrammarToken specialSubstitutionToken() {
    GrammarToken result = getGrammarToken();

    if (result == null || !result.isSpecial() || result == GrammarToken.END) {
      result = null;
    }
    // else we're at a repeating substitution token.

    return result;
  }

  private final GrammarToken getGrammarToken() {
    return rule == null ? null : rule.getRHS(curPos);
  }

  private final GrammarToken getPrevGrammarToken() {
    return rule == null ? null : rule.getRHS(rule.getPrevTokenPosition(curPos));
  }

  private final void setParent(StateImpl parent) {
    this.parent = parent;
    if (parent != null) parent.lastChild = this;
  }

  private final int getPositionIncrement(int pos) {
    int result = pos;
    if (rule != null && pos >= 0) result = rule.getRHS(pos).isSpecial() ? pos : pos + 1;
    return result;
  }

  private final List<State> nextPopStates(AbstractToken token, boolean recursing) {
    if (parent == null) return null;

    // need to duplicate the parent chain so that alternate paths don't interfere.
    final StateImpl thisDup = recursing ? this : this.duplicateUp(true);
    // note: don't hafta increment parent's curPos; we did that when we pushed.
    return thisDup.parent.getNextStates(token, true);
  }

  private final List<StateImpl> nextPushStates(AbstractToken inputToken, GrammarToken filter) {
    List<StateImpl> result = null;
    int nextPos = curPos;
    int nextPosInc = getPositionIncrement(nextPos);  // move forward when push
    GrammarToken curToken = (filter == null) ? (GrammarToken)getActiveGrammarToken() : filter;  // expected lhs token of rule to push

    while (curToken != null) {
      final List<StateImpl> pushes = collectPushStates(curToken, inputToken, nextPosInc);
      if (pushes != null) {
        if (result == null) result = new ArrayList<StateImpl>();
        result.addAll(pushes);
      }
      if (filter != null) break;
      nextPos = spinToNextPosition(nextPos);       // spin to next token
      nextPosInc = getPositionIncrement(nextPos);  // move forward for push
      curToken = rule.getRHS(nextPos);
    }

    return result;
  }

  private final List<StateImpl> collectPushStates(GrammarToken stopAt, AbstractToken inputToken, int nextPos) {
    final List<GrammarImpl.RulePath> rulePaths = grammar.findRules(stopAt, inputToken);
    return rulePaths2States(rulePaths, inputToken, nextPos);
  }

  private final List<StateImpl> rulePaths2States(List<GrammarImpl.RulePath> rulePaths, AbstractToken inputToken, int nextPos) {
    List<StateImpl> result = null;

    if (rulePaths != null) {
      result = new ArrayList<StateImpl>();
      for (GrammarImpl.RulePath rulePath : rulePaths) {
        final StateImpl nextState = createNextState(null, nextPos);
        result.add(rulePath2State(rulePath.iterator(), inputToken, nextState));
      }
    }

    return result;
  }

  private final StateImpl rulePath2State(Iterator<GrammarImpl.RulePos> iter, AbstractToken inputToken, StateImpl parentToPush) {
    StateImpl state = parentToPush;
    while (iter.hasNext()) {
      final GrammarImpl.RulePos rulePos = iter.next();

      if (iter.hasNext()) {  // do this for each rule except last
        state = new StateImpl(grammar, rulePos, state, null);
      }
      else {  // this is the last rule
        state = new StateImpl(grammar, rulePos, state, inputToken);
      }
    }
    return state;
  }

  private final List<State> collectDeepPushStates(AbstractToken inputToken) {
    List<State> result = null;
    final List<GrammarImpl.RulePath> rulePaths = grammar.findRules(null, inputToken);

    if (rulePaths != null) {
      result = new ArrayList<State>();

      for (GrammarImpl.RulePath rulePath : rulePaths) {
        final int num = rulePath.size();
        for (int i = num - 1; i >= 0; --i) {
          result.add(rulePath2State(rulePath.iterator(i), inputToken, null));
        }
      }
    }

    return result;
  }

  // get the current active non-special grammar token at the given position.
  private final Token getActiveGrammarToken() {
    GrammarToken result = getGrammarToken();
    if (result != null && result.isSpecial()) {
      result = getPrevGrammarToken();
    }
    return result;
  }

  private List<Integer> computeNextPos(AbstractToken token) {
    List<Integer> result = null;
    final GrammarToken curToken = getGrammarToken();

    if (curToken != null) {
      if (curToken.isSpecial()) {
        // we've matched the previous token and are sitting at a special
        result = computeNextPosFromSpecial(token, curToken);
      }
      else {
        // not at a special token
        if (curToken.matches(token)) {
          // we're at a normal token that we've matched
          result = new ArrayList<Integer>();
          result.add(curPos + 1);
        }

        // test forward matches, skipping all optionals, zero or mores, and ends
        int nextTokenPos = spinToNextPosition(curPos);
        GrammarToken nextToken = rule.getRHS(nextTokenPos);
        while (nextToken != null) {
          if (matches(token, nextToken)) {
            if (result == null) result = new ArrayList<Integer>();
            result.add(nextTokenPos + 1);
          }
        
          // increment for next go around
          nextTokenPos = spinToNextPosition(nextTokenPos);
          nextToken = rule.getRHS(nextTokenPos);
        }
      }
    }

    return result;
  }

  /**
   * Within the current rule and according to the special token, find the next
   * plausible positions (greedy and reluctant) from the given the input.
   */
  private List<Integer> computeNextPosFromSpecial(AbstractToken token, GrammarToken specialToken) {
    List<Integer> result = null;

    int nextTokenPos = rule.getNextTokenPosition(curPos);
    GrammarToken nextToken = rule.getRHS(nextTokenPos);

    if (specialToken == GrammarToken.END) {
      // we're at an "end" special token, but have more input. see if we match the next token.
      if (matches(token, nextToken)) {
        result = new ArrayList<Integer>();
        result.add(nextTokenPos + 1);
      }
    }
    else {
      //note: if here, we can assume that prevStateMatched == true

      // test repeat match (backward) where we can match more than once
      if (specialToken != GrammarToken.OPTIONAL) { // and it's not END if we're here
        final GrammarToken prevToken = getPrevGrammarToken();
        final boolean matchesPrevToken = matches(token, prevToken);
        if (matchesPrevToken) {
          if (result == null) result = new ArrayList<Integer>();
          result.add(curPos);
        }
      }

      // test forward matches, skipping all optionals, zero or mores, and ends
      while (nextToken != null) {
        if (matches(token, nextToken)) {
          if (result == null) result = new ArrayList<Integer>();
          result.add(nextTokenPos + 1);
        }
        
        // increment for next go around
        nextTokenPos = spinToNextPosition(nextTokenPos);
        nextToken = rule.getRHS(nextTokenPos);
      }
    }

    return result;
  }

  /**
   * find index of next comparable grammar token, skipping optional,
   * zero or more, and end tokens from the given non-special pos.
   *
   * @return -1 if there is nothing to spin to
   */
  private final int spinToNextPosition(int pos) {
    int result = -1;

    final GrammarToken nextToken = rule.getRHS(pos + 1);
    if (nextToken != null) {
      if (nextToken == GrammarToken.END) {
        result = spinToNextPosition(pos + 2);
      }
      else {
        if (nextToken == GrammarToken.OPTIONAL || nextToken == GrammarToken.ZERO_OR_MORE) {
          result = rule.getNextTokenPosition(pos + 1);
        }
      }
    }
    return result;
  }

  private final boolean matches(AbstractToken inputToken, GrammarToken grammarToken) {
    return grammarToken != null && grammarToken.matches(inputToken);
  }

  /**
   * Get the FSM state that lead to this state.
   *
   * @return the previous FSM state or null if this is the first state.
   */
  public State getPrevState() {
    return prevState;
  }

  /**
   * Get the FSM state that is a parent to this state.
   */
  public State getParentState() {
    return parent;
  }

  /**
   * Get the FSM state that is the last child to this state.
   */
  public State getLastChildState() {
    return lastChild;
  }

  /**
   * Get the input token that lead to this state.
   *
   * @return the input token or null if this is the first state.
   */
  public Token getInputToken() {
    return token;
  }

  public Token getMatchedGrammarToken() {
    return getPrevGrammarToken();
  }

  /**
   * Query whether this state is (or can be) terminal.
   * <p>
   * Note that being terminal does not prevent a state from having a next state;
   * only, this state represents a viable exit if warranted by the input.
   */
  public boolean isTerminal() {
    boolean result = atEndOfRule();

    if (result) {
      if (parent != null) {
        // then we need to be at the end of the rule and at the end of each parent's rule
        result = parent.isTerminal();
      }
    }

    return result;
  }

  private final boolean atEndOfRule() {
    if (rule == null) return false;

    GrammarToken gtoken = getGrammarToken();
    if (gtoken == null) return true;

    return rule.isTerminal(curPos);
  }

  /**
   * Get this state's rule.
   */
  public Rule getRule() {
    return rule;
  }

  /**
   * Build the tree that leads to this state using the default state decoder.
   */
  public Tree<Token> buildTree() {
    return grammar.getStateDecoder().getTree(this);
  }

  /**
   * Build the tree that leads to this state using the given state decoder.
   */
  public Tree<Token> buildTree(StateDecoder stateDecoder) {
    return stateDecoder.getTree(this);
  }

  private final int chainPos() {
    int result = 0;
    StateImpl chainState = this;

    while (chainState.prevState != null) {
      ++result;
      chainState = chainState.prevState;
    }

    return result;
  }

  private final int depth() {
    int result = 0;
    StateImpl chainState = this;

    while (chainState.parent != null) {
      ++result;
      chainState = chainState.parent;
    }

    return result;
  }

  /**
   * Get the position of the token being considered at this state.
   */
  public int getTokenPos() {
    return curPos;
  }

  /**
   * Get the position of this state's rule being matched against the current
   * token at this state.
   */
  public int getChainPos() {
    return chainPos();
  }

  /**
   * Get the depth of this state.
   */
  public int getDepth() {
    return depth();
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append("State[").
      append(rule).
      append(",tokenPos=").append(curPos).
      append(",token=").append(token).
      append(",chainPos=").append(chainPos()).
      append(",hasParent=").append(parent != null).
      append(",hasChildren=").append(lastChild != null).
      append(",depth=").append(depth()).
      append("]");

    return result.toString();
  }
}
