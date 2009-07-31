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
package org.sd.util;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the Escaper class.
 * <p>
 * @author Spence Koehler
 */
public class TestEscaper extends TestCase {

  private Escaper escaper;

  private static String[][] testPairs = new String[][]{
    {"(a[b=c,d;f=g] (h i))(r)", "&lp;a&sbl;b&eq;c&comma;d&sc;f&eq;g&sbr;&space;&lp;h&space;i&rp;&rp;&lp;r&rp;"},
  };

  public TestEscaper(String name) {
    super(name);
  }

  public void setUp() throws Exception {
    super.setUp();
    escaper = new Escaper(
      new String[][]{
        {"&", "&amp;"}, // ampersand -- this MUST be one of FIRST 2!
        {";", "&sc;"}, // semicolon -- this MUST be one of FIRST 2!

        {"[", "&sbl;"}, // square bracket left
        {"]", "&sbr;"}, // square bracket right
        {"(", "&lp;"}, // left parenthesis
        {")", "&rp;"}, // right parenthesis
        {"=", "&eq;"}, // equals
        {",", "&comma;"}, // comma
        {" ", "&space;"}, // space

      }, 2);
  }

  public void testEscapingPairs() {
    for (int i = 0; i < testPairs.length; ++i) {
      assertEquals(testPairs[i][1], escaper.escape(testPairs[i][0]));
    }
  }

  public void testUnscapingPairs() {
    for (int i = 0; i < testPairs.length; ++i) {
      assertEquals(testPairs[i][0], escaper.unescape(testPairs[i][1]));
    }
  }

  public void testDoubleEscape1() {
    for (int i = 0; i < testPairs.length; ++i) {
      String escaped1 = escaper.escape(testPairs[i][0]);
      String escaped2 = escaper.escape(escaped1);
      String unescaped2 = escaper.unescape(escaped2);
      String unescaped1 = escaper.unescape(unescaped2);

      assertEquals(testPairs[i][0], unescaped1);
    }
  }

  public void testDoubleEscape2() {
    for (int i = 0; i < testPairs.length; ++i) {
      assertEquals(testPairs[i][1], escaper.unescape(escaper.escape(testPairs[i][1])));
    }
  }

  public void testDoubleEscape3() {
    for (int i = 0; i < testPairs.length; ++i) {
      assertEquals(testPairs[i][0], escaper.unescape(escaper.escape(testPairs[i][0])));
    }
  }

  public void testUnescapeUnescaped() {
    for (int i = 0; i < testPairs.length; ++i) {
      assertEquals(testPairs[i][0], escaper.unescape(testPairs[i][0]));
    }
  }

  public void testEscapeCloseParen() {
    String original = "NoC) /JAEGGER/";
    String escaped1 = escaper.escape(original);
    String escaped2 = escaper.escape(escaped1);

    String unescaped1 = escaper.unescape(escaped2);
    String unoriginal = escaper.unescape(unescaped1);

    assertEquals(original, unoriginal);
  }

  //test unescaping twice; unescaping non-escaped; etc...

  public static Test suite() {
    TestSuite suite = new TestSuite(TestEscaper.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
