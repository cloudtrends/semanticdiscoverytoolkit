/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.token;


import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.sd.xml.DomElement;
import org.sd.xml.XmlFactory;

/**
 * JUnit Tests for the TokenWordPattern class.
 * <p>
 * @author Spence Koehler
 */
public class TestTokenWordPattern extends TestCase {

  public TestTokenWordPattern(String name) {
    super(name);
  }
  

  private final Tokenizer buildTokenizer(String tokenizerOptionsXml, String inputString) throws IOException {
    final DomElement tokenizerOptionsElt = (DomElement)XmlFactory.loadDocument(tokenizerOptionsXml, false).getDocumentElement();
    final StandardTokenizerOptions tokenizerOptions = new StandardTokenizerOptions(tokenizerOptionsElt);
    final Tokenizer tokenizer = StandardTokenizerFactory.getTokenizer(inputString, tokenizerOptions);
    return tokenizer;
  }

  public void doPatternTest(Tokenizer input, String[] flags, String[] expectedPatterns) {
    final TokenWordPattern wordPattern = new TokenWordPattern(input);

    for (int i = 0; i < flags.length; ++i) {
      final String squashFlags = flags[i];
      final String expectedPattern = expectedPatterns[i];

      final String pattern = wordPattern.getPattern(squashFlags);
      assertEquals(expectedPattern, pattern);
    }
  }

/*
  public void test1() throws IOException {
    final Tokenizer input =
      buildTokenizer(
        "",
        "");

    doPatternTest(input,
                  new String[] {
                  },
                  new String[] {
                  });
  }
*/


  private final void doSquashTest(String input, String[] flags, String[] expectedPatterns) {
    for (int i = 0; i < flags.length; ++i) {
      final String squashFlags = flags[i];
      final String expectedPattern = expectedPatterns[i];

      final String pattern = TokenWordPattern.squash(input, squashFlags);
      assertEquals("i=" + i + ", squashFlags=" + squashFlags, expectedPattern, pattern);
    }
  }

  public void testSquash() {
    doSquashTest("c: c, c I",
                 new String[]{null, "W", "WQP", "QP"},
                 new String[] {
                   "c: c, c I",
                   "c:c,cI",
                   "c:c,cI",
                   "c: c, c I"
                 });

    doSquashTest("c: c I (\"l c\") c",
                 new String[]{null, "W", "WQP", "QP", "WQ", "WP"},
                 new String[] {
                   "c: c I (\"l c\") c",
                   "c:cI(\"lc\")c",
                   "c:cIc",
                   "c: c I c",
                   "c:cI()c",
                   "c:cIc",
                 });
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestTokenWordPattern.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
