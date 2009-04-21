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
package org.sd.text;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the StringScorer class.
 * <p>
 * @author Spence Koehler
 */
public class TestStringScorer extends TestCase {

  public TestStringScorer(String name) {
    super(name);
  }
  
  private final void doPlausibilityTest(String string1, String string2, boolean expectedPlausibility) {
    final StringScorer scorer = new StringScorer(string1, string2);
    final boolean got = scorer.isPlausible();
    assertEquals("got(" + string1 + ", " + string2 + ")=" + got, expectedPlausibility, got);
  }

  public void testPlausibility() {
    doPlausibilityTest("dog", "cat", false);
    doPlausibilityTest("San Francisco", "Salt Lake City", false);
    doPlausibilityTest("slt lk cty", "salt lake city", true);
    doPlausibilityTest("kent", "keddington", false);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestStringScorer.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
