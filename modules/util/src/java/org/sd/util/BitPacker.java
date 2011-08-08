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
   * @param value the string value to encode
   * @param numBytes the number of bytes of the string to encode, if set to -1, 
   * first two bytes representing the number of bytes to be written are encoded, 
   * then all bytes in the string up to the max unsigned short value(65535) index 
   * are written
   */
  public int addAscii(String value, int numBytes) {

    final int result = bidx;
    if (locked) return result;

    _string = null;

    //NOTE: low bits end up with low bit indexes, appearing reversed in output.

    final byte[] bytes = value.getBytes(Charset.forName("US-ASCII"));
    if(numBytes == -1)
    {
      numBytes = (bytes.length > 65535 ? 65535 : bytes.length);
      addInt(numBytes, 16);
    }      

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
   * <li>ASCII 48 to 57 (inclusive) mapped to from 0 to 9 (inclusive)</li>
   * <li>ASCII 65 to 90 (inclusive) mapped to from 10 to 35 (inclusive)</li>
   * <li>ASCII 97 to 122 (inclusive) mapped to from 36 to 61 (inclusive)</li>
   * <li>ASCII 45 mapped to 62</li>
   * <li>ASCII 95 mapped to 63</li>
   * </ul>
   * This results in one character (8 bits) for every 6 set bits; a base 64
   * encoding.
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
      if ((i % 6) == 0) {
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

    // map [0-9] to ASCII [48-57] "0"-"9"
    // map [10-35] to ASCII [65-90] "A"-"Z"
    // map [36-61] to ASCII [97-122] "a"-"z"
    // map 62 to ASCII 45 "-"
    // map 63 to ASCII 95 "_"

    if (curByte < 10) {
      result = (char)(curByte + 48);
    }
    else if (curByte < 36) {
      result = (char)(curByte + 55);
    }
    else if (curByte < 62) {
      result = (char)(curByte + 61);
    }
    else if (curByte == 62) {
      result = '-';
    }
    else {
      result = '_';
    }

    return result;
  }

  private final void decodePackedString(String packedString) {
    final int len = packedString.length();
    for (int i = 0; i < len; ++i) {
      final char c = packedString.charAt(i);
      int value = 0;

      if (c == '-') {
        value = 62;
      }
      else if (c == '_') {
        value = 63;
      }
      else if (c < 58) {
        value = c - 48;
      }
      else if (c < 91) {
        value = c - 55;
      }
      else {  // c < 123
        value = c - 61;
      }

      for (int j = 0; j < 6; ++j) {
        if (get(value, j)) {
          bits.set(bidx);
        }
        ++bidx;
      }
    }
  }
}
