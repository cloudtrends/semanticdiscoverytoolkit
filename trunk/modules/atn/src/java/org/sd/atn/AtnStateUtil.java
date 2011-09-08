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
import java.util.LinkedList;
import java.util.List;
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
  
  // parse node attributes

  public static String TOKEN_KEY = "cToken";     // -> cToken:CategorizedToken (leafs' parents)
  public static String RULE_ID_KEY = "_ruleID";  // -> ruleID:String (id'd constituents)
  public static String INTERP_KEY = "_interps";  // -> interps:ParseInterpretation[] (pre-parsed constituents)

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

        // add ruleID as an attribute on the parse node
        final String ruleId = pathState.getRule().getRuleId();
        if (ruleId != null) {
          result.getAttributes().put(RULE_ID_KEY, ruleId);
        }

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
            // add ruleID as an attribute on the parse node
            if (!curResultNode.hasAttributes() || !curResultNode.getAttributes().containsKey(RULE_ID_KEY)) {
              final String ruleId = pathState.getRule().getRuleId();
              if (ruleId != null) {
                curResultNode.getAttributes().put(RULE_ID_KEY, ruleId);
              }
            }

            curResultNode = curResultNode.addChild(category);
          }
        }
        else {
          // add matched token
          final Tree<String> categoryNode = curResultNode.addChild(category);

          if (!category.equals(inputToken.getText())) {
            // add non-literal matched token
            final List<Tree<String>> tokenParses = goDeep ? getTokenParses(inputToken, category) : null;
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

  /**
   * Get the first state of the state's constituent.
   */
  public static final AtnState getConstituentStartState(AtnState curState) {
    AtnState result = null;

    final AtnState refPush = curState.getPushState();
    for (result = curState; result != null; result = result.getParentState()) {
      final AtnState curPush = result.getPushState();
      if (curPush == refPush && curPush == result.getParentState()) break;
    }

    return result;
  }

  /**
   * Get the 'match' states for the constituent ending at endState.
   */
  public static final LinkedList<AtnState> getConstituentMatchStates(AtnState endState) {
    final LinkedList<AtnState> result = new LinkedList<AtnState>();

    final AtnState refPush = endState.getPushState();
    for (AtnState curState = endState; curState != null; curState = curState.getParentState()) {
      if (curState.getMatched()) {
        result.add(0, curState);
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

  public static final boolean matchesCategory(AtnState atnState, String category) {
    return matchesCategory(atnState, category, null);
  }

  public static final boolean matchesCategory(AtnState atnState, String category, int[] levelDiff) {
    boolean result = false;

    if (atnState.getMatched()) {
      int popCount = 0;
      for (; !result && atnState != null; atnState = atnState.getPushState()) {
        result = category.equals(atnState.getRuleStep().getCategory());
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
  public static final AtnState findPriorMatch(AtnState atnState, String category, int[] levelDiff) {
    AtnState result = null;

    for (AtnState prevState = getLastMatchingState(atnState, levelDiff); prevState != null; prevState = getLastMatchingState(prevState, levelDiff)) {
      if (matchesCategory(prevState, category, levelDiff)) {
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
        for (Feature parseFeature : parseFeatures) {
          final ParseInterpretation interp = (ParseInterpretation)parseFeature.getValue();
          final AtnParse atnParse = interp.getSourceParse();
          if (atnParse != null && atnParse.getSelected()) {
            if (result == null) result = new ArrayList<Tree<String>>();
            final Tree<String> deepParseTree = atnParse.getParse().getParseTree();
            result.add(deepParseTree);

            reconcileDeepTokens(token, deepParseTree);
          }
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
      if (cToken != null && cToken.token.getTokenizer() != baseToken.getTokenizer()) {
        final int baseStart = baseToken.getStartIndex();
        final int startIndex = baseStart + cToken.token.getStartIndex();
        final int endIndex = baseStart + cToken.token.getEndIndex();
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
