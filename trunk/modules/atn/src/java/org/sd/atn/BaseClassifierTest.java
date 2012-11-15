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
import org.sd.util.range.IntegerRange;
import org.sd.util.Usage;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A base rule step test that uses RoteList- and Regex- Classifiers.
 * classifiers to determine pass/fail.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "An abstract base class implementation of org.sd.atn.AtnRuleStepTest\n" +
       "that sets up the following environment for tests:\n" +
       "\n" +
       "options:\n" +
       " - when reverse='true', fail on match (handled elsewhere)\n" +
       " - when ignoreLastToken='true', always succeed on last (full input) token\n" +
       " - when ignoreFirstToken='true', always succeed on first (full input) token\n" +
       " - when onlyFirstToken='true', only test against a \"first\" constituent token\n" +
       " - when onlyLastToken='true', only test against a \"last\" (full input) token\n" +
       " - when validTokenLength is specified, only succeed when the token's length is within the specified integer range\n" +
       " - when repeatCheck is used, the test is only applied to the defined repeats\n" +
       " \n" +
       " <test reverse='true|false' ignoreLastToken='true|false' ignoreLastToken='true|false' onlyFirstToken='true|false' onlyLastToken='true|false' validTokenLength='integerRangeExpression'>\n" +
       "   <repeatCheck type='ignore|test|fail'>integer-range-expression</repeatCheck>\n" +
       "   <jclass>org.sd.atn.*Test</jclass>\n" +
       "   <terms caseSensitive='true|false'>\n" +
       "     <term>...</term>\n" +
       "     ...\n" +
       "   </terms>\n" +
       "   <regexes>\n" +
       "     <regex type='...' groupN='...'>...</regex>\n" +
       "   </regexes>\n" +
       " </test>"
  )
public abstract class BaseClassifierTest implements AtnRuleStepTest {
  
  public enum TestResult { SOFT_SUCCESS, SOFT_FAILURE, HARD_SUCCESS, HARD_FAILURE };


  protected String id;
  protected RoteListClassifier roteListClassifier;
  protected RegexClassifier regexClassifier;
  protected boolean next;
  protected boolean prev;
  protected String delimMatch;
  protected boolean verbose;
  protected boolean ignoreLastToken;
  protected boolean ignoreFirstToken;
  protected boolean onlyFirstToken;
  protected boolean onlyLastToken;
  protected IntegerRange validTokenLength;
  private boolean reverse;
  private final boolean success;
  private final boolean failure;

  protected IntegerRange ignoreRepeatRange;
  protected IntegerRange failRepeatRange;
  protected IntegerRange testRepeatRange;

  private static int nextAutoId = 0;

