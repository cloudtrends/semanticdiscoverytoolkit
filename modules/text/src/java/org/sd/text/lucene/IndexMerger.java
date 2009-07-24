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
package org.sd.text.lucene;


import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;

/**
 * Utility to merge indexes into one index.
 * <p>
 * @author Spence Koehler
 */
public class IndexMerger {

  private File sourceContainer;
  private File mergedIndex;

  /**
   * Construct to merge the indexes at the given source location into an index
   * at the destination location.
   * <p>
   * Do optimize the newly built index.
   *
   * @param sourceContainer  Path to the directory containing indexes (directories).
   * @param mergedIndex  Path to the index that is to be built.
   */
  protected IndexMerger(File sourceContainer, File mergedIndex) {
    this.sourceContainer = sourceContainer;
    this.mergedIndex = mergedIndex;
  }

  /**
   * Execute the merge.
   *
   * @return true if the indexes were merged; otherwise, false.
   */
  public boolean merge() throws IOException {
    if (!sourceContainer.exists()) return false;
    if (mergedIndex.exists()) {
      throw new IllegalStateException("Merge destination already exists! Aborting! " + mergedIndex);
    }
    else {
      final File parentFile = mergedIndex.getParentFile();
      if (!parentFile.exists()) parentFile.mkdirs();
    }

    // find all indexes to merge
    final File[] indexesToMerge = sourceContainer.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return (name.charAt(0) != '.' && new File(dir, name).isDirectory());
        }
      });

    // open merged store
    final LuceneStore mergedStore = new LuceneStore(mergedIndex);
    mergedStore.open();

    System.out.println(new Date() + ": Merging " + indexesToMerge.length + " indexes to '" + mergedIndex + "'...");

    mergedStore.addIndexes(indexesToMerge, false);

    System.out.println(new Date() + ": Done merging. Optimizing...");

    mergedStore.close(true);

    System.out.println(new Date() + ": Done optimizing '" + mergedIndex + "'.");

    return true;
  }


  public static final void main(String[] args) throws IOException {
    // arg0: mergedIndex  -- name of merged index to create [must not exist]
    // arg1: sourceDir  -- directory containing indexes to merge

    System.out.println("arg0: " + args[0]);
    System.out.println("arg1: " + args[1]);

    final IndexMerger merger = new IndexMerger(new File(args[1]), new File(args[0]));
    merger.merge();
  }
}
