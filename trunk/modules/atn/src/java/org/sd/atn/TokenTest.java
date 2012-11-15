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
import org.sd.token.Feature;
import org.sd.token.KeyLabel;
import org.sd.token.KeyLabelMatcher;
import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.util.tree.Tree;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.XPath;
import org.sd.xml.XmlLite;
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
       " - when delimMatch='X', test against next or prev only succeeds if delims equal X\n" +
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
       "\n" +
       "   <!-- Additional token-based tests -->\n" +
       "   <wordPattern key='..........' onChange='fail|succeed' prevToken='true|false' curToken='true|false' nextToken='true|false'/>\n" +
       "   <hardBreak onExists='fail|succeed' prevToken='true|false' nextToken='true|false'/>\n" +
       "   <interp onExists='fail|succeed' type='featureKey' xpath='xpath'/>\n" +
       " </test>\n" +
       "\n" +
       " - wordPattern clauses are based on a KeyLabelMatcher with a key that identifies equivalence classes between keyLabels for token words.\n" +
       "   - it will test for pattern changes (or matches) between prevToken and curToken, curToken and nextToken, or prevToken and nextToken.\n" +
       " - hardBreak clauses are based on finding tokenizer hard breaks between the prevToken and curToken and/or curToken and nextToken.\n" +
       " - interp clauses are based on finding an interp with the given featureKey and, optionally, finding the given xpath in the interp on the curToken.\n"
  )
public class TokenTest extends BaseClassifierTest {
  
  private boolean verbose;
  private boolean revise;
  private List<String> classifiers;
  private List<DelimTest> delimTests;
  private List<TokenClause> tokenClauses;

  public TokenTest(DomNode testNode, ResourceManager resourceManager) {
    super(testNode, resourceManager);

    this.verbose = testNode.getAttributeBoolean("verbose", false);
    this.revise = testNode.getAttributeBoolean("revise", false);

    this.classifiers = loadValues(testNode, "classifier", "cat");

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

    final NodeList wordPatternNodes = testNode.selectNodes("wordPattern");
    if (wordPatternNodes != null && wordPatternNodes.getLength() > 0) {
      for (int nodeNum = 0; nodeNum < wordPatternNodes.getLength(); ++nodeNum) {
        final DomElement wpElt = (DomElement)wordPatternNodes.item(nodeNum);
        final WordPatternClause wordPatternClause = new WordPatternClause(wpElt);
        if (tokenClauses == null) tokenClauses = new ArrayList<TokenClause>();
        tokenClauses.add(wordPatternClause);
      }
    }

    final NodeList hardBreakNodes = testNode.selectNodes("hardBreak");
    if (hardBreakNodes != null && hardBreakNodes.getLength() > 0) {
      for (int nodeNum = 0; nodeNum < hardBreakNodes.getLength(); ++nodeNum) {
        final DomElement hbElt = (DomElement)hardBreakNodes.item(nodeNum);
        final HardBreakClause hardBreakClause = new HardBreakClause(hbElt);
        if (tokenClauses == null) tokenClauses = new ArrayList<TokenClause>();
        tokenClauses.add(hardBreakClause);
      }
    }

    final NodeList interpNodes = testNode.selectNodes("interp");
    if (interpNodes != null && interpNodes.getLength() > 0) {
      for (int nodeNum = 0; nodeNum < interpNodes.getLength(); ++nodeNum) {
        final DomElement iElt = (DomElement)interpNodes.item(nodeNum);
        final InterpClause interpClause = new InterpClause(iElt);
        if (tokenClauses == null) tokenClauses = new ArrayList<TokenClause>();
        tokenClauses.add(interpClause);
      }
    }

    //
    // under token node, setup allowed and disallowed tokens
    //
    // options:
    // - when reverse='true', fail on match (handled elsewhere)
    // - when revise='true', test against token revisions
    // - when ignoreLastToken='true', always accept the last token
    // - when ignoreFirstToken='true', always accept the first token
    // - when onlyFirstToken='true', only test against a "first" constituent token
    // - when onlyLastToken='true', only test against a "last" constituent token
    //
    // <test reverse='true|false' revise='true|false'>
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
    //
    //   <!-- Additional token-based tests -->
    //   <wordPattern key='..........' onChange='fail|succeed' prevToken='true|false' curToken='true|false' nextToken='true|false'/>
    //   <hardBreak onExists='fail|succeed' prevToken='true|false' nextToken='true|false'/>
    //   <interp onExists='fail|succeed' type='featureKey' xpath='xpath'/>
    // </test>
    //
    // - wordPattern clauses are based on a KeyLabelMatcher with a key that identifies equivalence classes between keyLabels for token words.
    //   - it will test for pattern changes (or matches) between prevToken and curToken, curToken and nextToken, or prevToken and nextToken.
    // - hardBreak clauses are based on finding tokenizer hard breaks between the prevToken and curToken and/or curToken and nextToken.
    // - interp clauses are based on finding an interp with the given featureKey and, optionally, finding the given xpath in the interp on the curToken.\n"
    //
  }
			