  public BaseClassifierTest(DomNode testNode, ResourceManager resourceManager) {
    this.id = testNode.getAttributeValue("id", Integer.toString(nextAutoId++));

    this.roteListClassifier = new RoteListClassifier((DomElement)testNode, resourceManager, resourceManager.getId2Normalizer());
    this.regexClassifier = new RegexClassifier((DomElement)testNode, resourceManager, resourceManager.getId2Normalizer());

    if (roteListClassifier.isEmpty()) roteListClassifier = null;
    if (regexClassifier.isEmpty()) regexClassifier = null;

    this.next = testNode.getAttributeBoolean("next", false);
    this.prev = testNode.getAttributeBoolean("prev", false);
    this.delimMatch = testNode.getAttributeValue("delimMatch", null);
    this.verbose = testNode.getAttributeBoolean("verbose", false);

    this.ignoreLastToken = testNode.getAttributeBoolean("ignoreLastToken", false);
    this.ignoreFirstToken = testNode.getAttributeBoolean("ignoreFirstToken", false);
    this.onlyFirstToken = testNode.getAttributeBoolean("onlyFirstToken", false);
    this.onlyLastToken = testNode.getAttributeBoolean("onlyLastToken", false);
    this.reverse = testNode.getAttributeBoolean("reverse", false);

    // return values to guarantee success/failure regardless of reversal.
    // NOTE: wrapper will "un-reverse"
    this.success = !reverse; 
    this.failure = reverse;

    this.validTokenLength = null;
    final String vtlString = testNode.getAttributeValue("validTokenLength", null);
    if (vtlString != null && !"".equals(vtlString)) {
      this.validTokenLength = new IntegerRange(vtlString);
    }

    // init repeat ranges
    this.ignoreRepeatRange = null;
    this.failRepeatRange = null;
    this.testRepeatRange = null;

    if (testNode.hasChildNodes()) {
      final NodeList childNodes = testNode.getChildNodes();
      for (int childIndex = 0; childIndex < childNodes.getLength(); ++childIndex) {
        final Node curNode = childNodes.item(childIndex);
        if (curNode.getNodeType() != DomNode.ELEMENT_NODE) continue;

        final DomElement childNode = (DomElement)curNode;
        final String childName = childNode.getLocalName();

        if ("repeatcheck".equalsIgnoreCase(childName)) {
          final String rcType = childNode.getAttributeValue("type", "test");
          if ("ignore".equalsIgnoreCase(rcType)) {
            ignoreRepeatRange = new IntegerRange(childNode.getTextContent().trim());
          }
          else if ("fail".equalsIgnoreCase(rcType)) {
            failRepeatRange = new IntegerRange(childNode.getTextContent().trim());
          }
          else {
            testRepeatRange = new IntegerRange(childNode.getTextContent().trim());
          }
        }
      }
    }

    //NOTE: "reverse" isn't checked here directly because this test will be wrapped within
    //      a ReversedAtnRuleStepTest on load through the AtnRuleStep and the reversal
    //      logic will be applied there; however, in cases where the test is "ignored" or
    //      to have no effect, the later "reverse" logic needs to be counteracted.

    // under token node, setup allowed and disallowed tokens
    //
    // options:
    // - when reverse='true', fail on match (handled elsewhere)
    // - when ignoreLastToken='true', always succeed on last (full input) token
    // - when ignoreFirstToken='true', always succeed on first (full input) token
    // - when onlyFirstToken='true', only test against a "first" constituent token
    // - when onlyLastToken='true', only test against a "last" (full input) token
    // - when validTokenLength is specified, only succeed when the token's length is within the specified integer range
    // - when repeatCheck is used, the test is only applied to the defined repeats\n" +
    // - when next='true', test against the next token
    // - when prev='true', test against the prior token (taken as smallest prior if not available through state)
    // - when delimMatch='X', test against next or prev only succeeds if delims equal X
    //
    // <test reverse='true|false' ignoreLastToken='true|false' ignoreLastToken='true|false' onlyFirstToken='true|false' onlyLastToken='true|false' validTokenLength='integerRangeExpression' next='true|false' prev='true|false' delimMatch="X">
    //   <repeatCheck type='ignore|test|fail'>integer-range-expression</repeatCheck>\n" +
    //   <jclass>org.sd.atn.*Test</jclass>
    //   <terms caseSensitive='true|false'>
    //     <term>...</term>
    //     ...
    //   </terms>
    //   <regexes>
    //     <regex type='...' groupN='...'>...</regex>
    //   </regexes>
    // </test>

  }
			

  /**
   * Given a testNode with children of the form:
   * <p>
   *   &lt;nodeName attName='attValue'&gt;,
   * <p>
   * Extract all of the attribute values.
   */
  public static final List<String> loadValues(DomNode testNode, String nodeName, String attName) {
    List<String> result = null;

    final NodeList nodes = testNode.selectNodes(nodeName);
    if (nodes != null && nodes.getLength() > 0) {
      for (int nodeNum = 0; nodeNum < nodes.getLength(); ++nodeNum) {
        final DomElement elt = (DomElement)nodes.item(nodeNum);
        final String value = elt.getAttributeValue(attName, null);
        if (value != null) {
          if (result == null) result = new ArrayList<String>();
          result.add(value);
        }
      }
    }

    return result;
  }

  // extenders of this abstract class  must implement 'doAccept':
  // if "verify" is overridden, then doAccept may be a no-op.
  protected abstract boolean doAccept(Token token, AtnState curState);

  // override this to return HARD successes/failures thru extender
  protected TestResult verify(Token token, AtnState curState) {
    final boolean verified = doAccept(token, curState);
    final TestResult result = verified ? TestResult.SOFT_SUCCESS : TestResult.SOFT_FAILURE;
    return result;
  }


