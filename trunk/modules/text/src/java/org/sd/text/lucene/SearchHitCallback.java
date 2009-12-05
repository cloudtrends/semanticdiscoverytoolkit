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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.lucene.document.Document;


/**
 * A HitCallback function that collects SearchHit instances.
 * <p>
 * @author Spence Koehler
 */
public class SearchHitCallback implements HitCallback {

  private LuceneFields luceneFields;
  private List<SearchHit> searchHits;

  /**
   * Construct to create SearchHits without storedFields.
   */
  public SearchHitCallback() {
    this(null);
  }
  
  /**
   * Construct to create SearchHits wih storedFields using luceneFields.
   */
  public SearchHitCallback(LuceneFields luceneFields) {
    this.luceneFields = luceneFields;
    this.searchHits = new ArrayList<SearchHit>();
  }

  /**
   * Handle the given hit.
   */
  public void handleHit(int rank, float score, int docID, Document doc) {
    final Map<String, String> storedFields = (luceneFields == null) ? null : luceneFields.getStoredFields(doc);
    this.searchHits.add(new SearchHit(rank, score, docID, storedFields));
  }

  /**
   * Get the collected search hits.
   */
  public List<SearchHit> getSearchHits() {
    return searchHits;
  }
}
