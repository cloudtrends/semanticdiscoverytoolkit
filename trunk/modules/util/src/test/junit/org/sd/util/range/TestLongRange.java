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
 * JUnit Tests for the LongRange class.
 * <p>
 * @author Spence Koehler
 */
public class TestLongRange extends TestCase {

  public TestLongRange(String name) {
    super(name);
  }
  
  public void verify(LongRange range, long[] low, boolean[] includeLow, long[] high, boolean[] includeHigh, String asString, Integer size) {
    final List<AbstractNumericRange.SimpleRange> simpleRanges = range.getSimpleRanges();

    assertEquals(low.length, simpleRanges.size());

    int index = 0;
    for (AbstractNumericRange.SimpleRange simpleRange : simpleRanges) {
      verify(simpleRange, low[index], includeLow[index], high[index], includeHigh[index]);
      ++index;
    }

    assertEquals(asString, range.asString());
    assertEquals(size, range.size());
  }

  public void verify(AbstractNumericRange.SimpleRange range, long low, boolean includeLow, long high, boolean includeHigh) {
    final long rangeLow = range.getLowAsLong(false);
    final long rangeHigh = range.getHighAsLong(false);

    assertEquals(low, rangeLow);
    assertEquals(high, rangeHigh);

    assertEquals(includeLow, range.includesLow());
    assertEquals(includeHigh, range.includesHigh());

    assertEquals(includeLow, range.includes(low));
    assertEquals(includeLow, range.includes(Long.toString(low)));
    assertEquals(includeLow, range.includes((double)low));

    assertEquals(includeHigh, range.includes(high));
    assertEquals(includeHigh, range.includes(Long.toString(high)));
    assertEquals(includeHigh, range.includes((double)high));

    for (long i = low + 1; i < high && i < (low + 100); ++i) {
      assertTrue(range.includes(i));
      assertTrue(range.includes(Long.toString(i)));
      assertTrue(range.includes((double)i));
    }

    for (long i = high - 100; i > low + 1 && i < high; ++i) {
      assertTrue(range.includes(i));
      assertTrue(range.includes(Long.toString(i)));
      assertTrue(range.includes((double)i));
    }

    if (low != Long.MIN_VALUE) {
      for (long i = low - 5; i < low; ++i) {
        assertFalse(range.includes(i));
        assertFalse(range.includes(Long.toString(i)));
        assertFalse(range.includes((double)i));
      }
    }

    if (high != Long.MAX_VALUE) {
      for (long i = high + 1; i < high + 5; ++i) {
        assertFalse(range.includes(i));
        assertFalse(range.includes(Long.toString(i)));
        assertFalse(range.includes((double)i));
      }
    }
  }

  public void testIncrementalConstruction() {
    final LongRange range = new LongRange();

    assertEquals(0, range.getLow());
    assertEquals(0, range.getHigh());

    range.add(3, true, 7, true);
    assertEquals(3, range.getLow());
    assertEquals(7, range.getHigh());

    range.add(5, true, 8, true);
    assertEquals(3, range.getLow());
    assertEquals(8, range.getHigh());
  }

  public void testUnspecifiedBoundsString() {
    final LongRange range = new LongRange("3-8");
    assertEquals(3, range.getLow());
    assertEquals(8, range.getHigh());
  }

  public void testSingleValue() {
    LongRange range = null;

    range = new LongRange(5);
    verify(range, new long[]{5}, new boolean[]{true}, new long[]{5}, new boolean[]{true}, "5", 1);

    range = new LongRange("5");
    verify(range, new long[]{5}, new boolean[]{true}, new long[]{5}, new boolean[]{true}, "5", 1);

    range = new LongRange("(5)");
    verify(range, new long[]{5}, new boolean[]{true}, new long[]{5}, new boolean[]{true}, "5", 1);

    range = new LongRange("[5]");
    verify(range, new long[]{5}, new boolean[]{true}, new long[]{5}, new boolean[]{true}, "5", 1);

    range = new LongRange("(5]");
    verify(range, new long[]{5}, new boolean[]{true}, new long[]{5}, new boolean[]{true}, "5", 1);

    range = new LongRange("[5)");
    verify(range, new long[]{5}, new boolean[]{true}, new long[]{5}, new boolean[]{true}, "5", 1);

    range = new LongRange("[ 5   ]");
    verify(range, new long[]{5}, new boolean[]{true}, new long[]{5}, new boolean[]{true}, "5", 1);

    range = new LongRange("5-5");
    verify(range, new long[]{5}, new boolean[]{true}, new long[]{5}, new boolean[]{true}, "5", 1);

    range = new LongRange(" 5 - 5 ");
    verify(range, new long[]{5}, new boolean[]{true}, new long[]{5}, new boolean[]{true}, "5", 1);
  }

