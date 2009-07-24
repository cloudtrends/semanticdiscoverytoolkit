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


/**
 * Utilities for manipulating bits.
 * <p>
 * @author Spence Koehler
 */
public class BitUtil {
  
  /**
   * Create a byte from 'b' by rotating its bits to the left and
   * rolling in '1's.
   */
  public static final byte rollInOnesFromRight(byte b, int num) {
    byte result = b;

    for (int i = 0; i < num; ++i) {
      result = (byte)((result << 1) | 1);
    }

    return result;
  }

  /**
   * Create a byte from 'b' by rotating its bits to the left and
   * rolling in '0's.
   */
  public static final byte rollInZerosFromRight(byte b, int num) {
    return (byte)(b << num);
  }


  /**
   * Set numBits bits in an integer starting at lowBit and working higher.
   */
  public static final int setBits(int lowBit, int numBits) {
    int result = 1;
    for (int i = 0; i < numBits - 1; ++i) {
      result = (result << 1) | 1;
    }
    for (int i = 0; i < lowBit; ++i) {
      result <<= 1;
    }
    return result;
  }

  public static final int flipBits(int value) {
    return ~value;
  }

  public static final byte flipBits(byte value) {
    return (byte)(~value & 0xFF);
  }

  /**
   * Get the number of bits necessary to encode the given number of values.
   */
  public static final int getNumBits(int numValues) {
    return numValues == 0 ? 0 : (int)(Math.ceil(MathUtil.log2(numValues)));
  }

  /**
   * Get the integer value's (4) bytes.
   */
  public static final byte[] getBytes(int value) {
    return new byte[] {
      (byte)(value >>> 24),
      (byte)(value >>> 16),
      (byte)(value >>>  8),
      (byte)(value),
    };
  }

  /**
   * Get the integer represented in the 4 bytes starting at offset.
   */
  public static final int getInteger(byte[] bytes, int offset) {
    return
      (bytes[offset++] << 24) |
      ((bytes[offset++] & 0xFF) << 16) |
      ((bytes[offset++] & 0xFF) <<  8) |
      (bytes[offset] & 0xFF);
  }

  /**
   * Get the long value's (4) bytes.
   */
  public static final byte[] getBytes(long value) {
    return new byte[] {
      (byte)(value >>> 56),
      (byte)(value >>> 48),
      (byte)(value >>> 40),
      (byte)(value >>> 32),
      (byte)(value >>> 24),
      (byte)(value >>> 16),
      (byte)(value >>>  8),
      (byte)(value),
    };
  }

  /**
   * Get the long represented in the 4 bytes starting at offset.
   */
  public static final long getLong(byte[] bytes, int offset) {
    return
      (((long)bytes[offset++]) << 56) |
      (((long)(bytes[offset++] & 0xFF)) << 48) |
      (((long)(bytes[offset++] & 0xFF)) << 40) |
      (((long)(bytes[offset++] & 0xFF)) << 32) |
      (((long)(bytes[offset++] & 0xFF)) << 24) |
      (((long)(bytes[offset++] & 0xFF)) << 16) |
      (((long)(bytes[offset++] & 0xFF)) <<  8) |
      (((long)bytes[offset] & 0xFF));
  }
}

