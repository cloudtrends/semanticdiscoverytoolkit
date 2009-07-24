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
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Utility to remove "no sitemap found" domains from work server output.
 * <p>
 * @author Spence Koehler
 */
public class WorkServerOutputFixer {
  
  private static final Set<String> loadFile(File file) throws IOException {
    final Set<String> result = new LinkedHashSet<String>();

    final BufferedReader reader = FileUtil.getReader(file);

    String line = null;
    while ((line = reader.readLine()) != null) {
      result.add(line);
    }

    reader.close();

    return result;
  }

  public static void main(String[] args) throws IOException {
    //arg0: work server output file [input]   lines of form "machine:path | timeInMillis | destinationNodeNum | timeString"
    //arg1: no sitemap found domains [input]  lines of form "machine:path"
    //arg2: fixed work server output file [output]

    final File originalWorkServerFile = new File(args[0]);
    final File noSitemapDomainsFile = new File(args[1]);
    final File fixedWorkServerFile = new File(args[2]);

    final Set<String> badMPaths = loadFile(noSitemapDomainsFile);

    final BufferedReader reader = FileUtil.getReader(originalWorkServerFile);
    final BufferedWriter writer = FileUtil.getWriter(fixedWorkServerFile);

    String line = null;
    while ((line = reader.readLine()) != null) {
      final String[] pieces = line.split("\\|");
      if (!badMPaths.contains(pieces[0])) {
        writer.write(line);
        writer.newLine();
      }
    }

    writer.close();
    reader.close();
  }
}
