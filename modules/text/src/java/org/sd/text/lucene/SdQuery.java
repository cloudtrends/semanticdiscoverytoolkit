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


import org.sd.text.KeywordSplitter;
import org.sd.text.sentiment.TopicSplitter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.QueryTermExtractor;
import org.apache.lucene.search.highlight.WeightedTerm;

/**
 * Query wrapper class.
 * <p>
 * @author Spence Koehler
 */
public class SdQuery implements Serializable, TopicSplitter {

  private static final long serialVersionUID = 42L;


  private String queryString;
  private Query query;
  private String[] queryTerms;
  private Set<String> fieldNames;

  private transient KeywordSplitter _splitter;
  private transient String highlightLeft;
  private transient String highlightRight;

  public SdQuery(String queryString, String fieldIdClassname) {
    this.queryString = queryString;

    // make sure queryString properly references the FieldId class name.
    if (fieldIdClassname != null && !queryString.startsWith(fieldIdClassname)) {
      queryString = fieldIdClassname + queryString;
    }

    final Set<String> queryTerms = new HashSet<String>();
    this.fieldNames = new HashSet<String>();
    this.query = LuceneUtils.buildQuery(queryString, queryTerms, fieldNames);

    // set queryTerms. note: don't include "not" terms
    this.queryTerms = queryTerms.toArray(new String[queryTerms.size()]);
  }

  public SdQuery(String queryString, SearchParser searchParser) {
    this.queryString = queryString;

    this.fieldNames = new HashSet<String>();
    final Set<String> queryTerms = new HashSet<String>();
    this.query = searchParser.parseQuery(queryString, queryTerms, fieldNames);

    // set queryTerms. note: don't include "not" terms
    this.queryTerms = queryTerms.toArray(new String[queryTerms.size()]);
  }

