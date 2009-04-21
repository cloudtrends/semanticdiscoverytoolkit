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
 * Utility to print an ascii table.
 * <p>
 * @author Spence Koehler
 */
public class AsciiTable {
  
  public static void main(String[] args) {

    int c = 0;
    for (int i = 0; i < 32; ++i) {
      for (int j = 0; j < 8; ++j) {
        System.out.print("   " + c + ": " + ((char)c));
        ++c;
      }
      System.out.println();
    }
  }

}
