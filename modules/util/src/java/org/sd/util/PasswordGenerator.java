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


import java.util.Random;

/**
 * Utility to generate a completely random password.
 * <p>
 * @author Spence Koehler
 */
public class PasswordGenerator {
  
  private Random random;

  public PasswordGenerator() {
    this.random = new Random(System.currentTimeMillis());
  }

  public String generatePassword(int length) {
    final StringBuilder result = new StringBuilder();

    for (int i = 0; i < length; ++i) {
      // any ASCII character from 33 to 126 (inclusive) is fair game.
      result.append((char)(33 + random.nextInt(94)));
    }

    return result.toString();
  }

  public static void main(String[] args) {
    // arg0: N (for a password of length N characters), default=6

    int[] lengths = null;

    if (args.length == 0) {
      // default to a single 6-character password
      lengths = new int[]{6};
    }
    else {
      lengths = new int[args.length];

      for (int i = 0; i < args.length; ++i) {
        try {
          lengths[i] = Integer.parseInt(args[i]);
        }
        catch (Exception e) {
          lengths[i] = 6;
        }
      }
    }

    final PasswordGenerator passwordGenerator = new PasswordGenerator();

    for (int length : lengths) {
      System.out.println(passwordGenerator.generatePassword(length));
    }
  }
}
