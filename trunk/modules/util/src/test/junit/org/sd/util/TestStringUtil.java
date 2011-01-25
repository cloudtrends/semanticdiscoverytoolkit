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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * JUnit Tests for the StringUtil class.
 * <p>
 * @author Spence Koehler
 */
public class TestStringUtil extends TestCase {

  public TestStringUtil(String name) {
    super(name);
  }
  
  public void testIsWord() {
    assertTrue(StringUtil.isWord("foo"));
    assertTrue(StringUtil.isWord("Foo"));
    assertTrue(StringUtil.isWord("a"));
    assertTrue(StringUtil.isWord("A"));
    assertFalse(StringUtil.isWord(""));
    assertFalse(StringUtil.isWord("123"));
    assertFalse(StringUtil.isWord("-#@!"));
    assertFalse(StringUtil.isWord("a-b"));
    assertFalse(StringUtil.isWord("a b"));
    assertFalse(StringUtil.isWord(" ab"));
    assertFalse(StringUtil.isWord("ab "));
  }

  public void testHyphenatedWord() {
    assertFalse(StringUtil.hyphenatedWord("foo"));
    assertFalse(StringUtil.hyphenatedWord("-foo"));
    assertFalse(StringUtil.hyphenatedWord("foo-"));
    assertFalse(StringUtil.hyphenatedWord("foo--bar"));
    assertTrue(StringUtil.hyphenatedWord("foo-bar"));
    assertFalse(StringUtil.hyphenatedWord("foo-bar-"));
    assertFalse(StringUtil.hyphenatedWord("-foo-bar-"));
    assertTrue(StringUtil.hyphenatedWord("foo-bar-baz"));
    assertFalse(StringUtil.hyphenatedWord("foo-bar-baz--"));
    assertFalse(StringUtil.hyphenatedWord("--foo-bar-baz"));
    assertFalse(StringUtil.hyphenatedWord("--foo-bar-baz--"));
    assertFalse(StringUtil.hyphenatedWord("foo-%%%"));
  }

