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


import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.sd.io.FileUtil;
import org.sd.util.PropertiesParser;
import org.sd.util.ReflectUtil;

/**
 * Utility class to submit a query for a field.
 * <p>
 * @author Spence Koehler
 */
public class FieldQueryRunner {
  
  public static final String MERGED_FIELD_KEY = "__MergedFieldNameKey__";


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
  public MergedHits query(String field, String queryString, int maxHits, boolean matchAll) throws IOException {

    if (field.indexOf(',') >= 0) {
      // handle a comma separated list of fields
      return query(field.split("\\s*,\\s*"), queryString, maxHits, matchAll);
    }

    final SearchResult searchResult =
      luceneFields.getSearchResult(luceneSearcher, field, queryString, maxHits, true/*collectStoredFields*/, matchAll);

    return new MergedHits(searchResult, field);
  }

  /**
   * Query the field(s), merging results by rank.
   */
  public synchronized MergedHits query(String[] fields, String queryString, int maxHits, boolean matchAll) throws IOException {
    final MergedHits result = new MergedHits();

    for (String field : fields) {
      final MergedHits mergedHits = query(field, queryString, maxHits, matchAll);
      result.merge(mergedHits);
    }

    return result;
  }

  /**
   * Format a hit for MergedHits.showResults.
   */
  protected String formatHit(SearchHit searchHit, MergedHits mergedHits, int hitNum) {
    final StringBuilder result = new StringBuilder();

    result.
      append(searchHit.rank).
      append('(').
      append(searchHit.score).
      append(")/").
      append(mergedHits.getHitField(searchHit));

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
    private Set<Integer> docIDs;
    private List<SearchHit> orderedHits;

    private int maxHits;
    private int totalHits;
    private QueryContainer query;

    /**
     * Construct an empty instance.
     */
    public MergedHits() {
      this(null, null);
    }

    /**
     * Construct an instance with the searchResult's info.
     */
    public MergedHits(SearchResult searchResult, String field) {
      this.docIDs = new HashSet<Integer>();
      if (searchResult == null) {
        this.orderedHits = null;
        this.maxHits = 0;
        this.totalHits = 0;
        this.query = null;
      }
      else {
        this.orderedHits = new LinkedList<SearchHit>();
        for (SearchHit searchHit : searchResult.getSearchHits()) {
          if (searchHit.getStoredFields() == null) {
            final Map<String, String> storedFields = new HashMap<String, String>();
            searchHit.setStoredFields(storedFields);
          }
          searchHit.getStoredFields().put(MERGED_FIELD_KEY, field);
          addSearchHit(searchHit, null);
        }
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

    public String getHitField(SearchHit searchHit) {
      return searchHit.getStoredField(MERGED_FIELD_KEY);
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
          addSearchHit(newHit, null);
        }
        else if (oldHit.docID != newHit.docID) {
          // increment old pointer up through the new's rank
          while (oldHit != null && oldHit.rank <= newHit.rank) {
            ++oldHitIndex;
            oldHit = (oldHitIndex < this.orderedHits.size()) ? this.orderedHits.get(oldHitIndex) : null;
          }

          if (oldHit == null) {
            addSearchHit(newHit, null);
          }
          else {
            if (addSearchHit(newHit, oldHitIndex)) {
              ++oldHitIndex;
            }
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
    public void showResults(BufferedWriter writer, boolean includeHeader) throws IOException {
      if (includeHeader) {
        writer.write("\n" + query + "' yielded " + totalHits + " results, " + size() + " hits:\n");
      }
      if (orderedHits != null) {
        int hitNum = 0;
        for (SearchHit hit : orderedHits) {
          if (includeHeader) writer.write("\t");
          writer.write("" + formatHit(hit, this, hitNum++) + "\n");
        }
      }
    }

    private final boolean addSearchHit(SearchHit searchHit, Integer position) {
      boolean result = false;

      if (!docIDs.contains(searchHit.docID)) {
        docIDs.add(searchHit.docID);
        if (position == null) {
          this.orderedHits.add(searchHit);
        }
        else {
          this.orderedHits.add(position, searchHit);
        }
        result = true;
      }

      return result;
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
   * <li>matchAll -- (optional, default=false) true or false.
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
    final boolean matchAll = "true".equalsIgnoreCase(properties.getProperty("matchAll", "false"));

    System.out.println("storeDir=" + storeDir);
    System.out.println("field=" + field);
    System.out.println("maxHits=" + maxHits);
    System.out.println("matchAll=" + matchAll);

    final FieldQueryRunner fieldQueryRunner = new FieldQueryRunner(storeDir, luceneFields);
    fieldQueryRunner.open();

    for (String queryString : args) {
      System.out.println("\n");
      MergedHits mergedHits = fieldQueryRunner.query(field, queryString, maxHits, matchAll);
      mergedHits.showResults(FileUtil.getWriter(System.out), true);
    }

    fieldQueryRunner.close();
  }
}
