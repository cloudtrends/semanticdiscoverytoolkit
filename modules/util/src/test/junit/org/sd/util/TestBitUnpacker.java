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
 * JUnit Tests for the BitUnpacker class.
 * <p>
 * @author Spence Koehler
 */
public class TestBitUnpacker extends TestCase {

  public TestBitUnpacker(String name) {
    super(name);
  }
  

  public void testBasics() {
    final BitPacker bitPacker = new BitPacker();
    bitPacker.addInt(5, 3);
    bitPacker.addAscii("test", 8);
    bitPacker.addAscii("it", 8);
    bitPacker.addInt(11, 4);

    final String packedString = bitPacker.toString();

    final BitUnpacker bitUnpacker1 = new  BitUnpacker(bitPacker);
    assertEquals(5, bitUnpacker1.readInt(3));
    assertEquals("test", bitUnpacker1.readAscii(8));
    assertEquals("it", bitUnpacker1.readAscii(8));
    assertEquals(11, bitUnpacker1.readInt(4));


    final BitUnpacker bitUnpacker2 = new  BitUnpacker(packedString);
    assertEquals(5, bitUnpacker2.readInt(3));
    assertEquals("test", bitUnpacker2.readAscii(8));
    assertEquals("it", bitUnpacker2.readAscii(8));
    assertEquals(11, bitUnpacker2.readInt(4));
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestBitUnpacker.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
