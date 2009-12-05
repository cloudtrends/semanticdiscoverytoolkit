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


import java.util.List;

/**
 * Container for the results of a search.
 * <p>
 * @author Spence Koehler
 */
public class SearchResult {

  public final QueryContainer queryContainer;
  public final int maxHits;
  public final int totalHits;
  public final HitCallback hitCallback;
  private List<SearchHit> searchHits;

  private HitCallback theHitCallback;

  public SearchResult(QueryContainer queryContainer, int maxHits, int totalHits,
                      HitCallback hitCallback) {
    this(queryContainer, maxHits, totalHits, hitCallback, null);
  }

  public SearchResult(QueryContainer queryContainer, int maxHits, int totalHits,
                      HitCallback hitCallback, List<SearchHit> searchHits) {
    this.queryContainer = queryContainer;
    this.maxHits = maxHits;
    this.totalHits = totalHits;
    this.hitCallback = hitCallback;
    this.searchHits = searchHits;
  }

  public void setSearchHits(List<SearchHit> searchHits) {
    this.searchHits = searchHits;
  }

  public List<SearchHit> getSearchHits() {
    return searchHits;
  }
}
