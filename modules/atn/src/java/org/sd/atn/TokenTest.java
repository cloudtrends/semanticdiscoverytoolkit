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
       " - when prev='true', test against the prior token (taken as smallest prior if not available through state)\n" +
       " - when revise='true', test against token revisions\n" +
       " - when ignoreLastToken='true', always accept the last token\n" +
       " - when ignoreFirstToken='true', always accept the first token\n" +
       " - when onlyFirstToken='true', only test against a \"first\" constituent token\n" +
       " - when onlyLastToken='true', only test against a \"last\" constituent token\n" +
       " \n" +
       " <test reverse='true|false' next='true|false' prev='true|false' revise='true|false' verbose='true|false'>\n" +
       "   <jclass>org.sd.atn.TokenTest</jclass>\n" +
       "   <terms caseSensitive='true|false'>\n" +
       "     <term>...</term>\n" +
       "     ...\n" +
       "   </terms>\n" +
       "   <regexes>\n" +
       "     <regex type='...' groupN='...'>...</regex>\n" +
       "   </regexes>\n" +
       "   <classifier cat='...'/>\n" +
       "\n" +
       "   <!-- NOTE: delims are applied only if something else succeeds -->\n" +
       "   <predelim>...</predelim>\n" +
       "   <postdelim>...</postdelim>\n" +
       " </test>"
  )
public class TokenTest extends BaseClassifierTest {
  
  private boolean verbose;
  private boolean next;
  private boolean prev;
  private boolean revise;
  private List<String> classifiers;
  private List<DelimTest> delimTests;

  public TokenTest(DomNode testNode, ResourceManager resourceManager) {
    super(testNode, resourceManager);

    this.verbose = testNode.getAttributeBoolean("verbose", false);
    this.next = testNode.getAttributeBoolean("next", false);
    this.prev = testNode.getAttributeBoolean("prev", false);
    this.revise = testNode.getAttributeBoolean("revise", false);

    final NodeList classifierNodes = testNode.selectNodes("classifier");
    if (classifierNodes != null && classifierNodes.getLength() > 0) {
      this.classifiers = new ArrayList<String>();
      for (int nodeNum = 0; nodeNum < classifierNodes.getLength(); ++nodeNum) {
        final DomElement classifierElement = (DomElement)classifierNodes.item(nodeNum);
        final String cat = classifierElement.getAttributeValue("cat");
        classifiers.add(cat);
      }
    }

    final NodeList preDelimNodes = testNode.selectNodes("predelim");
    if (preDelimNodes != null && preDelimNodes.getLength() > 0) {
      for (int nodeNum = 0; nodeNum < preDelimNodes.getLength(); ++nodeNum) {
        final DomElement delimElement = (DomElement)preDelimNodes.item(nodeNum);
        final DelimTest delimTest = new DelimTest(true, delimElement, resourceManager);
        if (delimTests == null) delimTests = new ArrayList<DelimTest>();
        delimTests.add(delimTest);
      }
    }

    final NodeList postDelimNodes = testNode.selectNodes("postdelim");
    if (postDelimNodes != null && postDelimNodes.getLength() > 0) {
      for (int nodeNum = 0; nodeNum < postDelimNodes.getLength(); ++nodeNum) {
        final DomElement delimElement = (DomElement)postDelimNodes.item(nodeNum);
        final DelimTest delimTest = new DelimTest(false, delimElement, resourceManager);
        if (delimTests == null) delimTests = new ArrayList<DelimTest>();
        delimTests.add(delimTest);
        delimTest.setIgnoreConstituents(true);
      }
    }

    //
    // under token node, setup allowed and disallowed tokens
    //
    // options:
    // - when reverse='true', fail on match (handled elsewhere)
    // - when next='true', test against the next token
    // - when prev='true', test against the prior token (taken as smallest prior if not available through state)\n" +
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
    //
    //   <!-- NOTE: delims are applied only if something else succeeds -->
    //   <predelim>...</predelim>
    //   <postdelim>...</postdelim>
    // </test>

  }
			
  protected boolean doAccept(Token token, AtnState curState) {
    boolean result = false;

    if (next) {
      if (verbose) {
        System.out.println("TokenTest token=" + token);
      }

      token = token.getNextToken();

      if (verbose) {
        System.out.println("\tnext token=" + token);
      }
    }
    else if (prev) {
      if (verbose) {
        System.out.println("TokenTest token=" + token);
      }

      token = getPrevToken(token, curState);

      if (verbose) {
        System.out.println("\tprev token=" + token);
      }
    }

    for (; token != null; token = revise ? token.getRevisedToken() : null) {
      if (!result && roteListClassifier != null) {
        result = roteListClassifier.doClassify(token);

        if (result && verbose) {
          System.out.println("\troteListClassifier.classify=true");
        }
      }

      if (!result && regexClassifier != null) {
        result = regexClassifier.doClassify(token);

        if (result && verbose) {
          System.out.println("\tregexClassifier.classify=true");
        }
      }

      if (!result && classifiers != null) {
        for (String cat : classifiers) {
          final AtnGrammar grammar = curState.getRule().getGrammar();
          final List<AtnStateTokenClassifier> tokenClassifiers = grammar.getClassifiers(cat);
          if (tokenClassifiers != null) {
            for (AtnStateTokenClassifier tokenClassifier : tokenClassifiers) {
              final MatchResult matchResult = tokenClassifier.classify(token, curState);
              if (matchResult.matched()) {

                if (verbose) {
                  System.out.println("\tclassifier(" + cat + ").classify=true");
                }

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

              if (result && verbose) {
                System.out.println("\tgrammarRule(" + cat + ").classify=true");
              }
            }

            // check for a token feature that matches the category
            if (!result) {
              result = token.getFeature(cat, null) != null;

              if (result && verbose) {
                System.out.println("\ttokenFeature(" + cat + "," + token.getFeature(cat, null) + ").classify=true");
              }
            }
          }
          if (result) break;
        }
      }

      if (result) break;
    }

    if (result && delimTests != null && token != null) {
      for (DelimTest delimTest : delimTests) {
        result = delimTest.accept(token, curState);
        if (!result) {
          if (verbose) {
            System.out.println("\tdelimTest(pre=" + delimTest.isPre() + ") FAILED! token=" + token + " delims=" +
                               (delimTest.isPre() ? token.getPreDelim() : token.getPostDelim()));
          }
          break;
        }
      }
    }

    return result;
  }

  private final Token getPrevToken(Token token, AtnState curState) {
    Token result = null;

    if (token.getStartIndex() == 0) return result;  // there is no prev token

    try {
      result = token.getPrevToken();
    }
    catch (Exception e) {
      if (verbose) {
        System.out.println("Can't compute prevToken(" + token + ")! " + e);
      }
    }

    return result;
  }
}
