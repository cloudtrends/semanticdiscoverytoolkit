/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sd.token.CategorizedToken;
import org.sd.token.FeatureConstraint;
import org.sd.token.Feature;
import org.sd.token.Features;
import org.sd.token.Token;
import org.sd.util.tree.Tree;

/**
 * Utilities for working with AtnState instances and trees.
 * <p>
 * @author Spence Koehler
 */
public class AtnStateUtil {
  
  public enum Gravity { POP, LAST_MATCH, FIRST_MATCH, PUSH };

  public static final Map<String, Gravity> GRAVITY_LOOKUP = new HashMap<String, Gravity>();
  static {
    GRAVITY_LOOKUP.put("pop", Gravity.POP);
    GRAVITY_LOOKUP.put("lastmatch", Gravity.LAST_MATCH);
    GRAVITY_LOOKUP.put("firstmatch", Gravity.FIRST_MATCH);
    GRAVITY_LOOKUP.put("push", Gravity.PUSH);
  }


  // parse node attributes

  public static String TOKEN_KEY = "cToken";     // -> cToken:CategorizedToken (leafs' parents)
  public static String RULE_ID_KEY = "_ruleID";  // -> ruleID:String (id'd constituents)
  public static String INTERP_KEY = "_interps";  // -> interps:ParseInterpretation[] (pre-parsed constituents)
  // public static String CATEGORY_KEY = "_category";  // for mapping a ruleStep label back to its original category (e.g., for interps)

  /**
   * A type for a feature on a token indicating the category (value) that
   * allowed a token match for the token. This is used, e.g., in TextTest
   * for determining the actual matched text from the token.
   */
  public static final String FEATURE_MATCH = "_featureMatch";

  public static final FeatureConstraint FEATURE_MATCH_CONSTRAINT = new FeatureConstraint();
  static {
    FEATURE_MATCH_CONSTRAINT.setType(FEATURE_MATCH);
    FEATURE_MATCH_CONSTRAINT.setClassType(AtnState.class);
    FEATURE_MATCH_CONSTRAINT.setFeatureValueType(String.class);
  }

  private static final LongestParseSelector PARSE_SELECTOR = new LongestParseSelector(false, true);
  private static final boolean RESOLVE_AMBIGUITY = false;

  /**
   * Set a feature on the token that indicates that it matched based on a
   * token feature of the given category.  (Used, e.g., in TextTest.)
   */
  public static final void setFeatureMatch(Token token, String category, AtnState state) {
    token.setFeature(FEATURE_MATCH, category, state);
  }

  /**
   * Create a feature match constraint targetting the given category.
   */
  public static FeatureConstraint createFeatureMatchConstraint(String category) {
    final FeatureConstraint result = new FeatureConstraint();

    result.setType(FEATURE_MATCH);
    result.setClassType(AtnState.class);
    result.setValue(category);

    return result;
  }

  /**
   * Get the feature match interpretations (only for selected parses) on
   * the given token with the given category if it exists, or null.
   */
  public static List<ParseInterpretation> getFeatureMatchInterps(Token token, String category) {
    List<ParseInterpretation> result = null;

    if (token.hasFeatures()) {
      final FeatureConstraint interpConstraint = AtnParseBasedTokenizer.createParseInterpretationFeatureConstraint(category);
      final List<Feature> interpFeatures = token.getFeatures().getFeatures(interpConstraint);
      if (interpFeatures != null) {
        for (Feature interpFeature : interpFeatures) {
          final ParseInterpretation interp = (ParseInterpretation)interpFeature.getValue();
          final AtnParse atnParse = interp.getSourceParse();
          if (atnParse != null && atnParse.getSelected()) {
            if (result == null) result = new ArrayList<ParseInterpretation>();
            result.add(interp);
          }
        }
      }
    }

    return result;
  }


  /**
   * Convert the state (automaton) path to a parse tree.
   * <p>
   * Note that terminal nodes in the tree will hold token text while their
   * parent nodes will hold the parsed categories. The immediate parents of
   * terminal nodes will have an attribute mapping TOKEN_KEY to a
   * CategorizedToken for the text.
   */
  public static Tree<String> convertToTree(Tree<AtnState> stateNode) {
    return convertToTree(stateNode, true);
  }

