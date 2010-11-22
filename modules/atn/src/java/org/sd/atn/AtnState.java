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
package org.sd.atn;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.sd.token.CategorizedToken;
import org.sd.token.Token;
import org.sd.token.TokenClassifier;
import org.sd.util.tree.Tree;

/**
 * Container class for a processing state to pair a grammar rule step with
 * an input token and generate successive states.
 * <p>
 * @author Spence Koehler
 */
public class AtnState {
  
  private Token inputToken;
  public Token getInputToken() {
    return inputToken;
  }

  private AtnRule rule;
  public AtnRule getRule() {
    return rule;
  }

  private int stepNum;
  int getStepNum() {
    return stepNum;
  }

  private int repeatNum;
  public int getRepeatNum() {
    return repeatNum;
  }


  private Tree<AtnState> parentStateNode;
  public Tree<AtnState> getParentStateNode() {
    return parentStateNode;
  }

  AtnParseOptions parseOptions;
  int skipNum;


  private boolean matched;
  public boolean getMatched() {
    return matched;
  }
  void setMatched(boolean matched) {
    this.matched = matched;
  }

  private AtnState pushState;
  public AtnState getPushState() {
    return pushState;
  }

  private boolean isPoppedState;
  boolean isPoppedState() {
    return isPoppedState;
  }

  private int popCount;
  int getPopCount() {
    return popCount;
  }

  public boolean isRepeat() {
    return repeatNum > 0;
  }

  private boolean _isSkipped;
  public boolean isSkipped() {
    return _isSkipped;
  }

  boolean isRuleEnd() {
    return (rule != null) ? rule.isTerminal(stepNum) : false;
  }

  private AtnRuleStep _ruleStep;
  public AtnRuleStep getRuleStep() {
    if (_ruleStep == null) {
      _ruleStep = rule.getSteps().get(stepNum);
    }
    return _ruleStep;
  }


  private boolean computedNextToken;
  private Token _nextToken;


  /**
   * Information used for verifying and incrementing a considered state
   * (token with rule step) match.
   */
  AtnState(Token inputToken, AtnRule rule, int stepNum, Tree<AtnState> parentStateNode, AtnParseOptions parseOptions, int repeatNum, int numSkipped, AtnState pushState) {
    this.inputToken = inputToken;
    this.rule = rule;
    this.stepNum = stepNum;
    this.parentStateNode = parentStateNode;
    this.parseOptions = parseOptions;
    this.repeatNum = repeatNum;
    this.skipNum = numSkipped;
    this.matched = false;
    this.pushState = pushState;
    this.popCount = 0;
    this._isSkipped = false;
  }

  /** Copy constructor */
  AtnState(AtnState other) {
    this.inputToken = other.inputToken;
    this.rule = other.rule;
    this.stepNum = other.stepNum;
    this.parentStateNode = other.parentStateNode;
    this.parseOptions = other.parseOptions;
    this.repeatNum = other.repeatNum;
    this.skipNum = other.skipNum;
    this.matched = other.matched;
    this.pushState = other.pushState;
    this.isPoppedState = other.isPoppedState;
    this.popCount = other.popCount;
    this._isSkipped = other._isSkipped;
    this._ruleStep = other._ruleStep;
    this.computedNextToken = other.computedNextToken;
    this._nextToken = other._nextToken;
  }

  /**
   * Determine whether this instance (if verified to match) is a valid end.
   */
  boolean isValidEnd(Set<Integer> stopList) {
    boolean result = false;

    if (matched && isRuleEnd() && isPushEnd()) {
      final Token nextToken = getNextToken(stopList);
      result = (nextToken == null) || !parseOptions.getConsumeAllText();
    }

    return result;
  }

  /**
   * Determine whether all push states would pop to rule ends.
   */
  private boolean isPushEnd() {
    boolean result = true;

    for (AtnState curPushState = pushState; curPushState != null; curPushState = curPushState.pushState) {
      if (!curPushState.isRuleEnd()) {
        result = false;
        break;
      }
    }

    return result;
  }

