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


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.XmlFactory;

/**
 * JUnit Tests for the XmlInputDecoder class.
 * <p>
 * @author Spence Koehler
 */
public class TestXmlInputDecoder extends TestCase {

  public TestXmlInputDecoder(String name) {
    super(name);
  }
  

  public void testEmpty() throws IOException {
    final String[] expected = new String[]{};
    doDecoderTest("", expected);
    doDecoderTest(" ", expected);
    doDecoderTest(" <text> </text> ", expected);
    doDecoderTest(" <text> <p> </p> </text> ", expected);
    doDecoderTest(" <text> <p> <t/>  <b/> </p> </text> ", expected);
    doDecoderTest(" <text> <p> <t>  </t> <b/> </p> </text> ", expected);
  }

  public void testSingleParagraph() throws IOException {
    final String[] expected = new String[]{"This is a test"};

    // single paragraph
    doDecoderTest("<text><p>This is a test</p></text>", expected);
    doDecoderTest("<text><p> This is a test </p></text>", expected);
    doDecoderTest("<text> <p> This  is  a  test </p> </text>", expected);
    doDecoderTest("<text> <p> This  is  a  test </p> <p>  </p> </text>", expected);

    // no paragraphs
    doDecoderTest("<text>This is a test</text>", expected);
    doDecoderTest("<text> This  is  a  test </text>", expected);
  }

  public void testMultipleParagraphs() throws IOException {
    final String[] expected = new String[]{"This is", "only", "a test"};

    // multiple paragraphs
    doDecoderTest("<text><p>This is</p><p>only</p><p>a test</p></text>", expected);
    doDecoderTest("<text> <p> This  is </p> <p> only </p> <p> a  test </p> </text>", expected);

    // extra text nodes (paragraphs)
    doDecoderTest("<text>This is<p>only</p>a test</text>", expected);
    doDecoderTest("<text> This  is <p> only </p> a  test </text>", expected);
  }

  public void testNonXmlConstruction() {
    final String[] expected = new String[]{"This is a test."};

    final XmlInputDecoder decoder = new XmlInputDecoder("This is a test.", true);
    final List<XmlInputDecoder.Paragraph> decodedParagraphs = decoder.getParagraphs();
    verifyParagraphs(decodedParagraphs, expected);
  }

  public void testTokens() throws IOException {
    final String[] expected = new String[]{"The Declaration of Independence was signed on July 4th, 1776 , launching a nation."};

    // extra spacing
    doDecoderTest("<text> <p> The  Declaration  of  Independence  was  signed  on  <t _cat=\"fulldate\" date=\"1776-07-04\"> July  4th,  1776 </t> ,  launching  a  nation. </p> </text>",
                  expected);

    // empty tokens
    doDecoderTest("<text> <p> The  Declaration <t/> of  Independence <t>  </t> was  signed  on  <t _cat=\"fulldate\" date=\"1776-07-04\"> July  4th,  1776 </t> ,  launching  a  nation. </p> </text>",
                  expected);

    // tokens
    final XmlInputDecoder decoder =
      doDecoderTest("<text><p>The Declaration of Independence was signed on <t _cat=\"fulldate\" date=\"1776-07-04\">July 4th, 1776</t>, launching a nation.</p></text>",
                    expected);

    final XmlInputDecoder.Paragraph p1 = decoder.getParagraphs().get(0);
    verifyTokens(p1, 1,
                 new String[]{"fulldate"},
                 new String[]{"July 4th, 1776"},
                 new String[][]{{"date"}},
                 new String[][]{{"1776-07-04"}});
  }

  public void testNestedTokens() throws IOException {
    final String[] expected = new String[]{"The Declaration of Independence was signed on July 4th , 1776 , launching a nation."};

    final XmlInputDecoder decoder =
      doDecoderTest("<text><p>The Declaration of Independence was signed on <t _cat=\"fulldate\" date=\"1776-07-04\"><t _cat=\"m\" month=\"7\">July</t> <t _cat=\"d\" day=\"4\">4th</t>, <t _cat=\"y\" year=\"1776\">1776</t></t>, launching a nation.</p></text>",
                    expected);

    final XmlInputDecoder.Paragraph p1 = decoder.getParagraphs().get(0);
    verifyTokens(p1, 4,
                 new String[]{"fulldate", "m", "d", "y"},
                 new String[]{"July 4th , 1776", "July", "4th", "1776"},
                 new String[][]{{"date"}, {"month"}, {"day"}, {"year"}},
                 new String[][]{{"1776-07-04"}, {"7"}, {"4"}, {"1776"}});
  }

  public void testBreaks() throws IOException {
    final String[] expected = new String[]{"testing 1 2#$%3 ,45 testing"};

    final XmlInputDecoder decoder =
      doDecoderTest("<text><p>testing 1<b/>2<b delim=\"#$%\"/>3<b/>,4<b delim=\"\"/>5 testing</p></text>",
                    expected);

    final XmlInputDecoder.Paragraph p1 = decoder.getParagraphs().get(0);

    assertTrue(p1.hasBreaks());
    assertEquals(4, p1.getBreakMarkers().size());
  }

