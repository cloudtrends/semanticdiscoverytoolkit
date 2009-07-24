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
 * JUnit Tests for the BitVector class.
 * <p>
 * @author Spence Koehler
 */
public class TestBitVector extends TestCase {

  public TestBitVector(String name) {
    super(name);
  }
  
  public void testGetSet() {
    final BitVector bits = new BitVector();

    bits.set(2);
    bits.set(6);
    bits.set(15);
    bits.set(19);
    bits.set(39);

    for (int i = 0; i < 64; ++i) {
      if (i == 2 || i == 6 || i == 15 || i == 19 || i == 39) {
        assertTrue("(should've been true) i=" + i, bits.get(i));
      }
      else {
        assertFalse("(should've been false) i=" + i, bits.get(i));
      }
    }
  }

  public void testCountSetBits() {
    final BitVector bits = new BitVector();

    bits.set(2);
    bits.set(6);
    bits.set(15);
    bits.set(19);
    bits.set(39);

    assertEquals(0, bits.countSetBits(0, 1));

    assertEquals(1, bits.countSetBits(0, 2));
    assertEquals(1, bits.countSetBits(1, 2));
    assertEquals(1, bits.countSetBits(1, 3));
    assertEquals(1, bits.countSetBits(2, 3));
    assertEquals(1, bits.countSetBits(2, 2));

    assertEquals(2, bits.countSetBits(2, 6));
    assertEquals(2, bits.countSetBits(1, 7));
    assertEquals(2, bits.countSetBits(0, 14));

    assertEquals(3, bits.countSetBits(7, 64));

    for (int i = 0; i < 7; ++i) bits.set(i);

    assertEquals(7, bits.countSetBits(0, 8));
  }

  public void testFirstSetBit1() {
    final BitVector bits = new BitVector();

    assertEquals(-1, bits.firstSetBit((byte)0x00));
    assertEquals(0, bits.firstSetBit((byte)0x80));
    assertEquals(1, bits.firstSetBit((byte)0x40));
    assertEquals(2, bits.firstSetBit((byte)0x20));
    assertEquals(3, bits.firstSetBit((byte)0x10));
  }

  public void testNextSetBit() {
    final BitVector bits = new BitVector();

    bits.set(2);
    bits.set(6);
    bits.set(15);
    bits.set(19);
    bits.set(39);

    for (int i = 0; i < 3; ++i) {
      assertEquals("i=" + i, 2, bits.nextSetBit(i));
    }

    for (int i = 3; i < 7; ++i) {
      assertEquals("i=" + i, 6, bits.nextSetBit(i));
    }

    for (int i = 7; i < 16; ++i) {
      assertEquals("i=" + i, 15, bits.nextSetBit(i));
    }

    for (int i = 16; i < 20; ++i) {
      assertEquals("i=" + i, 19, bits.nextSetBit(i));
    }

    for (int i = 20; i < 40; ++i) {
      assertEquals("i=" + i, 39, bits.nextSetBit(i));
    }

    for (int i = 40; i < 64; ++i) {
      assertEquals("i=" + i, -1, bits.nextSetBit(i));
    }
  }

  public void testPrevSetBit() {
    final BitVector bits = new BitVector();

    bits.set(2);
    bits.set(6);
    bits.set(15);
    bits.set(19);
    bits.set(39);

    for (int i = 64; i >= 40; --i) {
      assertEquals("i=" + i, 39, bits.prevSetBit(i));
    }

    for (int i = 39; i >= 20; --i) {
      assertEquals("i=" + i, 19, bits.prevSetBit(i));
    }

    for (int i = 19; i >= 16; --i) {
      assertEquals("i=" + i, 15, bits.prevSetBit(i));
    }

    for (int i = 15; i >= 7; --i) {
      assertEquals("i=" + i, 6, bits.prevSetBit(i));
    }

    for (int i = 6; i >= 3; --i) {
      assertEquals("i=" + i, 2, bits.prevSetBit(i));
    }

    for (int i = 2; i >= 0; --i) {
      assertEquals("i=" + i, -1, bits.prevSetBit(i));
    }
  }

  public void testClear() {
    final BitVector bits = new BitVector();

    bits.set(2);
    bits.set(6);
    bits.set(15);
    bits.set(19);
    bits.set(39);

    bits.clear(6, 7);
    bits.clear(14,21);
    bits.clear(38, 39);

    assertTrue(bits.get(2));
    assertFalse(bits.get(6));
    assertFalse(bits.get(15));
    assertFalse(bits.get(19));
    assertTrue(bits.get(39));
  }

  public void testShiftRight() {
    final BitVector bits = new BitVector();

    bits.set(2);
    bits.set(6);
    bits.set(15);
    bits.set(19);
    bits.set(39);

    bits.shiftRight(4, 3);  // 6->9, 15->18, 19->22, 39->42

    assertEquals(42, bits.prevSetBit(64));

    for (int i = 0; i < 3; ++i) {
      assertEquals("i=" + i, 2, bits.nextSetBit(i));
    }

    for (int i = 3; i < 10; ++i) {
      assertEquals("i=" + i, 9, bits.nextSetBit(i));
    }

    for (int i = 10; i < 19; ++i) {
      assertEquals("i=" + i, 18, bits.nextSetBit(i));
    }

    for (int i = 19; i < 23; ++i) {
      assertEquals("i=" + i, 22, bits.nextSetBit(i));
    }

    for (int i = 23; i < 43; ++i) {
      assertEquals("i=" + i, 42, bits.nextSetBit(i));
    }

    for (int i = 43; i < 67; ++i) {
      assertEquals("i=" + i, -1, bits.nextSetBit(i));
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestBitVector.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