  /**
   * Convert the state (automaton) path to a parse tree.
   * <p>
   * Note that terminal nodes in the tree will hold token text while their
   * parent nodes will hold the parsed categories. The immediate parents of
   * terminal nodes will have an attribute mapping TOKEN_KEY to a
   * CategorizedToken for the text.
   *
   * @param stateNode the end state node from which to build the tree
   * @param goDeep true to decompose tokens into parse trees where possible;
   *               false for a "shallow" or "high level" parse tree.
   */
  public static Tree<String> convertToTree(Tree<AtnState> stateNode, boolean goDeep) {
    final LinkedList<Tree<AtnState>> stateNodes = stateNode.getRootPath();

    AtnState lastPushState = null;
    Tree<String> result = null;
    Tree<String> curResultNode = null;

    for (int pathIndex = 1; pathIndex < stateNodes.size(); ++pathIndex) {
      final Tree<AtnState> pathStateNode = stateNodes.get(pathIndex);
      final AtnState pathState = pathStateNode.getData();
      final String category = pathState.getRuleStep().getLabel();
      final AtnState pushState = pathState.getPushState();

      if (!pathState.getRuleStep().consumeToken()) continue;

      if (result == null) {
        final String ruleName = pathState.getRule().getRuleName();
        result = new Tree<String>(ruleName);

        // add ruleID and category attributes on the parse node
        addParseNodeAttributes(result, pathState);

        curResultNode = result;
      }

      if (pathState.isPoppedState()) {
        final int pushDepth = getPushDepth(pathState);
        for (int i = curResultNode.depth(); i > pushDepth; --i) {
          curResultNode = curResultNode.getParent();
        }
      }
      else {
        final Token inputToken = pathState.getInputToken();
        if (!pathState.getMatched()) {
          // add skipped token
          if (pathState.isSkipped()) {
            final Tree<String> unknownNode = curResultNode.addChild("?");
            unknownNode.addChild(inputToken.getText()/*WithDelims()*/);
            unknownNode.getAttributes().put(TOKEN_KEY, new CategorizedToken(inputToken, "?"));
          }
          else {
            // add ruleID and category attributes on the parse node
            addParseNodeAttributes(curResultNode, pathState);

            curResultNode = curResultNode.addChild(category);
          }
        }
        else {
          // add matched token
          final Tree<String> categoryNode = curResultNode.addChild(category);

          // add ruleID and category attributes on the parse node
          addParseNodeAttributes(curResultNode, pathState);

          if (!category.equals(inputToken.getText())) {
            // add non-literal matched token
            final List<Tree<String>> tokenParses = goDeep ? getTokenParses(inputToken, pathState.getRuleStep().getCategory()) : null;  //...getCategory() was category
            if (tokenParses == null) {
              // add token text
              categoryNode.addChild(inputToken.getText()/*WithDelims()*/);
            }
            else {
              if (tokenParses.size() > 1) {
                categoryNode.getAttributes().put("ambiguous", "true");
              }
              for (Tree<String> tokenParse : tokenParses) {
                categoryNode.addChild(tokenParse);
              }
            }
          }

          // store CategorizedToken as an attribute on the parse tree node
          categoryNode.getAttributes().put(TOKEN_KEY, new CategorizedToken(inputToken, category));
        }

        lastPushState = pushState;
      }
    }

    return result;
  }

  private static final void addParseNodeAttributes(Tree<String> parseNode, AtnState pathState) {
    final Map<String, Object> attributes = parseNode.getAttributes();

    // add ruleID as an attribute on the parse node
    final String ruleId = pathState.getRule().getRuleId();
    if (ruleId != null && !"".equals(ruleId) && !attributes.containsKey(RULE_ID_KEY)) {
      attributes.put(RULE_ID_KEY, ruleId);
    }

    // // add original category as an attribute on the parse node
    // final AtnRuleStep ruleStep = pathState.getRuleStep();
    // final String origCat = ruleStep.getCategory();
    // final String label = ruleStep.getLabel();
    // if (!origCat.equals(label) && !attributes.containsKey(CATEGORY_KEY)) {
    //   attributes.put(CATEGORY_KEY, origCat);
    // }
  }