  public void testForwardRange() {
    LongRange range = null;

    range = new LongRange(3, true, 7, true);
    verify(range, new long[]{3}, new boolean[]{true}, new long[]{7}, new boolean[]{true}, "3-7", 5);

    range = new LongRange(3, false, 7, false);
    verify(range, new long[]{3}, new boolean[]{false}, new long[]{7}, new boolean[]{false}, "(3-7)", 3);

    range = new LongRange(3, false, 7, true);
    verify(range, new long[]{3}, new boolean[]{false}, new long[]{7}, new boolean[]{true}, "(3-7]", 4);

    range = new LongRange(3, true, 7, false);
    verify(range, new long[]{3}, new boolean[]{true}, new long[]{7}, new boolean[]{false}, "[3-7)", 4);


    range = new LongRange("3-7");
    verify(range, new long[]{3}, new boolean[]{true}, new long[]{7}, new boolean[]{true}, "3-7", 5);

    range = new LongRange("[3-7]");
    verify(range, new long[]{3}, new boolean[]{true}, new long[]{7}, new boolean[]{true}, "3-7", 5);

    range = new LongRange("(3-7)");
    verify(range, new long[]{3}, new boolean[]{false}, new long[]{7}, new boolean[]{false}, "(3-7)", 3);

    range = new LongRange("(3-7]");
    verify(range, new long[]{3}, new boolean[]{false}, new long[]{7}, new boolean[]{true}, "(3-7]", 4);

    range = new LongRange("[3-7)");
    verify(range, new long[]{3}, new boolean[]{true}, new long[]{7}, new boolean[]{false}, "[3-7)", 4);
  }

  public void testBackwardRange() {
    LongRange range = null;

    range = new LongRange(7, true, 3, true);
    verify(range, new long[]{3}, new boolean[]{true}, new long[]{7}, new boolean[]{true}, "3-7", 5);

    range = new LongRange(7, false, 3, false);
    verify(range, new long[]{3}, new boolean[]{false}, new long[]{7}, new boolean[]{false}, "(3-7)", 3);

    range = new LongRange(7, false, 3, true);
    verify(range, new long[]{3}, new boolean[]{true}, new long[]{7}, new boolean[]{false}, "[3-7)", 4);

    range = new LongRange(7, true, 3, false);
    verify(range, new long[]{3}, new boolean[]{false}, new long[]{7}, new boolean[]{true}, "(3-7]", 4);

    range = new LongRange("(3-7]");
    verify(range, new long[]{3}, new boolean[]{false}, new long[]{7}, new boolean[]{true}, "(3-7]", 4);

    range = new LongRange("[7-3)");
    verify(range, new long[]{3}, new boolean[]{false}, new long[]{7}, new boolean[]{true}, "(3-7]", 4);

    range = new LongRange(" ( 3 - 7 ] ");
    verify(range, new long[]{3}, new boolean[]{false}, new long[]{7}, new boolean[]{true}, "(3-7]", 4);
  }

  public void testToleranceRange() {
    LongRange range = null;

    range = new LongRange(3, 7, true, true);
    verify(range, new long[]{-4}, new boolean[]{true}, new long[]{10}, new boolean[]{true}, "-4-10", 15);

    range = new LongRange(3, 7, false, false);
    verify(range, new long[]{-4}, new boolean[]{false}, new long[]{10}, new boolean[]{false}, "(-4-10)", 13);

    range = new LongRange(3, 7, false, true);
    verify(range, new long[]{-4}, new boolean[]{false}, new long[]{10}, new boolean[]{true}, "(-4-10]", 14);

    range = new LongRange(3, 7, true, false);
    verify(range, new long[]{-4}, new boolean[]{true}, new long[]{10}, new boolean[]{false}, "[-4-10)", 14);

    range = new LongRange("[3^7)");
    verify(range, new long[]{-4}, new boolean[]{true}, new long[]{10}, new boolean[]{false}, "[-4-10)", 14);

    range = new LongRange(" [ 3 ^ 7 ) ");
    verify(range, new long[]{-4}, new boolean[]{true}, new long[]{10}, new boolean[]{false}, "[-4-10)", 14);
  }

