/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
 * JUnit Tests for the PrecisionRecall class.
 * <p>
 * @author Spence Koehler
 */
public class TestPrecisionRecall extends TestCase {

  public TestPrecisionRecall(String name) {
    super(name);
  }
  

  public void testPerfect() {
    final PrecisionRecall pr = new PrecisionRecall(18, 18, 18);

    assertEquals(18, pr.getTrueCount());
    assertEquals(18, pr.getGeneratedCount());
    assertEquals(18, pr.getAlignedCount());

    assertEquals(1.0, pr.getPrecision());
    assertEquals(1.0, pr.getRecall());
    assertEquals(1.0, pr.getFMeasure());

    assertEquals("100.000", pr.getPrecisionString());
    assertEquals("100.000", pr.getRecallString());
    assertEquals("100.000", pr.getFMeasureString());
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestPrecisionRecall.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