  public static final int countConstituentTokens(AtnState refState) {
    int result = 0;

    final AtnState refPush = refState.getPushState();
    for (AtnState curState = refState; curState != null; curState = curState.getParentState()) {
      if (curState.getMatched()) {
        ++result;
      }
      final AtnState curPush = curState.getPushState();
      if (curPush == refPush && curPush == curState.getParentState()) break;
    }

    return result;
  }

/*
  public static final int countConstituentSteps(AtnState refState) {
    int result = 0;

    final AtnState refPush = refState.getPushState();
    Token refToken = refState.getInputToken();
    for (AtnState curState = refState; curState != null; curState = curState.getParentState()) {
      final Token curToken = curState.getInputToken();
      final AtnState curPush = curState.getPushState();
      if (curPush == refPush) {
        if (refToken != curToken) {
          if (!curState.isPoppedState()) {
            ++result;
          }
          refToken = curToken;
        }
        if (curPush == curState.getParentState()) break;
      }
    }

    return result;
  }
*/

  public static final int countConstituentSteps(AtnState refState) {
    int result = 0;

    final AtnState refPush = refState.getPushState();
    for (AtnState curState = refState; curState != null; curState = curState.getParentState()) {
      final AtnState curPush = curState.getPushState();
      if (curPush == refPush) {
        if (curState.isPoppedState() || curState.getMatched()) {
          ++result;
        }
        if (curPush == curState.getParentState()) break;
      }
    }

    return result;
  }

  public static final int countRepeats(AtnState refState) {
    int result = 0;

    final AtnState refPush = refState.getPushState();
    final int refStepNum = refState.getStepNum();
    for (AtnState curState = refState; curState != null; curState = curState.getParentState()) {
      final AtnState curPush = curState.getPushState();
      if (curPush == refPush) {
        if ((curState.isPoppedState() || curState.getMatched())) {
          if (curState.getStepNum() == refStepNum) {
            ++result;
          }
          else break;
        }
        if (curPush == curState.getParentState()) break;
      }
    }

    return result;
  }

  /**
   * Determine whether the state is the first in its constituent.
   */
  public static final boolean isFirstConstituentState(AtnState curState) {
    boolean result = (curState.getStepNum() == 0 && curState.getRepeatNum() == 0);

    if (!result) {
      if (curState.isPoppedState()) {
        result = isFirstConstituentState(curState.getParentState());
      }
      else {
        result = (curState.getPushState() == curState.getParentState());
      }
    }

    return result;
  }

  /**
   * Get the first state of the state's constituent (just under the constituent top).
   */
  public static final AtnState getConstituentStartState(AtnState curState) {
    AtnState result = null;

    final AtnState refPush = curState.getConstituentTop();
    for (result = curState; result != null; result = result.getParentState()) {
      final AtnState curPush = result.getConstituentTop();
      if (curPush == refPush && curPush == result.getParentState()) break;
    }

    return result;
  }

  /**
   * Get the first state of the parse.
   */
  public static final AtnState getParseStartState(AtnState curState) {
    AtnState result = curState;

    if (curState.getParentStateNode() != null) {
      //NOTE: root has null data and single child for the true first state
      result = curState.getParentStateNode().getRoot().getChildren().get(0).getData();
    }

    return result;
  }

  /**
   * Get the 'match' states for the constituent ending at endState.
   */
  public static final LinkedList<AtnState> getConstituentMatchStates(AtnState endState) {
    final LinkedList<AtnState> result = new LinkedList<AtnState>();

    final AtnState refPush = endState.getConstituentTop();
    for (AtnState curState = endState; curState != null; curState = curState.getParentState()) {
      if (curState.getMatched()) {
        result.add(0, curState);
      }
      final AtnState curPush = curState.getPushState();
      if (curPush == refPush && curPush == curState.getParentState()) break;
    }

    return result;
  }

  public static final Set<Integer> getConstituentMatchedSteps(AtnState endState) {
    final Set<Integer> result = new HashSet<Integer>();

    final AtnState refPush = endState.getPushState();
    for (AtnState curState = endState; curState != null; curState = curState.getParentState()) {
      if (curState.getPushState() == refPush) {
        result.add(curState.getStepNum());
      }
      final AtnState curPush = curState.getPushState();
      if (curPush == refPush && curPush == curState.getParentState()) break;
    }

    return result;
  }

  /**
   * Find the first state prior to the given state whose token 'matched'.
   */
  public static final AtnState getLastMatchingState(AtnState curState) {
    return getLastMatchingState(curState, null);
  }