  public void testMerge1() {
    LongRange range = null;

    range = new LongRange("5, 6");
    verify(range, new long[]{5}, new boolean[]{true}, new long[]{6}, new boolean[]{true}, "5-6", 2);

    range = new LongRange("6, 5");
    verify(range, new long[]{5}, new boolean[]{true}, new long[]{6}, new boolean[]{true}, "5-6", 2);

    range = new LongRange("5-7,8,9-11");
    verify(range, new long[]{5}, new boolean[]{true}, new long[]{11}, new boolean[]{true}, "5-11", 7);

    range = new LongRange("[5-7),8,(9-11]");
    verify(range,
           new long[]{5, 8, 9}, new boolean[]{true, true, false},
           new long[]{7, 8, 11}, new boolean[]{false, true, true},
           "[5-7),8,(9-11]", 5);
  }

  public void testUnbounded() {
    LongRange range = null;

    range = new LongRange("(5-)");
    verify(range, new long[]{5}, new boolean[]{false}, new long[]{Long.MAX_VALUE}, new boolean[]{true}, "(5-", null);

    range = new LongRange("(-5)");
    verify(range, new long[]{-5}, new boolean[]{true}, new long[]{-5}, new boolean[]{true}, "-5", 1);

    range = new LongRange("-5");
    verify(range, new long[]{-5}, new boolean[]{true}, new long[]{-5}, new boolean[]{true}, "-5", 1);

    range = new LongRange("-(-5)");
    verify(range, new long[]{Long.MIN_VALUE}, new boolean[]{true}, new long[]{-5}, new boolean[]{false}, "-(-5)", null);

    range = new LongRange("-(5)");
    verify(range, new long[]{Long.MIN_VALUE}, new boolean[]{true}, new long[]{5}, new boolean[]{false}, "-(5)", null);

    range = new LongRange("-");
    verify(range, new long[]{Long.MIN_VALUE}, new boolean[]{true}, new long[]{Long.MAX_VALUE}, new boolean[]{true}, "-", null);

    range = new LongRange("(5-");
    verify(range, new long[]{5}, new boolean[]{false}, new long[]{Long.MAX_VALUE}, new boolean[]{true}, "(5-", null);

    range = new LongRange("5-");
    verify(range, new long[]{5}, new boolean[]{true}, new long[]{Long.MAX_VALUE}, new boolean[]{true}, "5-", null);

    range = new LongRange("-5-");
    verify(range, new long[]{-5}, new boolean[]{true}, new long[]{Long.MAX_VALUE}, new boolean[]{true}, "-5-", null);

    range = new LongRange("--5");
    verify(range, new long[]{Long.MIN_VALUE}, new boolean[]{true}, new long[]{-5}, new boolean[]{true}, "-(-5]", null);

    range = new LongRange("--5)");
    verify(range, new long[]{Long.MIN_VALUE}, new boolean[]{true}, new long[]{-5}, new boolean[]{false}, "-(-5)", null);
  }

  public void testAmbiguous() {
    LongRange range = null;

    range = new LongRange("-5-3");
    verify(range, new long[]{-5}, new boolean[]{true}, new long[]{3}, new boolean[]{true}, "-5-3", 9);

    range = new LongRange("-5--3");
    verify(range, new long[]{-5}, new boolean[]{true}, new long[]{-3}, new boolean[]{true}, "-5--3", 3);
  }

  public void testDontAddNonContiguousDuplicate() {
    final LongRange range = new LongRange(1947);
    range.add(13, false);
    range.add(1947, false);

    assertEquals("13,1947", range.toString());
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestLongRange.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
