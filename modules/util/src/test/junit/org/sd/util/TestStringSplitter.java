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
 * JUnit Tests for the StringSplitter class.
 * <p>
 * @author Spence Koehler
 */
public class TestStringSplitter extends TestCase {

  public TestStringSplitter(String name) {
    super(name);
  }
  
  public void testSplitOnWhitespace() {
    final String input = "  this is   a \t  test. \n ";
    final String[] expected = {"this", "is", "a", "test."};
    final String[] got = StringSplitter.splitOnWhitespace(input);

    checkExpectations(expected, got);
  }

  public void testSplitAlphaNumerics(String data) {
    final String[] expected = {"testing", "a123", "testing"};
    final String[] got = StringSplitter.splitAlphaNumerics(data);

    checkExpectations(expected, got);
  }

  private void checkExpectations(String[] expected, String[] got) {
    if (expected == null) {
      assertNull(got);
    }
    else {
      assertNotNull(got);
      assertEquals(expected.length, got.length);
      for (int i = 0; i < expected.length; ++i) {
        assertEquals(expected[i], got[i]);
      }
    }
  }

  public void testExpand() {
    checkExpand("Mr.T", "Mr. T");
    checkExpand("mr.t", "mr. t");
    checkExpand(".", ".");
    checkExpand("Mr.", "Mr.");
    checkExpand(".dat", ".dat");

    checkExpand("a123b", "a 123 b");
    checkExpand("43b", "43 b");
    checkExpand("a34", "a 34");
    checkExpand("1 2 a b 3", "1 2 a b 3");

    checkExpand("nothing to do", "nothing to do");
    checkExpand("expandCamelCase", "expand Camel Case");
    checkExpand("XXXYzz", "XXX Yzz");
    checkExpand("aaa BBB Ccc", "aaa BBB Ccc");
    checkExpand("StPaul", "St Paul");
  }

  private final void checkExpand(String input, String expected) {
    final String got = StringSplitter.expand(input, " ");
    
    if (expected == null) {
      assertNull(got);
    }
    else {
      assertNotNull(got);
      assertEquals("got: '" + got + "'", expected, got);
    }
  }

  public void testSplitOnChar1() {
    String input = ",a,b,,c,d,e,";
    String[] pieces = StringSplitter.splitOnChar(input, ',');

    assertEquals(8, pieces.length);
  }

  public void testSplitOnFirstEquals1() {
    String input = "a=b";
    String[] pieces = StringSplitter.splitOnFirstEquals(input);

    assertNotNull(pieces);
    assertEquals(2, pieces.length);

    assertEquals("a", pieces[0]);
    assertEquals("b", pieces[1]);
  }

  public void testSplitOnFirstEquals2() {
    String input = "=b";
    String[] pieces = StringSplitter.splitOnFirstEquals(input);

    assertNotNull(pieces);
    assertEquals(2, pieces.length);

    assertEquals("", pieces[0]);
    assertEquals("b", pieces[1]);
  }

  public void testSplitOnFirstEquals3() {
    String input = "a=";
    String[] pieces = StringSplitter.splitOnFirstEquals(input);

    assertNotNull(pieces);
    assertEquals(2, pieces.length);

    assertEquals("a", pieces[0]);
    assertEquals("", pieces[1]);
  }

  public void testSplitOnFirstEquals4() {
    String input = "=";
    String[] pieces = StringSplitter.splitOnFirstEquals(input);

    assertNotNull(pieces);
    assertEquals(2, pieces.length);

    assertEquals("", pieces[0]);
    assertEquals("", pieces[1]);
  }

  public void testSplitOnFirstEquals5() {
    String input = "a=b=c";
    String[] pieces = StringSplitter.splitOnFirstEquals(input);

    assertNotNull(pieces);
    assertEquals(2, pieces.length);

    assertEquals("a", pieces[0]);
    assertEquals("b=c", pieces[1]);
  }

  public void testSplitOnFirstEquals6() {
    String input = "";
    String[] pieces = StringSplitter.splitOnFirstEquals(input);

    assertNull(pieces);
  }

  public void testSplitOnFirstEquals7() {
    String input = null;
    String[] pieces = StringSplitter.splitOnFirstEquals(input);

    assertNull(pieces);
  }

