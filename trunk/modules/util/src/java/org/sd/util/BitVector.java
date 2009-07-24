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


import java.io.DataInput;
import java.io.DataOutput;

import java.io.IOException;

/**
 * Encapsulation of a vector of bits.
 * <p>
 * @author Spence Koehler
 */
public class BitVector {

  private static final int USR_MASK = 0x00ff;  // Unsigned Shift Right Bit (for a byte)

  private static final byte[] CLEAR_BIT_MASKS = new byte[] {
    (byte)0x7f,  // 01111111
    (byte)0xbf,  // 10111111
    (byte)0xdf,  // 11011111
    (byte)0xef,  // 11101111
    (byte)0xf7,  // 11110111
    (byte)0xfb,  // 11111011
    (byte)0xfd,  // 11111101
    (byte)0xfe,  // 11111110
  };

  private static final byte[] GET_BIT_MASKS = new byte[] {
    (byte)0x80,  // 10000000
    (byte)0x40,  // 01000000
    (byte)0x20,  // 00100000
    (byte)0x10,  // 00010000
    (byte)0x08,  // 00001000
    (byte)0x04,  // 00000100
    (byte)0x02,  // 00000010
    (byte)0x01,  // 00000001
  };


  private byte[] bits;
  private int lastSetBit;

  /**
   * Construct empty.
   */
  public BitVector() {
    this(0);
  }

  /**
   * Construct with capacity for the given number of bytes.
   */
  public BitVector(int initialSize) {
//    this.bits = new byte[initialSize / 8 + 1];
    this.bits = new byte[initialSize];
    this.lastSetBit = -1;
  }

  /**
   * Copy constructor.
   */
  public BitVector(BitVector other) {
    this.bits = new byte[other.bits.length];
    copyBytes(other.bits, this.bits);
    this.lastSetBit = other.lastSetBit;
  }

  /**
   * Get this vector's size (in bits).
   */
  public int size() {
    return lastSetBit + 1;
  }

  public boolean equals(Object o) {
    boolean result = false;
    if (o instanceof BitVector) {
      final BitVector other = (BitVector)o;
      if (lastSetBit == other.lastSetBit) {
        result = true;
        for (int index = (lastSetBit >> 3); index >= 0; --index) {
          if (bits[index] != other.bits[index]) {
            result = false;
            break;
          }
        }
      }
    }
    return result;
  }

  public int hashCode() {
    int result = 13;
    for (int index = lastSetBit >> 3; index >= 0; --index) {
      result = result * 13 + bits[index];
    }
    return result;
  }

  /**
   * Determine whether the bit at the offset is set.
   * 
   * @return true if it is set; otherwise, false.
   */
  public boolean get(int bitOffset) {
    boolean result = false;

    final int index = (bitOffset >> 3);  // bitOffset / 8
    final int  bit = bitOffset % 8;

    // 01234567
    if (index >= 0 && index < bits.length) {
//      result = (bits[index] & (1 << (7 - bit))) != 0;
      result = (bits[index] & GET_BIT_MASKS[bit]) != 0;
    }

    return result;
  }

  public void set(int bitOffset) {
    final int index = (bitOffset >> 3);  // bitOffset / 8
    final int bit = bitOffset % 8;

    if (index >= bits.length) {
      // grow the array
      growToSize(index + 1);
    }

    // 01234567
    bits[index] |= (1 << (7 - bit));

    if (bitOffset > lastSetBit) lastSetBit = bitOffset;
  }

  /**
   * Clear the given bit (i.e. set to false).
   */
  public final void clear(int bitOffset) {
    final int index = (bitOffset >> 3);  // bitOffset / 8
    final int bit = bitOffset % 8;

    if (index < bits.length && index >= 0) {
      bits[index] = (byte)(bits[index] & CLEAR_BIT_MASKS[bit]);
    }

    if (bitOffset == lastSetBit) {
      lastSetBit = prevSetBit(bitOffset);
    }
  }

