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
package org.sd.nlp;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the GeneralNormalizer class.
 * <p>
 * @author Spence Koehler
 */
public class TestGeneralNormalizer extends TestCase {

  public TestGeneralNormalizer(String name) {
    super(name);
  }
  
  private void verify(GeneralNormalizer normalizer, String input, String expected) {
    final String output = normalizer.normalize(input).getNormalized();
    assertEquals(expected, output);
  }

  public void testMiscellaneous() {
    final GeneralNormalizer n = new GeneralNormalizer(false);
    verify(n, "a,b", "a b");
  }

  public void testNormalization_noCommonCase() {
    final GeneralNormalizer n = new GeneralNormalizer(false);
    verify(n, "", "");
    verify(n, "a", "a");
    verify(n, "A", "A");
    verify(n, "@#$%", "");

    verify(n, ".123", ".123");
    verify(n, "0.123", "0.123");
    verify(n, "Ph.D.", "PhD");
    verify(n, "Mr.", "Mr");
    verify(n, ".Mr.", "Mr");

    verify(n, " - testing - ", "-testing-");
    verify(n, "testing-this", "testing-this");
    verify(n, "testing--this", "testing--this");
    verify(n, "testing - this", "testing-this");

    verify(n, "don't", "don't");
    verify(n, "'don't'", "don't");
    verify(n, "'do'", "do");

    verify(n, "ab/cd", "ab cd");
    verify(n, "c/o", "c/o");
    verify(n, "a/bc", "a bc");
    verify(n, "/bc", "bc");
    verify(n, "bc/", "bc");
    verify(n, "ab/c", "ab c");
    verify(n, "ab/ ", "ab");
    verify(n, " /ab", "ab");

    verify(n, "   a  b    cde   f", "a b cde f");
    
    verify(n, "this \"@#*! test\" had better work!", "this test had better work");

    verify(n, "Alex.-Wiegand-Str. 18", "Alex-Wiegand-Str 18");
  }

  public void testNormalization_commonCase() {
    final GeneralNormalizer n = new GeneralNormalizer(true);
    verify(n, "", "");
    verify(n, "a", "a");
    verify(n, "A", "a");
    verify(n, "@#$%", "");

    verify(n, ".123", ".123");
    verify(n, "0.123", "0.123");
    verify(n, "Ph.D.", "phd");
    verify(n, "Mr.", "mr");
    verify(n, ".Mr.", "mr");

    verify(n, " - Testing - ", "-testing-");
    verify(n, "testing-This", "testing-this");
    verify(n, "testinG--this", "testing--this");
    verify(n, "teSting - this", "testing-this");

    verify(n, "Don't", "don't");
    verify(n, "'don'T'", "don't");
    verify(n, "'dO'", "do");

    verify(n, "ab/Cd", "ab cd");
    verify(n, "C/O", "c/o");
    verify(n, "a/bC", "a/bc");  // camel casing
    verify(n, "/BC", "bc");
    verify(n, "bc/", "bc");
    verify(n, "AB/c", "ab c");
    verify(n, "ab/ ", "ab");
    verify(n, " /Ab", "ab");

    verify(n, "   a  B    cDe   f", "a b cde f");
    
    verify(n, "This \"@#*! test\" had better WORK!", "this test had better work");
  }

  public void testNormalization_subString() {
    final GeneralNormalizer n = new GeneralNormalizer(true);
    final StringWrapper sw = new StringWrapper("'Twas 100% $0.90 \"GUARANTEED\"");

    StringWrapper.SubString subString = sw.getShortestSubString(0);
    assertEquals("twas", subString.getNormalizedString(n));
    subString = subString.getNextShortestSubString();
    assertEquals("100", subString.getNormalizedString(n));
    subString = subString.getNextShortestSubString();
    assertEquals("0.90", subString.getNormalizedString(n));
    subString = subString.getNextShortestSubString();
    assertEquals("guaranteed", subString.getNormalizedString(n));

    subString = sw.getLongestSubString(0, 0);
    assertEquals("twas 100", subString.getNormalizedString(n));
    subString = subString.getNextLongestSubString(0);
    assertEquals("0.90", subString.getNormalizedString(n));
    subString = subString.getNextLongestSubString(0);
    assertEquals("guaranteed", subString.getNormalizedString(n));
  }

  public void testNormalization_subString2() {
    final GeneralNormalizer n = new GeneralNormalizer(true);
    final StringWrapper sw = new StringWrapper("Dec31st2005 10:05A.M.");

    StringWrapper.SubString subString = sw.getLongestSubString(0, 0);
    assertEquals("dec31st2005 10:05am", subString.getNormalizedString(n));
    subString = subString.getShorterSubString();
    assertEquals("dec31st2005 10:05am", subString.getNormalizedString(n));
    subString = subString.getShorterSubString();
    assertEquals("dec31st2005 10:05a", subString.getNormalizedString(n));
    subString = subString.getShorterSubString();
    assertEquals("dec31st2005 10", subString.getNormalizedString(n));
    subString = subString.getShorterSubString();
    assertEquals("dec31st2005", subString.getNormalizedString(n));
    subString = subString.getShorterSubString();
    assertEquals("dec31st", subString.getNormalizedString(n));
    subString = subString.getShorterSubString();
    assertEquals("dec", subString.getNormalizedString(n));
    subString = subString.getShorterSubString();
    assertNull(subString);
  }

  public void testNormalization_subString3() {
    final GeneralNormalizer n = new GeneralNormalizer(true);
    final StringWrapper sw = new StringWrapper("Dec31st2005 10:05A.M.");

    StringWrapper.SubString subString = sw.getLongestSubString(0, 1);
    assertEquals("dec31st2005", subString.getNormalizedString(n));
    subString = sw.getShortestSubString(0);
    assertEquals("dec", subString.getNormalizedString(n));
    subString = subString.getNextLongestSubString(1);
    assertEquals("31st2005", subString.getNormalizedString(n));
    subString = sw.getShortestSubString(subString.startPos);
    assertEquals("31st", subString.getNormalizedString(n));
    subString = subString.getNextLongestSubString(1);
    assertEquals("2005", subString.getNormalizedString(n));
    subString = subString.getNextLongestSubString(1);
    assertEquals("10", subString.getNormalizedString(n));
    subString = subString.getNextLongestSubString(1);
    assertEquals("05a", subString.getNormalizedString(n));
    subString = subString.getNextShortestSubString();
    assertEquals("m", subString.getNormalizedString(n));
  }

  public void testNormalizeRemovingWhiteFromFront() {
    final Normalizer normalizer = GeneralNormalizer.getCaseInsensitiveInstance();

    final String orig = ", testing, 1, 2, 3";
    final String norm1 = normalizer.normalize(orig).getNormalized();
    final String norm2 = normalizer.normalize(norm1).getNormalized();

    // the normalization of a normalized form should be the normalized form.
    assertEquals(norm1, norm2);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestGeneralNormalizer.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
