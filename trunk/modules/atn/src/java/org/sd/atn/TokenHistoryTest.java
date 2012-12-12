/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sd.token.Token;
import org.sd.util.Usage;
import org.sd.xml.DomNode;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An AtnRuleStep test that checks tokens in states leading up to the current
 * state.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "An org.sd.atn.AtnRuleStep test that checks tokens in states leading up\n" +
       "to the current state.\n" +
       "\n" +
       "Walk back through the states looking for a token with a category\n" +
       "whose text matches (or doesn't match) certain values.\n" +
       "\n" +
       "test attributes:\n" +
       "  category: (required) category (rule-step name) of token to test\n" +
       "  missing: (optional) pass/fail when the category is never found\n" +
       "  present: (optional) pass/fail when the category is found\n" +
       "           if this option is specified as 'fail', then once the category\n" +
       "           is seen, the test will fail regardless of any tests on the\n" +
       "           token's value.\n" +
       "           if this option is specified as 'pass', then tests on the token's\n" +
       "           value (if any) are first applied and searching terminates with\n" +
       "           the result upon visiting the first 'category' token. If no\n" +
       "           value checking is performed, then the test will pass.\n" +
       "  halt: (optional) state category (rule-step name) at which to terminate searching\n" +
       "        if same as category, then category is tested first and then search is halted.\n" +
       "\n" +
       "test directives (child nodes) are of the form:\n" +
       "  <categories>\n" +
       "    <category tokenClassifier='...' />\n" +
       "    ...\n" +
       "  </categories>\n" +
       "\n" +
       "  Each directive is applied in order to identified (normalized) token text.\n" +
       "  If any directive fails, then the test fails. [todo: parameterize this behavior]\n" +
       "\n" +
       "example:\n" +
       "\n" +
       "<!-- birth event fails if non-birth keyword found -->\n" +
       "<event>\n" +
       "  <test category='eventKeyword' halt='event' missing='pass'>\n" +
       "    <jclass>org.sd.atn.TokenHistoryTest</jclass>\n" +
       "    <categories>\n" +
       "      <eventKeyword tokenClassifier='birthKeyword' />\n" +
       "    </categories>\n" +
       "  </test>\n" +
       "</event>"
  )
public class TokenHistoryTest implements AtnRuleStepTest {
  
  //
  // Walk back through the states looking for a token with a category
  // whose text matches (or doesn't match) certain values.
  // 
  // test attributes:
  //   category: (required) category (rule-step name) of token to test
  //   missing: (optional) pass/fail when the category is never found
  //   present: (optional) pass/fail when the category is found
  //            if this option is specified as 'fail', then once the category
  //            is seen, the test will fail regardless of any tests on the
  //            token's value.
  //            if this option is specified as 'pass', then tests on the token's
  //            value (if any) are first applied and searching terminates with
  //            the result upon visiting the first 'category' token. If no
  //            value checking is performed, then the test will pass.
  //   halt: (optional) state category (rule-step name) at which to terminate searching
  //         if same as category, then category is tested first and then search is halted.
  //
  // test directives (child nodes) are of the form:
  //   <categories>
  //     <category tokenClassifier='...' />
  //     ...
  //   </categories>
  //
  //   Each directive is applied in order to identified (normalized) token text.
  //   If any directive fails, then the test fails. [todo: parameterize this behavior]
  //
  // example:
  //
  // <!-- birth event fails if non-birth keyword found -->
  // <event>
  //   <test category='eventKeyword' halt='event' missing='pass'>
  //     <jclass>org.sd.atn.TokenHistoryTest</jclass>
  //     <categories>
  //       <eventKeyword tokenClassifier='birthKeyword' />
  //     </categories>
  //   </test>
  // </event>
  //


  private Map<String, List<DirectiveData>> cat2directives;

  public TokenHistoryTest(DomNode testNode, ResourceManager resourceManager) {
    this.cat2directives = loadDirectives(testNode);
  }

  private final Map<String, List<DirectiveData>> loadDirectives(DomNode testNode) {
    final Map<String, List<DirectiveData>> result = new HashMap<String, List<DirectiveData>>();

    final DomNode categoriesNode = testNode.selectSingleNode("categories");
    if (categoriesNode != null) {
      final NodeList childNodes = categoriesNode.getChildNodes();
      for (int childNum = 0; childNum < childNodes.getLength(); ++childNum) {
        final Node childNode = childNodes.item(childNum);
        if (childNode.getNodeType() != DomNode.ELEMENT_NODE) continue;

        final DirectiveData dd = new DirectiveData((DomNode)childNode);
        List<DirectiveData> ddList = result.get(dd.getCategory());
        if (ddList == null) {
          ddList = new ArrayList<DirectiveData>();
          result.put(dd.getCategory(), ddList);
        }
        ddList.add(dd);
      }
    }

    return result;
  }