  protected void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  protected boolean isVerbose() {
    return verbose;
  }

  protected boolean doAccept(Token token, AtnState curState) {
    boolean result = false;

    boolean verifyAdditional = true;
    final Token orig = token;

    for (; token != null; token = revise ? token.getRevisedToken() : null) {
      if (!result && roteListClassifier != null) {
        verifyAdditional = result = roteListClassifier.doClassify(token);

        if (result && verbose) {
          System.out.println("\troteListClassifier.classify=true");
        }
      }

      if (!result && regexClassifier != null) {
        verifyAdditional = result = regexClassifier.doClassify(token);

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

                verifyAdditional = result = true;
                break;
              }
            }
          }
          else {
            // check for literal grammar token match
            if (!grammar.getCat2Rules().containsKey(cat)) {
              // use an "identity" classifier for literal grammar tokens
              verifyAdditional = result = cat.equals(token.getText());

              if (result && verbose) {
                System.out.println("\tgrammarRule(" + cat + ").classify=true");
              }
            }

            // check for a token feature that matches the category
            if (!result) {
              verifyAdditional = result = token.getFeature(cat, null) != null;

              if (result && verbose) {
                System.out.println("\ttokenFeature(" + cat + "," + token.getFeature(cat, null) + ").classify=true");
              }
            }
          }
          if (result) break;
        }
      }

      if (verifyAdditional && hasAdditionalChecks()) {
        verifyAdditional = result = doAdditionalChecks(token, curState);
        if (verbose) {
          System.out.println("\tTokenTest.doAdditionalCheck(" + token + ")=" + result);
        }
      }

      if (result) break;
    }

    if (result && delimTests != null && token != null) {
      for (DelimTest delimTest : delimTests) {
        verifyAdditional = result = delimTest.accept(token, curState).accept();
        if (!result) {
          if (verbose) {
            System.out.println("\tdelimTest(pre=" + delimTest.isPre() + ") FAILED! token=" + token + " delims=" +
                               (delimTest.isPre() ? token.getPreDelim() : token.getPostDelim()));
          }
          break;
        }
      }
    }

    if (verifyAdditional && tokenClauses != null) {
      for (TokenClause tokenClause : tokenClauses) {
        verifyAdditional = result = tokenClause.accept(orig, verbose);

        if (result && verbose) {
          System.out.println("\ttokenClause.accept(" + orig + ")=" + result);
        }

        if (result) break;
      }
    }

    return result;
  }

  /**
   * Method for overriding by extenders (pseudo-abstract).
   * <p>
   * If extending TokenTest and overriding doAdditionalChecks, this must return true.
   */
  protected boolean hasAdditionalChecks() {
    return false;
  }

  /**
   * Method for overriding by extenders (pseudo-abstract).
   * <p>
   * Extenders can put additional checks on the token(s) here.
   */
  protected boolean doAdditionalChecks(Token token, AtnState curState) {
    return true;
  }

  private static final Token getPrevToken(Token token) {
    Token result = null;

    if (token.getStartIndex() == 0) return result;  // there is no prev token

    try {
      result = token.getPrevToken();
    }
    catch (Exception e) {
      // leave result as null
    }

    return result;
  }


  private static abstract class TokenClause {
    
    protected abstract boolean doCompare(Token token1, Token token2, boolean verbose);


    protected boolean succeedOnTest;
    protected boolean prevToken;
    protected boolean curToken;
    protected boolean nextToken;
    protected boolean forwardLogic;

    protected TokenClause(DomElement elt, String testAtt, boolean forwardLogic) {
      this.succeedOnTest = elt.getAttributeValue(testAtt, "fail").equals("succeed");
      this.prevToken = elt.getAttributeBoolean("prevToken", true);
      this.curToken = elt.getAttributeBoolean("curToken", true);
      this.nextToken = elt.getAttributeBoolean("nextToken", true);
      this.forwardLogic = forwardLogic;
    }

    public final boolean accept(Token token, boolean verbose) {
      boolean result = true;

      if (token != null) {

        if (!prevToken && !nextToken) {
          result = accept(token, token, verbose);
        }
        else {
          final Token pToken = prevToken ? getPrevToken(token) : null;
          final Token nToken = nextToken ? token.getNextToken() : null;

          if (pToken != null) {
            if (curToken) {
              result = accept(pToken, token, verbose);
            }
            else if (nToken != null) {
              result = accept(pToken, nToken, verbose);
            }
          }

          if (result && nToken != null) {
            result = accept(token, nToken, verbose);
          }
        }
      }

      return result;
    }

    private final boolean accept(Token token1, Token token2, boolean verbose) {
      final boolean accept = doCompare(token1, token2, verbose);
      //return (succeedOnTest && accept) || (!succeedOnTest && !accept);
      return forwardLogic ? succeedOnTest == accept : succeedOnTest != accept;
    }
  }

  private static final class WordPatternClause extends TokenClause {
    
    private KeyLabelMatcher matcher;

    public WordPatternClause(DomElement wpElt) {
      super(wpElt, "onChange", false);

      final String pattern = wpElt.getAttributeValue("key");
      this.matcher = new KeyLabelMatcher(pattern);
    }

    protected boolean doCompare(Token token1, Token token2, boolean verbose) {
      final KeyLabel[] labels1 = token1.getKeyLabels();
      final KeyLabel[] labels2 = token2.getKeyLabels();

      // return true if there is a change
      final boolean result = matcher.matches(labels1, labels2);

      if (verbose) {
        System.out.println("\t" + token1 + " " + asString(labels1));
        System.out.println("\t" + token2 + " " + asString(labels2));
      }

      return result;
    }

    private final String asString(KeyLabel[] labels) {
      final StringBuilder result = new StringBuilder();

      result.append('{');
      for (KeyLabel label : labels) {
        result.append(label.getDefaultChar());
      }
      result.append('}');

      return result.toString();
    }
  }

  private static final class HardBreakClause extends TokenClause {

    public HardBreakClause(DomElement hbElt) {
      super(hbElt, "onExists", false);
      super.curToken = true;  // always compare against current
    }

    protected boolean doCompare(Token token1, Token token2, boolean verbose) {
      // return true if a hard break exists
      return token2.followsHardBreak();
    }
  }

  private static final class InterpClause extends TokenClause {

    private String type;
    private XPath xpath;

    public InterpClause(DomElement iElt) {
      super(iElt, "onExists", true);

      this.type = iElt.getAttributeValue("type", null);
      this.xpath = null;

      final String xpathString = iElt.getAttributeValue("xpath", null);
      if (xpathString != null) {
        this.xpath = new XPath(xpathString);
      }

      super.prevToken = false;
      super.curToken = true;  // always compare against current
      super.nextToken = false;
    }

    protected boolean doCompare(Token token1, Token token2, boolean verbose) {
      boolean result = false;

      // return true if the interp (w/xpath) exists

      if (type != null && token1.hasFeatures()) {
        final List<Feature> features = token1.getFeatures(type, null, ParseInterpretation.class);
        if (features != null) {
          for (Feature feature : features) {
            final ParseInterpretation interp = (ParseInterpretation)feature.getValue();
            if (interp != null) {
              if (xpath != null) {
                final Tree<XmlLite.Data> interpTree = interp.getInterpTree();
                if (interpTree != null) {
                  final List<Tree<XmlLite.Data>> matches = xpath.getNodes(interpTree);

                  if (verbose) {
                    System.out.println("\t" + token1 + " '" + type + "' interp:\n" + interpTree);
                    System.out.println("\t\tmatches:\n" + matches);
                  }

                  if (matches != null) {
                    // has interp w/specified xpath
                    result = true;
                    break;
                  }
                }
              }
              else {
                // has interp (no xpath specified)

                if (verbose) {
                  System.out.println("\t" + token1 + " '" + type + "' interp found.");
                }

                result = true;
                break;
              }
            }
          }
        }
      }

      return result;
    }
  }
}
