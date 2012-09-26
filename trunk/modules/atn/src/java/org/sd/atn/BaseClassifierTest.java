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
  
  protected String id;
  protected RoteListClassifier roteListClassifier;
  protected RegexClassifier regexClassifier;
  protected boolean verbose;
  protected boolean ignoreLastToken;
  protected boolean ignoreFirstToken;
  protected boolean onlyFirstToken;
  protected boolean onlyLastToken;
  protected IntegerRange validTokenLength;
  private boolean reverse;

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

    this.verbose = testNode.getAttributeBoolean("verbose", false);

    this.ignoreLastToken = testNode.getAttributeBoolean("ignoreLastToken", false);
    this.ignoreFirstToken = testNode.getAttributeBoolean("ignoreFirstToken", false);
    this.onlyFirstToken = testNode.getAttributeBoolean("onlyFirstToken", false);
    this.onlyLastToken = testNode.getAttributeBoolean("onlyLastToken", false);
    this.reverse = testNode.getAttributeBoolean("reverse", false);

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
    //
    // <test reverse='true|false' ignoreLastToken='true|false' ignoreLastToken='true|false' onlyFirstToken='true|false' onlyLastToken='true|false' validTokenLength='integerRangeExpression'>
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
			
  // extenders of this abstract class  must implement 'doAccept':
  protected abstract boolean doAccept(Token token, AtnState curState);


  public final boolean accept(Token token, AtnState curState) {
    boolean result = false;
    boolean applyTest = true;

    // first check the repeat range for applicability of this test
    if (ignoreRepeatRange != null || failRepeatRange != null || testRepeatRange != null) {
      final int repeat = curState.getRepeatNum();

      if (failRepeatRange != null && failRepeatRange.includes(repeat)) {
        return false;
      }

      if (ignoreRepeatRange != null && ignoreRepeatRange.includes(repeat)) {
        return true;
      }

      if (testRepeatRange != null && !testRepeatRange.includes(repeat)) {
        return true;
      }
    }


    if (ignoreLastToken && token.getNextToken() == null) {
      if (verbose) {
        System.out.println("***BaseClassifierTest(" + this.getClass().getName() +
                           ") skipping test on lastToken '" + token + "'! state=" +
                           curState);
      }
      applyTest = false;
      result = !reverse;  //NOTE: wrapper will "un-reverse"
    }
    else if (ignoreFirstToken) {
      if (token.getStartIndex() == 0 || AtnStateUtil.isFirstConstituentState(curState)) {
        if (verbose) {
          System.out.println("***BaseClassifierTest(" + this.getClass().getName() +
                             ") skipping test on firstToken '" + token + "'! state=" +
                             curState);
        }
        applyTest = false;
        result = !reverse;  //NOTE: wrapper will "un-reverse"
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
        result = !reverse;  //NOTE: wrapper will "un-reverse"
      }
    }

    if (validTokenLength != null && !validTokenLength.includes(token.getLength())) {
      applyTest = false;
      result = false;

      if (verbose) {
        System.out.println("***BaseClassifierTest(" + this.getClass().getName() +
                           ") doAccept(" + token + ", " + curState + ")=" + result +
                           " tokenLen=" + token.getLength());
      }
    }

    if (applyTest) {
      result = doAccept(token, curState);

      if (verbose) {
        System.out.println("***BaseClassifierTest(" + this.getClass().getName() +
                           ") doAccept(" + token + ", " + curState + ")=" + result);
      }
    }

    return result;
  }
}
