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
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.w3c.dom.NodeList;

/**
 * A rule to test text through RoteList- and/or Regex- Classifiers.
 * <p>
 * Note that this tests the full text of the constituent being considered,
 * not for example the last single token encountered. For testing against the
 * last token, see TokenTest instead.
 *
 * @author Spence Koehler
 */
public class TextTest extends BaseClassifierTest {
  
  public TextTest(DomNode testNode, ResourceManager resourceManager) {
    super(testNode, resourceManager);

    // under token node, setup allowed and disallowed tokens
    //
    // options:
    // - when reverse='true', fail on match (handled elsewhere)
    //
    // <test reverse='true|false'>
    //   <jclass>org.sd.atn.TextTest</jclass>
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

    String text = null;

    if (curState.isPoppedState()) {
      final AtnState startState = AtnStateUtil.getConstituentStartState(curState);
      final String fulltext = token.getTokenizer().getText();
      text = fulltext.substring(startState.getInputToken().getStartIndex(), token.getEndIndex());
    }
    else {
      text = token.getText();
    }

    
    if (!result && roteListClassifier != null) {
      result = (roteListClassifier.doClassify(text) != null);
    }

    if (!result && regexClassifier != null) {
      result = regexClassifier.doClassify(text);
      if (verbose) System.out.println("TextTest(" + text + ")=" + result);
    }

    return result;
  }
}
