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
import java.util.List;
import org.sd.token.Token;

/**
 * Utility class to contain and execute multiple tests.
 * <p>
 * @author Spence Koehler
 */
public class AtnRuleStepTestPipeline implements AtnRuleStepTest {
  
  private List<AtnRuleStepTest> tests;

  public AtnRuleStepTestPipeline() {
    this.tests = new ArrayList<AtnRuleStepTest>();
  }

  public void add(AtnRuleStepTest test) {
    tests.add(test);
  }

  /**
   * Determine whether to accept the (matched) state.
   * <p>
   * This is called as a last check on whether a token matches for the current
   * state after its category has been matched to its containing rule step.
   */
  public PassFail accept(Token token, AtnState curState) {
    boolean result = true;

    boolean allAreNA = true;
    for (AtnRuleStepTest test : tests) {
      final PassFail passFail = test.accept(token, curState);
      result = passFail.accept();

      if (!result) {
        break;
      }
      else {
        if (passFail != PassFail.NOT_APPLICABLE) {
          allAreNA = false;
        }
      }
    }

    return PassFail.getInstance(result, allAreNA);
  }
}