  public final PassFail accept(Token token, AtnState curState) {
    boolean result = false;
    boolean applyTest = true;

    // first check the repeat range for applicability of this test
    if (ignoreRepeatRange != null || failRepeatRange != null || testRepeatRange != null) {
      final int repeat = curState.getRepeatNum();

      if (failRepeatRange != null && failRepeatRange.includes(repeat)) {
        if (verbose) {
          System.out.println("***BaseClassifierTest HARD-FAILING due to failRepeatRange(" + repeat + "). " + curState);
        }
        return PassFail.FAIL;  //failure;
      }

      if (ignoreRepeatRange != null && ignoreRepeatRange.includes(repeat)) {
        if (verbose) {
          System.out.println("***BaseClassifierTest SKIPPING due to ignoreRepeatRange(" + repeat + "). " + curState);
        }
        return PassFail.NOT_APPLICABLE;  //success;
      }

      if (testRepeatRange != null && !testRepeatRange.includes(repeat)) {
        if (verbose) {
          System.out.println("***BaseClassifierTest SKIPPING by default due not in testRepeatRange(" + repeat + "). " + curState);
        }
        return PassFail.NOT_APPLICABLE;  //success;
      }
    }


    if (ignoreLastToken && token.getNextToken() == null) {
      if (verbose) {
        System.out.println("***BaseClassifierTest(" + this.getClass().getName() +
                           ") SKIPPING test on lastToken '" + token + "'! state=" +
                           curState);
      }
      applyTest = false;
      result = success;
    }
    else if (ignoreFirstToken) {
      if (token.getStartIndex() == 0 || AtnStateUtil.isFirstConstituentState(curState)) {
        if (verbose) {
          System.out.println("***BaseClassifierTest(" + this.getClass().getName() +
                             ") SKIPPING test on firstToken '" + token + "'! state=" +
                             curState);
        }
        applyTest = false;
        result = success;
      }
    }
    else if (onlyFirstToken) {
      if ((token.getStartIndex() == 0 || AtnStateUtil.isFirstConstituentState(curState))) {
        // is first token
        if (verbose) {
          System.out.println("***BaseClassifierTest(" + this.getClass().getName() +
                             ") applying test on firstToken '" + token + "! state=" +
                             curState);
        }
      }
      else {
        // not a first token
        applyTest = false;
        result = success;

        if (verbose) {
          System.out.println("***BaseClassifierTest SKIPPING non-firstToken state. " + curState);
        }
      }
    }


    if (applyTest) {
      // make the context switch here for applying the test to prev/next token
      if (next) {
        if (delimMatch != null) {
          final String delims = token.getPostDelim();
          if (!delimMatch.equals(delims)) {
            // can't match against next token because delimMatch fails
            if (verbose) {
              System.out.println("***BaseClassifierTest failing due to POST delimMatch SOFT-FAIL (delims=" +
                                 delims + ", state=" + curState + ")");
            }

            return PassFail.getInstance(false);  // reversible
          }
        }

        token = token.getNextToken();

        if (token == null) {
          if (verbose) {
            System.out.println("***BaseClassifierTest SKIPPING due to null (non-applicable) token (no next token).");
          }
          return PassFail.NOT_APPLICABLE;  // success;
        }

        if (verbose) {
          System.out.println("***BaseClassifierTest considering nextToken=" + token + " (from state=" + curState + ")");
        }
      }
      else if (prev) {
        if (delimMatch != null) {
          final String delims = token.getPreDelim();
          if (!delimMatch.equals(delims)) {
            // can't match against prev token because delimMatch fails
            if (verbose) {
              System.out.println("***BaseClassifierTest failing due to PRE delimMatch SOFT-FAIL (delims=" +
                                 delims + ", state=" + curState + ")");
            }

            return PassFail.getInstance(false);  // reversible
          }
        }

        token = token.getPrevToken();

        if (token == null) {
          if (verbose) {
            System.out.println("***BaseClassifierTest SKIPPING due to null (non-applicable) token (no prev token).");
          }
          return PassFail.NOT_APPLICABLE;  // success;
        }

        if (verbose) {
          System.out.println("***BaseClassifierTest considering prevToken=" + token + " (from state=" + curState + ")");
        }
      }
      else {
        if (verbose) {
          System.out.println("***BaseClassifierTest considering token=" + token + " (from state=" + curState + ")");
        }
      }
    }

    if (applyTest && validTokenLength != null && !validTokenLength.includes(token.getLength())) {
      applyTest = false;
      result = false;

      if (verbose) {
        System.out.println("***BaseClassifierTest(" + this.getClass().getName() +
                           ") verify(" + token + ", " + curState + ") SOFT-FAIL due to invalid token length" +
                           " tokenLen=" + token.getLength());
      }
    }

    if (applyTest) {

      final TestResult testResult = verify(token, curState);

      switch (testResult) {
        case SOFT_SUCCESS :
          result = true;
          break;
        case SOFT_FAILURE :
          result = false;
          break;
        case HARD_SUCCESS :
          result = success;
          break;
        case HARD_FAILURE :
          result = failure;
          break;
      }

      if (verbose) {
        System.out.println("***BaseClassifierTest(" + this.getClass().getName() +
                           ") verify(" + token + ", " + curState + ")=" + testResult +
                           " localResult=" + result);
      }
    }

    return PassFail.getInstance(result, !applyTest);
  }
}
