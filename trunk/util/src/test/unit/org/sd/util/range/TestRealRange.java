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
package org.sd.util.range;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;

/**
 * JUnit Tests for the RealRange class.
 * <p>
 * @author Spence Koehler
 */
public class TestRealRange extends TestCase {

  public TestRealRange(String name) {
    super(name);
  }
  
  public void verify(RealRange range, double[] low, boolean[] includeLow, double[] high, boolean[] includeHigh, String asString, Integer size) {
    final List<AbstractNumericRange.SimpleRange> simpleRanges = range.getSimpleRanges();

    assertEquals(low.length, simpleRanges.size());

    int index = 0;
    for (AbstractNumericRange.SimpleRange simpleRange : simpleRanges) {
      verify(simpleRange, low[index], includeLow[index], high[index], includeHigh[index]);
      ++index;
    }

    if (asString == null) {
      System.out.println(range.asString());
    }
    else {
      assertEquals(asString, range.asString());
    }

    assertEquals(size, range.size());
  }

  public void verify(AbstractNumericRange.SimpleRange range, double low, boolean includeLow, double high, boolean includeHigh) {
    final double rangeLow = range.getLowAsDouble();
    final double rangeHigh = range.getHighAsDouble();

    assertEquals(low, rangeLow);
    assertEquals(high, rangeHigh);

    assertEquals(includeLow, range.includesLow());
    assertEquals(includeHigh, range.includesHigh());

    assertEquals(includeLow, range.includes(low));
    assertEquals(includeLow, range.includes(Double.toString(low)));

    assertEquals(includeHigh, range.includes(high));
    assertEquals(includeHigh, range.includes(Double.toString(high)));

    for (double i = low + 1; i < high && i < (low + 100); i += 0.9) {
      assertTrue(range.includes(i));
      assertTrue(range.includes(Double.toString(i)));
      assertTrue(range.includes((int)i));
    }

    for (double i = high - 100; i > low + 1 && i < high - 1; i += 0.9) {
      assertTrue(range.includes(i));
      assertTrue(range.includes(Double.toString(i)));
      assertTrue(range.includes((int)i));
    }

    if (low != Double.NEGATIVE_INFINITY) {
      for (double i = low - 5; i < low; i += 0.9) {
        assertFalse(range.includes(i));
        assertFalse(range.includes(Double.toString(i)));
      }
    }

    if (high != Double.POSITIVE_INFINITY) {
      for (double i = high + 1; i < high + 5; i += 0.9) {
        assertFalse(range.includes(i));
        assertFalse(range.includes(Double.toString(i)));
      }
    }
  }

  public void testSingleValue() {
    RealRange range = null;

    range = new RealRange(5);
    verify(range, new double[]{5}, new boolean[]{true}, new double[]{5}, new boolean[]{true}, "5", 1);

    range = new RealRange("5");
    verify(range, new double[]{5}, new boolean[]{true}, new double[]{5}, new boolean[]{true}, "5", 1);

    range = new RealRange("(5)");
    verify(range, new double[]{5}, new boolean[]{true}, new double[]{5}, new boolean[]{true}, "5", 1);

    range = new RealRange("[5]");
    verify(range, new double[]{5}, new boolean[]{true}, new double[]{5}, new boolean[]{true}, "5", 1);

    range = new RealRange("(5]");
    verify(range, new double[]{5}, new boolean[]{true}, new double[]{5}, new boolean[]{true}, "5", 1);

    range = new RealRange("[5)");
    verify(range, new double[]{5}, new boolean[]{true}, new double[]{5}, new boolean[]{true}, "5", 1);

    range = new RealRange("[ 5   ]");
    verify(range, new double[]{5}, new boolean[]{true}, new double[]{5}, new boolean[]{true}, "5", 1);

    range = new RealRange("5-5");
    verify(range, new double[]{5}, new boolean[]{true}, new double[]{5}, new boolean[]{true}, "5", 1);

    range = new RealRange(" 5 - 5 ");
    verify(range, new double[]{5}, new boolean[]{true}, new double[]{5}, new boolean[]{true}, "5", 1);
  }

  public void testForwardRange() {
    RealRange range = null;

    range = new RealRange(3, true, 7, true);
    verify(range, new double[]{3}, new boolean[]{true}, new double[]{7}, new boolean[]{true}, "3-7", 5);

    range = new RealRange(3, false, 7, false);
    verify(range, new double[]{3}, new boolean[]{false}, new double[]{7}, new boolean[]{false}, "(3-7)", 3);

    range = new RealRange(3, false, 7, true);
    verify(range, new double[]{3}, new boolean[]{false}, new double[]{7}, new boolean[]{true}, "(3-7]", 4);

    range = new RealRange(3, true, 7, false);
    verify(range, new double[]{3}, new boolean[]{true}, new double[]{7}, new boolean[]{false}, "[3-7)", 4);
  }

