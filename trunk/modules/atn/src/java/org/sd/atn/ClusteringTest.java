package org.sd.atn;


import org.sd.token.Token;
import org.sd.util.tree.Tree;

/**
 * A rule test for ensuring that category matches are clustered within a
 * constituent.
 * <p>
 * This test helps to limit "spurious" parses in cases where constituents
 * with a repeating category are repeated grammatically, but consecutive
 * categorical matches should remain in a single constituent.
 * <p>
 * For example, consider the sequence "C C C" and the grammar:
 * <ul>
 * <li>A =- B+</li>
 * <li>B =- C+</li>
 * </ul>
 * Then potential parses are:
 * <ol>
 * <li>(A (B C C C))</li>
 * <li>(A (B C) (B C C))</li>
 * <li>(A (B C) (B C) (B C))</li>
 * <li>(A (B C C) (B C))</li>
 * </ol>
 * This test ensures that only parse #1 is valid when interpreting "C C C".
 * 
 * @author Spence Koehler
 */
public class ClusteringTest implements AtnRuleStepTest {
  
  public ClusteringTest() {
  }

  public boolean accept(Token token, AtnState curState) {
    boolean result = true;

    // Algorithm:
    // - Walk up the state tree to find the last matched token's category.
    //   - If the last matched token's category matches the current matched token category
    //     - If the match immediately precedes the current match, accept the current token match
    //     - Else, reject the current token match
    //   - Otherwise (category mismatch) accept the current token match

    int distance = 1;
    for (Tree<AtnState> parentStateNode = curState.getParentStateNode();
         parentStateNode != null && parentStateNode.getData() != null;
         parentStateNode = parentStateNode.getParent()) {

      final AtnState parentState = parentStateNode.getData();
      if (parentState.getMatched()) {
        final String clusterCategory = curState.getRuleStep().getCategory();
        final String matchedCategory = parentState.getRuleStep().getCategory();
        final boolean categoryMatches = clusterCategory.equals(matchedCategory);

        if (categoryMatches) {
          result = (distance == 1);
        }
        // otherwise, accept the token (result = true)

        break;  // time to go!
      }
      ++distance;
    }

    return result;
  }
}
