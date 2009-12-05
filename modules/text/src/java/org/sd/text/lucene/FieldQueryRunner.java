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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.sd.util.PropertiesParser;
import org.sd.util.ReflectUtil;

/**
 * Utility class to submit a query for a field.
 * <p>
 * @author Spence Koehler
 */
public class FieldQueryRunner {
  
  private LuceneSearcher luceneSearcher;
  private LuceneFields luceneFields;

  public FieldQueryRunner(File storeDir, LuceneFields luceneFields) {
    this.luceneSearcher = new LuceneSearcher(storeDir);    
    this.luceneFields = luceneFields;
  }

  public void open() throws IOException {
    luceneSearcher.open();
  }

  public void close() throws IOException {
    luceneSearcher.close();
  }

  /**
   * Query the comma-delimited field(s), merging results by rank.
   */
  public MergedHits query(String field, String queryString, int maxHits) throws IOException {

    if (field.indexOf(',') >= 0) {
      // handle a comma separated list of fields
      return query(field.split("\\s*,\\s*"), queryString, maxHits);
    }

    final SearchResult searchResult =
      luceneFields.getSearchResult(luceneSearcher, field, queryString, maxHits, true);

    return new MergedHits(searchResult);
  }

  /**
   * Query the field(s), merging results by rank.
   */
  public synchronized MergedHits query(String[] fields, String queryString, int maxHits) throws IOException {
    final MergedHits result = new MergedHits();

    for (String field : fields) {
      final MergedHits mergedHits = query(field, queryString, maxHits);
      result.merge(mergedHits);
    }

    return result;
  }

  /**
   * Format a hit for MergedHits.showResults.
   */
  protected String formatHit(SearchHit searchHit) {
    final StringBuilder result = new StringBuilder();

    result.
      append(searchHit.rank).
      append('(').
      append(searchHit.score).
      append(')');

    if (searchHit.getStoredFields() != null) {
      for (Map.Entry<String, String> storedField : searchHit.getStoredFields().entrySet()) {
        result.
          append('\t').
          append(storedField.getKey()).
          append('=').
          append(storedField.getValue());
      }
    }

    return result.toString();
  }


  /**
   * Container for merged hits.
   */
  public class MergedHits {
    private List<SearchHit> orderedHits;

    private int maxHits;
    private int totalHits;
    private QueryContainer query;

    /**
     * Construct an empty instance.
     */
    public MergedHits() {
      this(null);
    }

    /**
     * Construct an instance with the searchResult's info.
     */
    public MergedHits(SearchResult searchResult) {
      if (searchResult == null) {
        this.orderedHits = null;
        this.maxHits = 0;
        this.totalHits = 0;
        this.query = null;
      }
      else {
        this.orderedHits = new LinkedList<SearchHit>(searchResult.getSearchHits());
        this.maxHits = searchResult.maxHits;
        this.totalHits = searchResult.totalHits;
        this.query = searchResult.queryContainer;
      }
    }


    /**
     * Get the ordered hits.
     */
    public List<SearchHit> getOrderedHits() {
      return orderedHits;
    }

    /**
     * Get the max hits to retrieve.
     */
    public int getMaxHits() {
      return maxHits;
    }

    /**
     * Get the total hits found.
     */
    public int getTotalHits() {
      return totalHits;
    }

    /**
     * Get the query container of the first SearchResult.
     */
    public QueryContainer getQueryContainer() {
      return query;
    }

    /**
     * Merge the other's ordered hits into this instance.
     */
    public void merge(MergedHits other) {
      // initialize
      if (this.orderedHits == null) {
        this.orderedHits = new LinkedList<SearchHit>();
        this.query = other.query;
      }

      // increment
      this.maxHits = Math.max(this.maxHits, other.maxHits);
      this.totalHits += other.totalHits;

      // merge in the other's ordered hits, ignoring duplicates.
      int oldHitIndex = 0;
      SearchHit oldHit = (oldHitIndex < this.orderedHits.size()) ? this.orderedHits.get(oldHitIndex) : null;

      for (Iterator<SearchHit> newIter = other.getOrderedHits().iterator(); newIter.hasNext(); ) {
        final SearchHit newHit = newIter.hasNext() ? newIter.next() : null;
        if (oldHit == null) {
          orderedHits.add(newHit);
        }
        else if (oldHit.docID != newHit.docID) {
          // increment old pointer up through the new's rank
          while (oldHit != null && oldHit.rank <= newHit.rank) {
            ++oldHitIndex;
            oldHit = (oldHitIndex < this.orderedHits.size()) ? this.orderedHits.get(oldHitIndex) : null;
          }

          if (oldHit == null) {
            orderedHits.add(newHit);
          }
          else {
            orderedHits.add(oldHitIndex, newHit);
            ++oldHitIndex;
          }
        }
      }
    }

    /**
     * Get the number of ordered hits.
     */
    public int size() {
      return (orderedHits == null) ? 0 : orderedHits.size();
    }

    /**
     * Show the ordered hits.
     */
    public void showResults() {
      System.out.println("\n" + query + "' yielded " + totalHits + " results, " + size() + " hits:");
      if (orderedHits != null) {
        for (SearchHit hit : orderedHits) {
          System.out.println("\t" + formatHit(hit));
        }
      }
    }
  }


  /**
   * Properties:
   * <ul>
   * <li>storeDir -- lucene store directory</li>
   * <li>field -- field(s) to query. Can query multiple fields if comma-
   *              delimited. The same query is submitted for each field
   *              and results are merged using ranks. This is intended
   *              for experimenting with different analyzers, etc.
   *              to augment results.</li>
   * <li>maxHits -- maximum number of hits to show</li>
   * <li>fieldsClass -- LuceneFields class for analyzing fields, etc.
   * </ul>
   * Arguments (non-property):
   * <ol>
   * <li>query strings</li>
   * </ol>
   */
  public static void main(String[] args) throws IOException {
    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();
    args = pp.getArgs();

    final String storeDirString = properties.getProperty("storeDir");
    final File storeDir = new File(storeDirString);
    final String field = properties.getProperty("field");
    final int maxHits = Integer.parseInt(properties.getProperty("maxHits", "25"));
    final LuceneFields luceneFields = (LuceneFields)ReflectUtil.buildInstance(properties.getProperty("fieldsClass"), properties);

    System.out.println("storeDir=" + storeDir);
    System.out.println("field=" + field);
    System.out.println("maxHits=" + maxHits);

    final FieldQueryRunner fieldQueryRunner = new FieldQueryRunner(storeDir, luceneFields);
    fieldQueryRunner.open();

    for (String queryString : args) {
      System.out.println("\n");
      MergedHits mergedHits = fieldQueryRunner.query(field, queryString, maxHits);
      mergedHits.showResults();
    }

    fieldQueryRunner.close();
  }
}