  /**
   * Determine whether to accept the (matched) state.
   * <p>
   * This is called as a last check on whether a token matches for the current
   * state after its category has been matched to its containing rule step.
   */
  public PassFail accept(Token token, AtnState curState) {
    boolean result = true;

    final Set<String> categories = new HashSet<String>(cat2directives.keySet());

    final AtnState stopState = curState.getPushState();

    for (AtnState atnState = curState; result && atnState != null && atnState != stopState; atnState = atnState.getParentState()) {
      final String curCategory = atnState.getRuleStep().getCategory();

      final List<DirectiveData> directives = cat2directives.get(curCategory);

      if (directives != null) {
        categories.remove(curCategory);

        final DirectiveResult directiveResult = applyDirectives(directives, curState);

        if (directiveResult != null) {
          switch (directiveResult) {
            case FAIL :
              result = false; break;
            case PASS :
              result = true; break;
            case HALT :
              break;
          }
        }
      }
    }

    if (result) {
      for (String category : categories) {
        final List<DirectiveData> directives = cat2directives.get(category);
        for (DirectiveData directive : directives) {
          if (directive.getResultWhenMissing() == DirectiveResult.FAIL) {
            result = false;
            break;
          }
        }
        if (!result) break;
      }
    }

    return PassFail.getInstance(result);
  }
  
  private DirectiveResult applyDirectives(List<DirectiveData> directives, AtnState curState) {
    DirectiveResult result = null;

    for (DirectiveData directiveData : directives) {
      result = directiveData.examine(curState);
      if (result != null && result != DirectiveResult.IGNORE) break;
    }

    return result;
  }


  private enum DirectiveResult { PASS, FAIL, HALT, IGNORE };

  private static DirectiveResult getDirectiveResult(DomNode domNode, String attribute, DirectiveResult defaultResult) {
    DirectiveResult result = defaultResult;

    final String value = domNode.getAttributeValue(attribute, null);
    if (value != null) {
      result = DirectiveResult.valueOf(value.toUpperCase());
    }

    return result;
  }

  /**
   * Container class for examining a historical category.
   */
  private static final class DirectiveData {

    private String category;
    private String classifierId;
    private DirectiveResult resultWhenMatch;
    private DirectiveResult resultWhenMismatch;
    private DirectiveResult resultWhenMissing;

    private List<AtnStateTokenClassifier> _tokenClassifiers;

    DirectiveData(DomNode domNode) {
      this.category = domNode.getNodeName();
      this.classifierId = domNode.getAttributeValue("tokenClassifier", null);
      this.resultWhenMatch = getDirectiveResult(domNode, "match", DirectiveResult.IGNORE);
      this.resultWhenMismatch = getDirectiveResult(domNode, "mismatch", DirectiveResult.IGNORE);
      this.resultWhenMissing = getDirectiveResult(domNode, "missing", DirectiveResult.IGNORE);

      this._tokenClassifiers = null;
    }

    String getCategory() {
      return category;
    }

    String getClassifierId() {
      return classifierId;
    }

    DirectiveResult getResultWhenMissing() {
      return resultWhenMissing;
    }

    DirectiveResult examine(AtnState curState) {
      DirectiveResult result = resultWhenMatch;

      if (classifierId != null) {
        boolean foundMatch = false;

        if (curState.getRuleStep().getLabel().equals(classifierId)) {
          foundMatch = true;
        }
        else {
          final List<AtnStateTokenClassifier> tokenClassifiers = getTokenClassifiers(curState);

          if (tokenClassifiers != null) {
            final Token token = curState.getInputToken();

            for (AtnStateTokenClassifier tokenClassifier : tokenClassifiers) {
              final MatchResult matchResult = tokenClassifier.classify(token, curState);
              if (matchResult.matched()) {
                foundMatch = true;
                break;
              }
            }
          }
        }

        result = foundMatch ? resultWhenMatch : resultWhenMismatch;
      }

      return result;
    }

    private final List<AtnStateTokenClassifier> getTokenClassifiers(AtnState curState) {
      if (_tokenClassifiers == null) {
        _tokenClassifiers = curState.getRule().getGrammar().getClassifiers(classifierId);
      }
      return _tokenClassifiers;
    }
  }
}