  public void testSplitOnFirstEquals8() {
    String input = "gobbledygook";
    String[] pieces = StringSplitter.splitOnFirstEquals(input);

    assertNull(pieces);
  }

  public void testSplitOnParens1() {
    String input = "a(b)c";
    String[] pieces = StringSplitter.splitOnParens(input);

    assertNotNull(pieces);
    assertEquals(3, pieces.length);

    assertEquals("a", pieces[0]);
    assertEquals("b", pieces[1]);
    assertEquals("c", pieces[2]);
  }

  public void testSplitOnParens2() {
    String input = "a(b(inner()))c";
    String[] pieces = StringSplitter.splitOnParens(input);

    assertNotNull(pieces);
    assertEquals(3, pieces.length);

    assertEquals("a", pieces[0]);
    assertEquals("b(inner())", pieces[1]);
    assertEquals("c", pieces[2]);
  }

  public void testSplitOnParens3() {
    String input = ";)a(b(inner()))(()c)";
    String[] pieces = StringSplitter.splitOnParens(input);

    assertNotNull(pieces);
    assertEquals(3, pieces.length);

    assertEquals(";)a", pieces[0]);
    assertEquals("b(inner())", pieces[1]);
    assertEquals("(()c)", pieces[2]);
  }

  public void testCantSplitOnParens() {
    String input = "gobbledy)(gook(";
    String[] pieces = StringSplitter.splitOnParens(input);

    assertNull(pieces);
  }

  public void testSplitOnParensAndSquares() {
    String input = "a(b)[c]";
    String[] pieces = StringSplitter.splitOnParens(input);

    assertNotNull(pieces);
    assertEquals(3, pieces.length);

    assertEquals("a", pieces[0]);
    assertEquals("b", pieces[1]);
    assertEquals("[c]", pieces[2]);

    pieces = StringSplitter.splitOnSquareBrackets(pieces[2]);

    assertNotNull(pieces);
    assertEquals(3, pieces.length);

    assertEquals("", pieces[0]);
    assertEquals("c", pieces[1]);
    assertEquals("", pieces[2]);
    
  }

  public void testSplitOnFirstSpace() {
    String[] pieces = StringSplitter.splitOnFirstSpace("a");
    assertNotNull(pieces);
    assertEquals(1, pieces.length);
    assertEquals("a", pieces[0]);
  }

  public void testHypertrim() {
    assertEquals("a", StringSplitter.hypertrim("a"));
    assertEquals("testing", StringSplitter.hypertrim("testing"));
    assertEquals("testing this stuff", StringSplitter.hypertrim("   testing   this    stuff      "));
  }

  public void testHypertrimWithQuotes() {
    assertEquals("a\\ \\ b c", StringSplitter.hypertrim("a\\ \\ b c"));
    assertEquals("a\\  b c", StringSplitter.hypertrim("a\\  b c"));
    assertEquals("a 'b  c'", StringSplitter.hypertrim("a 'b  c'"));
    assertEquals("a \"b  c\"", StringSplitter.hypertrim("a \"b  c\""));
    assertEquals("a \"b  c's  \"", StringSplitter.hypertrim("a \"b  c's  \""));
  }

  public void testReplaceDiacritics() {
    assertEquals("Koehler", StringSplitter.replaceDiacritics("Köhler"));
    assertEquals("fuer", StringSplitter.replaceDiacritics("für"));
    assertEquals("schwaebisch", StringSplitter.replaceDiacritics("schwäbisch"));
    assertEquals("gmuend", StringSplitter.replaceDiacritics("gmünd"));
    assertEquals("OeHF", StringSplitter.replaceDiacritics("ÖHF"));
    assertEquals("krauss", StringSplitter.replaceDiacritics("krauß"));
    assertEquals("Camera", StringSplitter.replaceDiacritics("Caméra"));
    assertEquals("gross", StringSplitter.replaceDiacritics("groβ"));
    assertEquals("Decor", StringSplitter.replaceDiacritics("Décor"));  // emacs converted from DOS file
    assertEquals("Decor", StringSplitter.replaceDiacritics("D�cor"));  // when read in as utf-8 from DOS file
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestStringSplitter.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
