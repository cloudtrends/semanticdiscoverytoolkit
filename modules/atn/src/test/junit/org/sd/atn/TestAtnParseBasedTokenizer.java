/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import org.sd.token.StandardTokenizerFactory;
import org.sd.token.StandardTokenizerOptions;
import org.sd.token.TokenizeTest;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the AtnParseBasedTokenizer class.
 * <p>
 * @author Spence Koehler
 */
public class TestAtnParseBasedTokenizer extends TestCase {

  public TestAtnParseBasedTokenizer(String name) {
    super(name);
  }
  

  public void testRetainEndBreaks1() {
    final StandardTokenizerOptions defaultOptions = StandardTokenizerFactory.DEFAULT_OPTIONS;
    final XmlInputDecoder xmlInputDecoder = new XmlInputDecoder("<text oneLine=\"true\"><p oneLine=\"true\"><t>\"Smith\",</t><t>\"John\"!</t></p></text>", true);
    final XmlParseInputContext inputContext = new XmlParseInputContext(xmlInputDecoder.getParagraphs().get(0), 0);

    final AtnParseBasedTokenizer tokenizer = new AtnParseBasedTokenizer(inputContext, defaultOptions);
    tokenizer.setRetainEndBreaks(false);

    final TokenizeTest tokenizeTestNoRetain =
      new TokenizeTest("DontRetainEndBreaks", tokenizer,
                       new String[] { "Smith\",", "John\"!" },
                       null, null);

    assertTrue(tokenizeTestNoRetain.runTest());


    tokenizer.setRetainEndBreaks(true);

    final TokenizeTest tokenizeTestDoRetain =
      new TokenizeTest("DoRetainEndBreaks", tokenizer,
                       new String[] { "Smith", "John" },
                       null, null);

    assertTrue(tokenizeTestDoRetain.runTest());
  }

  public void testRetainEndBreaks2() {
    final StandardTokenizerOptions defaultOptions = StandardTokenizerFactory.DEFAULT_OPTIONS;
    final XmlInputDecoder xmlInputDecoder = new XmlInputDecoder("<text oneLine=\"true\"><p oneLine=\"true\"><t abc=\"xyz\">Smith*</t><t>John</t></p></text>", true);
    final XmlParseInputContext inputContext = new XmlParseInputContext(xmlInputDecoder.getParagraphs().get(0), 0);

    final AtnParseBasedTokenizer tokenizer = new AtnParseBasedTokenizer(inputContext, defaultOptions);
    tokenizer.setRetainEndBreaks(false);

    final TokenizeTest tokenizeTestNoRetain =
      new TokenizeTest("DontRetainEndBreaks", tokenizer,
                       new String[] { "Smith*", "John" },
                       null, null);

    assertTrue(tokenizeTestNoRetain.runTest());
    assertTrue(tokenizer.getToken(0).hasFeatureValue("abc", null, null, "xyz"));


    tokenizer.setRetainEndBreaks(true);

    final TokenizeTest tokenizeTestDoRetain =
      new TokenizeTest("DoRetainEndBreaks", tokenizer,
                       new String[] { "Smith", "John" },
                       null, null);

    assertTrue(tokenizeTestDoRetain.runTest());
    assertTrue(tokenizer.getToken(0).hasFeatureValue("abc", null, null, "xyz"));
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestAtnParseBasedTokenizer.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