  /**
   * Clear the bits from startOffset (inclusive) to endOffset (exclusive).
   */
  public void clear(int startOffset, int endOffset) {
    int startIndex = (startOffset >> 3);  // startOffset / 8
    final int startBit = (startOffset % 8);

    if (startIndex < 0) startIndex = 0;
    if (startIndex >= bits.length) return;

    --endOffset;  // interface is for before. implementation is for at or before.

    int endIndex = (endOffset >> 3);  // endOffset / 8
    int endBit = (endOffset % 8);

    if (endIndex >= bits.length) {
      endIndex = bits.length - 1;
      endBit = 7;
    }

    if (endIndex < startIndex) return;  // won't clear backwards.

    if (startIndex == endIndex) {
      bits[startIndex] = clearBits(bits[startIndex], startBit, endBit);
    }
    else {
      // clear start byte
      if (startBit > 0) {
        bits[startIndex] = clearBits(bits[startIndex], startBit, 7);
      }
      else {
        bits[startIndex] = 0;  // clear the whole first byte.
      }

      // clear bytes in between
      for (int i = startIndex + 1; i < endIndex; ++i) {
        bits[i] = 0;
      }

      // clear end byte
      if (endBit == 7) {  // clear all
        bits[endIndex] = 0;
      }
      else {
        bits[endIndex] = clearBits(bits[endIndex], 0, endBit);
      }
    }
  }

  /**
   * Clear b's bits from startIndex (inclusive) to endIndex (inclusive).
   */
  protected final byte clearBits(byte b, int startBit, int endBit) {
    byte result = 0;

    if (b == 0) return result;

    // build a mask with 1's from 0 up to startBit; 0's from startBit thru endBit; 1's after endBit.
    // 1110 0011
/*
//this way works, but is probably slower than the alternative below.
    byte mask = 0;

    // shift in 1's
    if (startBit > 0) {
      mask = (byte)1;
      for (int i = 0; i < startBit; ++i) {
        mask = (byte)((mask << 1) | 1);
      }
    }

    // shift in 0's
    mask <<= (endBit - startBit + 1);

    // shift in 1's
    int endCount = 7 - endBit;
    for (int i = 0; i < endCount; ++i) {
      mask = (byte)((mask << 1) | 1);
    }
*/

    byte mask = (byte)0xFF;
    for (int bit = startBit; bit <= endBit; ++bit) {
      mask &= CLEAR_BIT_MASKS[bit];
    }

    return (byte)(b & mask);
  }

  public void shiftRight/*_complexButFaster*/(int insertOffset, int numShiftBits) {
    if (numShiftBits == 0 || insertOffset > lastSetBit) return;

    final int shiftBits = numShiftBits % 8;                            // bits to shift within a byte
    final int lastByte = lastSetBit >> 3;                              // current end byte
    final int insertByte = insertOffset >> 3;                          // byte of first insertion
    final int insertBit = insertOffset % 8;                            // bit in first insertion byte
    final int shiftedInsertByte = (insertOffset + numShiftBits) >> 3;  // byte of last insertion
    final int shiftBytes = shiftedInsertByte - insertByte;             // number of bytes to shift
    final int insertBits = bits[insertByte];                           // insert byte's bits (content)

    final int keepLeftMask = BitUtil.rollInZerosFromRight((byte)0xff, 8 - insertBit);   // 11110000
    final int keepRightMask = ~keepLeftMask & USR_MASK;                // 00001111

    // reallocate if needed
    final int newLastByte = ((lastSetBit + numShiftBits) >> 3);        // new req'd size of bits array
    growToSize(newLastByte + 1);                                       // plus 1 because exclusive

    if (shiftBits == 0) {  // can just copy bytes! (except, maybe, for beginning byte if not on a boundary)
      // loop: copy bytes
      for (int index = lastByte; index > insertByte; --index) {
        bits[index + shiftBytes] = bits[index];
      }

      // deal with left boundary (including insertion bits)
      if (insertBit == 0) {  // on a boundary
        // just copy the full byte
        bits[shiftedInsertByte] = (byte)insertBits;

        // and clear the insert byte
        bits[insertByte] = 0;
      }
      else {
        // handle splitting byte at insertOffset
        bits[insertByte + shiftBytes] = (byte)(insertBits & keepRightMask);
        bits[insertByte] = (byte)(insertBits & keepLeftMask);
      }
    }
    else {                 // need to roll bits. possibly jumping over bytes if numShiftBits > 8
      final int reverseBitShift = 8 - shiftBits;

      // hand right edge
      final int lastBits = bits[lastByte] & USR_MASK;
      int lboffset = 0;
      if (newLastByte == lastByte) {
        bits[newLastByte] = (byte)((bits[lastByte - 1] << reverseBitShift) | (lastBits >>> shiftBits));
        lboffset = 1;
      }
      else {
        bits[newLastByte] = (byte)(bits[lastByte] << reverseBitShift);
      }

      // loop: split shifted bits across byte boundaries
      for (int index = lastByte - lboffset; index > insertByte; --index) {
        bits[index + shiftBytes] = (byte)((bits[index - 1] << reverseBitShift) | ((bits[index] & USR_MASK) >>> shiftBits));
      }

      // handle left edge (deal with insertion bits)
      if (insertBit + numShiftBits <= 8) {
        // special boundary case: splitting within a single byte
        bits[insertByte] = (byte)((insertBits & keepLeftMask) | ((insertBits & keepRightMask) >>> shiftBits));
      }
      else {
        bits[shiftedInsertByte] = (byte)((insertBits & keepRightMask) >>> shiftBits);
        bits[insertByte] = (byte)(insertBits & keepLeftMask);
      }      
    }

    // clear emptied space (bytes)
    for (int index = insertByte + 1; index < shiftedInsertByte; ++index) {
      bits[index] = 0;
    }

    // update lastSetBit
    lastSetBit += numShiftBits;
  }

