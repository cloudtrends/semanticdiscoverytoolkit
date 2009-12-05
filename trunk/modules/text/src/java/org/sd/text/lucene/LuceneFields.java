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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.sd.cio.MessageHelper;
import org.sd.io.Publishable;
import org.sd.nlp.NormalizedString;
import org.sd.nlp.Normalizer;
import org.sd.text.DetailedUrl;
import org.sd.text.IndexingNormalizer;

/**
 * Container for the lucene fields for a store.
 * <p>
 * @author Spence Koehler
 */
public class LuceneFields {
  
  public static final Analyzer DEFAULT_ANALYZER =
    new SdAnalyzer(IndexingNormalizer.getInstance(IndexingNormalizer.DEFAULT_INDEXING_OPTIONS),
                   LuceneUtils.DEFAULT_STOPWORDS,
                   true);


  private PerFieldAnalyzerWrapper analyzer;
  private Analyzer defaultAnalyzer;
  private Map<String, FieldInfo> name2info;
  private String keyField;  // optional NOT_ANALYZED field to use for deduping
  private List<FieldInfo> storedFields;

  /**
   * Construct with the default analyzer for all fields.
   */
  public LuceneFields() {
    this(DEFAULT_ANALYZER);
  }

  /**
   * Construct with the given analyzer as the default for all fields.
   */
  public LuceneFields(Analyzer defaultAnalyzer) {
    this.analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer);
    this.defaultAnalyzer = defaultAnalyzer;
    this.name2info = new HashMap<String, FieldInfo>();
    this.storedFields = null;
  }

  /**
   * Get the analyzer for all fields.
   */
  public PerFieldAnalyzerWrapper getAnalyzer() {
    return analyzer;
  }

  /**
   * Get the default analyzer for fields.
   */
  public Analyzer getDefaultAnalyzer() {
    return defaultAnalyzer;
  }

  
  /**
   * Optionally set a (NOT_ANALYZED) key field to use for deduping in the
   * LuceneStore.
   */
  public LuceneFields setKeyField(String keyField) {
    this.keyField = keyField;
    return this;
  }

  /**
   * Get a new (unopened) LuceneStore configured with these fields.
   */
  public LuceneStore getLuceneStore(File storeDir) {
    final LuceneStore luceneStore = new LuceneStore(storeDir, keyField);
    luceneStore.setAnalyzer(analyzer);
    return luceneStore;
  }

  /**
   * Get a query container with the query for the field.
   *
   * @return the QueryContainer or null if the field is unknown.
   */
  public QueryContainer getQueryContainer(String field, String query) {
    QueryContainer result = null;

    final FieldInfo fieldInfo = get(field);
    if (fieldInfo != null) {
      result = new QueryContainer(fieldInfo.getQuery(query, null), query);
    }

    return result;
  }

  public SearchResult getSearchResult(LuceneSearcher searcher, String field,
                                      String query, int maxHits,
                                      boolean collectStoredFields) throws IOException {
    return getSearchResult(searcher, getQueryContainer(field, query),
                           maxHits, collectStoredFields);
  }

  public SearchResult getSearchResult(LuceneSearcher searcher,
                                      QueryContainer queryContainer, int maxHits,
                                      boolean collectStoredFields) throws IOException {

    if (searcher == null || queryContainer == null) return null;

    final SearchResult result =
      searcher.search(queryContainer, maxHits, collectStoredFields ? this : null);

    return result;
  }

  /**
   * Get the fields that are stored.
   *
   * @return the non-empty stored fields list or null.
   */
  public List<FieldInfo> getStoredFields() {
    return storedFields;
  }

  /**
   * Extract the stored field information from the document, mapping the field
   * name to its document value.
   *
   * @return the storedFields or null if there are no stored fields or doc is null.
   */
  public Map<String, String> getStoredFields(Document doc) {
    if (storedFields == null || doc == null) return null;

    final Map<String, String> result = new LinkedHashMap<String, String>();

    for (FieldInfo fieldInfo : storedFields) {
      result.put(fieldInfo.name, doc.get(fieldInfo.name));
    }

    return result;
  }

  /**
   * Convenience method to apply a field's tokenization to a string.
   */
  public String[] tokenize(String field, String string) {
    String[] result = null;

    final FieldInfo fieldInfo = get(field);
    if (fieldInfo != null) {
      result = fieldInfo.tokenize(string);
    }

    return result;
  }

  /**
   * Get the FieldInfo instance with the given field name.
   */
  public FieldInfo get(String fieldname) {
    return name2info.get(fieldname);
  }

  /**
   * Add a field with the given parameters.
   */
  public LuceneFields add(String name, Field.Store store, Field.Index index) {
    return add(name, store, index, null);
  }

  /**
   * Add a field with the given parameters.
   */
  public LuceneFields add(String name, Field.Store store, Analyzer analyzer) {
    return add(name, store, Field.Index.ANALYZED, null, analyzer, null, null, true, false);
  }

  /**
   * Add a field with the given parameters.
   */
  public LuceneFields add(String name, Field.Store store, Field.Index index, boolean offByOne) {
    return add(name, store, index, null, null, null, null, true, offByOne);
  }

  /**
   * Add a field with the given parameters.
   */
  public LuceneFields add(String name, Field.Store store, Field.Index index, Field.TermVector termVector) {
    return add(new FieldInfo(name, store, index, termVector, null, null, null, true, false));
  }

  /**
   * Add a field with the given parameters.
   */
  public LuceneFields add(String name, Field.Store store, int normalizerOptions,
                          Set<String> stopwords, boolean treatMultipleAddsAsContinuous) {
    return add(name, store, normalizerOptions, stopwords, treatMultipleAddsAsContinuous, false);
  }

  /**
   * Add a field with the given parameters.
   */
  public LuceneFields add(String name, Field.Store store, int normalizerOptions,
                          Set<String> stopwords, boolean treatMultipleAddsAsContinuous,
                          boolean offByOne) {
    return add(new FieldInfo(name, store, Field.Index.ANALYZED, null,
                             null, IndexingNormalizer.getInstance(normalizerOptions),
                             stopwords, treatMultipleAddsAsContinuous, offByOne));
  }

  /**
   * Add a field with the given parameters.
   */
  public LuceneFields add(String name, Field.Store store, Field.Index index,
                          Field.TermVector termVector, Analyzer analyzer,
                          Normalizer normalizer, Set<String> stopwords,
                          boolean treatMultipleAddsAsContinuous, boolean offByOne) {
    return add(new FieldInfo(name, store, index, termVector, analyzer, normalizer,
                             stopwords, treatMultipleAddsAsContinuous, offByOne));
  }

  public LuceneFields add(FieldInfo fieldInfo) {
    // add field's analyzer
    if (fieldInfo.index != Field.Index.NO) {  // if indexed
      final Analyzer fieldAnalyzer = fieldInfo.getAnalyzer();
      if (fieldAnalyzer != defaultAnalyzer) {  // and non-default
        analyzer.addAnalyzer(fieldInfo.name, fieldAnalyzer);
      }
    }

    // add mapping
    name2info.put(fieldInfo.name, fieldInfo);

    // add stored field
    if (fieldInfo.store != Field.Store.NO) {  // if stored
      if (storedFields == null) storedFields = new ArrayList<FieldInfo>();
      storedFields.add(fieldInfo);
    }

    return this;
  }


  public final class FieldInfo {
    public final String name;  // field's name
    public final Field.Store store;
    public final Field.Index index;
    public final Field.TermVector termVector;

    private Normalizer normalizer;
    private Analyzer analyzer;
    private Set<String> stopwords;
    private boolean treatMultipleAddsAsContinuous;
    private boolean offByOne;

    /**
     * Construct with the given parameters.
     */
    FieldInfo(String name, Field.Store store, Field.Index index,
                     Field.TermVector termVector, Analyzer analyzer,
                     Normalizer normalizer, Set<String> stopwords,
                     boolean treatMultipleAddsAsContinuous, boolean offByOne) {
      this.name = name;
      this.store = store;
      this.index = index;
      this.termVector = termVector;
      this.normalizer = normalizer;
      this.stopwords = stopwords;
      this.treatMultipleAddsAsContinuous = treatMultipleAddsAsContinuous;
      this.offByOne = offByOne;

      this.analyzer = computeAnalyzer(index, analyzer, normalizer, stopwords, treatMultipleAddsAsContinuous);
    }

    private final Analyzer computeAnalyzer(Field.Index index, Analyzer analyzer,
                                           Normalizer normalizer, Set<String> stopwords,
                                           boolean treatMultipleAddsAsContinuous) {
      Analyzer result = analyzer;

      if (index == Field.Index.NO || index == Field.Index.NOT_ANALYZED) {  // field not indexed
        result = null;  // so no analyzer
      }
      else {
        if (result == null) {  // no analyzer specified
          if (normalizer != null) {  // build analyzer from given normalizer
            result = new SdAnalyzer(normalizer, stopwords, treatMultipleAddsAsContinuous);
          }
          else {  // analyzer and normalizer are null ==> use defaultAnalyzer
            result = defaultAnalyzer;
          }
        }
      }

      return result;
    }

    /**
     * Get this field's normalizer.
     */
    public Normalizer getNormalizer() {
      return normalizer;
    }

    /**
     * Get this field's analyzer.
     */
    public Analyzer getAnalyzer() {
      return analyzer;
    }

    /**
     * Get stopwords for this field.
     */
    public Set<String> getStopwords() {
      return stopwords;
    }

    /**
     * Determine whether to treat multiple adds for this field as continuous.
     * <p>
     * If true, then queries that search for terms that "wrap" from one add to
     * the next will succeed; if false, then queries for terms that "wrap" from
     * one add to the next will fail.
     * <p>
     * Default is to return true.
     */
    public boolean treatMultipleAddsAsContinuous() {
      return treatMultipleAddsAsContinuous;
    }

    /**
     * Determine whether query values for this field are off by one (greater)
     * when compared to indexed values. For example, if numeric month values
     * from 0 to 11 are indexed while queries will have 1 through 12, this
     * would be true.
     */
    public boolean isOffByOne() {
      return offByOne;
    }

    /**
     * Apply this field's normalizer to the string.
     *
     * @return the normalized string or null if there is no normalizer.
     */
    public NormalizedString normalize(String string) {
      NormalizedString result = null;

      if (normalizer != null && string != null && !"".equals(string)) {
        result = normalizer.normalize(string);
      }

      return result;
    }

    /**
     * Use this field's analyzer (or normalizer if analyzer is null) to
     * tokenize the string, falling back to the lowercase string as a single
     * token filtered by stopwords.
     *
     * @return the tokens or null if unable to tokenize.
     */
    public String[] tokenize(String string) {
      String[] result = null;

      if (string != null && !"".equals(string)) {
        if (analyzer != null) {
          final List<String> tokens = LuceneUtils.getTokenTexts(analyzer, name, string);
          if (tokens != null && tokens.size() > 0) {
            result = tokens.toArray(new String[tokens.size()]);
          }
        }

        if (result == null && normalizer != null) {
          final NormalizedString nString = normalize(string);
          if (nString != null) {
            result = nString.split(stopwords);
          }
        }

        if (result == null) {
          final String norm = string.toLowerCase();
          if (stopwords == null || !stopwords.contains(norm)) {
            result = new String[]{norm};
          }
        }
      }

      return result;
    }

    ////////
    //
    //  Utility methods for creating queries for a field
    //

    /**
     * Get a query for this field having the given value.
     * <p>
     * If termCollector is non-null, collect query terms in it, discarding "not" terms.
     */
    public Query getQuery(String value, Collection<String> termCollector) {
      Query result = null;

      String termValue = value;
      if(isOffByOne()){
        termValue = String.valueOf(Integer.parseInt(termValue) - 1);
      }

      if (analyzer != null) {
        result = LuceneUtils.toQuery(analyzer, name, termValue, termCollector);
      }
      else {
        final String[] tokens = tokenize(termValue);
        if (tokens != null) {
          result = LuceneUtils.tokensToQuery(name, tokens, termCollector);
        }
      }

      return result;
    }

    /**
     * Get a query for this field having the given values.
     * <p>
     * If termCollector is non-null, collect query terms in it, discarding "not" terms.
     */
    public Query getQuery(String[] values, Collection<String> termCollector) {
      Query result = null;

      if (values == null || values.length == 0) return result;

      String[] termValues = values;
      if (isOffByOne()) {
        for(int i = 0; i < values.length; i++) {
          termValues[i] = String.valueOf(Integer.parseInt(values[i]) - 1);
        }
      }

      if (analyzer != null) {
        result = LuceneUtils.toQuery(analyzer, name, termValues, termCollector);
      }
      else {
        if (termValues.length == 1) {
          result = LuceneUtils.tokensToQuery(name, tokenize(termValues[0]), termCollector);
        }
        else {
          final PhraseQuery phraseQuery = new PhraseQuery();
          for (String value : termValues) {
            final String[] tokens = tokenize(value);
            if (tokens != null && tokens.length > 0) {
              for (String token : tokens) {
                phraseQuery.add(new Term(name, token));
                if (termCollector != null) termCollector.add(token);
              }
            }
          }
          result = phraseQuery;
        }
      }

      return result;
    }


    ////////
    //
    //  Utility methods for creating indexes for a field
    //

    /**
     * Add the integer (as a string) to the document for this field.
     */
    public boolean addField(Document document, Integer data) {
      boolean result = false;

      if (data != null) {
        result = true;
        document.add(makeField(data.toString()));
      }

      return result;
    }

    /**
     * Add the bytes as a binary value for this field to the document.
     */
    public boolean addField(Document document, byte[] bytes) {
      boolean result = false;

      if (bytes != null) {
        result = true;
        document.add(new Field(name, bytes, store));
      }

      return result;
    }

    /**
     * Add the publishable (as bytes) value for this field to the document.
     * <p>
     * Note that the binary byte[] data retrieved from the document for
     * this field can be restored as a publishable using
     * MessageHelper.deserialize(byte[] bytes)
     */
    public boolean addField(Document document, Publishable publishable) {
      boolean result = false;

      if (publishable != null) {
        byte[] bytes = null;
        try {
          bytes = MessageHelper.serialize(publishable);
        }
        catch(IOException e) {
          throw new IllegalStateException(e);
        }
        result = addField(document, bytes);
      }

      return result;
    }

    /**
     * Add the date to the document for this field at a day resolution.
     */
    public boolean addDayResolutionField(Document document, Date date) {
      boolean result = false;

      if (date != null) {
        result = true;
        document.add(makeField(LuceneDateUtil.asDay(date)));
      }

      return result;
    }

    /**
     * Add the date to the document for this field at a year resolution.
     */
    public boolean addYearResolutionField(Document document, Date date) {
      boolean result = false;

      if (date != null) {
        result = true;
        document.add(makeField(LuceneDateUtil.asYear(date)));
      }

      return result;
    }

    /**
     * Add the 'substantive' domain to the document for this field.
     */
    public boolean addDomainField(Document document, String url) {
      return (url == null || "".equals(url)) ? false : addDomainField(document, new DetailedUrl(url));
    }

    /**
     * Add the 'substantive' domain to the document for this field.
     */
    public boolean addDomainField(Document document, DetailedUrl dUrl) {
      boolean result = false;

      if (dUrl != null) {
        final String indexableDomain = dUrl.splitHostExtensions(false)[0];  // not normalized so we key off camel casing
        result = addField(document, indexableDomain);
      }

      return result;
    }

    /**
     * Add the url's path to the document for this field.
     */
    public boolean addPathField(Document document, DetailedUrl dUrl, boolean withoutTargetOrExtension) {
      boolean result = false;

      if (dUrl != null) {
        final String path = dUrl.getPath(withoutTargetOrExtension);
        result = addField(document, path);
      }

      return result;
    }

    /**
     * Add the url's target to the document for this field.
     */
    public boolean addTargetField(Document document, DetailedUrl dUrl, boolean withTargetExtension) {
      boolean result = false;

      if (dUrl != null) {
        final String target = dUrl.getTarget(withTargetExtension);
        result = addField(document, target);
      }

      return result;
    }

    /**
     * Add the url's targetExtension to the document for this field.
     */
    public boolean addTargetExtensionField(Document document, DetailedUrl dUrl) {
      boolean result = false;

      if (dUrl != null) {
        final String targetExtension = dUrl.getTargetExtension(true);
        result = addField(document, targetExtension);
      }

      return result;
    }

    /**
     * Add the url's query to the document for this field.
     */
    public boolean addQueryField(Document document, DetailedUrl dUrl) {
      boolean result = false;

      if (dUrl != null) {
        final String query = dUrl.getQuery();
        result = addField(document, query);
      }

      return result;
    }

    /**
     * Add the full url to the document for this field.
     */
    public boolean addUrlField(Document document, String url, boolean asSingleToken) {
      return (url == null || "".equals(url)) ? false : addUrlField(document, new DetailedUrl(url), asSingleToken);
    }

    /**
     * Add the full url to the document for this field.
     */
    public boolean addUrlField(Document document, DetailedUrl dUrl, boolean asSingleToken) {
      boolean result = false;

      if (dUrl != null) {
        // normalized if asSingleToken; otherwise, not normalized so we can key off camel casing
        final String nUrl = asSingleToken ? dUrl.getNormalizedUrl() : dUrl.getOriginal();
        result = addField(document, nUrl);
      }

      return result;
    }

    /**
     * Add field data to the document if non-null and non-empty.
     */
    public final boolean addField(Document document, String[] data) {
      boolean result = false;

      if (data != null && data.length > 0) {
        // add each string.
        for (String piece : data) {
          result |= addField(document, piece);
        }
      }

      return result;
    }

    /**
     * Add field data to the document if non-null and non-empty.
     */
    public final boolean addField(Document document, Collection<String> data) {
      boolean result = false;

      if (data != null && data.size() > 0) {
        // add each string.
        for (String piece : data) {
          result |= addField(document, piece);
        }
      }

      return result;
    }

    /**
     * Add field data to the document if non-null and non-empty.
     */
    public final boolean addField(Document document, String data) {
      if (data == null || "".equals(data)) return false;

      boolean result = false;

      if (index == Field.Index.ANALYZED) {
        // add data to be tokenized by lucene
        document.add(makeField(data));
        result = true;
      }
      else {
        // tokenize and add 'outside' lucene
        final String[] tokens = tokenize(data);
        if (tokens != null && tokens.length > 0) {
          for (String token : tokens) {
            if (token != null && token.length() > 0) {
              result = true;
              document.add(makeField(token));
            }
          }
        }
      }

      return result;
    }

    /**
     * Instantiate a field instance with this field id's params and the
     * given data.
     */
    private final Field makeField(String data) {
      Field result = null;

      if (termVector == null) {
        result = new Field(name, data, store, index);
      }
      else {
        result = new Field(name, data, store, index, termVector);
      }

      return result;
    }
  }
}