  public SdQuery(String queryString, QueryParser queryParser) {
    this.queryString = queryString;
    try {
      this.query = queryParser.parse(queryString);

      //todo: populate fieldNames. Currently, these are left null such that
      //      if we need them we'll get a NullPointerException which will
      //      alert us that it is time to implement this method!

      // It turns out that lucene handles the functionality we need out of the box!
      // The search.highlight package handles term extraction, including exclustion of prohibited terms
      //final WeightedTerm[] queryTerms = QueryTermExtractor.getTerms(this.query.rewrite(new IndexReader()));
      // todo: implement the rewrite() call to incorporate an index reader
      final WeightedTerm[] queryTerms = QueryTermExtractor.getTerms(this.query);
      this.queryTerms = new String[queryTerms.length];
      for(int i = 0; i < queryTerms.length; i++){
        this.queryTerms[i] = queryTerms[i].getTerm();
      }
    }
    catch (ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Get the query string.
   */
  public String getQueryString() {
    return queryString;
  }

  /**
   * Get the lucene query object.
   */
  public Query getQuery() {
    return query;
  }

  /**
   * Get the query terms (without field info).
   */
  public String[] getQueryTerms() {
    return queryTerms;
  }

  /**
   * Determine whether this query has (references) the given named field.
   */
  public boolean hasField(String fieldName) {
    return fieldNames.contains(fieldName);
  }

  /**
   * Get the (non-negated) field names referenced by this query.
   */
  public Set<String> getFieldNames() {
    return fieldNames;
  }

  /**
   * Split the string on query terms such that result[EVEN] hold string
   * text around consecutive query terms and result[ODD} hold string text
   * matching this query's query terms.
   */
  public String[] split(String string) {
    final KeywordSplitter splitter = getKeywordSplitter();

    if (splitter == null) return new String[]{string};

    final KeywordSplitter.Split[] splits = splitter.splitOnlyLast(string);
    final List<String> strings = new ArrayList<String>();

    for (KeywordSplitter.Split split : splits) {
      final int len = strings.size();
      if (split.type == KeywordSplitter.SplitType.HAS_LAST_WORD) {
        if ((len % 2) == 0) strings.add("");
        strings.add(split.string);
      }
      else {
        if ((len % 2) == 1) strings.add("");
        strings.add(split.string);
      }
    }

    return strings.toArray(new String[strings.size()]);
  }

  /**
   * Set the string to add to the left of query terms for highlighting.
   * <p>
   * If undefined, '*' will be used.
   */
  public void setHighlightLeft(String highlightLeft) {
    this.highlightLeft = highlightLeft;
  }

  /**
   * Set the string to add to the right of query terms for highlighting.
   * <p>
   * If undefined, '*' will be used.
   */
  public void setHighlightRight(String highlightRight) {
    this.highlightRight = highlightRight;
  }

  /**
   * Highlight query terms in the string.
   * <p>
   * Use the values of highlight-Left and -Right or '*' to highlight.
   */
  public String highlightString(String string) {
    return highlightString(string, highlightLeft, highlightRight);
  }

  /**
   * Highlight query terms in the string.
   * <p>
   * Use the values of highlight-Left and -Right or '*' to highlight.
   */
  public String highlightString(String string, String highlightLeft, String highlightRight) {
    return highlightString(string, highlightLeft, highlightRight, null);
  }

  /**
   * Highlight query terms in the string.
   * <p>
   * Use the values of highlight-Left and -Right or '*' to highlight.
   */
  public String highlightString(String string, String highlightLeft, String highlightRight, Integer maxChars) {
    final StringBuilder result = new StringBuilder();

    final int len = string.length();
    final String[] pieces = split(string);  // split on query terms

    if (maxChars != null && pieces.length > 0) {
      // keep to limit by removing from front of first non-query piece
      int numCharsToKeep = Math.min(pieces[0].length(), maxChars);

      // if we have a query term
      if (pieces.length > 1 &&
          (pieces[0].length() + pieces[1].length() + 1) > maxChars) {

        // the following would put the query term at the end
        numCharsToKeep -= (pieces[1].length() - 1);

        // let's move it to about the middle
        numCharsToKeep -= ((maxChars / 2) + pieces[1].length());
      }

      if (numCharsToKeep <= 0) {
        pieces[0] = "...";
      }
      else if (numCharsToKeep < pieces[0].length()) {
        pieces[0] = "..." + pieces[0].substring(pieces[0].length() - numCharsToKeep);
      }
    }

    for (int i = 0; i < pieces.length; ++i) {
      String piece = pieces[i];

      if (maxChars != null && result.length() + piece.length() > maxChars) {
        piece = piece.substring(0, maxChars - result.length());
      }

      if (i > 0) result.append(' ');
      if ((i % 2) == 1) result.append(highlightLeft == null ? "*" : highlightLeft);
      result.append(piece);
      if ((i % 2) == 1) result.append(highlightRight == null ? "*" : highlightRight);

      //todo: don't count html markup in the length
      if (maxChars != null && result.length() >= maxChars) {
        result.append("...");
        break;
      }
    }

    return result.toString();
  }

  public String toString() {
    return query != null ? query.toString() : queryString;
  }

  public boolean equals(Object other) {
    if (this == other) return true;

    boolean result = false;

    if (other instanceof SdQuery) {
      final SdQuery o = (SdQuery)other;

      if (queryString == null) {
        result = o.queryString == null;
      }
      else {
        result = queryString.equals(o.queryString);
      }

      if (result) {
        if (query == null) {
          result = o.query == null;
        }
        else {
          result = query.equals(o.query);
        }
      }

      if (result) {
        if (queryTerms == null) {
          result = o.queryTerms == null;
        }
        else {
          result = queryTerms.length == o.queryTerms.length;

          if (result) {
            for (int i = 0; i < queryTerms.length; ++i) {
              final String queryTerm = queryTerms[i];
              final String otherQueryTerm = o.queryTerms[i];

              if (queryTerm == null) {
                result = otherQueryTerm == null;
              }
              else {
                result = queryTerm.equals(otherQueryTerm);
              }

              if (!result) break;
            }
          }
        }
      }
    }

    return result;
  }

  public int hashCode() {
    int result = 7;

    if (queryString != null) {
      result = result * 31 + queryString.hashCode();
    }

    if (query != null) {
      result = result * 31 + query.hashCode();
    }

    if (queryTerms != null) {
      for (String queryTerm : queryTerms) {
        if (queryTerm != null) {
          result = result * 31 + queryTerm.hashCode();
        }
      }
    }
    
    return result;
  }

  public final KeywordSplitter getKeywordSplitter() {
    if (_splitter == null) {
      final String[] queryTerms = getQueryTerms();
      if (queryTerms != null) {
        _splitter = new KeywordSplitter(null, queryTerms);
      }
    }
    return _splitter;
  }
}
