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
 * A rule test for pre- or post- delims.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "Delim tests are special tests for analyzing delimiters between tokens\n" +
       "\n" +
       "    postDelim test considers input delimiter characters after the matched token\n" +
       "    preDelim test considers input delimiter characters before the matched token\n" +
       "    delim test configuration elements are:\n" +
       "        <repeatCheck type='ignore|test|fail'>integer-range-expression</repeatCheck>\n" +
       "            configures the delim test for application at specific zero-based state repeat numbers\n" +
       "            the 'type' attribute controls behavior at the repeat numbers: (note: only last one of each type is kept)\n" +
       "                ignore -- the test will always return true\n" +
       "                test -- execute the test normally\n" +
       "                fail -- the test will always return false\n" +
       "            this enables defining applicability for the test e.g. after the first repeating instance\n" +
       "            integer-range-expression has comma-separated terms like:\n" +
       "                A -- a single 0-based repeat number, A\n" +
       "                A-B -- the range of integers from A to B\n" +
       "                (A-B) -- the range of integers from A (exclusive) to B (exclusive)\n" +
       "                (A-B] -- the range of integers from A (exclusive) to B (inclusive)\n" +
       "                [A-B] -- the range of integers from A (inclusive) to B (inclusive)\n" +
       "                [A-B) -- the range of integers from A (inclusive) to B (exclusive)\n" +
       "                A- -- the range of integers from A (inclusive) to infinity\n" +
       "            if absent, behaves as 'test' for all state repeat values.\n" +
       "        <disallowall />\n" +
       "            when encountered, denotes that any delimiter will cause the test to fail\n" +
       "            subsequent configuration elements will override this effect\n" +
       "                one strategy for identifying pertinent delimiters would be to first disallow all and then allow specific delimiters\n" +
       "        <allowall />\n" +
       "            when encountered, denotes that no delimiter will cause the test to fail\n" +
       "            subsequent configuration elements will override this effect\n" +
       "        <allow type='substr|exact'>...delims-to-allow...</allow>\n" +
       "            when encountered, denotes to allow specific delimiters represented by the node's text\n" +
       "                note that allowing specific delimiters has no effect unless other delimiters are disallowed\n" +
       "                    consequently, if \"allow\" is encountered before either \"allowall\" or \"disallowall\", then \"disallowall\" will be assumed\n" +
       "            if type='exact', then the test succeeds only when the delims-to-allow text equals the token's (pre- or post-) delims\n" +
       "            if type='substr', then the test succeeds when the token's delims are a substring of the delims-to-allow\n" +
       "                note: a token's delims are the non-trimmed delimiters either before (when \"pre\") or after (when \"post\") the token\n" +
       "        <disallow type='substr|exact'>...delims-to-disallow...</disallow>\n" +
       "            when encountered, denotes to disallow specific delimiters represented by the node's test\n" +
       "                note that disallowing specific delimiters has no effect unless other delimiters are allowed\n" +
       "                    consequently, if \"disallow\" is encountered before either \"allowall\" or \"disallowall\", then \"allowall\" will be assumed\n" +
       "            type of 'exact' and 'substr' operates the same as described above for 'allow'.\n" +
       "        <require type='substr|exact'>...delims-to-require...</require>\n" +
       "            when encountered, denotes that the identified delimiters *must* be present for the test to pass.\n" +
       "            type of 'exact' and 'substr' operates the same as described above for 'allow'."
  )
public class DelimTest implements AtnRuleStepTest {
  
  private boolean isPre;
  boolean isPre() {
    return isPre;
  }

  boolean isPost() {
    return !isPre;
  }

  private boolean allowAll;
  boolean allowAll() {
    return allowAll;
  }

  private boolean disallowAll;
  boolean disallowAll() {
    return disallowAll;
  }

  private List<DelimString> delimStrings;
  List<DelimString> getDelimStrings() {
    return delimStrings;
  }

  private List<DelimString> requiredDelimStrings;
  List<DelimString> getRequiredDelimStrings() {
    return requiredDelimStrings;
  }

  private IntegerRange ignoreRepeatRange;
  IntegerRange getIgnoreRepeatRange() {
    return ignoreRepeatRange;
  }

  private IntegerRange failRepeatRange;
  IntegerRange getFailRepeatRange() {
    return failRepeatRange;
  }

  private IntegerRange testRepeatRange;
  IntegerRange getTestRepeatRange() {
    return testRepeatRange;
  }

  // used only in conjunction with postDelim
  private boolean remainingText;
  boolean getRemainingText() {
    return remainingText;
  }

  private boolean ignoreConstituents;