  public static final AtnState getLastMatchingState(AtnState curState, int[] levelDiff) {
    AtnState result = null;

    if (curState != null) {
      for (AtnState prevState = curState.getParentState(); prevState != null; prevState = prevState.getParentState()) {
        if (prevState.getMatched()) {
          result = prevState;
          break;
        }
      }
    }

    if (result != null && levelDiff != null) {
      levelDiff[0] += (getPushDepth(curState) - getPushDepth(result));
    }

    return result;
  }

  public static final int getPushDepth(AtnState atnState) {
    int result = 0;

    for (AtnState pushState = atnState.getPushState(); pushState != null; pushState = pushState.getPushState()) {
      ++result;
    }

    return result;
  }

  /**
   * Find the last matching token prior to the given state.
   */
  public static final Token getLastMatchingToken(AtnState curState) {
    Token result = null;

    final AtnState prevState = getLastMatchingState(curState);
    if (prevState != null) {
      result = prevState.getInputToken();
    }

    return result;
  }

  /**
   * Find the last matching category prior to the given state.
   */
  public static final String getLastMatchingCategory(AtnState curState) {
    String result = null;

    final AtnState prevState = getLastMatchingState(curState);
    if (prevState != null) {
      result = prevState.getRuleStep().getCategory();
    }

    return result;
  }

  public static final boolean matchesCategory(AtnState atnState, Set<String> categories) {
    return matchesCategory(atnState, categories, null);
  }

  public static final boolean matchesCategory(AtnState atnState, Set<String> categories, int[] levelDiff) {
    boolean result = false;

    int popCount = 0;
    for (; !result && atnState != null; atnState = atnState.getPushState()) {
      result = categories.contains(atnState.getRuleStep().getCategory());
      if (!result && levelDiff != null) ++popCount;
    }
    if (result &&levelDiff != null) levelDiff[0] += popCount;

    return result;
  }

  public static final boolean matchesCategory(AtnState atnState, StepRequirement requirement) {
    return matchesCategory(atnState, requirement, null);
  }

  public static final boolean matchesCategory(AtnState atnState, StepRequirement requirement, int[] levelDiff) {
    boolean result = false;

    if (atnState.getMatched()) {
      int popCount = 0;

      for (; !result && atnState != null; atnState = atnState.getPushState()) {
        final AtnRuleStep ruleStep = atnState.getRuleStep();
        final String category = ruleStep.getCategory();
        final String label = ruleStep.getLabel();

        result = requirement.matches(category, label, (levelDiff == null ? null : levelDiff[0] + popCount));
        if (!result && levelDiff != null) ++popCount;
      }
      if (result && levelDiff != null) levelDiff[0] += popCount;
    }

    return result;
  }

  /**
   * Find the first state prior to the given state that matches the given
   * category, or null.
   */
  public static final AtnState findPriorMatch(AtnState atnState, StepRequirement requirement, int[] levelDiff) {
    AtnState result = null;

    for (AtnState prevState = getLastMatchingState(atnState, levelDiff); prevState != null; prevState = getLastMatchingState(prevState, levelDiff)) {
      if (matchesCategory(prevState, requirement, levelDiff)) {
        result = prevState;
        break;
      }
    }

    return result;
  }

  /**
   * Pull the (unambiguous) parse tree off of the token if it exists
   * for the category.
   * <p>
   * A token holds a parse tree if it has a feature (as placed by an
   * AtnParseBasedTokenizer) with an AtnParse. If there is unresolved
   * ambiguity, then null will be returned.
   */
  public static final List<Tree<String>> getTokenParses(Token token, String category) {
    List<Tree<String>> result = null;

    final Features tokenFeatures = token.getFeatures();
    if (tokenFeatures != null) {
      final FeatureConstraint parseConstraint = AtnParseBasedTokenizer.createParseInterpretationFeatureConstraint(category);
      final List<Feature> parseFeatures = tokenFeatures.getFeatures(parseConstraint);

      if (parseFeatures != null) {
        final AtnParseResult parseResult = new AtnParseResult(); // for selecting "longest" parse

        for (Feature parseFeature : parseFeatures) {
          final ParseInterpretation interp = (ParseInterpretation)parseFeature.getValue();
          final AtnParse atnParse = interp.getSourceParse();
          if (atnParse != null && atnParse.getSelected()) {
            parseResult.addParse(atnParse);
          }
        }

        final List<AtnParse> selected = RESOLVE_AMBIGUITY ? PARSE_SELECTOR.selectParses(parseResult) : parseResult.getParses();
        for (AtnParse atnParse : selected) {
          if (result == null) result = new ArrayList<Tree<String>>();
          final Tree<String> deepParseTree = atnParse.getParse().getParseTree();
          result.add(deepParseTree);
          
          reconcileDeepTokens(token, deepParseTree);
        }
      }
    }

    return result;
  }

