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

/**
 * Wrapper around an AtnRuleStepTest that reverses its 'accept' logic.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "Wrapper around an org.sd.atn.AtnRuleStepTest that\n" +
       "reverses its 'accept' logic.")
public class ReversedAtnRuleStepTest implements AtnRuleStepTest {
  
  private AtnRuleStepTest test;

  public ReversedAtnRuleStepTest(AtnRuleStepTest test) {
    this.test = test;
  }

  public PassFail accept(Token token, AtnState curState) {
    return test.accept(token, curState).reverse();
  }
}
