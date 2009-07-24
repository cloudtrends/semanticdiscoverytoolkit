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
 * JUnit Tests for the ByteArray class.
 * <p>
 * @author Spence Koehler
 */
public class TestByteArray extends TestCase {

  public TestByteArray(String name) {
    super(name);
  }
  

  public void testSetBytes() {
    final ByteArray ba = new ByteArray();

    ba.addBytes(BitUtil.getBytes(123));
    ba.addBytes(BitUtil.getBytes(456));
    ba.addBytes(BitUtil.getBytes(789));

    assertEquals(123, ba.getInt(0));
    assertEquals(456, ba.getInt(4));
    assertEquals(789, ba.getInt(8));

    ba.setBytes(4, BitUtil.getBytes(654));

    assertEquals(123, ba.getInt(0));
    assertEquals(654, ba.getInt(4));
    assertEquals(789, ba.getInt(8));

    ba.setBytes(12, BitUtil.getBytes(987));

    assertEquals(123, ba.getInt(0));
    assertEquals(654, ba.getInt(4));
    assertEquals(789, ba.getInt(8));
    assertEquals(987, ba.getInt(12));

    // test setting bytes that overlap the current end boundary
    ba.setBytes(9, BitUtil.getBytes(3456));
    assertEquals(3456, ba.getInt(9));

    // test removeBytes
    ba.removeBytes(1, 8);
    assertEquals(3456, ba.getInt(1));  // 3456 should have shifted over
    ba.removeBytes(0, 1);
    assertEquals(3456, ba.getInt(0));  // 3456 should have shifted over
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestByteArray.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