  /**
   * Get a "pop" state based on this instance's push state and this state as
   * the end state, where a "pop" state is a temporary state used to generate
   * the next states in a parent rule after completing a 'pushed' rule.
   * 
   * The pop state also acts as a marker when constructing a parse tree
   * from the states tree to identify when to move back up to a parent
   * in the parse tree for adding subsequent children from later matched
   * states.
   */
  AtnState popState(Tree<AtnState> parentStateNode) {
    AtnState result = null;

    if (pushState != null) {
      // verify rule (constituent) with the (pushState.)rule test
      result = new AtnState(inputToken, pushState.rule, pushState.stepNum,
                            parentStateNode, pushState.parseOptions,
                            pushState.repeatNum, pushState.skipNum,
                            pushState.pushState);
      result.isPoppedState = true;
      result.popCount = 1;
    }

    return result;
  }

  /**
   * Get the next state for a repeat of the step if the step could
   * repeat.
   */
  AtnState getNextRepeatState(Tree<AtnState> curStateNode, AtnState referenceState, boolean incToken, Set<Integer> stopList) {
    AtnState result = null;

    if (referenceState == null) referenceState = this;

    if (referenceState.getRuleStep().repeats()) {
      if (incToken && !getRuleStep().consumeToken()) incToken = false;

      final Token nextToken = incToken ? getNextToken(stopList) : this.inputToken;
      if (nextToken != null) {
        result =
          new AtnState(
            nextToken, rule, stepNum,
            curStateNode, parseOptions, repeatNum + (incToken ? 1 : 0), 0, pushState);
      }
    }

    return result;
  }

  /**
   * Get the next state for incrementing to the next rule step if
   * incrementing is possible.
   */
  AtnState getNextStepState(Tree<AtnState> curStateNode, boolean incToken, Set<Integer> stopList) {
    AtnState result = null;

    if (!rule.isLast(stepNum)) {
      final int nextStepNum = getNextStepNum();
      if (nextStepNum >= 0) {

        if (incToken && !getRuleStep().consumeToken()) incToken = false;

        final Token nextToken = incToken ? getNextToken(stopList) : this.inputToken;
        if (nextToken != null) {
          result =
            new AtnState(
              nextToken, rule, nextStepNum,
              curStateNode, parseOptions, repeatNum, 0, pushState);
        }
      }
    }

    return result;
  }

  /**
   * Get the next state for retrying this info's step with a revised
   * input token.
   */
  AtnState getNextRevisedState() {
    AtnState result = null;

    if (getRuleStep().consumeToken()) {
      final Token nextToken = computeRevisedToken();

      if (nextToken != null) {
        result = new AtnState(
          nextToken, rule, stepNum,
          parentStateNode, parseOptions, repeatNum, skipNum, pushState);
      }
    }

    return result;
  }

  AtnState getSkipOptionalState() {
    AtnState result = null;

    if (getRuleStep().isOptional()) {
      if (!rule.isLast(stepNum)) {
        final int nextStepNum = getNextStepNum();
        if (nextStepNum >= 0) {
          // increment step
          result =
            new AtnState(
              inputToken, rule, nextStepNum,
              parentStateNode, parseOptions, 0, 0, pushState);
        }
      }
      // else, return null and let caller add Pop state
    }

    return result;
  }

  private boolean canBeSkipped() {
    boolean result = (skipNum + inputToken.getWordCount() - 1) < Math.max(parseOptions.getSkipTokenLimit(), getRuleStep().getSkip());

    if (result && !parseOptions.getConsumeAllText()) {
      // when not consuming all text,
      // disable skip functionality for first matching rule step in a rule
      final AtnState parentState = (parentStateNode != null) ? parentStateNode.getData() : null;
      if (parentState == null || !(parentState.getMatched() || parentState.isPoppedState() || parentState.isSkipped())) {
        result = false;
      }
    }

    return result;
  }

