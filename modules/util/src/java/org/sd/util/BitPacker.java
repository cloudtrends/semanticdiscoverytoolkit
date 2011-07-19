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


import java.nio.charset.Charset;
import java.util.BitSet;

/**
 * A utility to pack values as adjacent bits in a vector, rendering the
 * resultant BitSet as a printable (albeit nonsensical) string.
 * <p>
 * @author Spence Koehler
 */
public class BitPacker {

  private BitSet bits;
  private int bidx;
  private String _string;
  private boolean locked;

  /**
   * Default constructor.
   */
  public BitPacker() {
    this.bits = new BitSet();
    this.bidx = 0;
    this._string = null;
    this.locked = false;
  }

  /**
   * Construct with the given initial size.
   */
  public BitPacker(int nbits) {
    this.bits = new BitSet(nbits);
    this.bidx = 0;
    this._string = null;
    this.locked = false;
  }

  /**
   * Reconstruct with the packed string (see toString).
   * <p>
   * This is meant to serve as a mechanism for retrieving values encoded
   * in the packed string and cannot be added to.
   */
  public BitPacker(String packedString) {
    this.bits = new BitSet();
    this.bidx = 0;
    this._string = packedString;
    this.locked = true;

    decodePackedString(packedString);
  }

  /**
   * Get the underlying BitSet.
   */
  BitSet getBitSet() {
    return bits;
  }

  /**
   * Get the number of bits consumed.
   */
  public int getNumBits() {
    return bidx;
  }

  /**
   * Get the bit at the given index.
   */
  public boolean getBit(int idx) {
    return bits.get(idx);
  }

  /**
   * Add up to numBits (lowest) bits of the given integer.
   */
  public int addInt(int value, int numBits) {

    final int result = bidx;
    if (locked) return result;

    _string = null;

    //NOTE: low bits end up with low bit indexes, appearing reversed in output.

    for (int bit = 0; bit < numBits; ++bit) {
      if (get(value, bit)) {
        bits.set(bidx);
      }
      ++bidx;
    }

    return result;
  }

  /**
   * Add up to numBytes bytes of the string's (presumed to be in ASCII ranges)
   * characters.
   */
  public int addAscii(String value, int numBytes) {

    final int result = bidx;
    if (locked) return result;

    _string = null;

    //NOTE: low bits end up with low bit indexes, appearing reversed in output.

    final byte[] bytes = value.getBytes(Charset.forName("US-ASCII"));
    for (int i = 0; i < numBytes; ++i) {
      if (i >= bytes.length) {
        bidx += 8;  // terminate with a byte of 0's
        break;
      }

      final byte b = bytes[i];
      for (int j = 0; j < 8; ++j) {
        if (get(b, j)) {
          bits.set(bidx);
        }
        ++bidx;
      }
    }

    return result;
  }

  /**
   * Get a packed string representation of this instance's bits.
   * <p>
   * The string is formed of printable ASCII characters from the following
   * ranges:
   * <ul>
   * <li>ASCII 32 to 126 (inclusive) mapped to from 0 to 94 (inclusive)</li>
   * <li>ASCII 192 to 223 (inclusive) mapped to from 95 to 127 (inclusive)</li>
   * </ul>
   * This results in one character (8 bits) for every 7 set bits.
   * <p>
   * Decoding note: Lower set bits correspond to low ASCII bits.
   */
  public String toString() {
    if (_string == null) {
      _string = computeString();
    }
    return _string;
  }

  private final boolean get(int value, int bit) {
    final int mask = BitUtil.setBits(bit, 1);
    return (value & mask) != 0;
  }

  private String computeString() {
    final StringBuilder result = new StringBuilder();

    int curBit = 1;
    byte curByte = 0;

    for (int i = 0; i < bidx; ++i) {
      if ((i % 7) == 0) {
        if (i > 0) {
          result.append(getChar(curByte));
        }

        curBit = 1;
        curByte = 0;
      }
      if (bits.get(i)) {
        curByte |= curBit;
      }
      curBit <<= 1;
    }

    if (bidx > 0) {
      result.append(getChar(curByte));
    }

    return result.toString();
  }

  private char getChar(byte curByte) {
    char result = (char)0;

    // map [0-94] to ASCII [32-126]
    // map [95-127] to ASCII [192-224]

    if (curByte < 95) {
      result = (char)(curByte + 32);
    }
    else {
      result = (char)(curByte + 97);
    }

    return result;
  }

  private final void decodePackedString(String packedString) {
    final int len = packedString.length();
    for (int i = 0; i < len; ++i) {
      final char c = packedString.charAt(i);
      final int value = (c >= 192) ? c - 97 : c - 32;
      for (int j = 0; j < 7; ++j) {
        if (get(value, j)) {
          bits.set(bidx);
        }
        ++bidx;
      }
    }
  }
}
