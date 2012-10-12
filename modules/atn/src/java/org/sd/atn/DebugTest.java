/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
import org.sd.xml.DomNode;

/**
 * A special rule test for parser debugging.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "A special org.sd.atn.AtnRuleStepTest purely for parser debugging\n" +
       "that always returns a true or false result."
  )
public class DebugTest implements AtnRuleStepTest {
  
  private DomNode testNode;
  private ResourceManager resourceManager;
  private boolean result;

  public DebugTest(DomNode testNode, ResourceManager resourceManager) {
    this.testNode = testNode;
    this.resourceManager = resourceManager;

    this.result = testNode.getAttributeBoolean("result", true);
  }
			
  public PassFail accept(Token token, AtnState curState) {
    boolean result = this.result;

    final boolean stopHere = true;

    return PassFail.getInstance(result);
  }
}