  AtnState getNextSkippedState(Tree<AtnState> curStateNode, Set<Integer> stopList) {
    AtnState result = null;

if (inputToken.getText().startsWith("songwriter") && this.toString().startsWith("person-event")) {
  final boolean stopHere = true;
}

    if (canBeSkipped()) {
      // increment the token, not the rule step
      final Token nextToken = getNextSmallestToken(stopList);
      if (nextToken != null) {
        markAsSkipped();
        result = new AtnState(nextToken, rule, stepNum, curStateNode, parseOptions, repeatNum, skipNum, pushState);
      }
    }

    return result;
  }


  /**
   * Get the (possibly cached) next token following this instance's considered state.
   */
  Token getNextToken(Set<Integer> stopList) {
    if (!computedNextToken) {
      _nextToken = computeNextToken(inputToken);

      if (_nextToken != null && stopList != null && stopList.contains(_nextToken.getStartIndex())) {
        _nextToken = null;
      }

      computedNextToken = true;
    }
    return _nextToken;
  }

  private Token getNextSmallestToken(Set<Integer> stopList) {
    Token result = inputToken.getNextSmallestToken();

    if (result != null) {
      result = rule.getGrammar().getAcceptedToken(rule.getTokenFilterId(), result, false, inputToken, false, false, this);

      if (result != null && stopList != null && stopList.contains(result.getStartIndex())) {
        result = null;
      }
    }

    return result;
  }

  /**
   * Get the next step num or -1 if there isn't another step after all.
   */
  private final int getNextStepNum() {
    int result = stepNum + 1;

    // check the step's 'require' attribute
    while (true) {
      final AtnRuleStep step = rule.getStep(result);
      if (step == null) {
        result = -1;
        break;
      }
      final String require = step.getRequire();
      if (require == null) break;
      else {
        // if require is met, we're done
        if (haveRequired(require)) {
          break;
        }

        // if require isn't met, increment and loop
        else {
          ++result;
        }
      }
    }

    return result;
  }

