/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package com.ancestry.util;


import java.util.Random;

/**
 * Simple utility to print a random string of chars (e.g., for password
 * generation)
 * <p>
 * @author Spence Koehler
 */
public class RandomKey {
  
  public static void main(String[] args) {
    //arg0 : length of key
    final int len = Integer.parseInt(args[0]);

    final Random r = new Random();

    for (int i = 0 ; i < len; ++i) {
      final char c = (char)(r.nextInt(127 - 33) + 33);
      System.out.print(c);
    }

    System.out.println();
  }
}
