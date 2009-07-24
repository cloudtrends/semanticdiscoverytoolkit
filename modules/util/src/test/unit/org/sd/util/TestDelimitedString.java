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

import java.util.List;

/**
 * JUnit Tests for the DelimitedString class.
 * <p>
 * @author Spence Koehler
 */
public class TestDelimitedString extends TestCase {

  public TestDelimitedString(String name) {
    super(name);
  }
  
  private void doToggleTest(String[] expected, String input, char[] stringDelims) {
    final List<String> toggle = DelimitedString.getToggleStrings(input, stringDelims);

    assertEquals("got=" + toggle, expected.length, toggle.size());
    for (int i = 0; i < expected.length; ++i) {
      assertEquals("mismatch at pos=" + i + ", got=" + toggle, expected[i], toggle.get(i));
    }
  }

  public void testToggleStrings() {
    doToggleTest(new String[]{"", "a", "", "b", "", "c"}, "a b c", null);
    doToggleTest(new String[]{". . .", "a", ". . .", "b", ". . .", "c"}, ". . .a. . .b. . .c", null);
    doToggleTest(new String[]{". . .", "a", ". . . ", "b", " . . . ", "c"}, ".  .   .a.  .  . b  .   . . c", null);
    doToggleTest(new String[]{"", "a", ", ", "b-c", ", ", "d"}, "a, b-c, d", new char[]{'-'});
    doToggleTest(new String[]{"", "a", "; ", "b-c", "; ", "-d", "; ", "e-"}, "a; b-c; -d; e-", new char[]{'-'});
    doToggleTest(new String[]{"", "Dr.", "", "Z.", "", "M.D.", ", ", "Ph.D."}, "Dr. Z. M.D., Ph.D.", new char[]{'.'});
    doToggleTest(new String[]{"", "Dr.", "", "Z.", "", "M.D.", " , ", "Ph.D."}, "  Dr.   Z.   M.D. ,  Ph.D.  " , new char[]{'.'});
    doToggleTest(new String[]{"", "Dr.", "", "Z.", "", "M.D.", " , ", "Ph.D.",  " ;"}, "  Dr.   Z.   M.D. ,  Ph.D.  ; " , new char[]{'.'});
    doToggleTest(new String[]{"", "Dr.", "", "Z.", "", "M.D.", " , ", "Ph.D.",  " ;"}, "  Dr.   Z.   M.D. ,  Ph.D.  ; " , new char[]{'.'});
  }

  private void doCleanTest(String expected, String input, char[] stringDelims, boolean keepDelims) {
    final DelimitedString dString = new DelimitedString(input, stringDelims);
    final String clean = dString.getCleanInput(keepDelims);
    assertEquals("expected=" + expected + " got=" + clean, expected, clean);

    // ensure clean string exhibits lossless parsing
    final List<String> toggleOrig = DelimitedString.getToggleStrings(input, stringDelims);
    final List<String> toggleClean = DelimitedString.getToggleStrings(clean, stringDelims);
    if (keepDelims) assertEquals(toggleOrig, toggleClean);
  }

  public void testGetCleanInput() {
    doCleanTest("a b c", "a b c", null, true);
    doCleanTest("a b c", "a b c", null, false);
    doCleanTest(". . .a. . .b. . .c", ". . .a. . .b. . .c", null, true);
    doCleanTest("a b c", ". . .a. . .b. . .c", null, false);
    doCleanTest(". . .a. . . b . . . c", ".  .   .a.  .  . b  .   . . c", null, true);
    doCleanTest("a b c", ".  .   .a.  .  . b  .   . . c", null, false);
    doCleanTest("a, b-c, d", "a, b-c, d", new char[]{'-'}, true);
    doCleanTest("a b-c d", "a, b-c, d", new char[]{'-'}, false);
    doCleanTest("a; b-c; -d; e-", "a; b-c; -d; e-", new char[]{'-'}, true);
    doCleanTest("a b-c -d e-", "a; b-c; -d; e-", new char[]{'-'}, false);
    doCleanTest("Dr. Z. M.D., Ph.D.", "Dr. Z. M.D., Ph.D.", new char[]{'.'}, true);
    doCleanTest("Dr. Z. M.D. Ph.D.", "Dr. Z. M.D., Ph.D.", new char[]{'.'}, false);
    doCleanTest("Dr. Z. M.D. , Ph.D.", "  Dr.   Z.   M.D. ,  Ph.D.  " , new char[]{'.'}, true);
    doCleanTest("Dr. Z. M.D. Ph.D.", "  Dr.   Z.   M.D. ,  Ph.D.  " , new char[]{'.'}, false);
    doCleanTest("Dr. Z. M.D. , Ph.D. ;", "  Dr.   Z.   M.D. ,  Ph.D.  ; ", new char[]{'.'}, true);
    doCleanTest("Dr. Z. M.D. Ph.D.", "  Dr.   Z.   M.D. ,  Ph.D.  ; ", new char[]{'.'}, false);
    doCleanTest("a'b./cd-ef", "a'b./cd-ef", new char[]{'-', '\'', '/', '.'}, false);
    doCleanTest("foo 12-5", "foo 12-5", new char[]{'-'}, false);
  }