  public void shiftRight_simpleButSlow(int insertOffset, int numShiftBits) {
    if (numShiftBits == 0 || insertOffset > lastSetBit) return;

    // from end, find last set bit. set(x + numShiftBits). move before. repeat until x < insertOffset
    for (int bitOffset = lastSetBit; bitOffset >= insertOffset; bitOffset = prevSetBit(bitOffset)) {
      set(bitOffset + numShiftBits);
      clear(bitOffset);
    }

//     // zero out inserted bits
// //no longer necessary when clear after set in loop above.
// //    clear(insertOffset, insertOffset + numShiftBits);
  }

  /**
   * Count the number of set bits from startBitIndex (inclusive) to endBitIndex
   * (inclusive).
   */
  public int countSetBits() {
    return countSetBits(0, lastSetBit);
  }

  /**
   * Count the number of set bits from startBitIndex (inclusive) to endBitIndex
   * (inclusive).
   */
  public int countSetBits(int startBitIndex, int endBitIndex) {
    int result = 0;

    for (int setBit = nextSetBit(startBitIndex); setBit <= endBitIndex && setBit >= 0; setBit = nextSetBit(setBit + 1)) {
      ++result;
    }

    return result;
  }

  /**
   * Find the index of the first bit before bitIndex (exclusive) that is set
   * to true.
   *
   * @return the index of a set bit or -1 if none are set.
   */
  public int prevSetBit(int bitIndex) {
    int result = -1;

    --bitIndex;  // interface is for before. implementation is for at or before.
    if (bitIndex < 0) return result;

    int index = (bitIndex >> 3);  // bitOffset / 8

    if (index >= bits.length) {
      result = lastSetBit;
    }
    else {
      int bitOffset = -1;
      final byte b = bits[index];

      if (b != 0) {
        final int bit = bitIndex % 8;
        bitOffset = lastSetBit(b, bit);

        if (bitOffset < 0) --index;
      }

      if (bitOffset< 0) {  // not found in last byte
        while (index >= 0) {
          bitOffset = lastSetBit(bits[index]);
          if (bitOffset >= 0) {
            break;
          }
          --index;
        }
      }

      if (bitOffset >= 0) {
        result = (index << 3) + bitOffset;  // index * 8 + bitOffset
      }
    }

    return result;
  }

  /**
   * Find the index of the first bit forward from bitIndex (inclusive) that
   * is set to true.
   *
   * @return the index of a set bit or -1 if none are set.
   */
  public int nextSetBit(int bitIndex) {
    int result = -1;

    int index = (bitIndex >> 3);  // bitOffset / 8

    if (index < bits.length) {
      int bitOffset = -1;
      final byte b = bits[index];

      if (b != 0) {
        final int bit = bitIndex % 8;
        bitOffset = firstSetBit(b, bit);

        if (bitOffset < 0) ++index;
      }

      if (bitOffset < 0) {  // not found in first byte
        while (index < bits.length) {
          bitOffset = firstSetBit(bits[index]);
          if (bitOffset >= 0) {
            break;
          }
          ++index;
        }
      }

      if (bitOffset >= 0) {
        result = (index << 3) + bitOffset;  // index * 8 + bitOffset
      }
    }

    return result;
  }