  public void testBreaksWithTokens() throws IOException {
    final String[] expected = new String[]{"The (\"Declaration of Independence\") was signed on July 4th , 1776 , launching a nation."};

    final XmlInputDecoder decoder =
      doDecoderTest("<text><p>The <b delim=\" (&quot;\"/>Declaration of Independence <b delim=\"&quot;) \"/> was signed on <t _cat=\"fulldate\" date=\"1776-07-04\"><t _cat=\"m\" month=\"7\">July</t> <t _cat=\"d\" day=\"4\">4th</t>, <t _cat=\"y\" year=\"1776\">1776</t></t>, launching a nation.</p></text>",
                    expected);

    final XmlInputDecoder.Paragraph p1 = decoder.getParagraphs().get(0);

    assertTrue(p1.hasBreaks());
    assertEquals(2, p1.getBreakMarkers().size());
                 
    // make sure the extra delim chars didn't break the tokens
    verifyTokens(p1, 4,
                 new String[]{"fulldate", "m", "d", "y"},
                 new String[]{"July 4th , 1776", "July", "4th", "1776"},
                 new String[][]{{"date"}, {"month"}, {"day"}, {"year"}},
                 new String[][]{{"1776-07-04"}, {"7"}, {"4"}, {"1776"}});
  }

  public void testSplitParagraphs() throws IOException {
    final String[] expected = new String[]{"The (\"Declaration of Independence\") was signed on July 4th , 1776 , launching a nation."};

    final XmlInputDecoder decoder =
      doDecoderTest("<text><p>The <b delim=\" (&quot;\"/>Declaration of Independence <b delim=\"&quot;) \"/> was signed on <t _cat=\"fulldate\" date=\"1776-07-04\"><t _cat=\"m\" month=\"7\">July</t> <t _cat=\"d\" day=\"4\">4th</t>, <t _cat=\"y\" year=\"1776\">1776</t></t>, launching a nation.</p></text>",
                    expected);

    final XmlInputDecoder.Paragraph p1 = decoder.getParagraphs().get(0);

    // split on non letters/digits (w/accompanying whitespace)
    final String[] pieces = expected[0].split(" *[^a-zA-Z0-9 ]+ *");
    final List<String> piecesList = Arrays.asList(pieces);
    final List<XmlInputDecoder.Paragraph> splits = p1.split(piecesList.iterator());

    verifyParagraphs(splits, new String[] {
        "The",
        "Declaration of Independence",
        "was signed on July 4th",
        "1776",
        "launching a nation",
      });
  }


  private final XmlInputDecoder doDecoderTest(String xmlString, String[] paragraphs) throws IOException {
    final DomNode textNode = XmlFactory.buildDomNode(xmlString, false);

    final XmlInputDecoder decoder = new XmlInputDecoder(textNode != null ? textNode.asDomElement() : null);
    final List<XmlInputDecoder.Paragraph> decodedParagraphs = decoder.getParagraphs();

    verifyParagraphs(decodedParagraphs, paragraphs);

    return decoder;
  }

  private final void verifyParagraphs(List<XmlInputDecoder.Paragraph> decodedParagraphs, String[] paragraphs) {
    if (paragraphs.length != decodedParagraphs.size()) {
      System.out.println("got:");
      int count = 1;
      for (XmlInputDecoder.Paragraph decodedParagraph : decodedParagraphs) {
        System.out.println("\t" + (count++) + ": " + decodedParagraph);
      }
    }

    assertEquals(paragraphs.length, decodedParagraphs.size());

    for (int i = 0; i < paragraphs.length; ++i) {
      final String paragraph = paragraphs[i];
      final XmlInputDecoder.Paragraph decodedParagraph = decodedParagraphs.get(i);
      assertEquals("p #" + i + "/" + paragraphs.length,
                   paragraph, decodedParagraph.getText());
    }
  }

  private final void verifyTokens(XmlInputDecoder.Paragraph p, int numTokens, String[] categories, String[] texts, String[][] atts, String[][] vals) {
    // p -- paragraph to check
    // numTokens -- expected number of tokens
    // categories -- expected category for each token
    // texts -- expected text for each token
    // atts -- attributes for each token to check
    // vals -- corresponding values for each token to find

    if (numTokens <= 0) {
      assertFalse(p.hasTokens());
    }
    else {
      assertTrue(p.hasTokens());

      final List<XmlInputDecoder.MarkerInfo> tokenStarts = p.getTokenStarts();
      assertEquals(numTokens, tokenStarts.size());

      for (int i = 0; i < numTokens; ++i) {
        final XmlInputDecoder.MarkerInfo tokenStart = tokenStarts.get(i);
        if (categories != null) {
          assertEquals(categories[i], tokenStart.getCategory());
        }
        assertTrue(tokenStart.hasOtherInfo());
        assertEquals(texts[i], p.getText().substring(tokenStart.getPos(), tokenStart.getOtherInfo().getPos()));

        // check attributes
        final Map<String, String> tokenAttributes = tokenStart.getAttributes();
        if (tokenAttributes != null) {
          assertFalse(tokenAttributes.containsKey("_cat"));
        }
        if (atts != null && vals != null) {
          assertEquals(atts[i].length, tokenAttributes.size());

          for (int j = 0; j < atts[i].length; ++j) {
            assertEquals("Token #" + i + ", att #" + j + " mismatch (" + atts[i][j] + "=" + vals[i][j] + ")",
                         vals[i][j], tokenAttributes.get(atts[i][j]));
          }
        }
      }
    }
  }


  public void testTokenAfterExplicitDelim() throws IOException {
    final DomNode textNode = XmlFactory.buildDomNode(
      "<text oneLine=\"true\"><p oneLine=\"true\"><t>Smith</t><b _type=\"soft\" delim=\", \"/><t>John</t></p></text>",
      false);
    final XmlInputDecoder decoder = new XmlInputDecoder(textNode.asDomElement());
    final XmlInputDecoder.Paragraph p = decoder.getParagraphs().get(0);

    //NOTE: defect was that second token came out as "ohn" -- fixed.
    verifyTokens(p, 2, null, new String[]{"Smith", "John"}, null, null);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestXmlInputDecoder.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
