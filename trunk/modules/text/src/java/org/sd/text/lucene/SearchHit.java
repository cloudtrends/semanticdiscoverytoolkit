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


import java.util.Map;

/**
 * Container for information from a search hit.
 * <p>
 * @author Spence Koehler
 */
public class SearchHit implements Comparable<SearchHit> {

  public final int rank;
  public final float score;
  public final int docID;    // lucene docID
  private Map<String, String> storedFields;

  /**
   * Construct without stored fields.
   */
  public SearchHit(int rank, float score, int docID) {
    this.rank = rank;
    this.score = score;
    this.docID = docID;
    this.storedFields = null;
  }

  /**
   * Construct with the given data.
   * <p>
   * Typically, stored fields will be the result of LuceneFields.getStoredFields(Document).
   */
  public SearchHit(int rank, float score, int docID, Map<String, String> storedFields) {
    this(rank, score, docID);
    this.storedFields = storedFields;
  }

  /**
   * Set this instance's stored fields.
   */
  public void setStoredFields(Map<String, String> storedFields) {
    this.storedFields = storedFields;
  }

  /**
   * Get this instance's stored fields.
   */
  public Map<String, String> getStoredFields() {
    return storedFields;
  }

  /**
   * Get the stored field's value if it exists.
   *
   * @return the stored field's value or null.
   */
  public String getStoredField(String fieldName) {
    String result = null;

    if (storedFields != null) {
      result = storedFields.get(fieldName);
    }

    return result;
  }

  /**
   * Compare this searchHit to the other to sort primarily by
   * rank (increasing) and secondarily by docID (increasing).
   */
  public int compareTo(SearchHit other) {
    int result = this.rank - other.rank;

    if (result == 0) {
      result = (this.docID - other.docID);
    }

    return result;
  }
}
