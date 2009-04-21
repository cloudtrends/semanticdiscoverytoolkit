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
package org.sd.text.radixtree;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.util.GeneralUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * JUnit Tests for the TestGeneralizedSuffixTree class.
 * <p>
 * @author Spence Koehler
 */
public class TestGeneralizedSuffixTree extends TestCase {

  public TestGeneralizedSuffixTree(String name) {
    super(name);
  }
  

  private final void doLongestSubstrsTest(String[] strings, int minLen, String[] expectedResults) {
    final GeneralizedSuffixTree gst = new GeneralizedSuffixTree(strings);
    final Set<String> result = gst.longestSubstrs(minLen);

    assertEquals("got=" + result, expectedResults.length, result.size());
    for (String expectedResult : expectedResults) {
      assertTrue("expected=" + expectedResult + ", got=" + result, result.contains(expectedResult));
    }
  }

  private final void doCombinatorialTest(String[] strings, int minLen, String[] expectedResults) {
    // repeat this test while constructing by adding the strings in every order
    // possible to make sure input order doesn't matter.
    final List<String> stringsList = new ArrayList<String>();
    for (String string : strings) stringsList.add(string);
    final List<List<String>> permutations = GeneralUtil.permute(stringsList);

    for (List<String> permutation : permutations) {
      String[] curStrings = permutation.toArray(new String[permutation.size()]);
      doLongestSubstrsTest(curStrings, minLen, expectedResults);
    }
  }

  public void testLongestSubstrs() {
    doCombinatorialTest(new String[]{"abab", "baba"}, 1, new String[]{"aba", "bab"});
    doCombinatorialTest(new String[]{"abba", "abab", "baba"}, 1, new String[]{"ab", "ba"});

    doCombinatorialTest(new String[] {
        "polytetrafluoroethylene lined duplex valves",
        "ptfe lined duplex valves",
        "polytetrafluoroethylene (ptfe) lined duplex valves"},
      1,
      new String[]{" lined duplex valves"});
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestGeneralizedSuffixTree.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
