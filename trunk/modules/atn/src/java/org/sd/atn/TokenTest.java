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
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;

/**
 * A rule test tokens through RoteList- and/or Regex- Classifiers.
 * <p>
 * @author Spence Koehler
 */
public class TokenTest implements AtnRuleStepTest {
  
  private RoteListClassifier roteListClassifier;
  private RegexClassifier regexClassifier;
  private boolean next;
  private boolean revise;

  public TokenTest(DomNode testNode, ResourceManager resourceManager) {
    this.roteListClassifier = new RoteListClassifier((DomElement)testNode, resourceManager, resourceManager.getId2Normalizer());
    this.regexClassifier = new RegexClassifier((DomElement)testNode, resourceManager, resourceManager.getId2Normalizer());

    if (roteListClassifier.isEmpty()) roteListClassifier = null;
    if (regexClassifier.isEmpty()) regexClassifier = null;

    this.next = testNode.getAttributeBoolean("next", false);
    this.revise = testNode.getAttributeBoolean("revise", false);

    // under token node, setup allowed and disallowed tokens
    //
    // options:
    // - when reverse='true', fail on match (handled elsewhere)
    // - when next='true', test against the next token
    // - when revise='true', test against token revisions
    //
    // <test reverse='true|false' next='true|false' revise='true|false'>
    //   <jclass>org.sd.atn.TokenTest</jclass>
    //   <terms caseSensitive='true|false'>
    //     <term>...</term>
    //     ...
    //   </terms>
    //   <regexes>
    //     <regex type='...' groupN='...'>...</regex>
    //   </regexes>
    // </test>

  }
			
  public boolean accept(Token token, AtnState curState) {
    boolean result = false;

    if (next) {
      token = token.getNextToken();
    }

    for (; token != null; token = revise ? token.getRevisedToken() : null) {
      if (!result && roteListClassifier != null) {
        result = roteListClassifier.doClassify(token);
      }

      if (!result && regexClassifier != null) {
        result = regexClassifier.doClassify(token);
      }
    }

    return result;
  }
}