  private final boolean haveRequired(String require) {
    boolean result = false;

    for (AtnState curState = this; curState != null; curState = curState.getParentState()) {

      // quit when pushState is reached
      if (curState == pushState) break;

      // check for 'require' on states with the same pushState
      if (curState.pushState == this.pushState) {
        if (require.equals(curState.getRuleStep().getCategory()) && curState.getMatched()) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  public final AtnState getParentState() {
    AtnState result = null;

    if (parentStateNode != null) {
      result = parentStateNode.getData();
    }

    return result;
  }

  private final Token computeNextToken(Token inputToken) {
    Token result = null;

    // get the next token without crossing a hard break boundary
    final Token nextToken = inputToken.getNextToken();
    if (nextToken != null) {
      result = rule.getGrammar().getAcceptedToken(rule.getTokenFilterId(), nextToken, false, inputToken, false, true, this);
    }

    return result;
  }

  private Token computeRevisedToken() {
    Token result = null;

    if (!isSkipped()) {
      result = rule.getGrammar().getAcceptedToken(rule.getTokenFilterId(), inputToken.getRevisedToken(), true, inputToken, true, false, this);
    }

    return result;
  }

  /**
   * Determine whether this instance's token matches the step category
   * according to the grammar.
   */
  boolean tokenMatchesStepCategory(AtnGrammar grammar) {
    boolean result = false;

    final AtnRuleStep ruleStep = getRuleStep();
    if (ruleStep.getIgnoreToken()) {
      result = ruleStep.verify(inputToken, this);
    }
    else {
      String category = ruleStep.getCategory();

      if (grammar.getCat2Classifiers().containsKey(category)) {
        for (TokenClassifier classifier : grammar.getCat2Classifiers().get(category)) {
          if (classifier.classify(inputToken) && ruleStep.verify(inputToken, this)) {
            result = true;
            break;
          }
        }
      }
      else {
        if (!grammar.getCat2Rules().containsKey(category)) {
          // use an "identity" classifier for literal grammar tokens.
          result = category.equals(inputToken.getText());
        }

        // check for a feature that matches the category
        if (!result) {
          result = inputToken.getFeature(category, null) != null;
        }

        if (result) {
          result = ruleStep.verify(inputToken, this);
        }
      }
    }

    return result;
  }

  private final boolean applyTests() {
    return getRuleStep().verify(inputToken, this);
  }

  void markAsSkipped() {
    this._isSkipped = true;
    ++this.skipNum;
  }


  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append(rule.getRuleName()).
      append('-').
      append(getRuleStep().getCategory()).
      append('(').
      append(inputToken).
      append(')');

    return result.toString();
  }


  private final boolean matchesRulePath(AtnState other) {
    if (other == null) return false;
    if (this == other) return true;

    boolean result = false;

    if (rule == other.getRule() &&
        stepNum == other.getStepNum() &&
        repeatNum == other.getRepeatNum()) {

      if (parentStateNode == other.getParentStateNode()) {
        result = true;
      }
      else if (parentStateNode != null && other.getParentStateNode() != null) {
        final AtnState parentState = parentStateNode.getData();
        final AtnState otherParentState = other.getParentStateNode().getData();

        if (parentState == otherParentState) {
          result = true;
        }
        else if (parentState != null) {
          result = parentState.matchesRulePath(otherParentState);
        }
      }
    }

    return result;
  }

  private final boolean applyAllPops(Tree<AtnState> nextStateNode, LinkedList<AtnState> states, LinkedList<AtnState> skipStates, Set<Integer> stopList) {
    boolean result = true;

    if (isRuleEnd()) {
      final int statesSize = states.size();
      final int skipStatesSize = skipStates.size();

      final AtnGrammar grammar = rule.getGrammar();
      Tree<AtnState> popStateNode = nextStateNode;
      AtnState popState = popState(popStateNode);
      while (popState != null) {
        // apply popState test
        result = popState.applyTests();
        if (!result) {
          // back out of popping
          while (states.size() > statesSize) states.removeLast();
          while (skipStates.size() > skipStatesSize) skipStates.removeLast();

          break;
        }

        popStateNode = popStateNode.addChild(popState);
        if (addNextStates(grammar, states, skipStates, popState, popStateNode, true, true, stopList)) {
          if (!popState.isRuleEnd()) popState = null;
          else {
            popState = popState.popState(popStateNode);
          }
        }
        else break;
      }

      if (result) {
        // remove now unnecessary skipped states
        for (AtnState parentState = this; skipStates.size() > 0 && parentState != null; parentState = parentState.getParentState()) {
          if (parentState == this || parentState.getMatched()) {
            for (Iterator<AtnState> skipIter = skipStates.iterator(); skipIter.hasNext(); ) {
              final AtnState skipState = skipIter.next();
              if (parentState.encompassesToken(skipState.getInputToken())) {
                skipIter.remove();
              }
            }
          }
        }
      }
    }

    return result;
  }

  /**
   * Determine whether this state's input token encompasses the given token.
   */
  private final boolean encompassesToken(Token token) {
    return inputToken.encompasses(token);
  }


  private static boolean trace = false;

  public static final void setTrace(boolean traceValue) {
    trace = traceValue;
  }

  static List<CategorizedToken> computeTokens(Tree<AtnState> stateNode) {
    final List<CategorizedToken> result = new ArrayList<CategorizedToken>();
    final LinkedList<Tree<AtnState>> stateNodes = stateNode.getRootPath();

    for (int pathIndex = 1; pathIndex < stateNodes.size(); ++pathIndex) {
      final Tree<AtnState> pathStateNode = stateNodes.get(pathIndex);
      final AtnState pathState = pathStateNode.getData();
      if (pathState.getMatched() && pathState.getRuleStep().consumeToken()) {
        result.add(new CategorizedToken(pathState.getInputToken(), pathState.getRuleStep().getCategory()));
      }
    }

    return result;
  }

  static boolean matchTokenToRule(AtnGrammar grammar, LinkedList<AtnState> states, LinkedList<AtnState> skipStates, Set<Integer> stopList) {
    boolean result = false;

    while ((states.size() + skipStates.size() > 0) && !result) {
      final AtnState curstate = states.size() > 0 ? states.removeFirst() : skipStates.removeFirst();

      boolean success = false;
      boolean matches = curstate.tokenMatchesStepCategory(grammar);
      final Tree<AtnState> nextStateNode = curstate.parentStateNode.addChild(curstate);

      if (trace) {
        System.out.println(curstate + "\t" + matches + "\t" + (matches ? AtnStateUtil.showStateTree(nextStateNode) : ""));
      }

      if (matches) {
        matches = curstate.applyAllPops(nextStateNode, states, skipStates, stopList);
      }

      if (matches) {
        success = true;
        curstate.setMatched(true);

        if (curstate.isValidEnd(stopList)) {
          // found a valid full parse
          result = true;
        }
      }

      success = addNextStates(grammar, states, skipStates, curstate, nextStateNode, false, matches, stopList);
    }

    return result;
  }

  private static boolean addNextStates(AtnGrammar grammar, LinkedList<AtnState> states, LinkedList<AtnState> skipStates, AtnState curstate, Tree<AtnState> nextStateNode, boolean isPop, boolean inc, Set<Integer> stopList) {
    if (curstate == null) return false;

    boolean foundOne = inc || isPop;
    AtnState nextstate = null;

    // increment token
    if (foundOne) {
      nextstate = curstate.getNextRepeatState(nextStateNode, isPop ? curstate : null, inc, stopList);
      if (nextstate != null) {
        addState(states, nextstate);
      }

      nextstate = curstate.getNextStepState(nextStateNode, inc, stopList);
      if (nextstate != null) {
        addState(states, nextstate);
      }

      // revise token
      nextstate = curstate.getNextRevisedState();
      if (nextstate != null) { addState(states, nextstate); foundOne = true; }

      return foundOne;
    }

    // revise token
    nextstate = curstate.getNextRevisedState();
    if (nextstate != null) { addState(states, nextstate); foundOne = true; }

    // account for optional step.
    if (curstate.getRuleStep().isOptional() && !curstate.isRepeat()) {
      nextstate = curstate.getSkipOptionalState();
      if (nextstate != null) {
        addState(states, nextstate); foundOne = true;
      }
    }

    // apply (push) rules
    final String category = curstate.getRuleStep().getCategory();
    if (grammar.getCat2Rules().containsKey(category)) {
      foundOne = true;
      for (AtnRule rule : grammar.getCat2Rules().get(category)) {
        addState(states, new AtnState(curstate.getInputToken(), rule, 0, nextStateNode, curstate.parseOptions, 0, 0, curstate));
      }

      // skip constituents
      if (curstate.canBeSkipped()) {
        final AtnState dupstate = new AtnState(curstate);
        final Tree<AtnState> dupstateNode = new Tree<AtnState>(dupstate);
        nextstate = dupstate.getNextSkippedState(dupstateNode, stopList);
        if (nextstate != null) {
          nextStateNode.getParent().addChild(dupstateNode);
          dupstate.parentStateNode = nextStateNode.getParent();
          addState(skipStates, nextstate);
        }
      }
    }

    // skip tokens
    if (!foundOne) {
      nextstate = curstate.getNextSkippedState(nextStateNode, stopList);
      if (nextstate != null) addState(skipStates, nextstate);
    }

    return foundOne;
  }

  private static final void addState(LinkedList<AtnState> states, AtnState nextstate) {
    boolean isDup = false;

    // check for duplicates
    final Token token = nextstate.getInputToken();
    for (AtnState state : states) {
      if (token == state.getInputToken() && nextstate.matchesRulePath(state)) {
        isDup = true;
        break;
      }
    }

    if (!isDup) states.addLast(nextstate);
  }
}