  private void doStringsTest(String[] expected, String input, char[] stringDelims) {
    final DelimitedString dString = new DelimitedString(input, stringDelims);
    assertEquals(expected.length, dString.numStrings());
    for (int i = 0; i < expected.length; ++i) {
      assertEquals(expected[i], dString.getString(i));
    }
  }

  public void testGetString() {
    doStringsTest(new String[]{"a", "b", "c"}, "a b c", null);
    doStringsTest(new String[]{"a", "b", "c"}, ". . .a. . .b. . .c", null);
    doStringsTest(new String[]{"a", "b", "c"}, ".  .   .a.  .  . b  .   . . c", null);
    doStringsTest(new String[]{"a", "b-c", "d"}, "a, b-c, d", new char[]{'-'});
    doStringsTest(new String[]{"a", "b-c", "-d", "e-"}, "a; b-c; -d; e-", new char[]{'-'});
    doStringsTest(new String[]{"Dr.", "Z.", "M.D.", "Ph.D."}, "Dr. Z. M.D., Ph.D.", new char[]{'.'});
    doStringsTest(new String[]{"Dr.", "Z.", "M.D.", "Ph.D."}, "  Dr.   Z.   M.D. ,  Ph.D.  " , new char[]{'.'});
    doStringsTest(new String[]{"Dr.", "Z.", "M.D.", "Ph.D."}, "  Dr.   Z.   M.D. ,  Ph.D.  ; " , new char[]{'.'});
  }

  private void doPreDelimTest(String[] expected, String input, char[] stringDelims) {
    final DelimitedString dString = new DelimitedString(input, stringDelims);
    for (int i = 0; i < expected.length; ++i) {
      assertEquals(expected[i], dString.getPreDelim(i));
    }
  }

  public void testGetPreDelim() {
    doPreDelimTest(new String[]{null, null, null}, "a b c", null);
    doPreDelimTest(new String[]{". . .", ". . .", ". . ."}, ". . .a. . .b. . .c", null);
    doPreDelimTest(new String[]{". . .", ". . .", ". . ."}, ".  .   .a.  .  . b  .   . . c", null);
    doPreDelimTest(new String[]{null, ",", ","}, "a, b-c, d", new char[]{'-'});
    doPreDelimTest(new String[]{null, ";", ";", ";"}, "a; b-c; -d; e-", new char[]{'-'});
    doPreDelimTest(new String[]{null, null, null, ","}, "Dr. Z. M.D., Ph.D.", new char[]{'.'});
    doPreDelimTest(new String[]{null, null, null, ","}, "  Dr.   Z.   M.D. ,  Ph.D.  " , new char[]{'.'});
    doPreDelimTest(new String[]{null, null, null, ","}, "  Dr.   Z.   M.D. ,  Ph.D.  ; " , new char[]{'.'});
  }

  private void doPostDelimTest(String[] expected, String input, char[] stringDelims) {
    final DelimitedString dString = new DelimitedString(input, stringDelims);
    for (int i = 0; i < expected.length; ++i) {
      assertEquals(expected[i], dString.getPostDelim(i));
    }
  }

  public void testGetPostDelim() {
    doPostDelimTest(new String[]{null, null, null}, "a b c", null);
    doPostDelimTest(new String[]{". . .", ". . .", null}, ". . .a. . .b. . .c", null);
    doPostDelimTest(new String[]{". . .", ". . .", null}, ".  .   .a.  .  . b  .   . . c", null);
    doPostDelimTest(new String[]{",", ",", null}, "a, b-c, d", new char[]{'-'});
    doPostDelimTest(new String[]{";", ";", ";", null}, "a; b-c; -d; e-", new char[]{'-'});
    doPostDelimTest(new String[]{null, null, ",", null}, "Dr. Z. M.D., Ph.D.", new char[]{'.'});
    doPostDelimTest(new String[]{null, null, ",", null}, "  Dr.   Z.   M.D. ,  Ph.D.  " , new char[]{'.'});
    doPostDelimTest(new String[]{null, null, ",", ";"}, "  Dr.   Z.   M.D. ,  Ph.D.  ; " , new char[]{'.'});
  }

