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
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

/**
 * A searcher over a lucene store.
 * <p>
 * @author Spence Koehler
 */
public class LuceneSearcher extends BaseSearcher {
  
  private SearchResources resources;

  /**
   * Construct a lucene searcher.
   *
   * @param dirPath  The path to the lucene store directory.
   */
  public LuceneSearcher(String dirPath) {
    super(dirPath);
    this.resources = null;
  }

  /**
   * Construct a lucene searcher.
   *
   * @param dirPath  The file of the lucene store directory.
   */
  public LuceneSearcher(File dirPath) {
    super(dirPath);
    this.resources = null;
  }

  /**
   * Open this lucene searcher, expecting the store to already exist.
   */
  public void open() throws IOException {
    this.resources = new SearchResources(getDirPath());
  }

  /**
   * Close this lucene searcher.
   */
  public void close() throws IOException {
    if (resources != null) resources.close();
  }
  
  /**
   * Get this searcher's resources.
   */
  protected SearchResources getResources() {
    return resources;
  }

  /**
   * Release the resources (if meaninful) after performing a search or
   * executing a search strategy.
   */
  protected void releaseResources(SearchResources resources) {
    //do nothing. the resources will persist across searches.
  }

  /**
   * Get this searcher's index searcher.
   */
  public IndexSearcher getIndexSearcher() {
    return resources != null ? resources.indexSearcher : null;
  }

  public static void main(String[] args) throws IOException {
    //argi: path to lucene index dir

    for (int i = 0; i < args.length; ++i) {
      final LuceneSearcher luceneSearcher = new LuceneSearcher(args[i]);
      luceneSearcher.open();
      
      final IndexSearcher indexSearcher = luceneSearcher.getIndexSearcher();
      final IndexReader indexReader = indexSearcher.getIndexReader();
      
      System.out.println("numDocs(" + args[i] + ")=" + indexReader.numDocs());

      luceneSearcher.close();
    }
  }
}
