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

import org.sd.cluster.job.UnitOfWork;
import org.sd.cluster.job.WorkBatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Utility to distribute work batches from lines in a file.
 * <p>
 * @author Spence Koehler
 */
public class WorkBatchDistributor {
  
  private static final Set<String> loadFilterFile(String filename) throws IOException {
    final Set<String> result = new HashSet<String>();

    final BufferedReader reader = FileUtil.getReader(filename);
    String line = null;
    while ((line = reader.readLine()) != null) {
      result.add(line);
    }

    return result;
  }

  // java -Xmx640m org.sd.cluster.job.WorkBatchDistributor 5 ~/tmp/impressum/german.071706.batch.txt ~/tmp/impressum/batches/workbatch
  // java -Xmx640m org.sd.cluster.job.WorkBatchDistributor 15 /home/sbk/sd/resources/ontology/300806/run/5k_test.120406/5k_test.120406.farpoint.txt.gz /home/sbk/sd/resources/ontology/240107/run/200_sample_01/200_sample_01-batches/workbatch /home/sbk/sd/resources/ontology/240107/run/filter.sites.txt 200 4893
  public static void main(String[] args) throws IOException {
    // arg0: number of new batches to create
    // arg1: file containing lines for batches
    // arg2: prefix name of new batches to create
    // arg3: (optional) filter (inclusive) file -- unless using arg4 -- with just the domain names.
    // arg4: (optional) random-sample-size; changes arg3 to be an exclusive filter.
    // arg5: total-size: the total number from which to select the random sample.

    final int numNewPartitions = Integer.parseInt(args[0]);
    final String batchLinesFile = args[1];
    final String newBatchName = args[2];
    final String filterFileName = (args.length > 3) ? args[3] : null;
    final String sampleSizeString = (args.length > 4) ? args[4] : null;
    final Integer sampleSize = (sampleSizeString == null) ? null : Integer.parseInt(sampleSizeString);
    final int totalSize = (sampleSize != null) ? Integer.parseInt(args[5]) : 0;

    final Random random = new Random();

    final List<List<UnitOfWork>> newWorkUnits = new ArrayList<List<UnitOfWork>>();
    for (int i = 0; i < numNewPartitions; ++i) {
      newWorkUnits.add(new ArrayList<UnitOfWork>());
    }

    final Set<String> inclusiveFilter = (args.length == 4) ? loadFilterFile(filterFileName) : null;
    final Set<String> exclusiveFilter = (args.length > 4) ? loadFilterFile(filterFileName) : null;

    // partition just initialized work units
    final BufferedReader reader = FileUtil.getReader(batchLinesFile);
    int index = 0;
    String line = null;
    while ((line = reader.readLine()) != null) {
      final String[] pieces = line.split("/");

      if (inclusiveFilter != null) {
        if (!inclusiveFilter.contains(pieces[pieces.length - 1])) {
          continue;
        }
      }
      if (exclusiveFilter != null) {
        if (exclusiveFilter.contains(pieces[pieces.length - 1])) {
          continue;
        }
      }

      if (sampleSize != null) {
        // determine whether this will be a sample.
        final int v = random.nextInt(totalSize - 1);
        if (v > sampleSize) {
          continue;
        }
        System.out.println(line);
      }

      final UnitOfWork workUnit = new StringUnitOfWork(line);
      final List<UnitOfWork> newUnitList = newWorkUnits.get(index % numNewPartitions);
      newUnitList.add(workUnit);
      ++index;

      if (sampleSize != null && index >= sampleSize) break;
    }

    // write the new batches
    int num = 0;
    for (List<UnitOfWork> workList : newWorkUnits) {
      WorkBatch.writeWorkFile(newBatchName + "-" + num + ".dat", workList);
      ++num;
    }
  }
}
