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
import org.sd.xml.DomNode;

/**
 * A rule test intended to be used as a popTest for ensuring that a constituent
 * does not end (and/or start) with a bracket unless it starts (and/or ends)
 * with a bracket. This is used to ensure bracketed text is parsed as a
 * separate constituent even when it may otherwise appear to belong with its
 * preceding (or following) text.
 * 
 * @author Spence Koehler
 */
public class BracketPopTest implements AtnRuleStepTest {
  
  private boolean rejectEnd;
  private boolean rejectStart;

  /**
   * 'test' attributes: rejectEnd (default=true), rejectStart (default=true)
   * <ul>
   * <li>rejectEnd rejects the pop if the consistuent ends with a bracket not
   *     fully encompassing the constituent</li>
   * <li>rejectStart rejects the pop if the consistuent starts with a bracket
   *     not fully encompassing the constituent</li>
   */
  public BracketPopTest(DomNode testNode, ResourceManager resourceManager) {
    this.rejectEnd = testNode.getAttributeBoolean("rejectEnd", true);
    this.rejectStart = testNode.getAttributeBoolean("rejectStart", true);
  }

  public boolean accept(Token token, AtnState curState) {
    boolean result = true;

    boolean verified = false;
    final AtnState startState = AtnStateUtil.getConstituentStartState(curState);

    if (rejectEnd && startState != null) {
      final AtnGrammar.Bracket endBracket = curState.getRule().getGrammar().findEndBracket(token);
      if (endBracket != null) {  // the constituent ends in a bracket
        // only accept if the corresponding startBracket exists at the start of the constituent
        result = endBracket.matchesStart(startState.getInputToken());
        verified = result;
      }
    }

    if (result && rejectStart && !verified && startState != null) {
      final Token startToken = startState.getInputToken();
      final AtnGrammar.Bracket startBracket = startState.getRule().getGrammar().findStartBracket(startToken);
      if (startBracket != null) { // the constituent starts in a bracket
        // only accept if the corresponding endBracket exists at the end of the constituent (curState)
        result = startBracket.matchesEnd(token);
      }
    }

    return result;
  }
}
