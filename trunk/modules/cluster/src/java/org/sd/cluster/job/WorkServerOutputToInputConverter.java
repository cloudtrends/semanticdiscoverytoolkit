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


import org.sd.cio.RemoteFile;
import org.sd.util.LineBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Utility to convert work server output back into input for re-using caches.
 * <p>
 * @author Spence Koehler
 */
public class WorkServerOutputToInputConverter {

  /**
   * Processor to identify cache destinations for data server directories
   * in the form: cachedMachine^dataServer:path, where dataServer:path was
   * the original location of the data as distributed out through a work
   * server.
   */
  private static final BatchUtil.BatchLineProcessor PROCESSOR =
    new BatchUtil.BatchLineProcessor() {
      private int numLines = 0;

      /**
       * Split the batch line as read from an input file into the "domain" part
       * (in result[0]) and the "machine path" part (in result[1]).
       *
       * @return a String[2] or null.
       */
      public String[] split(String batchLine) {
        ++numLines;
        final String[] pieces = batchLine.split("\\|");

        // batchLine is of the form: "originalPath | timeInMillis | destinationNodeNum | timeString"
        // where originalPath may have vertical bars, so count from the back.

        String originalPath = pieces[0];        //of the form: "machine:path"
        final int cPos = originalPath.indexOf('^');  // or "dest^machine:path"
        if (cPos >= 0) {
          originalPath = originalPath.substring(cPos + 1);  // now of the form: "machine:path"
        }

        final String newOwner = pieces[pieces.length - 2];      //of the form: "Machine-jvmNum"

        final String newMachine = BatchUtil.getMachineName(newOwner);
        final String origMachineAndPath = RemoteFile.getRemoteName(originalPath);
        final String mpath = newMachine + "^" + origMachineAndPath;
        final String domain = getDomain(mpath);

        final String finalMPath = stitch(mpath, pieces, 1, pieces.length - 3);

        return new String[]{domain, finalMPath};
      }

      /**
       * Get the number of lines split by this processor.
       */
      public int getNumLines() {
        return numLines;
      }

      private final String getDomain(String path) {
        String result = BatchUtil.getDomain(path);
        if (result.length() == 2) {
          // special case: when the 'domain' is just 2 (hex) chars, the "key" needs to be the text after '^'.
          result = path.split("\\^")[1];
        }
        return result;
      }

      private final String stitch(String first, String[] pieces, int startInd, int endInd) {
        final LineBuilder result = new LineBuilder();

        result.append(first);
        for (int i = startInd; i < endInd; ++i) result.append(pieces[i]);

        return result.toString();
      }
    };

  public static final int convertOutputToInput(File wsOutputFile, File wsInputFile, boolean append, boolean keepFirst) throws IOException {
    final Map<String, String> domain2mpath = BatchUtil.buildDomainToMachinePathMap(wsOutputFile, PROCESSOR, keepFirst);
    BatchUtil.writeBatchLines(domain2mpath, wsInputFile, append);
    return domain2mpath.size();
  }

  public static void main(String[] args) throws IOException {
    //arg0: new workserver input file  [output from this process]
    //arg1+: workserver output file(s)  [input to this process]

    final File newWorkServerInputFile = new File(args[0]);

    System.out.println("Creating new work server file '" + newWorkServerInputFile + "'...");

    int numWritten = 0;

    for (int i = 1; i < args.length; ++i) {
      final File workServerOutputFile = new File(args[i]);
      final boolean didFirst = (i > 1);  // append if did first, otherwise, overwrite

      System.out.println("\tAdding data from '" + workServerOutputFile + "' (append=" + didFirst + ")");

      numWritten += convertOutputToInput(workServerOutputFile, newWorkServerInputFile, didFirst, false);

      System.out.println("\t\ttotalRead=" + PROCESSOR.getNumLines() + ", totalWritten=" + numWritten);
    }
  }
}
