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
package org.sd.util;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the MathUtil class.
 * <p>
 * @author Spence Koehler
 */
public class TestMathUtil extends TestCase {

  public TestMathUtil(String name) {
    super(name);
  }
  
  public void testComputeEntropy() {
    assertTrue(MathUtil.computeEntropy(10, 0) == 0d);
    assertTrue(MathUtil.computeEntropy(10, 10) == 1d);
    assertEquals(MathUtil.computeEntropy(6, 2), 0.811, 0.005);
  }
  
  public void testParseIntegers() {
    final int[] oneTwoThree = new int[]{1, 2, 3};

    doParseIntegerTest("1", new int[]{1});

    doParseIntegerTest("1-3", oneTwoThree);
    doParseIntegerTest("1,2,3", oneTwoThree);
    doParseIntegerTest("1, 2, 3", oneTwoThree);

    doParseIntegerTest("1-2,3", oneTwoThree);
    doParseIntegerTest("1, 2-3", oneTwoThree);
    doParseIntegerTest("1 - 3", oneTwoThree);
    doParseIntegerTest("-3", new int[]{-3});
    doParseIntegerTest("1,-3", new int[]{1, -3});

    doParseIntegerTest("3-1", new int[]{3, 2, 1});
    doParseIntegerTest("3--3", new int[]{3, 2, 1, 0, -1, -2, -3});
    doParseIntegerTest("3 - -3", new int[]{3, 2, 1, 0, -1, -2, -3});
    doParseIntegerTest("-1--3", new int[]{-1, -2, -3});
  }

  private final void doParseIntegerTest(String integerString, int[] expected) {
    final int[] got = MathUtil.parseIntegers(integerString);
    assertEquals(expected.length, got.length);
    for (int i = 0; i < expected.length; ++i) {
      assertEquals(expected[i], got[i]);
    }
  }

  public void testDoubleString() {
    assertEquals("1.000", MathUtil.doubleString(1.0, 3));
    assertEquals("0.000", MathUtil.doubleString(0.0, 3));
    assertEquals("3.142", MathUtil.doubleString(3.14159265, 3));
    assertEquals("0.001", MathUtil.doubleString(0.0011, 3));
    assertEquals("-0.061224", MathUtil.doubleString(-0.061224, 6));
    assertEquals("-0.606060", MathUtil.doubleString(-0.606060, 6));

    assertEquals("1", MathUtil.doubleString(1.0, 0));
    assertEquals("1", MathUtil.doubleString(1.1234, 0));
    assertEquals("2", MathUtil.doubleString(1.5, 0));
  }
  
  public void testIntegerString() {
    assertEquals("  0", MathUtil.integerString(0, 3));
    assertEquals("  1", MathUtil.integerString(1, 3));
    assertEquals("100", MathUtil.integerString(100, 3));
    assertEquals("999", MathUtil.integerString(999, 3));
    assertEquals("1000", MathUtil.integerString(1000, 3));
  }

  public void testNumDigits() {
    assertEquals(1, MathUtil.getNumDigits(0));
    assertEquals(1, MathUtil.getNumDigits(1));
    assertEquals(1, MathUtil.getNumDigits(9));
    assertEquals(2, MathUtil.getNumDigits(10));
    assertEquals(2, MathUtil.getNumDigits(99));
    assertEquals(3, MathUtil.getNumDigits(100));
    assertEquals(3, MathUtil.getNumDigits(999));
    assertEquals(4, MathUtil.getNumDigits(1000));
    assertEquals(4, MathUtil.getNumDigits(9999));
  }

  public void testToInt() {
    assertTrue(10 == MathUtil.toInt(10.0));
    assertTrue(10 == MathUtil.toInt(10.499999999999));
    assertTrue(10 == MathUtil.toInt(9.5));
    assertTrue(10 == MathUtil.toInt(9.9999999999));

    assertTrue(-10 == MathUtil.toInt(-10.0));
    assertTrue(-10 == MathUtil.toInt(-10.499999999999));
    assertTrue(-10 == MathUtil.toInt(-9.5));
    assertTrue(-10 == MathUtil.toInt(-9.9999999999));
    
    assertTrue(0 == MathUtil.toInt(0.0));
    assertTrue(0 == MathUtil.toInt(0.49999));
    assertTrue(0 == MathUtil.toInt(-0.49999));
    
    assertTrue(1 == MathUtil.toInt(0.5));
    assertTrue(-1 == MathUtil.toInt(-0.5));
}

  public void testAddCommas() {
    assertEquals("1,234", MathUtil.addCommas("1234"));
    assertEquals("1,234.45", MathUtil.addCommas("1234.45"));
    assertEquals("123.45", MathUtil.addCommas("123.45"));
    assertEquals("1", MathUtil.addCommas("1"));
    assertEquals("1,234,567.89", MathUtil.addCommas("1234567.89"));
  }

  public void testParseBytes() {
    assertEquals(15L, MathUtil.parseBytes("15"));
    assertEquals(15L, MathUtil.parseBytes("15B"));
    assertEquals(15360L, MathUtil.parseBytes("15K"));
    assertEquals(15728640L, MathUtil.parseBytes("15M"));
    assertEquals(16106127360L, MathUtil.parseBytes("15G"));
    assertEquals(16492674416640L, MathUtil.parseBytes("15T"));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestMathUtil.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
