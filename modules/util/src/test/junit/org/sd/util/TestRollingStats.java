/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.util.Random;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the RollingStats class.
 * <p>
 * @author Spence Koehler
 */
public class TestRollingStats extends TestCase {

  public TestRollingStats(String name) {
    super(name);
  }
  

  public void testSimple1() {
    final RollingStats rollingStats = new RollingStats(100, 50);

    final Random r = new Random();
    final StatsAccumulator stats1 = new StatsAccumulator();
    int seg2value = -1;

    // add stats during segment 0
    while (rollingStats.getLastSegmentNum() == 0) {
      final int value = r.nextInt(1000);
      final int segNum = rollingStats.add(value);
      stats1.add(value);
      if (segNum != 0) {
        seg2value = value;
        break;
      }
    }

    // get window stats with segment0 filled (should equal stats1)
    final StatsAccumulator window0Stats = rollingStats.getWindowStats();

    // don't add anything during segment1
    while (rollingStats.getCurSegment() != 0) {
      try {
        Thread.sleep(1);
      }
      catch (InterruptedException ie) {}
    }

    // get window stats after rolling beyond segment1. (should match seg2value)
    final StatsAccumulator window1Stats = rollingStats.getWindowStats();

    // don't add anything during segment0
    while (rollingStats.getCurSegment() == 0) {
      try {
        Thread.sleep(1);
      }
      catch (InterruptedException ie) {}
    }

    // window stats should now be empty
    final StatsAccumulator emptyStats = rollingStats.getWindowStats();


    // do checks...
    assertEquals(2, rollingStats.getNumSegments());

    // window0Stats should match stats1
    assertEquals(stats1.getN(), window0Stats.getN());
    assertEquals(stats1.getMean(), window0Stats.getMean(), 0.005);

    // window1Stats should match seg2value
    if (seg2value < 0) {
      // window1Stats should be empty
      assertEquals(window1Stats.toString(), 0, window1Stats.getN());
    }
    else {
      // window1Stats should have 1 value: seg2value
      assertEquals(window1Stats.toString(), 1, window1Stats.getN());
      assertEquals((double)seg2value, window1Stats.getMean(), 0.005);
    }

    // emptyStats should be empty
    assertEquals(emptyStats.toString(), 0, emptyStats.getN());

    // cumulativeStats should match stats1
    final StatsAccumulator cumulativeStats = rollingStats.getCumulativeStats();
    assertEquals(stats1.getN(), cumulativeStats.getN());
    assertEquals(stats1.getMean(), cumulativeStats.getMean(), 0.005);
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestRollingStats.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
