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
import org.sd.xml.DomNode;
import org.sd.xml.XmlFactory;


/**
 * JUnit Tests for the DelimTest class.
 * <p>
 * @author Spence Koehler
 */
public class TestDelimTest extends TestCase {

  public TestDelimTest(String name) {
    super(name);
  }
  

  public void testSpaceDashSpace() throws IOException {
    final String delimConfig =
      "<predelim>\n" +
      "  <disallowall />\n" +
			"  <allow type='substr'>.</allow>\n" +
			"  <allow type='exact'>-</allow>\n" +
      "</predelim>";
    
    final DomNode delimNode = XmlFactory.buildDomNode(delimConfig, false);
    final DelimTest delimTest = new DelimTest(true, delimNode, new ResourceManager());

    final StandardTokenizerOptions tokenizerOptions = new StandardTokenizerOptions();
    tokenizerOptions.setRevisionStrategy(TokenRevisionStrategy.SO);

    Token firstToken = StandardTokenizerFactory.getFirstToken("Testing - 123", tokenizerOptions);
    Token secondToken = firstToken.getNextToken();
    boolean accept = delimTest.accept(secondToken, null).accept();
    assertFalse(accept);

    firstToken = StandardTokenizerFactory.getFirstToken("Testing-123", tokenizerOptions);
    secondToken = firstToken.getNextToken();
    accept = delimTest.accept(secondToken, null).accept();
    assertTrue(accept);
  }

  public void testDotSpaceDashSpace() throws IOException {
    final String delimConfig =
      "<predelim>\n" +
      "  <disallowall />\n" +
			"  <allow type='substr'>.</allow>\n" +
			"  <allow type='exact'>-</allow>\n" +
      "</predelim>";
    
    final DomNode delimNode = XmlFactory.buildDomNode(delimConfig, false);
    final DelimTest delimTest = new DelimTest(true, delimNode, new ResourceManager());

    final StandardTokenizerOptions tokenizerOptions = new StandardTokenizerOptions();
    tokenizerOptions.setRevisionStrategy(TokenRevisionStrategy.SO);

    Token firstToken = StandardTokenizerFactory.getFirstToken("A. - Xyz", tokenizerOptions);
    Token secondToken = firstToken.getNextToken();
    boolean accept = delimTest.accept(secondToken, null).accept();
    assertFalse(accept);

    firstToken = StandardTokenizerFactory.getFirstToken("A.-Xyz", tokenizerOptions);
    secondToken = firstToken.getNextToken();
    accept = delimTest.accept(secondToken, null).accept();
    assertFalse(accept);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestDelimTest.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
