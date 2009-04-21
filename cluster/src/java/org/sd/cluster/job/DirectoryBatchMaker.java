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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A batch maker that creates a string unit of work that is the path to each
 * (deep) directory contained under a path.
 * <p>
 * @author Spence Koehler
 */
public class DirectoryBatchMaker implements BatchMaker {

  private String dirPath;
  private int partitionNum;
  private int numPartitions;

  public DirectoryBatchMaker(String dirPath, int partitionNum, int numPartitions) {
    this.dirPath = dirPath;
    this.partitionNum = partitionNum;
    this.numPartitions = numPartitions;
  }

  public String getDirPath() {
    return dirPath;
  }

  /**
   * Create the work units for this batch.
   */
  public List<UnitOfWork> createWorkUnits() throws IOException {
    final Collection<String> paths = new ArrayList<String>();
    final File file = new File(dirPath);
    if (file.isDirectory()) walkDirectories(file, paths);

    final List<UnitOfWork> result = new LinkedList<UnitOfWork>();
    int count = 0;
    for (String path : paths) {
      if ((count % numPartitions) == partitionNum) {
        result.add(new StringUnitOfWork(path));
      }
      ++count;
    }
    return result;
  }
  
  /**
   * Create work lists for all partitions.
   */
  public List<List<UnitOfWork>> createWorkLists() throws IOException {
    final List<List<UnitOfWork>> result = new ArrayList<List<UnitOfWork>>();

    for (int i = 0; i < numPartitions; ++i) result.add(new LinkedList<UnitOfWork>());
    createWorkUnits(result);

    return result;
  }

  /**
   * Auxiliary to createWorkLists.
   */
  private final void createWorkUnits(List<List<UnitOfWork>> workLists) throws IOException {
    final Collection<String> paths = new ArrayList<String>();
    final File file = new File(dirPath);
    if (file.isDirectory()) walkDirectories(file, paths);

    int count = 0;
    for (String path : paths) {
      List<UnitOfWork> curList = workLists.get(count % numPartitions);
      curList.add(new StringUnitOfWork(path));
      ++count;
    }
  }

  /**
   * Add all terminal directories (directories that don't contain other directories)
   * to the result.
   */
  public void walkDirectories(File curDir, Collection<String> result) throws IOException {
    final File[] files = curDir.listFiles();

    Arrays.sort(files);

    boolean foundDir = false;
    for (File file : files) {
      if (file.isDirectory()) {
        foundDir = true;
        walkDirectories(file, result);
      }
    }
    if (!foundDir) {
      result.add(curDir.getAbsolutePath());

      if ((result.size() % 1000) == 0) System.out.println("\t\tfound " + result.size() + " workUnit candidates so far... (" + curDir.getAbsolutePath() + ")");
    }
  }

  // traverse directories and create work batch files for N partitions into the current working directory
  // named workbatch-N.dat
  public static void main(String[] args) throws IOException {
    // args[0]: path to directory
    // args[1]: number of partitions

    final DirectoryBatchMaker batchMaker = new DirectoryBatchMaker(args[0], 0, Integer.parseInt(args[1]));
    final List<List<UnitOfWork>> workLists = batchMaker.createWorkLists();

    int index = 0;
    for (List<UnitOfWork> workList : workLists) {
      WorkBatch.writeWorkFile(args[2] + "/workbatch-" + index + ".dat", workList);
      ++index;
    }
  }
}
