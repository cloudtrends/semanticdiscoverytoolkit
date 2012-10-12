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


import org.sd.token.Token;
import org.sd.util.Usage;

/**
 * Interface for testing a token's applicability at a step.
 * <p>
 * Note that implementations referenced from an xml config require a
 * constructor accepting the DomNode in the config that defines the
 * test. For example, if a "test" node has a "jclass" child that gives
 * the java class for an AtnRuleStepTest implementation, the "test"
 * node will be passed into the constructor.
 *
 * @author Spence Koehler
 */
@Usage(notes =
       "Interface for testing a token's applicability at a step.\n" +
       "\n" +
       "Note that implementations referenced from an xml config require a\n" +
       "constructor accepting the DomNode in the config that defines the\n" +
       "test. For example, if a \"test\" node has a \"jclass\" child that gives\n" +
       "the java class for an AtnRuleStepTest implementation, the \"test\"\n" +
       "node will be passed into the constructor."
  )
public interface AtnRuleStepTest {

  /**
   * Determine whether to accept the (matched) state.
   * <p>
   * This is called as a last check on whether a token matches for the current
   * state after its category has been matched to its containing rule step.
   */
  public PassFail accept(Token token, AtnState curState);
  
}
