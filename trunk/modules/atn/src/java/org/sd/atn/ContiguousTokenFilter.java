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


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;

/**
 * A generic parameter-driven TokenFilter implementation.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "This org.sd.atn.TokenFilter is applied to each token encountered within its\n" +
       "associated rule and is intended to ensure that each of the rule's tokens\n" +
       "follows the specified matching rules.\n" +
       "\n" +
       "If the 'tokenreg' (optional) matches, the 'innerdelim' (optional) delim test\n" +
       "is applied.\n" +
       "\n" +
       "Note that there are currently limitations to token filtering with respect to\n" +
       "constituents since pushed rules do not inherit token filters and the filter is\n" +
       "applied purely at a token (not constituent) level."
  )
public class ContiguousTokenFilter implements TokenFilter {
  
  private DelimTest delimTest;
  private String tokenreg;
  private Pattern tokenpattern;

  public ContiguousTokenFilter(DomElement domElement, ResourceManager resourceManager) {
    final DomNode innerdelimNode = (DomNode)domElement.selectSingleNode("innerdelim");
    if (innerdelimNode != null) {
      this.delimTest = new DelimTest(true, innerdelimNode, resourceManager);
      this.delimTest.setIgnoreConstituents(true);
    }

    DomNode tokenregNode = (DomNode)domElement.selectSingleNode("tokenreg");
    if (tokenregNode != null) {
      this.tokenreg = tokenregNode.getTextContent();
      this.tokenpattern = Pattern.compile(tokenreg);
    }
  }

  public TokenFilterResult checkToken(Token token, boolean isRevision, Token prevToken, AtnState curState) {
    TokenFilterResult result = TokenFilterResult.ACCEPT;

    if (tokenpattern != null) {
      final Matcher m = tokenpattern.matcher(token.getText());
      if (!m.matches()) {
        result = TokenFilterResult.HALT;
      }
    }

    if (delimTest != null && result == TokenFilterResult.ACCEPT && prevToken != null) {
      if (!delimTest.accept(token, curState).accept()) {
        result = TokenFilterResult.HALT;
      }
    }

    return result;
  }
}
