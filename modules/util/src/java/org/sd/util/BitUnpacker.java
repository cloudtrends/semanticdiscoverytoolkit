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


/**
 * Utility to unpack BitPacker's bits.
 * <p>
 * @author Spence Koehler
 */
public class BitUnpacker {
  
  private BitPacker bitPacker;
  private int bidx;

  /**
   * Construct with the bitPacker instance to unpack.
   */
  public BitUnpacker(BitPacker bitPacker) {
    this.bitPacker = bitPacker;
    this.bidx = 0;
  }

  /**
   * Construct with the packed string (bitPacker.toString()) to unpack.
   */
  public BitUnpacker(String packedString) {
    this.bitPacker = new BitPacker(packedString);
    this.bidx = 0;
  }

  /**
   * Read the next n bits as an int.
   */
  public int readInt(int numBits) {
    int result = 0;
    int curBit = 1;

    for (int bit = 0; bit < numBits; ++bit) {
      if (bitPacker.getBit(bidx)) {
        result |= curBit;
      }
      curBit <<= 1;
      ++bidx;
    }

    return result;
  }

  /**
   * Read the next bits as an ASCII string.
   */
  public String readAscii(int numBytes) {
    final StringBuilder result = new StringBuilder();

    for (int i = 0; i < numBytes; ++i) {
      final char c = readChar();
      if (c == 0) break;
      result.append(c);
    }

    return result.toString();
  }

  private final char readChar() {
    byte result = 0;
    int curBit = 1;

    for (int bit = 0; bit < 8; ++bit) {
      if (bitPacker.getBit(bidx)) {
        result |= curBit;
      }
      curBit <<= 1;
      ++bidx;
    }

    return (char)result;
  }
}