  public void testHyphenatedCapitalizedWord() {
    assertFalse(StringUtil.hyphenatedCapitalizedWord("foo"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("-foo"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("foo-"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("foo--bar"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("foo-bar"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("foo-bar-"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("-foo-bar-"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("foo-bar-baz"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("foo-bar-baz--"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("--foo-bar-baz"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("--foo-bar-baz--"));

    assertFalse(StringUtil.hyphenatedCapitalizedWord("Foo"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("-Foo"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("Foo-"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("Foo--Bar"));
    assertTrue(StringUtil.hyphenatedCapitalizedWord("Foo-Bar"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("Foo-Bar-"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("-Foo-Bar-"));
    assertTrue(StringUtil.hyphenatedCapitalizedWord("Foo-Bar-Baz"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("Foo-bar-Baz"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("foo-Bar-Baz"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("Foo-Bar-baz"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("Foo-Bar-Baz--"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("--Foo-Bar-Baz"));
    assertFalse(StringUtil.hyphenatedCapitalizedWord("--Foo-Bar-Baz--"));
  }

  public void testAllLowerCase() {
    assertTrue(StringUtil.allLowerCase("foo"));
    assertTrue(StringUtil.allLowerCase("a"));
    assertFalse(StringUtil.allLowerCase("Foo"));
    assertFalse(StringUtil.allLowerCase("A"));
    assertFalse(StringUtil.allLowerCase(""));
    assertFalse(StringUtil.allLowerCase("123"));
    assertFalse(StringUtil.allLowerCase("-#@!"));
    assertTrue(StringUtil.allLowerCase("a-b"));
    assertTrue(StringUtil.allLowerCase("a b"));
    assertTrue(StringUtil.allLowerCase(" ab"));
    assertTrue(StringUtil.allLowerCase("ab "));
  }

  public void testAllCaps() {
    assertTrue(StringUtil.allCaps("FOO"));
    assertTrue(StringUtil.allCaps("A"));
    assertFalse(StringUtil.allCaps("Foo"));
    assertFalse(StringUtil.allCaps("foo"));
    assertFalse(StringUtil.allCaps("a"));
    assertFalse(StringUtil.allCaps(""));
    assertFalse(StringUtil.allCaps("123"));
    assertFalse(StringUtil.allCaps("-#@!"));
    assertTrue(StringUtil.allCaps("A-B"));
    assertTrue(StringUtil.allCaps("A B"));
    assertTrue(StringUtil.allCaps(" AB"));
    assertTrue(StringUtil.allCaps("AB "));
  }

  public void testCapitalizedWord() {
    assertTrue(StringUtil.capitalizedWord("Foo"));
    assertFalse(StringUtil.capitalizedWord("foo"));
    assertFalse(StringUtil.capitalizedWord("-#!$"));
    assertFalse(StringUtil.capitalizedWord("Foo-bar"));
    assertFalse(StringUtil.capitalizedWord("FooBar"));
  }

  public void testCamelCaseWord() {
    assertTrue(StringUtil.camelCasedWord("ThisIsATest"));
    assertFalse(StringUtil.camelCasedWord("THISISATEST"));
    assertTrue(StringUtil.camelCasedWord("fooBarBaz"));
  }

  private final void doStringContextTest(String string, int windowSize) {
    final int len = string.length();

    int index = 0;
    for (StringUtil.StringContext context = new StringUtil.StringContext(windowSize, string);
         context.hasNext();
         ++index) {

      final int codePoint = context.next();
      assertEquals("index=" + index, string.codePointAt(index), codePoint);

      for (int j = -windowSize; j <= windowSize; ++j) {
        final int curCodePoint = context.getCodePoint(j);
        final int realOffset = index + j;
        if (realOffset < 0 || realOffset >= len) {
          assertEquals(0, curCodePoint);
        }
        else {
          assertEquals(string.codePointAt(realOffset), curCodePoint);
        }
      }

      assertEquals(index, context.getPosition());
    }

    assertEquals(len, index);
  }

  public void testStringContext() {
    doStringContextTest("0123456789", 2);
    doStringContextTest("", 2);
    doStringContextTest("0", 2);
    doStringContextTest("01", 2);
    doStringContextTest("012", 2);
    doStringContextTest("0123", 2);

    doStringContextTest("0123456789", 1);
    doStringContextTest("", 1);
    doStringContextTest("0", 1);
    doStringContextTest("01", 1);
    doStringContextTest("012", 1);
    doStringContextTest("0123", 1);

    doStringContextTest("0123456789", 0);
    doStringContextTest("", 0);
    doStringContextTest("0", 0);
    doStringContextTest("01", 0);
    doStringContextTest("012", 0);
    doStringContextTest("0123", 0);
  }

  public void testIsCapital() {
    assertTrue(StringUtil.isCapital("Foo", 0));
    assertFalse(StringUtil.isCapital("foo", 0));
    assertTrue(StringUtil.isCapital("Bar", 0));
    assertFalse(StringUtil.isCapital("bar", 0));
  }

  private void verifyTrimWithCodePointBag(String input, StringUtil.CodePointBag bag, String expected) {
    final String got = StringUtil.trim(input, bag);
    assertEquals("got: " + got + " expected: " + expected, expected, got);
  }

  public void testTrim() {
    // test spaces and tabs on ends and in middle
    assertEquals("this is a string", StringUtil.trim("  this   is  \t \t  a   \tstring\t "));
    
    // test carriage returns, tabs, and spaces in middle
    assertEquals("this is a string", StringUtil.trim("this\n\tis\t \t  a\tstring"));
    
    // should not change an already trimmed string
    assertEquals("this is a string", StringUtil.trim("this is a string"));

    // should turn 'null' into ""
    assertEquals("", StringUtil.trim(null));

    // should turn 'null' into ""
    assertEquals("", StringUtil.trim("   \t\n  \n\t "));
  }
  
  public void testTrimAndStore() {
    HashMap<String, String[]> results = new HashMap<String, String[]>();
    results.put("  \tthis is a string\t ", new String[]{"  \t","this is a string","\t "});
    results.put("\t  \tthis is a string", new String[]{"\t  \t","this is a string",""});
    results.put("this is a string\t  \t ", new String[]{"","this is a string","\t  \t "});
    results.put(null, new String[]{"","",""});
    results.put("   \t\n  \n\t ", new String[]{"   \t\n  \n\t ","",""});

    for(Entry<String, String[]> entry : results.entrySet())
    {
      String[] expected = entry.getValue();
      String[] actual = StringUtil.trimAndStore(entry.getKey());
      assertEquals("returned value should be an array of Strings of size 3!", 3, actual.length);

      for(int i = 0; i < 3; i++)
      {
        assertEquals(expected[i], actual[i]);
      }
    }
  }
  
  public void testTrimWithCodePointBag() {
    // keep all delims but # in middle; keep all letters.
    final StringUtil.CodePointBag bag = new StringUtil.CodePointBag() {
        public StringUtil.Response getResponse(int codePoint) {
          StringUtil.Response result = StringUtil.Response.MAYBE;

          if (Character.isLetter(codePoint)) {
            result = StringUtil.Response.YES;
          }
          else if (codePoint == '#') {
            result = StringUtil.Response.NO;
          }

          return result;
        }
      };

    verifyTrimWithCodePointBag("(#...blah  .*#*. blah...#)", bag, "blah  .**. blah");
    verifyTrimWithCodePointBag("(#...  .*#*. ...#)", bag, "");
    verifyTrimWithCodePointBag("blah blah", bag, "blah blah");
    verifyTrimWithCodePointBag("", bag, "");
    verifyTrimWithCodePointBag("b", bag, "b");
    verifyTrimWithCodePointBag("b#b", bag, "bb");
    verifyTrimWithCodePointBag(".b#b.", bag, "bb");
  }

  public void testChomp() {
    String[] chomped = null;

    chomped = StringUtil.chomp("a b");
    assertEquals("a", chomped[0]);
    assertEquals("b", chomped[1]);

    chomped = StringUtil.chomp(" a  b");
    assertEquals("a", chomped[0]);
    assertEquals("b", chomped[1]);

    chomped = StringUtil.chomp(" a1234b  b");
    assertEquals("a1234b", chomped[0]);
    assertEquals("b", chomped[1]);

    chomped = StringUtil.chomp(" a1234b  b  ");
    assertEquals("a1234b", chomped[0]);
    assertEquals("b  ", chomped[1]);

    assertNull(StringUtil.chomp(""));
    assertNull(StringUtil.chomp("a")[1]);
    assertNull(StringUtil.chomp("  a")[1]);
    assertNull(StringUtil.chomp("  a  ")[1]);
  }

  public void testChompAtWhite() {
    String[] chomped = null;

    chomped = StringUtil.chomp("tag att1=val1", StringUtil.CHOMP_WHITE_BAG);
    assertEquals("tag", chomped[0]);
    assertEquals("att1=val1", chomped[1]);

    chomped = StringUtil.chomp("tag att2=\"val2\"", StringUtil.CHOMP_WHITE_BAG);
    assertEquals("tag", chomped[0]);
    assertEquals("att2=\"val2\"", chomped[1]);

    chomped = StringUtil.chomp("tag att1=val1 att2=\"val2\"", StringUtil.CHOMP_WHITE_BAG);
    assertEquals("tag", chomped[0]);
    chomped = StringUtil.chomp(chomped[1], StringUtil.CHOMP_WHITE_BAG);
    assertEquals("att1=val1", chomped[0]);
    assertEquals("att2=\"val2\"", chomped[1]);

    chomped = StringUtil.chomp("tag", StringUtil.CHOMP_WHITE_BAG);
    assertEquals("tag", chomped[0]);
    assertNull(chomped[1]);
  }

  public void testGetLastWord() {
    assertEquals("dialling", StringUtil.getLastWord("inc dialling"));
    assertEquals("PLC-Systemen", StringUtil.getLastWord("PLC-Systemen"));
  }

  public void testJoin() {
    final String[] array1 = {"this", "is", "a", "test", "this", "is", "only", "a", "test"};
    final String[] array2 = {"this", "is", "a", "test", "this", "is", "only", "a", "test"};
    final String[] array3 = {"this", "is", "a", "test", "this", "is", "only", "a", "test"};
    final String[] array4 = {"this", null , "a", "test", "this", "is", "only", "a", "test"};
    final String[] array5 = {"this", "is", "a", "test", "this", "is", "", "only", "a", "test"};
  
    assertEquals(StringUtil.join(array1, " "), "this is a test this is only a test");
    assertEquals(StringUtil.join(array2, "tacos"), "thistacosistacosatacostesttacosthistacosistacosonlytacosatacostest");
    assertEquals(StringUtil.join(array3, "."), "this.is.a.test.this.is.only.a.test");
    assertEquals(StringUtil.join(array4, "."), "this.a.test.this.is.only.a.test");
    assertEquals(StringUtil.join(array5, "."), "this.is.a.test.this.is..only.a.test");
  }

  private final void doSplitOnSymbolsTest(String input, String[] expected) {
    final List<String> pieces = StringUtil.splitOnSymbols(input);

    assertEquals(expected.length, pieces.size());
    for (int i = 0; i < expected.length; ++i) {
      assertEquals("index=" + i + " got='" + pieces.get(i) + "'",
                   expected[i], pieces.get(i));
    }
  }

  public void testSplitOnSymbols() {
    doSplitOnSymbolsTest("single", new String[]{"single"});
    doSplitOnSymbolsTest("1.9", new String[]{"1.9"});
    doSplitOnSymbolsTest("this is a test", new String[]{"this is a test"});
    doSplitOnSymbolsTest("this -- is a test", new String[]{"this", "is a test"});
    doSplitOnSymbolsTest("-*-this.  is a *test*! ", new String[]{"this", "is a", "test"});
  }

  public void testSplitFields() {
    assertEquals(14, StringUtil.splitFields("1", 14).length);
    assertEquals(14, StringUtil.splitFields("1|2", 14).length);
  }

  public void testEnsureNumFields() {
    assertEquals(14, StringUtil.ensureNumFields(null, 14).length);
    assertEquals(14, StringUtil.ensureNumFields(new String[]{"1"}, 14).length);
    assertEquals(14, StringUtil.ensureNumFields(new String[]{"1", "2"}, 14).length);
  }

  public void testHasConsecutiveRepeats() {
    assertTrue(StringUtil.hasConsecutiveRepeats("baaabb", 3));
    assertFalse(StringUtil.hasConsecutiveRepeats("baabb", 3));
    assertTrue(StringUtil.hasConsecutiveRepeats("baaaabb", 3));
    assertTrue(StringUtil.hasConsecutiveRepeats("abcddd", 3));
  }


  private final void doLongestSubstrTest(String string1, String string2, int expectedLen, String expectedResult) {
    final StringBuilder result = new StringBuilder();
    int len = StringUtil.longestSubstr(string1, string2, result);

    assertEquals(expectedLen, len);
    assertEquals(expectedResult, result.toString());
  }

  public void testLongestSubstr() {
    doLongestSubstrTest("abab", "baba", 3, "aba");
    doLongestSubstrTest(
      "cogeneration temperature hydraulic control valves foo bar baz",
      "cogeneration temp hydraulic control valves bar bar baz",
      26,
      " hydraulic control valves ");
  }

  private final void doLongestSubstrTest(String[] strings, int expectedLen, String expectedResult) {
    final StringBuilder result = new StringBuilder();
    int len = StringUtil.longestSubstr(strings, result);

    assertEquals(expectedLen, len);
    assertEquals(expectedResult, result.toString());
  }

  public void testLongestSubstrs() {
    doLongestSubstrTest(new String[]{"abab", "baba"}, 3, "aba");
    doLongestSubstrTest(new String[]{"abab", "baba", "cdabad"}, 3, "aba");

    doLongestSubstrTest(new String[]{
        "polytetrafluoroethylene lined duplex valves",
        "ptfe lined duplex valves",
        "polytetrafluoroethylene (ptfe) lined duplex valves"},
      20,
      " lined duplex valves");

    // note: order matters!
    doLongestSubstrTest(new String[]{
        "polytetrafluoroethylene (ptfe) lined duplex valves",
        "polytetrafluoroethylene lined duplex valves",
        "ptfe lined duplex valves"},
      2,
      "le");

  }

  private final void doLongestSubstr2Test(String[] strings, String[] expectedResults) {
    final Set<String> result = StringUtil.longestSubstr2(strings);

    assertEquals(expectedResults.length, result.size());
    for (String expectedResult : expectedResults) {
      assertTrue(expectedResult, result.contains(expectedResult));
    }
  }

  public void testLongestSubstrs2() {
    doLongestSubstr2Test(new String[]{
        "polytetrafluoroethylene lined duplex valves",
        "ptfe lined duplex valves",
        "polytetrafluoroethylene (ptfe) lined duplex valves"},
      new String[]{
        "lined duplex valves",
        "polytetrafluoroethylene",
      });
  }

  public void testCollapseStrings() {
    String[] strings = null;

    strings = new String[]{"foo", "boofoo", "foobarbaz", "abc"};
    StringUtil.collapseStrings(strings);
    assertEquals("foo", strings[0]);
    assertNull(strings[1]);
    assertNull(strings[2]);
    assertEquals("abc", strings[3]);

    strings = new String[]{"boofoo", "foo", "foobarbaz", "abc"};
    StringUtil.collapseStrings(strings);
    assertEquals("foo", strings[1]);
    assertNull(strings[0]);
    assertNull(strings[2]);
    assertEquals("abc", strings[3]);

    strings = new String[]{"boofoo", "foobarbaz", "foo", "abc"};
    StringUtil.collapseStrings(strings);
    assertEquals("foo", strings[2]);
    assertNull(strings[0]);
    assertNull(strings[1]);
    assertEquals("abc", strings[3]);
  }

  /**
   * Verify that the map contains all expected pairs of the form
   * {key1, value1, key2, value2, ...}.
   */
  private final void verifyMappings(String[] expectedPairs, Map<String, String> map) {
    for (int i = 0; i < expectedPairs.length; i += 2) {
      assertEquals(expectedPairs[i + 1], map.get(expectedPairs[i]));
    }
  }

  public void testToMap() {
    verifyMappings(
      new String[]{"a", "b", "c", "d"},
      StringUtil.toMap(new String[][]{{"a", "b"}, {"c", "d"}}));

    verifyMappings(
      new String[]{"a", "b", "c", "d"},
      StringUtil.toMap(new String[][]{{"a", "b", "c", "d"}}));

    verifyMappings(
      new String[]{"a", "b", "c", "d"},
      StringUtil.toMap(new String[][]{{"a", "b", "c", "d", "e"}}));

    verifyMappings(
      new String[]{"a", "b", "c", "d", "e", "f"},
      StringUtil.toMap(new String[][]{{"a", "b"}, {"c", "d"}, {"e", "f", "g"}}));

    verifyMappings(
      new String[]{"a", "b", "c", "d", "e", "f"},
      StringUtil.toMap(new String[]{"a", "b", "c", "d", "e", "f", "g"}));
  }

  public void testIsLikelyAbbreviation() {
    assertFalse(StringUtil.isLikelyAbbreviation("Schmidt"));
    assertFalse(StringUtil.isLikelyAbbreviation("remembrance"));
    assertFalse(StringUtil.isLikelyAbbreviation("A'li"));
    assertFalse(StringUtil.isLikelyAbbreviation("w/o"));

    assertTrue(StringUtil.isLikelyAbbreviation("Ph.D."));
    assertTrue(StringUtil.isLikelyAbbreviation("M.D."));
    assertTrue(StringUtil.isLikelyAbbreviation("Mr."));
    assertTrue(StringUtil.isLikelyAbbreviation("Mrs."));
    assertTrue(StringUtil.isLikelyAbbreviation("PhD"));
    assertTrue(StringUtil.isLikelyAbbreviation("MD"));
    assertTrue(StringUtil.isLikelyAbbreviation("Mr"));
    assertTrue(StringUtil.isLikelyAbbreviation("Mrs"));
    assertTrue(StringUtil.isLikelyAbbreviation("phd"));
    assertTrue(StringUtil.isLikelyAbbreviation("md"));
    assertTrue(StringUtil.isLikelyAbbreviation("mr"));
    assertTrue(StringUtil.isLikelyAbbreviation("mrs"));

    assertTrue(StringUtil.isLikelyAbbreviation("wt."));
    assertTrue(StringUtil.isLikelyAbbreviation("cc"));
    assertTrue(StringUtil.isLikelyAbbreviation("mgmnt"));
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestStringUtil.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}

