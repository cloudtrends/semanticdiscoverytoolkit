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
 * A wrapper around the whirlpool algorithm implementing the hashing function
 * interface.
 * <p>
 * NOTE: this implementation is NOT thread-safe. Use one instance per thread.
 *
 * @author Spence Koehler
 */
public class WhirlpoolHashingFunction extends Whirlpool implements HashingFunction {

  private byte[] digest = new byte[DIGESTBYTES];

  public WhirlpoolHashingFunction() {
    super();
  }

  /**
   * Hash the given string and return as an array of bytes.
   */
  public byte[] getHashBytes(String string) {
    NESSIEinit();
    NESSIEadd(string);
    NESSIEfinalize(digest);
    return digest;
  }

  /**
   * Get a human-readable string of the given bytes.
   * <p>
   * The resulting string is a text representation of hexadecimal values.
   */
  public String getHashString(byte[] bytes) {
    return display(bytes);
  }
  
  public static void main(String[] args) {
    final WhirlpoolHashingFunction hasher = new WhirlpoolHashingFunction();
    System.out.println("hash(" + args[0] + ")=" + hasher.getHashString(hasher.getHashBytes(args[0])));
  }
}
