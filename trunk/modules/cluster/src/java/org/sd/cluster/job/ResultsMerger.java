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
import org.sd.util.ExecUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for merging resutls from two job runs.
 * <p>
 * @author Spence Koehler
 */
public abstract class ResultsMerger {
  
  /**
   * Given two sets of conflicting lines, resolve the conflicts and write the results.
   */
  protected abstract void mergeConflictingLines(List<String> lines1, List<String> lines2, BufferedWriter writer) throws IOException;

  /**
   * Get a sort command relevant to the file.
   * <p>
   * example: "/usr/bin/sort -t| -k1,2 " + fileToSort
   *
   * @return a sort command or null if no sorting is necessary.
   */
  protected abstract String getSortCommand(String fileToSort);



  protected ResultsMerger() {
  }

  /**
   * Merge the two results files.
   */
  public void mergeResults(String results1, String results2, String mergedOutput) throws IOException {
    // combine results: diff -DFOO results1 results2 > mergedOutput.foo
    final String tempMergedOutput = mergedOutput + ".foo.1";
    final String preSortedOutput = mergedOutput + ".foo.2";
    combineResults(results1, results2, tempMergedOutput);

    // process mergedOutput.foo, writing mergedOutput
    processMergedOutput(tempMergedOutput, preSortedOutput);

    // cleanup temporary file
    ExecUtil.executeProcess("rm -f " + tempMergedOutput);

    // sort results
    sortResults(preSortedOutput, mergedOutput);

    // cleanup temporary file
    ExecUtil.executeProcess("rm -f " + preSortedOutput);
  }

  private final void processMergedOutput(String tempMergedOutput, String mergedOutput) throws IOException {
    final BufferedReader reader = FileUtil.getReader(tempMergedOutput);
    final BufferedWriter writer = FileUtil.getWriter(mergedOutput);
    String line = null;
    final List<String> lines1 = new ArrayList<String>();
    final List<String> lines2 = new ArrayList<String>();

    while (true) {
      // read lines until starts with #, sending directly to merged output.
      while ((line = reader.readLine()) != null && !line.startsWith("#")) {
        writer.write(line);
        writer.newLine();
      }
      if (line == null) break;

      // collect lines from #if until starts with #
      while ((line = reader.readLine()) != null && !line.startsWith("#")) {
        lines1.add(line);
      }

      if (line.startsWith("#else")) {
        // if #else, collect alternate lines until starts with #(endif), merge collected/alternate lines.
        while ((line = reader.readLine()) != null && !line.startsWith("#")) {
          lines2.add(line);
        }

        mergeConflictingLines(lines1, lines2, writer);
      }
      else {
        // else at #endif, send collected lines to merged output.
        for (String line1 : lines1) {
          writer.write(line1);
          writer.newLine();
        }
      }
      if (line == null) break;

      lines1.clear();
      lines2.clear();
    }

    writer.close();
    reader.close();
  }

  private final void combineResults(String results1, String results2, String tempMergedOutput) throws IOException {
    final String command = "/usr/bin/diff -DFOO " + results1 + " " + results2;
    final BufferedWriter writer = FileUtil.getWriter(tempMergedOutput);
    final int response = ExecUtil.executeProcess(command, writer);
    writer.close();
  }

  private final void sortResults(String preSorted, String postSorted) throws IOException {
    final String command = getSortCommand(preSorted);
    if (command != null) {
      final BufferedWriter writer = FileUtil.getWriter(postSorted);
      final int response = ExecUtil.executeProcess(command, writer);
      writer.close();
    }
  }
}
