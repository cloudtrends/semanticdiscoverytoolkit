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
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Utility to create a work batch from lines in a file, interleaving
 * data sprayed across multiple nodes.
 * <p>
 * @author Spence Koehler
 */
public class WorkPathInterleaver {
  
  private Map<String, LinkedList<String>> node2paths;

  // assuming input data of the form:  "/mnt/<nodeName>/...", partition
  // the data by nodeName.
  public WorkPathInterleaver(File file) throws IOException {
    this.node2paths = new HashMap<String, LinkedList<String>>();

    load(file);
  }

  private final void load(File file) throws IOException {
    final BufferedReader reader = FileUtil.getReader(file);

    String line = null;
    while ((line = reader.readLine()) != null) {
      final int slashPos = line.indexOf('/', 5);
      final String nodeName = line.substring(0, slashPos);

      LinkedList<String> paths = node2paths.get(nodeName);
      if (paths == null) {
        paths = new LinkedList<String>();
        node2paths.put(nodeName, paths);
      }
      paths.add(line);
    }

    reader.close();
  }

  // dump the lines such that data from each node is interleaved.
  public void dump(PrintStream out) throws IOException {
    final List<String> nodeNames = new ArrayList<String>(node2paths.keySet());

    while (nodeNames.size() > 0) {
      for (Iterator<String> iter = nodeNames.iterator(); iter.hasNext(); ) {
        final String nodeName = iter.next();
        final LinkedList<String> paths = node2paths.get(nodeName);
        final String path = paths.removeFirst();

        out.println(path);

        if (paths.size() == 0) {
          iter.remove();
        }
      }
    }
  }

  /**
   * Re-order work paths so that they are interleaved.
   */
  public static void main(String[] args) throws IOException {
    //arg0: pathsFileName
    //stdout: interleaved paths

    //java -Xmx640m org.sd.cluster.job.WorkPathInterleaver /home/sbk/sd/resources/ontology/batch/global.011007-take2.batch.2.txt.gz | gzip > /home/sbk/sd/resources/ontology/batch/global.011007-take2.interleaved.batch.2.txt.gz

    final String pathsFileName = args[0];
    final WorkPathInterleaver interleaver = new WorkPathInterleaver(new File(args[0]));
    interleaver.dump(System.out);
  }
}
