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


import org.sd.io.NumberedFile;
import org.sd.util.MathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;

/**
 * Utility to split an index into smaller chunks.
 * <p>
 * @author Spence Koehler
 */
public abstract class IndexSplitter {

  /**
   * Given a document retrieved from the existing index that contains only
   * stored fields, create a full document to be added to the new index.
   */
  protected abstract Document generateDocument(Document storedDoc);


  private File indexToSplit;
  private int numDocsPerSplit;
  private File destContainer;
  private String splitNamePrefix;
  private String splitNameSuffix; 
  private Analyzer analyzer;
  private DocumentConverter converter;

  /**
   * Construct to split the index at the given location into indexes
   * at the destination location.
   * <p>
   * Each chunk should have at most numDocsPerSplit documents.
   * <p>
   * Each chunk should be named splitNamePrefix + nextId + splitNameSuffix.
   * <p>
   * Don't split the index if it is already smaller than numDocsPerSplit.
   * <p>
   * Don't optimize the newly built indexes.
   *
   * @param indexToSplit  Path to the index that is to be split.
   * @param numDocsPerSplit  Maximum number of documents to hold in each chunk.
   * @param destContainer  Directory to contain the split indexes.
   * @param splitNamePrefix  Prefix for numbered index names.
   * @param splitNameSuffix  Suffix for numbered index names.
   * @param analyzer  Analyzer to use.
   * @param converter  Document converter.
   */
  protected IndexSplitter(File indexToSplit, int numDocsPerSplit, File destContainer, String splitNamePrefix, String splitNameSuffix, Analyzer analyzer, DocumentConverter converter) {
    this.indexToSplit = indexToSplit;
    this.numDocsPerSplit = numDocsPerSplit;
    this.destContainer = destContainer;
    this.splitNamePrefix = splitNamePrefix;
    this.splitNameSuffix = splitNameSuffix;
    this.analyzer = analyzer;
    this.converter = converter;
  }

  /**
   * Execute the split.
   * <p>
   * Note that an index already smaller than the split size will NOT be split.
   *
   * @return true if the index was split; otherwise, false.
   */
  public boolean split() throws IOException {
    if (!indexToSplit.exists()) return false;
    if (!destContainer.exists()) destContainer.mkdirs();

    // open an iterator over indexToSplit
    final IndexIterator indexIter = new IndexIterator(indexToSplit);
    if (indexIter.getNumDocs() < numDocsPerSplit) return false;

    // open rolling lucene store
    final MyRollingLuceneStore myStore = new MyRollingLuceneStore(numDocsPerSplit, destContainer, splitNamePrefix, splitNameSuffix, analyzer, converter);

    long nextCheck = 60000L;  // check rates in 1 minute

    // iterate over the index, adding docs to the rolling store.
    while (indexIter.hasNext()) {
      final Document storedDoc = indexIter.next();
      final Document newDoc = generateDocument(storedDoc);

      if (newDoc != null) {
        myStore.addDocument(newDoc);
      }

      final long elapsed = indexIter.getElapsed();
      if (elapsed > nextCheck) {
        System.out.println(new Date() + ": Processed " + indexIter.getCount() + " documents in " +
                           MathUtil.timeString(elapsed, false) + " at " +
                           MathUtil.doubleString(indexIter.getRate(), 3) + " docs/sec  ETA=" +
                           indexIter.getETA());
        nextCheck += 60000;
      }
    }

    myStore.close();
    indexIter.close();

    System.out.println(new Date() + ": Processed " + indexIter.getCount() + " documents in " +
                       MathUtil.timeString(indexIter.getElapsed(), false) + " at " +
                       MathUtil.doubleString(indexIter.getRate(), 3) + " docs/sec");

    return true;
  }

  private static final class MyRollingLuceneStore extends RollingLuceneStore {
    private File storeDir;
    private NumberedFile nFile;

    MyRollingLuceneStore(int numDocsPerSplit, File storeDir, String splitNamePrefix, String splitNameSuffix, Analyzer analyzer, DocumentConverter converter) {
      super("IndexSplitter", numDocsPerSplit, 0L, false, null, analyzer, null, null, 0, false, converter);

      this.storeDir = storeDir;
      this.nFile = new NumberedFile(storeDir, splitNamePrefix, splitNameSuffix);

      super.initialize(null);
    }

    protected List<File> nextAvailableIndexDir() {
      return nFile.getExistingAndNext();
    }
    
    public File getStoreRoot(){
      return this.storeDir;
    }
  }
}
