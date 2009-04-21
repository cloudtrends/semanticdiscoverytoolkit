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
package org.sd.cluster.job;


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * One-off utility to read in multiple batches of the form /mnt/.../domain, keeping only the last version of a domain.
 * <p>
 * @author Spence Koehler
 */
public class WorkCombiner {
  
  public static void main(String[] args) throws IOException {

    final Map<String, String> domain2path = new HashMap<String, String>();

    for (int i = 0; i < args.length; ++i) {
      final BufferedReader reader = FileUtil.getReader(args[i]);
      String line = null;
      while ((line = reader.readLine()) != null) {
        final String[] pieces = line.split("/");
        domain2path.put(pieces[pieces.length - 1], line);
      }
    }

    for (String path : domain2path.values()) {
      System.out.println(path);
    }
  }
}