  DelimTest(boolean isPre, DomNode delimNode) {
    this.isPre = isPre;
    this.allowAll = false;
    this.disallowAll = false;
    this.delimStrings = new ArrayList<DelimString>();
    this.requiredDelimStrings = null;
    this.ignoreRepeatRange = null;
    this.failRepeatRange = null;
    this.testRepeatRange = null;
    this.ignoreConstituents = false;

    this.remainingText = delimNode.getAttributeBoolean("remainingText", false);

    // under delim node, setup allowed and disallowed delims
    //
    // <pre-or-post-delim>
    //   <repeatCheck type='ignore|test|fail'>integer-range-expression</repeatCheck>
    //   <disallowall />
    //   <allowall />
    //   <allow type='substr|exact'>delims-to-allow</allow>
    //   <disallow type='substr|exact'>delims-to-disallow</disallow>
    //   <require type='substr|exact'>delims-to-require</require>
    // </pre-and-or-post-delim>

    if (delimNode.hasChildNodes()) {
      final NodeList childNodes = delimNode.getChildNodes();
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
        else {

          final boolean exact = "exact".equalsIgnoreCase(childNode.getAttributeValue("type", "substr"));
        
          if ("disallowall".equalsIgnoreCase(childName)) {
            disallowAll = true;
            allowAll = false;
            delimStrings.clear();
          }
          else if ("allowall".equalsIgnoreCase(childName)) {
            allowAll = true;
            disallowAll = false;
            delimStrings.clear();
          }
          else if ("allow".equalsIgnoreCase(childName)) {
            if (!disallowAll && !allowAll) disallowAll = true;
            delimStrings.add(new DelimString(true, exact, childNode.getTextContent()));
          }
          else if ("disallow".equalsIgnoreCase(childName)) {
            if (!disallowAll && !allowAll) allowAll = true;
            delimStrings.add(new DelimString(false, exact, childNode.getTextContent()));
          }
          else if ("require".equalsIgnoreCase(childName)) {
            if (requiredDelimStrings == null) requiredDelimStrings = new ArrayList<DelimString>();
            requiredDelimStrings.add(new DelimString(true, exact, childNode.getTextContent()));
          }
        }
      }
    }
  }
			
  public void setIgnoreConstituents(boolean ignoreConstituents) {
    this.ignoreConstituents = ignoreConstituents;
  }

  public boolean accept(Token token, AtnState curState) {
    final String delim = getDelim(token, curState);

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


    if (!meetsRequiredConstraints(delim)) return false;
    else if (delimStrings.size() == 0) return true;

    // ignore (always accept) purely whitespace (unless there are requiredDelimStrings)
    if ("".equals(delim.trim())) return true;

    boolean result = allowAll;

    boolean foundMatch = false;
    for (DelimString delimString : delimStrings) {
      if (delimString.matches(delim)) {
        if ((allowAll && !delimString.isAllowed) || (disallowAll && delimString.isAllowed)) {
          foundMatch = true;
        }

        //if definitive, break
        if (delimString.exact) break;
      }
      else if (delimString.exact && delimString.isInexactMatch(delim)) {
        // exact delimString only matched inexactly
        foundMatch = false;
        break;
      }
    }

    if (foundMatch) {
      result = !result;
    }

    return result;
  }

  private boolean meetsRequiredConstraints(String delim) {
    boolean result = false;

    // require only one of the required constraints, not all
    if (requiredDelimStrings != null) {
      for (DelimString delimString : requiredDelimStrings) {
        if (delimString.matches(delim)) {
          result = true;
          break;
        }
      }
    }
    else {
      result = true;
    }

    return result;
  }

  private String getDelim(Token token, AtnState curState) {
    String delim = null;

    if (isPre) {  // predelim
      final AtnState parentState = curState != null ? curState.getParentState() : null;
      final boolean constituent =
        !ignoreConstituents &&
        (parentState != null && parentState.isPoppedState()) &&
        curState.getRuleStep().getCategory().equals(parentState.getRule().getRuleName());

      if (constituent) {
        // we're looking at a pre-test on the result of a constituent pop
        // get the delimiter string that precedes the constituent, not the token
        final AtnState startState = AtnStateUtil.getConstituentStartState(parentState);
        final Token startToken = startState.getInputToken();
        delim = startToken.getPreDelim();
      }
      else {
        // get the delimiter preceding the token
        delim = token.getPreDelim();
      }
    }
    else {  // postdelim
      delim = token.getPostDelim();
      if (remainingText) {
        delim += token.getTokenizer().getNextText(token);
      }
    }

    return delim;
  }

  class DelimString {

    boolean isAllowed;
    boolean exact;
    String delim;

    DelimString(boolean isAllowed, boolean exact, String delim) {
      this.isAllowed = isAllowed;
      this.exact = exact;
      this.delim = delim;
    }

    boolean matches(String delim) {
      return exact ? this.delim.equals(delim) : isInexactMatch(delim);
    }

    boolean isInexactMatch(String delim) {
      return delim.indexOf(this.delim) != -1;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.append(isAllowed ? '+' : '-');
      result.append(exact ? '=' : '~');
      result.append(delim);

      return result.toString();
    }
  }
}