  /**
   * Update deep cached tokens (narrower) in relation to the base (broader).
   */
  private static final void reconcileDeepTokens(Token baseToken, Tree<String> deepParseTree) {
    if (deepParseTree.hasAttributes()) {
      final CategorizedToken cToken = (CategorizedToken)deepParseTree.getAttributes().get(AtnStateUtil.TOKEN_KEY);
      if (cToken != null && cToken.token.getTokenizer() != baseToken.getTokenizer() &&
          !cToken.token.getTokenizer().getText().equals(baseToken.getTokenizer().getText())) {

        final int baseStart = baseToken.getStartIndex();
        int startIndex = cToken.token.getStartIndex();
        int endIndex = cToken.token.getEndIndex();
        if (cToken.token.getTokenizer().getText() != baseToken.getTokenizer().getText()) {
          // yes, we're using "!=" instead of "!.equals" here!
          startIndex += baseStart;
          endIndex += baseStart;
        }
        final Token reconciledToken = baseToken.getTokenizer().buildToken(startIndex, endIndex);
        deepParseTree.getAttributes().put(AtnStateUtil.TOKEN_KEY, new CategorizedToken(reconciledToken, cToken.category));
      }
    }
    if (deepParseTree.hasChildren()) {
      for (Tree<String> deepChild : deepParseTree.getChildren()) {
        reconcileDeepTokens(baseToken, deepChild);
      }
    }
  }

  /**
   * Utility to retrieve the categorized token stored in the parse tree for
   * the node. Note that for nodes other than terminal nodes and their
   * immediate parents, this will always return null.
   */
  public static final CategorizedToken getCategorizedToken(Tree<String> parseTreeNode) {
    CategorizedToken result = null;

    if (!parseTreeNode.hasChildren() && parseTreeNode.getParent().numChildren() == 1) parseTreeNode = parseTreeNode.getParent();
    if (parseTreeNode.hasAttributes()) {
      result = (CategorizedToken)(parseTreeNode.getAttributes().get(AtnStateUtil.TOKEN_KEY));
    }

    return result;
  }

  /**
   * Given a (matched) token and its ATN state (as would be presented to an
   * AtnRuleStepTest), collect the consecutive tokens with the same category
   * from prior states in the order encountered (so token is the last in the
   * list.)
   */
  public static LinkedList<Token> collectConsecutiveCategoryTokens(Token token, AtnState curState) {
    final LinkedList<Token> result = new LinkedList<Token>();
    final String clusterCategory = curState.getRuleStep().getCategory();

    result.addLast(token);

    for (Tree<AtnState> parentStateNode = curState.getParentStateNode();
         parentStateNode != null && parentStateNode.getData() != null;
         parentStateNode = parentStateNode.getParent()) {

      boolean keepGoing = false;

      final AtnState parentState = parentStateNode.getData();
      if (parentState.getMatched()) {
        final String matchedCategory = parentState.getRuleStep().getCategory();
        final boolean categoryMatches = clusterCategory.equals(matchedCategory);

        if (categoryMatches) {
          keepGoing = true;
          result.addFirst(parentState.getInputToken());
        }
      }

      if (!keepGoing) break;
    }

    return result;
  }

  /**
   * Visit the endState's ancestors from its parent to the root using the given
   * stateVisitor. Note that this is the automaton path to the state in reverse.
   *
   * @return the depth of the last visited state node (1-based), where 0
   *         indicates that all nodes were visited.
   */
  public static int visitStatesInReverse(AtnState endState, AtnStateVisitor stateVisitor) {
    int depth = endState.getParentStateNode().depth();

    for (Tree<AtnState> parentStateNode = endState.getParentStateNode();
         parentStateNode != null && parentStateNode.getData() != null;
         parentStateNode = parentStateNode.getParent()) {
      final AtnState parentState = parentStateNode.getData();
      if (!stateVisitor.visit(parentState, depth)) {
        break;
      }

      --depth;
    }

    return depth;
  }


