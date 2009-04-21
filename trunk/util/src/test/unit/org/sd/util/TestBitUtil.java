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
 * JUnit Tests for the BitUtil class.
 * <p>
 * @author Spence Koehler
 */
public class TestBitUtil extends TestCase {

  public TestBitUtil(String name) {
    super(name);
  }
  
  public void testNumBits() {
    assertEquals(0, BitUtil.getNumBits(0));
    assertEquals(0, BitUtil.getNumBits(1));
    assertEquals(1, BitUtil.getNumBits(2));
    assertEquals(2, BitUtil.getNumBits(3));
    assertEquals(2, BitUtil.getNumBits(4));
    assertEquals(3, BitUtil.getNumBits(5));
    assertEquals(3, BitUtil.getNumBits(8));
    assertEquals(4, BitUtil.getNumBits(9));
    assertEquals(4, BitUtil.getNumBits(16));
    assertEquals(5, BitUtil.getNumBits(17));
    assertEquals(5, BitUtil.getNumBits(32));
  }

  public void testIntToBytesAndBack() {
    final int[] samples = {0, 1, 3, 17, 159, 26535, 897932384, 2147483647,
                           -1, -3, -17, -159, -26535, -897932384, -2147483647};

    for (int sample : samples) {
      assertEquals(sample, BitUtil.getInteger(BitUtil.getBytes(sample), 0));
    }
  }

  public void testSetBits() {
    assertEquals(0x0F, BitUtil.setBits(0, 4));
    assertEquals(0x1E, BitUtil.setBits(1, 4));
    assertEquals(0x04, BitUtil.setBits(2, 1));
  }

  public void testFlipBits() {
    assertEquals(0xF0, BitUtil.flipBits(BitUtil.setBits(0, 4)) & 0xFF);
    assertEquals(0xE1, BitUtil.flipBits(BitUtil.setBits(1, 4)) & 0xFF);
    assertEquals(0xFB, BitUtil.flipBits(BitUtil.setBits(2, 1)) & 0xFF);

    assertEquals((byte)0xF0, BitUtil.flipBits((byte)BitUtil.setBits(0, 4)));
    assertEquals((byte)0xE1, BitUtil.flipBits((byte)BitUtil.setBits(1, 4)));
    assertEquals((byte)0xFB, BitUtil.flipBits((byte)BitUtil.setBits(2, 1)));
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestBitUtil.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
