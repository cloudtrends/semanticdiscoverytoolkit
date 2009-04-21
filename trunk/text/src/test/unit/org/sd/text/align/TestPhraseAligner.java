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
package org.sd.text.align;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the PhraseAligner class.
 * <p>
 * @author Spence Koehler
 */
public class TestPhraseAligner extends TestCase {

  public TestPhraseAligner(String name) {
    super(name);
  }
  
  private final void verify(String phrase1, String phrase2,
                            int expectedMatchScore,
                            int expectedUnmatchedCount,
                            int expectedExtraCount,
                            boolean expectedExactMatch,
                            boolean expectedFuzzyMatch) {
    final PhraseAligner aligner = PhraseAligner.getInstance(phrase1, phrase2);

    assertEquals(expectedMatchScore, aligner.getMatchScore());
    assertEquals(expectedUnmatchedCount, aligner.getUnmatchedCount());
    assertEquals(expectedExtraCount, aligner.getExtraCount());
    assertEquals(expectedExactMatch, aligner.isExactMatch());
    assertEquals(expectedFuzzyMatch, aligner.isFuzzyMatch());
  }

  public void testAlignment() {
    verify("valves, butterfly", "butterfly valves", 1, 0, 0, false, true);
    verify("valves, butterfly", "buterfly valve", 1, 0, 0, false, true);
    verify("i/o jack", "input/output jacks", -1, 0, 0, false, true);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestPhraseAligner.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