  /**
   * Find the first set bit in the byte on or after index 'bit'.
   */
  protected final int firstSetBit(byte b, int bit) {
    final byte mask = (byte)(0xFF >>> bit);
    final byte c = (byte)(mask & b);

    return firstSetBit(c);
  }

  /**
   * Find the bit index of the first set bit in the byte.
   * <p>
   * 01234567
   *
   * @return the first set bit index or -1.
   */
  protected final int firstSetBit(byte b) {
    int result = -1;

    if (b != 0) {
      byte marker = (byte)0x80;  // 1000 0000
      for (int i = 0; i < 8; ++i) {
        if ((b & marker) != 0) {
          result = i;
          break;
        }
        marker >>>= 1;
      }
    }

    return result;
  }

  /**
   * Find the last set bit in the byte on or after index 'bit'.
   */
  protected final int lastSetBit(byte b, int bit) {
    final byte mask = (byte)(0xFF << (7 - bit));
    final byte c = (byte)(mask & b);

    return lastSetBit(c);
  }

  /**
   * Find the bit index of the last set bit in the byte.
   * <p>
   * 01234567
   *
   * @return the last set bit index or -1.
   */
  protected final int lastSetBit(byte b) {
    int result = -1;

    if (b != 0) {
      byte marker = (byte)0x01;  // 0000 0001
      for (int i = 7; i >= 0; --i) {
        if ((b & marker) != 0) {
          result = i;
          break;
        }
        marker <<= 1;
      }
    }

    return result;
  }

  //todo: implement public void shiftLeft(...) to squash/delete bits if/when needed


  protected final void growToSize(int numBytes) {
    if (numBytes <= bits.length) return;  // we're already big enough.

    final byte[] newBits = new byte[numBytes];
    copyBytes(bits, newBits);
    this.bits = newBits;
  }

  protected final void copyBytes(byte[] fromBits, byte[] toBits) {
    for (int i = 0; i < fromBits.length; ++i) {
      toBits[i] = fromBits[i];
    }
  }

  /**
   * Write this instance to the dataOutput stream such that it
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    dataOutput.writeInt(lastSetBit);
    
    int numBytes = ((lastSetBit + 1) >> 3) + 1;
    if (numBytes > bits.length) numBytes = bits.length;

    dataOutput.writeInt(numBytes);
    dataOutput.write(bits, 0, numBytes);
  }

  /**
   * Read this instance's contents from the dataInput stream that was written by
   * this.write(dataOutput).
   * <p>
   * NOTE: this requires all implementing classes to have a default constructor
   *       with no args.
   *
   * @param dataInput  the data output to write to.
   */
  public void read(DataInput dataInput) throws IOException {
    this.lastSetBit = dataInput.readInt();

    final int numBytes = dataInput.readInt();

    this.bits = new byte[numBytes];
//    dataInput.readFully(bits);
    dataInput.readFully(bits, 0, numBytes);
  }

  private static final String asString(byte b) {
    final StringBuilder result = new StringBuilder();

    for (int i = 0; i < 8; ++i) {
      boolean isSet = (b & GET_BIT_MASKS[i]) != 0;
      result.append(isSet ? '1' : '0');

      if (((i + 1) % 4) == 0) result.append(' ');
    }

    return result.toString();
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    final int numBits = bits.length << 3;  // *8
    for (int i = 0; i < numBits; ++i) {
      result.append(get(i) ? '1' : '0');

      if (((i + 1) % 4) == 0) result.append(' ');
      if (((i + 1) % 8) == 0) result.append(' ');
    }

    return result.toString();
  }

  public String toString(int[] rowEndInds) {
    final StringBuilder result = new StringBuilder();

    int row = 0;
    for (int i = 0; i <= lastSetBit; ++i) {
      result.append(get(i) ? '1' : '0');

      if ((i + 1) == rowEndInds[row]) {
        result.append('\n');
        ++row;
      }
    }

    return result.toString();
  }

  public String toString(int numBitsPerLine) {
    final StringBuilder result = new StringBuilder();

    for (int i = 0; i <= lastSetBit; ++i) {
      result.append(get(i) ? '1' : '0');

      if (((i + 1) % numBitsPerLine) == 0) {
        result.append('\n');
      }
    }

    return result.toString();
  }
}
