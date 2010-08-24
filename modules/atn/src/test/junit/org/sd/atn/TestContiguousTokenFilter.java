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


import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.sd.token.StandardTokenizerFactory;
import org.sd.token.StandardTokenizerOptions;
import org.sd.token.Token;
import org.sd.token.TokenRevisionStrategy;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.XmlFactory;

/**
 * JUnit Tests for the ContiguousTokenFilter class.
 * <p>
 * @author Spence Koehler
 */
public class TestContiguousTokenFilter extends TestCase {

  public TestContiguousTokenFilter(String name) {
    super(name);
  }
  

  public void testSpaceDashSpace() throws IOException {
    final String configXml =
      "	<tokenFilter id=\"contiguous\">\n" +
      "		<jclass>org.sd.atn.ContiguousTokenFilter</jclass>\n" +
      "\n" +
      "		<innerdelim>\n" +
      "			<disallowall />\n" +
      "			<allow type='substr'>,</allow>\n" +
      "			<allow type='substr'>.</allow>\n" +
      "			<allow type='exact'>-</allow>\n" +
      "			<allow type='substr'>(</allow>\n" +
      "			<allow type='substr'>)</allow>\n" +
      "			<allow type='substr'>/</allow>\n" +
      "			<allow type='substr'>\\</allow>\n" +
      "			<allow type='substr'>\"</allow>\n" +
      "			<allow type='substr'>'</allow>\n" +
      "		</innerdelim>\n" +
      "\n" +
      "		<!-- only allow lower/upper letters, single-quotes, dashes and periods within a token's text -->\n" +
      "		<tokenreg>[A-Za-z'\\-.]+</tokenreg>\n" +
      "\n" +
      "	</tokenFilter>\n";

    final DomElement configNode = (DomElement)XmlFactory.buildDomNode(configXml, false);
    final ContiguousTokenFilter filter = new ContiguousTokenFilter(configNode, new ResourceManager());

    final StandardTokenizerOptions tokenizerOptions = new StandardTokenizerOptions();
    tokenizerOptions.setRevisionStrategy(TokenRevisionStrategy.SO);

    Token firstToken = StandardTokenizerFactory.getFirstToken("Testing - ABC", tokenizerOptions);
    Token secondToken = firstToken.getNextToken();
    TokenFilterResult filterResult = filter.checkToken(secondToken, false, firstToken, null);
    assertEquals(TokenFilterResult.HALT, filterResult);

    firstToken = StandardTokenizerFactory.getFirstToken("Testing-ABC", tokenizerOptions);
    secondToken = firstToken.getNextToken();
    filterResult = filter.checkToken(secondToken, false, firstToken, null);
    assertEquals(TokenFilterResult.ACCEPT, filterResult);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestContiguousTokenFilter.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