  public void testContinuousSegment1() {
    final DelimitedString dString = new DelimitedString("this is a test, only a test", null);
    DelimitedString.ContinuousSegment segment = null;

    segment = dString.getSegment(0);
    assertEquals("this is a test", segment.getString());
    assertEquals(4, segment.length());
    assertEquals("only a test", segment.next().getString());
    segment = segment.shorten();
    assertEquals("this is a", segment.getString());
    assertEquals(3, segment.length());
    assertEquals("test", segment.next().getString());
    segment = segment.shorten();
    assertEquals("this is", segment.getString());
    assertEquals(2, segment.length());
    assertEquals("a test", segment.next().getString());
    segment = segment.shorten();
    assertEquals("this", segment.getString());
    assertEquals(1, segment.length());
    assertEquals("is a test", segment.next().getString());
    segment = segment.shorten();
    assertEquals(null, segment);

    segment = dString.getSegment(1);
    assertEquals("is a test", segment.getString());
    assertEquals(3, segment.length());
    assertEquals("only a test", segment.next().getString());
    segment = segment.shorten();
    assertEquals("is a", segment.getString());
    assertEquals(2, segment.length());
    assertEquals("test", segment.next().getString());
    segment = segment.shorten();
    assertEquals("is", segment.getString());
    assertEquals(1, segment.length());
    assertEquals("a test", segment.next().getString());
    segment = segment.shorten();
    assertNull(segment);

    segment = dString.getSegment(2);
    assertEquals("a test", segment.getString());
    assertEquals(2, segment.length());
    assertEquals("only a test", segment.next().getString());
    segment = segment.shorten();
    assertEquals("a", segment.getString());
    assertEquals(1, segment.length());
    assertEquals("test", segment.next().getString());
    segment = segment.shorten();
    assertNull(segment);

    segment = dString.getSegment(3);
    assertEquals("test", segment.getString());
    assertEquals(1, segment.length());
    assertEquals("only a test", segment.next().getString());
    segment = segment.shorten();
    assertNull(segment);

    segment = dString.getSegment(4);
    assertEquals("only a test", segment.getString());
    assertEquals(3, segment.length());
    assertNull(segment.next());
    segment = segment.shorten();
    assertEquals("only a", segment.getString());
    assertEquals(2, segment.length());
    assertEquals("test", segment.next().getString());
    segment = segment.shorten();
    assertEquals("only", segment.getString());
    assertEquals(1, segment.length());
    assertEquals("a test", segment.next().getString());
    segment = segment.shorten();
    assertNull(segment);

    segment = dString.getSegment(5);
    assertEquals("a test", segment.getString());
    assertEquals(2, segment.length());
    assertNull(segment.next());
    segment = segment.shorten();
    assertEquals("a", segment.getString());
    assertEquals(1, segment.length());
    assertEquals("test", segment.next().getString());
    segment = segment.shorten();
    assertNull(segment);

    segment = dString.getSegment(6);
    assertEquals("test", segment.getString());
    assertEquals(1, segment.length());
    assertNull(segment.next());
    segment = segment.shorten();
    assertNull(segment);

    segment = dString.getSegment(7);
    assertNull(segment);
  }

