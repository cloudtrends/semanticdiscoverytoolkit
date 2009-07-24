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


import org.sd.util.BitUtil;

/**
 * Class to encapsulate defining bit roles.
 * <p>
 * @author Spence Koehler
 */
public abstract class BitDefinition {

// hierarchical subsumption -- linked list; named levels. marker + value
  
  private int freeBit;    // next free bit
  private Level topLevel; // top level
  private Level prevLevel;

  protected BitDefinition() {
    this.freeBit = 0;
    this.topLevel = null;
    this.prevLevel = null;
  }

  public int numMarkerBytes() {
    return ((freeBit + 7) / 8);
  }

  public Marker startMarker(int value) {
    return new Marker(topLevel, value, numMarkerBytes());
  }

  /**
   * If the named level is marked at the given offset, get and return
   * its value; otherwise, return null.
   */
  public Integer getValue(String levelName, byte[] bytes, int offset) {
    Integer result = null;

    for (Level level = topLevel; level != null; level = level.next) {
      if (levelName.equals(level.name)) {
        if (level.isSet(bytes, offset)) {
          result = level.getValue(bytes, offset);
        }
        break;
      }
    }

    return result;
  }

  Level getTopLevel() {
    return topLevel;
  }

  // add levels in order from top to bottom
  protected final void addLevel(String name, int numValues) {
    if (numValues == 1) --numValues; // special case single choice. always assume 0.
    final int numValueBits = BitUtil.getNumBits(numValues);

    if (numValueBits > 8) {
      throw new IllegalArgumentException("numValues (" + numValues + ") can't exceed 8 bits (" + numValueBits + ")! [name=" + name + "]");
    }

    // if there aren't enough bits in the byte, skip up to the next byte
    final int inc = ((freeBit % 8) + numValueBits + 1);
    if (inc > 8) {
      freeBit = ((freeBit / 8) + 1) * 8;  // align to start of next byte
    }

    // markerBit is at freeBit; valueBits are next N higher bits.
    final Level level = new Level(name, freeBit, numValueBits, prevLevel);
    freeBit += numValueBits + 1;

    if (this.topLevel == null) {
      this.topLevel = level;
    }
    this.prevLevel = level;
  }

  public static final class Marker {
    private byte[] bytes;
    private Level curLevel;

    Marker(Level level, int value, int numBytes) {
      this.curLevel = level;
      this.bytes = new byte[numBytes];
      level.markBits(bytes, value);
    }

    public byte[] getMarkerBytes() {
      return bytes;
    }

    public void startNext(String name, int value) {
      Level level = curLevel.next;

      if (level == null || !name.equals(level.name)) {
        // restarting at an equal or higher level
        level = curLevel;
        while (level != null) {
          // clear out this level's bits
          level.clearBits(bytes);

          if (name.equals(level.name)) {
            // found the new current level
            break;
          }

          level = level.prev;
        }

        if (level == null) {
          // couldn't find the name level. either it is lower or non-existant.
          throw new IllegalStateException("can't transition from '" + curLevel.name + "' to '" + name + "'!");
        }

        // clear out higher level bits
        for (Level prevLevel = level.prev; prevLevel != null; prevLevel = prevLevel.prev) {
          prevLevel.clearBits(bytes);
        }

        // drop down and set level bits.
      }
      // else, moved down one level. don't need to do anything but set level bits.

      // set level bits
      level.markBits(bytes, value);

      this.curLevel = level;
    }
  }

  public static final class Level {
    final String name;
    final int byteNum;
    final int markerBitPos;
    final int valueBitPos;
    final int numValueBits;

    final byte markerBit;  // 1 for marker bit; 0's elsewhere
    final byte levelMask;  // 1's for level bits; 0's elsewhere
    final byte clearMask;  // 0's for level bits; 1's elsewhere

    private Level next;
    private Level prev;

    Level(String name, int markerBitPos, int numValueBits, Level prevLevel) {
      this.name = name;
      this.byteNum = (markerBitPos / 8);
      this.markerBitPos = markerBitPos % 8;
      this.valueBitPos = this.markerBitPos + 1;
      this.numValueBits = numValueBits;

      this.markerBit = (byte)BitUtil.setBits(this.markerBitPos, 1);
      this.levelMask = (byte)BitUtil.setBits(this.markerBitPos, numValueBits + 1);
      this.clearMask = BitUtil.flipBits(levelMask);

      this.prev = prevLevel;
      if (prevLevel != null) {
        prevLevel.next = this;
      }
    }

    public void markBits(byte[] bytes, int value) {
      if (numValueBits == 0) {
        // ignore value
        bytes[byteNum] |= markerBit;
      }
      else {
        bytes[byteNum] |= (markerBit | (value << valueBitPos));
      }
    }

    public void clearBits(byte[] bytes) {
      bytes[byteNum] &= clearMask;
    }

    public int getValue(byte[] bytes, int offset) {
      return (numValueBits == 0) ? 0 : (bytes[offset + byteNum] & levelMask) >>> valueBitPos;
    }

    public boolean isSet(byte[] bytes, int offset) {
      return (bytes[offset + byteNum] & markerBit) != 0;
    }

    Level getNext() {
      return next;
    }

    Level getPrev() {
      return prev;
    }
  }
}
