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


import org.apache.lucene.search.Query;

/**
 * Container for a query.
 * <p>
 * @author Spence Koehler
 */
public class QueryContainer {

  public final Query query;
  private String queryText;

  public QueryContainer(Query query) {
    this(query, null);
  }

  public QueryContainer(Query query, String queryText) {
    this.query = query;
    this.queryText = queryText;
  }
  
  public void setQueryText(String queryText) {
    this.queryText = queryText;
  }

  public String getQueryText() {
    return queryText;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append("query(").
      append(queryText).
      append(")=").
      append(query);

    return result.toString();
  }
}
