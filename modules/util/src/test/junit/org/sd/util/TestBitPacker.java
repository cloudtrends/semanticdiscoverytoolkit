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
 * JUnit Tests for the BitPacker class.
 * <p>
 * @author Spence Koehler
 */
public class TestBitPacker extends TestCase {

  public TestBitPacker(String name) {
    super(name);
  }
  
  //NOTE: bits come back out "reversed".

  private final void verify(BitPacker bitPacker, boolean[] expected) {
    assertEquals(expected.length, bitPacker.getNumBits());
    verifyBits(bitPacker, expected);

    // verify packed string
    final String packedString = bitPacker.toString();
    final BitPacker bitPacker2 = new BitPacker(packedString);
    verifyBits(bitPacker2, expected);

    // verify bitPacker2 is locked to adds
    final int numBits2 = bitPacker2.getNumBits();
    bitPacker2.addInt(1, 3);
    assertEquals(numBits2, bitPacker2.getNumBits());
    bitPacker2.addAscii("test", 4);
    assertEquals(numBits2, bitPacker2.getNumBits());
  }

  private final void verifyBits(BitPacker bitPacker, boolean[] expected) {
    for (int i = 0; i < expected.length; ++i) {
      assertEquals("i=" + i, expected[i], bitPacker.getBit(i));
    }
  }


  public void testInt1() {
    final BitPacker bitPacker = new BitPacker();
    bitPacker.addInt(7, 3);  // enough bits: expect to get 7 back out (111)
    verify(bitPacker, new boolean[]{true, true, true});
  }

  public void testInt2() {
    final BitPacker bitPacker = new BitPacker();
    bitPacker.addInt(11, 4);  // enough bits: expect to get 11 back out (1011)
    verify(bitPacker, new boolean[]{true, true, false, true});
  }

  public void testTwoInts() {
    final BitPacker bitPacker = new BitPacker();
    bitPacker.addInt(7, 3);  // enough bits: expect to get 7 back out (111)
    bitPacker.addInt(11, 4);  // enough bits: expect to get 11 back out (1011)
    verify(bitPacker, new boolean[]{true, true, true, true, true, false, true});
  }

  public void testLimitedInt1() {
    final BitPacker bitPacker = new BitPacker();
    bitPacker.addInt(7, 2);  // not enough bits: expect to get 3 out (11)
    verify(bitPacker, new boolean[]{true, true});
  }

  public void testAscii1() {
    final BitPacker bitPacker = new BitPacker();
    bitPacker.addAscii("test", 4);  // t:0111'0100 e:0110'0101 s:01110011 t:011'0100

    final boolean[] expected = new boolean[] {
      false, false, true, false, true, true, true, false,  // t
      true, false, true, false, false, true, true, false,  // e
      true, true, false, false, true, true, true, false,   // s
      false, false, true, false, true, true, true, false,  // t
    };

    verify(bitPacker, expected);
  }

  public void testIntAsciiInt() {
    final BitPacker bitPacker = new BitPacker();
    bitPacker.addInt(7, 3);  // enough bits: expect to get 7 back out (111)
    bitPacker.addAscii("test", 4);
    bitPacker.addInt(11, 4);

    final boolean[] expected = new boolean[] {
      true, true, true,                                    // 7
      false, false, true, false, true, true, true, false,  // t
      true, false, true, false, false, true, true, false,  // e
      true, true, false, false, true, true, true, false,   // s
      false, false, true, false, true, true, true, false,  // t
      true, true, false, true,                             // 11
    };

    verify(bitPacker, expected);
  }

  public void testLimitedAscii() {
    final BitPacker bitPacker = new BitPacker();
    bitPacker.addAscii("test", 3);  // t:0111'0100 e:0110'0101 s:01110011 t:011'0100

    final boolean[] expected = new boolean[] {
      false, false, true, false, true, true, true, false,  // t
      true, false, true, false, false, true, true, false,  // e
      true, true, false, false, true, true, true, false,   // s
    };

    verify(bitPacker, expected);
  }

  public void testShorterAscii() {
    final BitPacker bitPacker = new BitPacker();
    bitPacker.addAscii("test", 8);

    final boolean[] expected = new boolean[] {
      false, false, true, false, true, true, true, false,  // t
      true, false, true, false, false, true, true, false,  // e
      true, true, false, false, true, true, true, false,   // s
      false, false, true, false, true, true, true, false,  // t
      false, false, false, false, false, false, false, false,  // EOS
    };

    verify(bitPacker, expected);
  }

  public void testUnspecifiedAscii() {
    final BitPacker bitPacker = new BitPacker();
    bitPacker.addAscii("test", -1);

    final boolean[] expected = new boolean[] {
      false, false, true, false, false, false, false, false,  // ...
      false, false, false, false, false, false, false, false, // 0x04
      false, false, true, false, true, true, true, false,     // t
      true, false, true, false, false, true, true, false,     // e
      true, true, false, false, true, true, true, false,      // s
      false, false, true, false, true, true, true, false,     // t
    };

    verify(bitPacker, expected);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestBitPacker.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
