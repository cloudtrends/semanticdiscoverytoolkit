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


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

/**
 * Utility to extract random lines from a file.
 * <p>
 * @author Spence Koehler
 */
public class RandomLines {
  
  private static final void dumpLine(String line, PrintStream out) throws IOException {
    int cpos = line.indexOf(':');
    cpos = line.indexOf(':', cpos + 1);
    line = line.substring(cpos + 1);
    System.out.println(line);
  }

  // arg1: source file path
  // arg2: integer N where 1 of every N lines randomly selected.
  // stdout: random lines
  public static void main(String[] args) throws IOException {
    final BufferedReader reader = FileUtil.getReader(args[0]);
    final Random random = new Random();
    final int n = Integer.parseInt(args[1]);
    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
      if (random.nextInt(n) == 0) dumpLine(line, System.out);
    }
  }
}
