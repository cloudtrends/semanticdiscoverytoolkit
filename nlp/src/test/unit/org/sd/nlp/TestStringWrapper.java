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

//import org.sd.cci.GermanBreakStrategy;

/**
 * JUnit Tests for the StringWrapper class.
 * <p>
 * @author Spence Koehler
 */
public class TestStringWrapper extends TestCase {

  public TestStringWrapper(String name) {
    super(name);
  }
  
  private int tryLongestToShortest(StringWrapper sw, int startIndex, String[] longestToShortest) {
    StringWrapper.SubString subString = sw.getLongestSubString(startIndex, 0);
    final int result = subString.endPos;

    for (int i = 0; i < longestToShortest.length; ++i) {
      assertNotNull("LTS[" + i + "]=" + longestToShortest[i], subString);
      assertEquals("i=" + i + "; " + subString.toString(), longestToShortest[i], subString.originalSubString);
      subString = sw.getShorterSubString(subString);
    }

    assertNull(subString);
    return result;
  }

  private int tryShortestToLongest(StringWrapper sw, int startIndex, String[] shortestToLongest) {
    StringWrapper.SubString subString = sw.getShortestSubString(startIndex);
    final int result = subString.endPos;

    for (int i = 0; i < shortestToLongest.length; ++i) {
      assertEquals("i=" + i + "; " + subString.toString(), shortestToLongest[i], subString.originalSubString);
      subString = sw.getLongerSubString(subString);
    }

    assertNull(subString);
    return result;
  }

  public void testShortenToSoftSplit() {
    final StringWrapper sw = new StringWrapper("kaliber5 GmbH");
    tryLongestToShortest(sw, 0, new String[]{"kaliber5 GmbH", "kaliber5 Gmb", "kaliber5", "kaliber"});
  }

  public void testTokenization() {
    final StringWrapper sw = new StringWrapper(", testing: testing123. this is a test; oneTwoThree. one-two-three!");

    int index1 = tryLongestToShortest(sw, 0, new String[]{"testing"});
    int index2 = tryShortestToLongest(sw, 0, new String[]{"testing"});
    assertEquals(index1, index2);

    int index = sw.getNextStartIndex(index1);

    index1 = tryLongestToShortest(sw, index, new String[]{"testing123", "testing"});
    index2 = tryShortestToLongest(sw, index, new String[]{"testing", "testing123"});
    index2 = tryShortestToLongest(sw, index2, new String[]{"123"});
    assertEquals(index1, index2);

    index = sw.getNextStartIndex(index1);

    // "oneTwoThree" index
    index2 = tryLongestToShortest(sw, index, new String[]{"this is a test", "this is a", "this is", "this"});
    
    // "is a test" index
    index1 = tryShortestToLongest(sw, index, new String[]{"this", "this is", "this is a", "this is a test"});
    index1 = tryShortestToLongest(sw, index1, new String[]{"is", "is a", "is a test"});
    index1 = tryShortestToLongest(sw, index1, new String[]{"a", "a test"});
    index1 = tryShortestToLongest(sw, index1, new String[]{"test"});

    assertEquals(index1, index2);

    // "one-two-three" index
    index2 = tryLongestToShortest(sw, index2, new String[]{"oneTwoThree", "oneTwo", "one"});

    // "TwoThree" index
    index1 = tryShortestToLongest(sw, index1, new String[]{"one", "oneTwo", "oneTwoThree"});
    index1 = tryShortestToLongest(sw, index1, new String[]{"Two", "TwoThree"});
    index1 = tryShortestToLongest(sw, index1, new String[]{"Three"});

    assertEquals(index1, index2);
    
    index2 = tryLongestToShortest(sw, index2, new String[]{"one-two-three", "one-two", "one"});

    index1 = tryShortestToLongest(sw, index1, new String[]{"one", "one-two", "one-two-three"});
    index1 = tryShortestToLongest(sw, index1, new String[]{"two", "two-three"});
    index1 = tryShortestToLongest(sw, index1, new String[]{"three"});

    assertEquals(index1, index2);

    assertNull(sw.getLongestSubString(index2, 0));
    assertNull(sw.getShortestSubString(index1));
  }

  public void testEmptyStringTokenization1() {
    final StringWrapper sw = new StringWrapper("");

    assertNull(sw.getLongestSubString(0, 0));
    assertNull(sw.getShortestSubString(0));
  }

  public void testEmptyStringTokenization2() {
    final StringWrapper sw = new StringWrapper(", --.");

    assertNull(sw.getLongestSubString(0, 0));
    assertNull(sw.getShortestSubString(0));
  }

  public void testTokenization2() {
    final StringWrapper sw = new StringWrapper("Alex.-Wiegand-Str. 18");
    tryLongestToShortest(sw, 0, new String[]{"Alex.-Wiegand-Str", "Alex.-Wiegand", "Alex"});
  }

//   public void testGermanTokenization1() {
//     final StringWrapper sw = new StringWrapper("Friedrich-Ebert-Str. - Technologiepark Gebäude 22", new GermanBreakStrategy());
//     tryLongestToShortest(sw, 0, new String[]{
//       "Friedrich-Ebert-Str. - Technologiepark Gebäude 22",
//       "Friedrich-Ebert-Str. - Technologiepark Gebäude",
//       "Friedrich-Ebert-Str. - Technologiepark",
//       "Friedrich-Ebert-Str",
//     });
//   }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestStringWrapper.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
