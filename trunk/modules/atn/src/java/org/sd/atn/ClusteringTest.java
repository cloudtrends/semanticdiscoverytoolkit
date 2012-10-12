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


import org.sd.token.Token;
import org.sd.util.Usage;
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
@Usage(notes =
       "An org.sd.atn.AtnRuleStepTest for ensuring that category matches are\n" +
       "clustered within a constituent.\n" +
       "\n" +
       "This test helps to limit \"spurious\" parses in cases where constituents\n" +
       "with a repeating category are repeated grammatically, but consecutive\n" +
       "categorical matches should remain in a single constituent.\n" +
       "\n" +
       "For example, consider the sequence \"C C C\" and the grammar:\n" +
       "    A <- B+\n" +
       "    B <- C+\n" +
       "\n" +
       "Then potential parses are:\n" +
       "  1: (A (B C C C))\n" +
       "  2: (A (B C) (B C C))\n" +
       "  3: (A (B C) (B C) (B C))\n" +
       "  4: (A (B C C) (B C))\n" +
       "\n" +
       "This test ensures that only parse #1 is valid when interpreting \"C C C\"."
  )
public class ClusteringTest implements AtnRuleStepTest {
  
  public ClusteringTest() {
  }

  public PassFail accept(Token token, AtnState curState) {
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

    return PassFail.getInstance(result);
  }
}
