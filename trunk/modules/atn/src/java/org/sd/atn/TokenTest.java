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


import java.util.ArrayList;
import java.util.List;
import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.w3c.dom.NodeList;

/**
 * A rule to test tokens through RoteList-, Regex- and/or Token- Classifiers.
 * <p>
 * Note that this tests the last single token seen, not for example the full
 * token text of a constituent that has been matched. For testing against the
 * full text of a constituent, see TextTest instead.
 *
 * @author Spence Koehler
 */
@Usage(notes =
       "An org.sd.atn.BaseClassifierTest to evaluate tokens through\n" +
       "org.sd.atn.RoteListClassifier, org.sd.atn.RegexClassifier and/or\n" +
       "named org.sd.atn.AtnStateTokenClassifier instances.\n" +
       "\n" +
       "Note that this tests the last single token seen, not for example the full\n" +
       "token text of a constituent that has been matched. For testing against the\n" +
       "full text of a constituent, see TextTest instead.\n" +
       " \n" +
       " under token node, setup allowed and disallowed tokens\n" +
       " \n" +
       " options:\n" +
       " - when reverse='true', fail on match (handled elsewhere)\n" +
       " - when next='true', test against the next token\n" +
       " - when revise='true', test against token revisions\n" +
       " - when ignoreLastToken='true', always accept the last token\n" +
       " - when ignoreFirstToken='true', always accept the first token\n" +
       " - when onlyFirstToken='true', only test against a \"first\" constituent token\n" +
       " - when onlyLastToken='true', only test against a \"last\" constituent token\n" +
       " \n" +
       " <test reverse='true|false' next='true|false' revise='true|false'>\n" +
       "   <jclass>org.sd.atn.TokenTest</jclass>\n" +
       "   <terms caseSensitive='true|false'>\n" +
       "     <term>...</term>\n" +
       "     ...\n" +
       "   </terms>\n" +
       "   <regexes>\n" +
       "     <regex type='...' groupN='...'>...</regex>\n" +
       "   </regexes>\n" +
       "   <classifier cat='...'/>\n" +
       " </test>"
  )
public class TokenTest extends BaseClassifierTest {
  
  private boolean next;
  private boolean revise;
  private List<String> classifiers;

  public TokenTest(DomNode testNode, ResourceManager resourceManager) {
    super(testNode, resourceManager);

    this.next = testNode.getAttributeBoolean("next", false);
    this.revise = testNode.getAttributeBoolean("revise", false);

    final NodeList classifierNodes = testNode.selectNodes("classifier");
    if (classifierNodes != null) {
      this.classifiers = new ArrayList<String>();
      for (int nodeNum = 0; nodeNum < classifierNodes.getLength(); ++nodeNum) {
        final DomElement classifierElement = (DomElement)classifierNodes.item(nodeNum);
        final String cat = classifierElement.getAttributeValue("cat");
        classifiers.add(cat);
      }
    }

    //
    // under token node, setup allowed and disallowed tokens
    //
    // options:
    // - when reverse='true', fail on match (handled elsewhere)
    // - when next='true', test against the next token
    // - when revise='true', test against token revisions
    // - when ignoreLastToken='true', always accept the last token
    // - when ignoreFirstToken='true', always accept the first token
    // - when onlyFirstToken='true', only test against a "first" constituent token
    // - when onlyLastToken='true', only test against a "last" constituent token
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
    //   <classifier cat='...'/>
    // </test>

  }
			
  protected boolean doAccept(Token token, AtnState curState) {
    boolean result = false;

    if (next) {
      token = token.getNextToken();
    }

    for (; !result && token != null; token = revise ? token.getRevisedToken() : null) {
      if (!result && roteListClassifier != null) {
        result = roteListClassifier.doClassify(token);
      }

      if (!result && regexClassifier != null) {
        result = regexClassifier.doClassify(token);
      }

      if (!result && classifiers != null) {
        for (String cat : classifiers) {
          final AtnGrammar grammar = curState.getRule().getGrammar();
          final List<AtnStateTokenClassifier> tokenClassifiers = grammar.getClassifiers(cat);
          if (tokenClassifiers != null) {
            for (AtnStateTokenClassifier tokenClassifier : tokenClassifiers) {
              final MatchResult matchResult = tokenClassifier.classify(token, curState);
              if (matchResult.matched()) {
                result = true;
                break;
              }
            }
          }
          else {
            // check for literal grammar token match
            if (!grammar.getCat2Rules().containsKey(cat)) {
              // use an "identity" classifier for literal grammar tokens
              result = cat.equals(token.getText());
            }

            // check for a token feature that matches the category
            if (!result) {
              result = token.getFeature(cat, null) != null;
            }
          }
          if (result) break;
        }
      }
    }

    return result;
  }
}
