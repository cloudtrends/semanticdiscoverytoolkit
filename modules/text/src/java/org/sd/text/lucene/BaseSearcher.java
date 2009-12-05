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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;

/**
 * Base searcher class.
 * <p>
 * @author Spence Koehler
 */
public abstract class BaseSearcher implements Searcher {
  
  /**
   * The highest number of hits we ever expect to reasonably manage.
   * <p>
   * Note that Integer.MAX_VALUE is *NOT* an option because of how lucene
   * uses this variable. Sometimes lucene adds to the number of and sometimes
   * it fails to put other bounds on what is collected, causing OutOfMemory
   * errors.
   */
  static final int UNLIMITED_HITS = 10000;


  /**
   * Get this searcher's resources.
   */
  protected abstract SearchResources getResources();

  /**
   * Release the resources (if meaninful) after performing a search or
   * executing a search strategy.
   */
  protected abstract void releaseResources(SearchResources resources);


  private final File dirPath;

  /**
   * Construct with the given dirPath.
   *
   * @param dirPath  The path to the lucene store directory.
   */
  protected BaseSearcher(String dirPath) {
    this(new File(dirPath));
  }

  /**
   * Construct with the given dirPath.
   *
   * @param dirPath  The file of the lucene store directory.
   */
  protected BaseSearcher(File dirPath) {
    this.dirPath = dirPath;
  }

  /**
   * Get the directory of the index being searched.
   */
  public File getDirPath() {
    return dirPath;
  }

  /**
   * Submit the given query to search for unlimited hits.
   */
  public TopDocs search(Query query) throws IOException {
    return search(query, UNLIMITED_HITS);
  }

  /**
   * Submit the give query to search for unlimited hits.
   */
  public TopDocs search(Query query, Sort sort) throws IOException {
    return search(query, UNLIMITED_HITS);
  }

  /**
   * Submit the given query to search for up to N hits.
   */
  public TopDocs search(Query query, int n) throws IOException {
    return search(query, n, null);
  }

  /**
   * Submit the give query to search for up to N hits.
   */
  public synchronized TopDocs search(Query query, int n, Sort sort) throws IOException {
    TopDocs result = null;

    final SearchResources resources = getResources();
    if (resources != null) {
      result = resources.search(query, n, sort);
    }
    releaseResources(resources);

    return result;
  }

  /**
   * Submit the queryContainer's query, collecting stored fields in up to
   * maxHits search hits through the given LuceneFields instance.
   *
   * @param queryContainer  Containing the query to submit.
   * @param maxHits  The maximum number of hits to collect.
   * @param luceneFields  LuceneFields for collecting SearchHit instances
   *                      with stored fields.
   *
   * @return a SearchResult with SearchHits.
   */
  public SearchResult search(QueryContainer queryContainer, int maxHits, LuceneFields luceneFields) throws IOException {
    final SearchHitCallback searchHitCallback = new SearchHitCallback(luceneFields);
    final SearchResult searchResult = search(queryContainer, maxHits, searchHitCallback);
    searchResult.setSearchHits(searchHitCallback.getSearchHits());
    return searchResult;
  }

  /**
   * Submit the queryContainer's query, calling the hitCallback (if present)
   * on up to the maxHits top hits.
   *
   * @param queryContainer  Containing the query to submit.
   * @param maxHits  The maximum number of hits to collect.
   * @param hitCallback  Optional HitCallback function.
   *
   * @return a SearchResult without SearchHits.
   */
  public SearchResult search(QueryContainer queryContainer, int maxHits, HitCallback hitCallback) throws IOException {

    final TopDocs topDocs = search(queryContainer.query, maxHits);
    if (topDocs == null) return null;

    int rank = 0;
    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
      final int docID = scoreDoc.doc;
      final Document doc = getDocument(docID);
      if (hitCallback != null) {
        hitCallback.handleHit(rank, scoreDoc.score, docID, doc);
      }
      ++rank;
    }

    return new SearchResult(queryContainer, maxHits, topDocs.totalHits, hitCallback);
  }

  /**
   * Get the identified document from this searcher.
   */
  public synchronized Document getDocument(int doc) throws IOException {
    Document result = null;

    final SearchResources resources = getResources();
    if (resources != null) {
      result = resources.getDocument(doc);
    }
    releaseResources(resources);

    return result;
  }

  /**
   * Open this searcher.
   */
  public void open() throws IOException {
    // nothing to do!
  }

  /**
   * Close this searcher.
   */
  public void close() throws IOException {
    // nothing to do!
  }

  /**
   * Execute the search strategy in the context of this searcher.
   */
  public void executeSearchStrategy(SearchStrategy searchStrategy) {
    final SearchResources resources = getResources();
    searchStrategy.execute(this, resources);
    releaseResources(resources);
  }
}
