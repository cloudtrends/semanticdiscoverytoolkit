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

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;

/**
 * Interface for a searcher.
 * <p>
 * @author Spence Koehler
 */
public interface Searcher {

  /**
   * Get the directory of the index being searched.
   */
  public File getDirPath();

  /**
   * Open this searcher.
   */
  public void open() throws IOException;

  /**
   * Close this searcher.
   */
  public void close() throws IOException;

  /**
   * Submit the given query to search for unlimited hits.
   */
  public TopDocs search(Query query) throws IOException;

  /**
   * Submit the give query to search for unlimited hits.
   */
  public TopDocs search(Query query, Sort sort) throws IOException;

  /**
   * Submit the given query to search for up to N hits.
   */
  public TopDocs search(Query query, int n) throws IOException;

  /**
   * Submit the give query to search for up to N hits.
   */
  public TopDocs search(Query query, int n, Sort sort) throws IOException;

  /**
   * Get the identified document from this searcher.
   */
  public Document getDocument(int doc) throws IOException;

  /**
   * Execute the search strategy in the context of this searcher.
   */
  public void executeSearchStrategy(SearchStrategy searchStrategy);
}
