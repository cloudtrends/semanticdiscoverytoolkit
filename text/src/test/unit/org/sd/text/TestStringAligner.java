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
 * JUnit Tests for the StringAligner class.
 * <p>
 * @author Spence Koehler
 */
public class TestStringAligner extends TestCase {

  public TestStringAligner(String name) {
    super(name);
  }
  
  private final void doAlignmentTest(boolean expectedAlignment, int expectedPenalty, String expectedCandString, String base, String cand) {
    final StringAligner aligner = new StringAligner(base, cand);
    assertEquals(expectedAlignment, aligner.aligns());
    assertEquals(expectedPenalty, aligner.getPenalty());
    assertEquals(expectedCandString, aligner.getMatchedCandidateString());
  }

  public void testAlignment() {
    doAlignmentTest(true, 0, "Appleton", "appleton", "Appleton Capital Management - alternative investment management");
    doAlignmentTest(true, 0, "Mobility Pricewatch", "mobilitypricewatch", "Mobility Pricewatch - Mobility Scooters & Wheelchairs");
    doAlignmentTest(true, 0, "Direct Submit", "directsubmit", "Internet marketing Newcastle. SEO services including copywriting, site submission and optimisation by Direct Submit search engine services");
    doAlignmentTest(false, -1, null, "sandwichinfo", "Sandwich Events");
    doAlignmentTest(true, 1, "Appletoon", "appleton", "Appletoon Capital Management");
    doAlignmentTest(false, -1, null, "saphos", "Tshirt & Mug Printing Systems From Keani Ltd,");
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestStringAligner.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
