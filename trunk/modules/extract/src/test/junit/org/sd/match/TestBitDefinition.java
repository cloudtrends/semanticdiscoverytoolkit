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
package org.sd.match;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the BitDefinition class.
 * <p>
 * @author Spence Koehler
 */
public class TestBitDefinition extends TestCase {

  public TestBitDefinition(String name) {
    super(name);
  }
  
  public void test1() {
    final MyBitDefinition bdef = new MyBitDefinition();

    final BitDefinition.Level topLevel = bdef.getTopLevel();

    BitDefinition.Level level = topLevel;

    // name, byteNum, markerPos, valuePos, numValues, markerBit, levelMask, clearMask
    verifyLevelParams(level, "A", 0, 0, 1, 3, (byte)0x01, (byte)0x0F, (byte)0xF0);

    level = level.getNext();
    verifyLevelParams(level, "B", 1, 0, 1, 4, (byte)0x01, (byte)0x1F, (byte)0xE0);

    level = level.getNext();
    verifyLevelParams(level, "C", 2, 0, 1, 3, (byte)0x01, (byte)0x0F, (byte)0xF0);

    level = level.getNext();
    verifyLevelParams(level, "D", 2, 4, 5, 0, (byte)0x10, (byte)0x10, (byte)0xEF);

    level = level.getNext();
    verifyLevelParams(level, "E", 3, 0, 1, 3, (byte)0x01, (byte)0x0F, (byte)0xF0);
  }

  private final void verifyLevelParams(BitDefinition.Level level,
                                       String name,
                                       int byteNum, int markerPos,
                                       int valuePos, int numValues,
                                       byte markerBit, byte levelMask,
                                       byte clearMask) {
    assertEquals(name, level.name);
    assertEquals(byteNum, level.byteNum);
    assertEquals(markerPos, level.markerBitPos);
    assertEquals(valuePos, level.valueBitPos);
    assertEquals(numValues, level.numValueBits);
    assertEquals(markerBit, level.markerBit);
    assertEquals(levelMask, level.levelMask);
    assertEquals(clearMask, level.clearMask);
  }

  private static final class MyBitDefinition extends BitDefinition {
    MyBitDefinition() {
      super();

      addLevel("A", 5);
      addLevel("B", 16);
      addLevel("C", 5);
      addLevel("D", 1);
      addLevel("E", 6);
    }
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestBitDefinition.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
