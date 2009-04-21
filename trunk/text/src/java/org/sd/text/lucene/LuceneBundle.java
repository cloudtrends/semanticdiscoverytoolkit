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


import java.io.IOException;
import java.util.Date;

import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.Query;

/**
 * Container for a lucene store and a lucene searcher over the same index.
 * <p>
 * @author Spence Koehler
 */
public class LuceneBundle {

  private String directory;
  private LuceneStore luceneStore;
  private LuceneSearcher luceneSearcher;

  public LuceneBundle(String directory) {
    this.directory = directory;
    this.luceneStore = new LuceneStore(directory);
    this.luceneSearcher = new LuceneSearcher(directory);
  }

  /**
   * Get the directory containing the lucene index.
   */
  public String getDirectory() {
    return directory;
  }

  /**
   * Get the lucene store for the index.
   */
  public LuceneStore getLuceneStore() {
    return luceneStore;
  }

  /**
   * Get the lucene searcher for the index.
   */
  public LuceneSearcher getLuceneSearcher() {
    return luceneSearcher;
  }

  /**
   * Open the store and the searcher.
   */
  public void open() throws IOException {
    luceneStore.open();
    luceneSearcher.open();
  }

  /**
   * Close the store and the searcher.
   */
  public void close(boolean optimize) throws IOException {
    luceneStore.close(optimize);
    luceneSearcher.close();
  }

  /**
   * Perform a search over the index.
   * <p>
   * Where queryString is of the form: "field:value,..." where
   * field if preceded by '+' is 'anded' or if preceded by '^' is 'not-ed'.
   */
  public TopDocs search(String queryString, int n) throws IOException {
    TopDocs result = null;
    Query query = null;

    try {
      query = LuceneUtils.buildQuery(queryString, null, null);
      result = luceneSearcher.search(query, n);
    }
    catch (NullPointerException e) {
      System.err.println(new Date() + ": LuceneBundle.search(" + queryString + ")");
      e.printStackTrace(System.err);
    }

    return result;
  }
}
