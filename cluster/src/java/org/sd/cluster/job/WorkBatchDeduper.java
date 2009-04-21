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


import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Utility to remove duplicate domains from a crawl_map, keeping the first.
 * <p>
 * @author Spence Koehler
 */
public class WorkBatchDeduper {
  
  public static void main(String[] args) throws IOException {
    //arg0: crawl_map (input)
    //arg1: batch_file (output, deduped)

    //NOTE: crawl_map input lines are of the form: domain|machine:path (or just machine:path works, too)
    //      batch_file output lines are of the form: machine:path

    final BatchUtil.BatchLineProcessor processor =
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

          String domain = pieces[0];
          String mpath = pieces[0];

          if (pieces.length == 1) {
            domain = BatchUtil.getDomain(pieces[0]);
          }
          else {  // assume length == 2
            mpath = pieces[1];
          }
          return new String[]{domain, mpath};
        }

        /**
         * Get the number of lines split by this processor.
         */
        public int getNumLines() {
          return numLines;
        }
      };

    System.out.println("Reading crawl_map '" + args[0] + "'");
    final Map<String, String> domain2mpath = BatchUtil.buildDomainToMachinePathMap(new File(args[0]), processor, true);
                                                                                   
    final int numInputLines = processor.getNumLines();
    final int numOutputLines = domain2mpath.size();
    final int numDups = numInputLines - numOutputLines;

    System.out.println("Writing batch_file '" + args[1] + "'");
    BatchUtil.writeBatchLines(domain2mpath, new File(args[1]), false);

    System.out.println("Found " + numDups + " dups. Wrote " + numOutputLines + " units.");
  }
}
