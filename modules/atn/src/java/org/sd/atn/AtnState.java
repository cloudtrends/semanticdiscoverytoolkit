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
  
  static String TOKEN_KEY = "cToken";

  private Token inputToken;
  Token getInputToken() {
    return inputToken;
  }

  private AtnRule rule;
  AtnRule getRule() {
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
  boolean getMatched() {
    return matched;
  }
  void setMatched(boolean matched) {
    this.matched = matched;
  }

  private AtnState pushState;
  AtnState getPushState() {
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

  boolean isRepeat() {
    return repeatNum > 0;
  }

  private boolean _isSkipped;
  boolean isSkipped() {
    return _isSkipped;
  }

  boolean isRuleEnd() {
    return (rule != null) ? rule.isTerminal(stepNum) : false;
  }

  private AtnRuleStep _ruleStep;
  AtnRuleStep getRuleStep() {
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
  private boolean isPushEnd()
  {
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
   * Get a "pop" state based on this instance's push state and the given
   * end state, where a "pop" state is a temporary state used to generate
   * the next states in a parent rule after completing a 'pushed' rule.
   * 
   * The pop state also acts as a marker when constructing a parse tree
   * from the states tree to identify when to move back up to a parent
   * in the parse tree for adding subsequent children from later matched
   * states.
   */
  AtnState popState(AtnState endState, Tree<AtnState> parentStateNode) {
    AtnState result = null;

    if (pushState != null) {
      result = new AtnState(endState.inputToken, pushState.rule, pushState.stepNum, parentStateNode, pushState.parseOptions, pushState.repeatNum, pushState.skipNum, pushState.pushState);
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
      if (incToken && !getRuleStep().consumeToken()) incToken = false;

      final Token nextToken = incToken ? getNextToken(stopList) : this.inputToken;
      if (nextToken != null) {
        result =
          new AtnState(
            nextToken, rule, stepNum + 1,
            curStateNode, parseOptions, repeatNum, 0, pushState);
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
      final Token nextToken = computeRevisedToken(inputToken);

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
        // increment step
        result =
          new AtnState(
            inputToken, rule, stepNum + 1,
            parentStateNode, parseOptions, 0, 0, pushState);
      }
      // else, return null and let caller add Pop state
    }

    return result;
  }

  AtnState getNextSkippedState(Tree<AtnState> curStateNode, Set<Integer> stopList) {
    AtnState result = null;

    if (skipNum < parseOptions.getSkipTokenLimit()) {
      // increment the token, not the rule step
      final Token nextToken = getNextToken(stopList);
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

  private Token computeNextToken(Token inputToken) {
    Token result = null;

    // get the next token without crossing a hard break boundary
    final Token nextToken = inputToken.getNextToken();
    if (nextToken != null) {
      result = rule.getGrammar().getAcceptedToken(rule.getTokenFilterId(), nextToken, false, inputToken, false, true, this);
    }

    return result;
  }

  private Token computeRevisedToken(Token inputToken) {
    Token result = rule.getGrammar().getAcceptedToken(rule.getTokenFilterId(), inputToken.getRevisedToken(), true, inputToken, true, false, this);
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

  /**
   * Convert this state to a parse tree.
   */
  static Tree<String> convertToTree(Tree<AtnState> stateNode) {
    final LinkedList<Tree<AtnState>> stateNodes = stateNode.getRootPath();

    AtnState lastPushState = null;
    Tree<String> result = null;
    Tree<String> curResultNode = null;

    for (int pathIndex = 1; pathIndex < stateNodes.size(); ++pathIndex) {
      final Tree<AtnState> pathStateNode = stateNodes.get(pathIndex);
      final AtnState pathState = pathStateNode.getData();
      final String category = pathState.getRuleStep().getCategory();
      final AtnState pushState = pathState.getPushState();

      if (!pathState.getRuleStep().consumeToken()) continue;

      if (result == null) {
        final String ruleName = pathState.getRule().getRuleName();
        result = new Tree<String>(ruleName);
        curResultNode = result;
      }

      if (pathState.isPoppedState()) {
        for (int popCounter = 0; popCounter < pathState.getPopCount(); ++popCounter) {
          curResultNode = curResultNode.getParent();
        }
      }
      else {
        if (!pathState.getMatched()) {
          if (pathState.isSkipped()) {
            final Tree<String> unknownNode = curResultNode.addChild("?");
            unknownNode.addChild(pathState.getInputToken().getText());
            unknownNode.getAttributes().put(TOKEN_KEY, new CategorizedToken(pathState.getInputToken(), "?"));
          }
          else {
            curResultNode = curResultNode.addChild(category);
          }
        }
        else {
          final Tree<String> categoryNode = curResultNode.addChild(category);

          if (!category.equals(pathState.getInputToken().getText())) {
            final Tree<String> terminalNode = categoryNode.addChild(pathState.getInputToken().getText());
            terminalNode.getAttributes().put(TOKEN_KEY, new CategorizedToken(pathState.getInputToken(), category));
          }
        }

        lastPushState = pushState;
      }
    }

    return result;
  }

  static boolean matchTokenToRule(AtnGrammar grammar, LinkedList<AtnState> states, Set<Integer> stopList) {
    boolean result = false;

    while (states.size() > 0 && !result) {
      final AtnState curstate = states.removeFirst();

      boolean success = false;
      boolean matches = curstate.tokenMatchesStepCategory(grammar);
      final Tree<AtnState> nextStateNode = curstate.parentStateNode.addChild(curstate);

      if (matches) {
        success = true;
        curstate.setMatched(true);

        if (curstate.isValidEnd(stopList)) {
          // found a valid full parse
          result = true;
        }

        if (curstate.isRuleEnd()) {
          final AtnState popState = curstate.popState(curstate, nextStateNode);
          final Tree<AtnState> popStateNode = nextStateNode.addChild(popState);
          addNextStates(grammar, states, popState, popStateNode, true, true, stopList);
        }
      }

      success = addNextStates(grammar, states, curstate, nextStateNode, false, matches, stopList);
    }

    return result;
  }

  private static boolean addNextStates(AtnGrammar grammar, LinkedList<AtnState> states, AtnState curstate, Tree<AtnState> nextStateNode, boolean isPop, boolean inc, Set<Integer> stopList) {
    if (curstate == null) return false;

    boolean foundOne = inc || isPop;
    AtnState nextstate = null;

    // increment token
    if (foundOne) {
      nextstate = curstate.getNextRepeatState(nextStateNode, isPop ? curstate : null, inc, stopList);
      if (nextstate != null) {
        states.addLast(nextstate);

        if (nextstate.getRuleStep().isOptional() && nextstate.isRuleEnd()) {
          // is optional end, so pop rule
          final AtnState popState = nextstate.popState(nextstate, nextStateNode);
          final Tree<AtnState> popStateNode = nextStateNode.addChild(popState);
          addNextStates(grammar, states, popState, popStateNode, true, false, stopList);
        }
      }

      nextstate = curstate.getNextStepState(nextStateNode, inc, stopList);
      if (nextstate != null) {
        states.addLast(nextstate);
      }

      return foundOne;
    }

    // revise token
    nextstate = curstate.getNextRevisedState();
    if (nextstate != null) { states.addLast(nextstate); foundOne = true; }

    // account for optional step.
    if (curstate.getRuleStep().isOptional() && !curstate.isRepeat()) {
      nextstate = curstate.getSkipOptionalState();
      if (nextstate != null) {
        states.addLast(nextstate); foundOne = true;
      }
    }

    // apply (push) rules
    final String category = curstate.getRuleStep().getCategory();
    if (grammar.getCat2Rules().containsKey(category)) {
      foundOne = true;
      for (AtnRule rule : grammar.getCat2Rules().get(category)) {
        states.addLast(new AtnState(curstate.getInputToken(), rule, 0, nextStateNode, curstate.parseOptions, 0, 0, curstate));
      }
    }

    // skip tokens
    if (!foundOne) {
      nextstate = curstate.getNextSkippedState(nextStateNode, stopList);
      if (nextstate != null) states.addLast(nextstate);
    }

    return foundOne;
  }
}