  public void testBackwardRange() {
    RealRange range = null;

    range = new RealRange(7, true, 3, true);
    verify(range, new double[]{3}, new boolean[]{true}, new double[]{7}, new boolean[]{true}, "3-7", 5);

    range = new RealRange(7, false, 3, false);
    verify(range, new double[]{3}, new boolean[]{false}, new double[]{7}, new boolean[]{false}, "(3-7)", 3);

    range = new RealRange(7, false, 3, true);
    verify(range, new double[]{3}, new boolean[]{true}, new double[]{7}, new boolean[]{false}, "[3-7)", 4);

    range = new RealRange(7, true, 3, false);
    verify(range, new double[]{3}, new boolean[]{false}, new double[]{7}, new boolean[]{true}, "(3-7]", 4);

    range = new RealRange("(3-7]");
    verify(range, new double[]{3}, new boolean[]{false}, new double[]{7}, new boolean[]{true}, "(3-7]", 4);

    range = new RealRange("[7-3)");
    verify(range, new double[]{3}, new boolean[]{false}, new double[]{7}, new boolean[]{true}, "(3-7]", 4);

    range = new RealRange(" ( 3 - 7 ] ");
    verify(range, new double[]{3}, new boolean[]{false}, new double[]{7}, new boolean[]{true}, "(3-7]", 4);
  }

  public void testToleranceRange() {
    RealRange range = null;

    range = new RealRange(3, 7, true, true);
    verify(range, new double[]{-4}, new boolean[]{true}, new double[]{10}, new boolean[]{true}, "-4-10", 15);

    range = new RealRange(3, 7, false, false);
    verify(range, new double[]{-4}, new boolean[]{false}, new double[]{10}, new boolean[]{false}, "(-4-10)", 13);

    range = new RealRange(3, 7, false, true);
    verify(range, new double[]{-4}, new boolean[]{false}, new double[]{10}, new boolean[]{true}, "(-4-10]", 14);

    range = new RealRange(3, 7, true, false);
    verify(range, new double[]{-4}, new boolean[]{true}, new double[]{10}, new boolean[]{false}, "[-4-10)", 14);

    range = new RealRange("[3^7)");
    verify(range, new double[]{-4}, new boolean[]{true}, new double[]{10}, new boolean[]{false}, "[-4-10)", 14);

    range = new RealRange(" [ 3 ^ 7 ) ");
    verify(range, new double[]{-4}, new boolean[]{true}, new double[]{10}, new boolean[]{false}, "[-4-10)", 14);
  }

  public void testMerge1() {
    RealRange range = null;

    range = new RealRange("5, 6");
    verify(range, new double[]{5.0, 6.0}, new boolean[]{true, true}, new double[]{5.0, 6.0}, new boolean[]{true, true}, "5,6", 2);

    range = new RealRange("6, 5");
    verify(range, new double[]{5.0, 6.0}, new boolean[]{true, true}, new double[]{5.0, 6.0}, new boolean[]{true, true}, "5,6", 2);

    range = new RealRange("5-7,8,9-11");
    verify(range,
           new double[]{5.0, 8.0, 9.0}, new boolean[]{true, true, true},
           new double[]{7.0, 8.0, 11.0}, new boolean[]{true, true, true}, "5-7,8,9-11", 7);

    range = new RealRange("[5-7),8,(9-11]");
    verify(range,
           new double[]{5, 8, 9}, new boolean[]{true, true, false},
           new double[]{7, 8, 11}, new boolean[]{false, true, true}, "[5-7),8,(9-11]", 5);
  }

  public void testUnbounded() {
    RealRange range = null;

    range = new RealRange("(5-)");
    verify(range, new double[]{5}, new boolean[]{false}, new double[]{Double.POSITIVE_INFINITY}, new boolean[]{true}, "(5-", null);

    range = new RealRange("(-5)");
    verify(range, new double[]{-5}, new boolean[]{true}, new double[]{-5}, new boolean[]{true}, "-5", 1);

    range = new RealRange("-5");
    verify(range, new double[]{-5}, new boolean[]{true}, new double[]{-5}, new boolean[]{true}, "-5", 1);

    range = new RealRange("-(-5)");
    verify(range, new double[]{Double.NEGATIVE_INFINITY}, new boolean[]{true}, new double[]{-5}, new boolean[]{false}, "-(-5)", null);

    range = new RealRange("-(5)");
    verify(range, new double[]{Double.NEGATIVE_INFINITY}, new boolean[]{true}, new double[]{5}, new boolean[]{false}, "-(5)", null);

    range = new RealRange("-");
    verify(range, new double[]{Double.NEGATIVE_INFINITY}, new boolean[]{true}, new double[]{Double.POSITIVE_INFINITY}, new boolean[]{true}, "-", null);

    range = new RealRange("5-");
    verify(range, new double[]{5}, new boolean[]{true}, new double[]{Double.POSITIVE_INFINITY}, new boolean[]{true}, "5-", null);

    range = new RealRange("-5-");
    verify(range, new double[]{-5}, new boolean[]{true}, new double[]{Double.POSITIVE_INFINITY}, new boolean[]{true}, "-5-", null);

    range = new RealRange("--5");
    verify(range, new double[]{Double.NEGATIVE_INFINITY}, new boolean[]{true}, new double[]{-5}, new boolean[]{true}, "-(-5]", null);

    range = new RealRange("--5)");
    verify(range, new double[]{Double.NEGATIVE_INFINITY}, new boolean[]{true}, new double[]{-5}, new boolean[]{false}, "-(-5)", null);
  }

  public void testAmbiguous() {
    RealRange range = null;

    range = new RealRange("-5-3");
    verify(range, new double[]{-5}, new boolean[]{true}, new double[]{3}, new boolean[]{true}, "-5-3", 9);

    range = new RealRange("-5--3");
    verify(range, new double[]{-5}, new boolean[]{true}, new double[]{-3}, new boolean[]{true}, "-5--3", 3);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestRealRange.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