  public static String showStateTree(Tree<AtnState> stateTree) {
    final StringBuilder result = new StringBuilder();
    final Tree<AtnState> root = stateTree.getRoot();
    result.append('\n');
    showStateTree(result, root, stateTree, 0);
    return result.toString();
  }

  private static final void showStateTree(StringBuilder result, Tree<AtnState> current, Tree<AtnState> marker, int indent) {
    final AtnState curstate = current.getData();

    buildStateInfo(result, curstate, current == marker, indent);

    if (current.hasChildren()) {
      for (Tree<AtnState> child : current.getChildren()) {
        showStateTree(result, child, marker, indent + 2);
      }
    }
  }

  private static final void buildStateInfo(StringBuilder result, AtnState curstate, boolean marker, int indent) {
    for (int i = 0; i < indent; ++i) result.append(' ');
    result.append(curstate == null ? "null" : curstate.toString());

    if (curstate != null) {
      final StringBuilder flags = new StringBuilder();
      if (curstate.getMatched()) flags.append('m');
      if (curstate.isSkipped()) flags.append('s').append(curstate.skipNum);
      if (curstate.isRepeat()) flags.append('r').append(curstate.getRepeatNum());
      if (curstate.isPoppedState()) {
        flags.append('p').append(curstate.getPopCount());
        if (curstate.popFailed()) {
          flags.append('F');
        }
      }
      if (flags.length() > 0) result.append("  ").append(flags);
    }

    if (marker) result.append(" ***");
    result.append('\n');
  }

  public static String showStatePath(AtnState state) {
    final StringBuilder result = new StringBuilder();

    result.append('\n');

    int indent = 0;
    buildStateInfo(result, state, true, indent);

    for (AtnState parentState = state.getParentState(); parentState != null; parentState = parentState.getParentState()) {
      indent += 2;
      buildStateInfo(result, parentState, false, indent);
    }

    return result.toString();
  }


  public static final AtnState getPriorConstituentState(AtnState atnState, AtnStateUtil.Gravity gravity) {
    AtnState result = null;

    if (atnState != null) {
      final AtnState curParent = atnState.getPushState();
      for (AtnState prevState = atnState.getParentState();
           prevState != null && prevState != curParent;
           prevState = prevState.getParentState()) {

        if (prevState.isPoppedState()) {
          final AtnState curPush = prevState.getPushState();
          if (curPush.getPushState() == curParent) {
            result = AtnStateUtil.adjustForGravity(prevState, gravity);
            break;
          }
        }
        else if (prevState.getPushState() != curParent) continue;

        if (prevState.getMatched()) {
          result = prevState;
          break;
        }
      }
    }

    return result;
  }

  public static final AtnState adjustForGravity(AtnState selectedState, AtnStateUtil.Gravity gravity) {
    AtnState result = selectedState;

    if (!selectedState.getMatched() && selectedState.isPoppedState()) {
      final AtnState pushState = selectedState.getPushState();
      if (pushState != null) {
        switch (gravity) {
          case PUSH :
            result = pushState; break;
          case POP :
            result = selectedState; break;
          case FIRST_MATCH :
            // look backwards from selectedState to pushState for match closest to pushState
            result = findMatchState(selectedState, pushState, true); break;
          case LAST_MATCH :
            // look backwards from selectedState to pushState for match closest to selectedState
            result = findMatchState(selectedState, pushState, false); break;
        }
      }
    }

    return result;
  }

  public static final AtnState findMatchState(AtnState startState, AtnState endState, boolean lastMatch) {
    AtnState result = lastMatch ? endState : startState;

    for (AtnState curState = startState; curState != null && curState != endState; curState = curState.getParentState()) {
      if (curState.getMatched()) {
        result = curState;
        if (!lastMatch) {
          // found first matching state
          break;
        }
        //else continue to end to pick up the last match
      }
    }

    return result;
  }


  public static interface AtnStateVisitor {
    /**
     * Visit the given state.
     *
     * @param atnState  The atn state currently being visited.
     * @param depth  The depth (1-based) of the current state.
     *
     * @return to continue visiting more states or false to halt.
     */
    public boolean visit(AtnState atnState, int depth);
  }
}