  public void testContinuousSegment2() {
    final DelimitedString dString = new DelimitedString("this is a test, only a test!", null);
    DelimitedString.ContinuousSegment segment = null;

    segment = dString.getSegment(0);
    assertEquals("this is a test", segment.getString());
    assertEquals(4, segment.length());
    assertEquals("only a test", segment.next().getString());
    segment = segment.shorten();
    assertEquals("this is a", segment.getString());
    assertEquals(3, segment.length());
    assertEquals("test", segment.next().getString());

    assertEquals("this is a test,", segment.next().getInputThrough(" this  is a test,  only a  test!"));
    assertEquals("only a test!", segment.next().getInputBeyond(" this  is a test,  only a  test!"));

    segment = segment.shorten();
    assertEquals("this is", segment.getString());
    assertEquals(2, segment.length());
    assertEquals("a test", segment.next().getString());
    segment = segment.shorten();
    assertEquals("this", segment.getString());
    assertEquals(1, segment.length());

    assertEquals("this", segment.getInputThrough(" this  is a test,  only a  test!"));
    assertEquals("is a test, only a test!", segment.getInputBeyond(" this  is a test,  only a  test!"));

    assertEquals("is a test", segment.next().getString());
    segment = segment.shorten();
    assertEquals(null, segment);

    segment = dString.getSegment(1);
    assertEquals("is a test", segment.getString());
    assertEquals(3, segment.length());
    assertEquals("only a test", segment.next().getString());
    segment = segment.shorten();
    assertEquals("is a", segment.getString());
    assertEquals(2, segment.length());
    assertEquals("test", segment.next().getString());

    assertEquals("this is a test,", segment.next().getInputThrough(" this  is a test,  only a  test!"));
    assertEquals("only a test!", segment.next().getInputBeyond(" this  is a test,  only a  test!"));

    segment = segment.shorten();
    assertEquals("is", segment.getString());
    assertEquals(1, segment.length());
    assertEquals("a test", segment.next().getString());
    segment = segment.shorten();
    assertNull(segment);

    segment = dString.getSegment(2);
    assertEquals("a test", segment.getString());
    assertEquals(2, segment.length());
    assertEquals("only a test", segment.next().getString());
    segment = segment.shorten();
    assertEquals("a", segment.getString());
    assertEquals(1, segment.length());
    assertEquals("test", segment.next().getString());
    segment = segment.shorten();
    assertNull(segment);

    segment = dString.getSegment(3);
    assertEquals("test", segment.getString());
    assertEquals(1, segment.length());
    assertEquals("only a test", segment.next().getString());
    segment = segment.shorten();
    assertNull(segment);

    segment = dString.getSegment(4);
    assertEquals("only a test", segment.getString());
    assertEquals(3, segment.length());
    assertNull(segment.next());
    segment = segment.shorten();
    assertEquals("only a", segment.getString());
    assertEquals(2, segment.length());
    assertEquals("test", segment.next().getString());
    segment = segment.shorten();
    assertEquals("only", segment.getString());
    assertEquals(1, segment.length());
    assertEquals("a test", segment.next().getString());
    segment = segment.shorten();
    assertNull(segment);

    segment = dString.getSegment(5);
    assertEquals("a test", segment.getString());
    assertEquals(2, segment.length());
    assertNull(segment.next());
    segment = segment.shorten();
    assertEquals("a", segment.getString());
    assertEquals(1, segment.length());
    assertEquals("test", segment.next().getString());
    segment = segment.shorten();
    assertNull(segment);

    segment = dString.getSegment(6);
    assertEquals("test", segment.getString());

    assertEquals("this is a test, only a test!", segment.getInputThrough(" this  is a test,  only a  test!"));
    assertEquals("", segment.getInputBeyond(" this  is a test,  only a  test!"));

    assertEquals(1, segment.length());
    assertNull(segment.next());
    segment = segment.shorten();
    assertNull(segment);

    segment = dString.getSegment(7);
    assertNull(segment);
  }

  public void testGetStringSequence() {
    final DelimitedString dString = new DelimitedString("this is a test, only a test!", null);

    assertEquals("this is a test only a test", dString.getString(0, 7, false));
    assertEquals("this is a test, only a test", dString.getString(0, 7, true));
    assertEquals("this is a test only a", dString.getString(0, 6, false));
    assertEquals("this is a test, only a", dString.getString(0, 6, true));

    assertEquals("is a test only", dString.getString(1, 5, false));
    assertEquals("is a test, only", dString.getString(1, 5, true));
  }

  public void testUTF8() {
    doStringsTest(new String[]{"München", "Ühlingen-Birkendorf", "Ölbronn-Dürrn"}, "München Ühlingen-Birkendorf Ölbronn-Dürrn", new char[]{'-'});
    doStringsTest(new String[]{"München", "Ühlingen", "Birkendorf", "Ölbronn", "Dürrn"}, "München Ühlingen-Birkendorf Ölbronn-Dürrn", null);
    doStringsTest(new String[]{"münchen", "ühlingen-birkendorf", "ölbronn-dürrn"}, "münchen ühlingen-birkendorf ölbronn-dürrn", new char[]{'-'});
    doStringsTest(new String[]{"münchen", "ühlingen", "birkendorf", "ölbronn", "dürrn"}, "münchen ühlingen-birkendorf ölbronn-dürrn", null);

    doCleanTest("München Ühlingen-Birkendorf Ölbronn-Dürrn", "München Ühlingen-Birkendorf Ölbronn-Dürrn", new char[]{'-'}, false);
    doCleanTest("München Ühlingen Birkendorf Ölbronn Dürrn", "München Ühlingen-Birkendorf Ölbronn-Dürrn", null, false);
    doCleanTest("münchen ühlingen-birkendorf ölbronn-dürrn", "münchen ühlingen-birkendorf ölbronn-dürrn", new char[]{'-'}, false);
    doCleanTest("münchen ühlingen birkendorf ölbronn dürrn", "münchen ühlingen-birkendorf ölbronn-dürrn", null, false);
    doCleanTest("münchen ühlingen-birkendorf ölbronn-dürrn", "münchen ühlingen-birkendorf ölbronn-dürrn", null, true);
    doCleanTest("Über dem Dieterstedter Bache", "Über dem Dieterstedter Bache", new char[]{'-'}, false);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestDelimitedString.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
