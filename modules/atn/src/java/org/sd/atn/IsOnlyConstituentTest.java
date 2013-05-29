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
import org.sd.xml.DomNode;

/**
 * A rule to test whether there is another token in the input.
 *
 * @author Spence Koehler
 */
@Usage(notes =
       "An org.sd.atn.BaseClassifierTest implementation that succeeds\n" +
       "for a token when it is the only token in the input."
  )
public class IsOnlyConstituentTest extends BaseClassifierTest {
  
  public IsOnlyConstituentTest(DomNode testNode, ResourceManager resourceManager) {
    super(testNode, resourceManager);
  }
			
  protected boolean doAccept(Token token, AtnState curState) {
    boolean result = (token.getNextToken() == null);

    if (result) {
      // this is the last token, succeed if there is no input prior to the constituent
      final AtnState cStart = curState.isPoppedState() ? curState.getPushState() : curState;
      if (cStart != null) {
        result = (cStart.getInputToken().getStartIndex() == 0);
      }
    }

    return result;
  }
}
