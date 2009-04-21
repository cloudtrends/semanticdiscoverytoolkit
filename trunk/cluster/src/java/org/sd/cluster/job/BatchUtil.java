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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utilities for working with batches.
 * <p>
 * @author Spence Koehler
 */
public class BatchUtil {
  
  /**
   * Given nodeId of form "machineName-jvmNum", get the machine name.
   */
  public static final String getMachineName(String nodeId) {
    final String[] nodeNamePieces = nodeId.split("-");
    return nodeNamePieces[0].toLowerCase();
  }

  /**
   * Given a path of the form <path>/domain, get the domain.
   */
  public static final String getDomain(String path) {
    final int lastcpos = path.lastIndexOf('/', path.length() - 2);  // ignore potential trailing slash
    return path.substring(lastcpos + 1);
  }

  public static final Map<String, String> buildDomainToMachinePathMap(File file, BatchLineProcessor processor, boolean keepFirst) throws IOException {
    final Map<String, String> result = new LinkedHashMap<String, String>();

    final BufferedReader reader = FileUtil.getReader(file);
    String line = null;

    while ((line = reader.readLine()) != null) {
      final String[] dmp = processor.split(line);

      if (dmp != null) {
        final boolean hasKey = result.containsKey(dmp[0]);

        if ((keepFirst && !hasKey) || !keepFirst) {
          result.put(dmp[0], dmp[1]);
        }
      }
    }

    reader.close();

    return result;
  }

  public static final void writeBatchLines(Map<String, String> domain2mpath, File outputFile, boolean append) throws IOException {
    final BufferedWriter writer = FileUtil.getWriter(outputFile, append);

    for (String mpath : domain2mpath.values()) {
      writer.write(mpath);
      writer.newLine();
    }

    writer.close();
  }

  public static interface BatchLineProcessor {
    /**
     * Split the batch line as read from an input file into the "domain" part
     * (in result[0]) and the "machine path" part (in result[1]).
     *
     * @return a String[2] or null.
     */
    public String[] split(String batchLine);

    /**
     * Get the number of lines split by this processor.
     */
    public int getNumLines();
  }
}
