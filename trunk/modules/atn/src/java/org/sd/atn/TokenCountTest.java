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
import org.sd.util.Usage;
import org.sd.util.range.IntegerRange;
import org.sd.xml.DomNode;

/**
 * A rule to test token counts.
 * <p>
 *
 * @author Spence Koehler
 */
@Usage(notes =
       "An org.sd.atn.BaseClassifierTest to test token counts.\n" +
       "\n" +
       " options:\n" +
       " - when countWords='true' (default='false') words instead of tokens are counted\n" +
       " - when globalCount='true' (default='false') all tokens instead of current constituent tokens are counted\n" +
       " - range identifies the valid integer count range for a passing result.\n" +
       " \n" +
       " <test reverse='true|false' countWords='true|false' globalCount='true|false' range='range-expression' verbose='true|false'>\n" +
       "   <jclass>org.sd.atn.TokenCountTest</jclass>\n" +
       " </test>"
  )
public class TokenCountTest extends BaseClassifierTest {
  
  private boolean verbose;
  private boolean countWords;
  private boolean globalCount;
  private IntegerRange range;

  public TokenCountTest(DomNode testNode, ResourceManager resourceManager) {
    super(testNode, resourceManager);

    this.verbose = testNode.getAttributeBoolean("verbose", false);
    this.countWords = testNode.getAttributeBoolean("countWords", false);
    this.globalCount = testNode.getAttributeBoolean("globalCount", false);

    final String rangeString = testNode.getAttributeValue("range");
    this.range = new IntegerRange(rangeString);
  }
			
  protected boolean doAccept(Token token, AtnState curState) {
    boolean result = false;

    int numTokens = token.getSequenceNumber() + 1;

    final Token refToken =
      globalCount ?
      AtnStateUtil.getParseStartState(curState).getInputToken() :
      AtnStateUtil.getConstituentStartState(curState).getInputToken();

    numTokens -= refToken.getSequenceNumber();

    int curValue = numTokens;

    if (countWords) {
      //curValue = token.getTokenizer().computeWordCount(refToken, token/*curState.getInputToken()*/);
      final String text = token.getTokenizer().getText();
      curValue = countWords(text, refToken.getStartIndex(), token.getEndIndex());
    }

    result = range.includes(curValue);

    if (verbose) {
      System.out.println("TokenCountTest token=" + token +
                         (countWords ? " numWords" : " numTokens") +
                         "=" + curValue + "; range=" + range +
                         " result=" + result);
    }

    return result;
  }

  private final int countWords(String text, int startIdx, int endIdx) {
    int result = 1;

    startIdx = Math.max(0, startIdx);
    endIdx = Math.min(text.length() - 1, endIdx);

    char lastC = (char)0;
    for (int idx = startIdx; idx <= endIdx; ++idx) {
      final char c = text.charAt(idx);
      if (c == ' ' && lastC != ' ') ++result;
      lastC = c;
    }

    return result;
  }
}
