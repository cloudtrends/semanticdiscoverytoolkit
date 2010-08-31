package org.sd.atn;


import java.util.LinkedList;
import org.sd.token.CategorizedToken;
import org.sd.token.Token;
import org.sd.util.tree.Tree;

/**
 * Utilities for working with AtnState instances and trees.
 * <p>
 * @author Spence Koehler
 */
public class AtnStateUtil {
  
  public static String TOKEN_KEY = "cToken";


  /**
   * Convert the state (automaton) path to a parse tree.
   * <p>
   * Note that terminal nodes in the tree will hold token text while their
   * parent nodes will hold the parsed categories. The immediate parents of
   * terminal nodes will have an attribute mapping TOKEN_KEY to a
   * CategorizedToken for the text.
   */
  public static Tree<String> convertToTree(Tree<AtnState> stateNode) {
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
            categoryNode.addChild(pathState.getInputToken().getText());
            categoryNode.getAttributes().put(TOKEN_KEY, new CategorizedToken(pathState.getInputToken(), category));
          }
        }

        lastPushState = pushState;
      }
    }

    return result;
  }

  /**
   * Utility to retrieve the categorized token stored in the parse tree for
   * the node. Note that for nodes other than terminal nodes and their
   * immediate parents, this will always return null.
   */
  public static final CategorizedToken getCategorizedToken(Tree<String> parseTreeNode) {
    CategorizedToken result = null;

    if (!parseTreeNode.hasChildren()) parseTreeNode = parseTreeNode.getParent();
    if (parseTreeNode.hasAttributes()) {
      result = (CategorizedToken)(parseTreeNode.getAttributes().get(TOKEN_KEY));
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
