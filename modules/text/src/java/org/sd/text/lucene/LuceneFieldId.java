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


import org.sd.cio.MessageHelper;
import org.sd.io.Publishable;
import org.sd.nlp.NormalizedString;
import org.sd.nlp.Normalizer;
import org.sd.text.DetailedUrl;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;

/**
 * Base class for identifying lucene fields.
 * <p>
 * The purpose of this class is to guarantee that indexing and querying
 * are consistent for fields in a lucene document. It further provides
 * the ability to analyze each field individually when indexing.
 * <p>
 * Fields that are tokenized by lucene (i.e. index == Field.Index.ANALYZED)
 * use the analyzer given for that field while fields that are not tokenized
 * by lucene use the normalizer along with NormalizedString.split(stopwords)
 * to pre-tokenize field data.
 * <p>
 * Extending classes should create a static constant FieldAnalyzer instance
 * for their class if different fields require different analyzers. This
 * fieldAnalyzer should then be referenced by the addDocument(doc, analyzer)
 * call. This will enable each field to use its own analyzer.
 * <p>
 * Fields that may hold asian and/or non-asian data should have
 * index = Field.Index.NOT_ANALYZED and normalizer = an appropriate
 * Normalizer instance. Often an IndexingNormalizer will work well.
 * <p>
 * Still left TODO in the future: apply language-specific stopwords to
 * fields.
 *
 * @author Spence Koehler
 */
public abstract class LuceneFieldId {

  /**
   * A minimal set of english stopwords.
   */
//  private static final String[] DEFAULT_STOPWORD_STRINGS = new String[] {"an", "the", "to"};
  private static final String[] DEFAULT_STOPWORD_STRINGS = StopAnalyzer.ENGLISH_STOP_WORDS;

  /**
   * A minimal set of english stopwords: an, the, to
   */
  public static final Set<String> DEFAULT_STOPWORDS = new HashSet<String>();
  static {
    for (String stopword : DEFAULT_STOPWORD_STRINGS) {
      DEFAULT_STOPWORDS.add(stopword);
    }
  }

  protected static final Analyzer DEFAULT_ANALYZER = new SdAnalyzer(null, DEFAULT_STOPWORDS, true);

  public static final Analyzer getDefaultAnalyzer() {
    return DEFAULT_ANALYZER;
  }


  private int id;
  private String label;
  public final Field.Store store;
  public final Field.Index index;
  public final Field.TermVector termVector;

  private Analyzer analyzer;
  private Normalizer normalizer;
  private Set<String> stopwords;
  private boolean treatMultipleAddsAsContinuous;
  private boolean offByOne;

  /** Always constructs a unique instance. */
  protected LuceneFieldId(String label, Field.Store store, Field.Index index,
                          Field.TermVector termVector,
                          Map<Integer, LuceneFieldId> id2type,
                          Map<String, LuceneFieldId> label2type,
                          int nextId, boolean offByOne) {
    this(label, store, index, termVector, id2type, label2type, nextId, null, null, null, true, offByOne);
  }

  /** Always constructs a unique instance. */
  protected LuceneFieldId(String label, Field.Store store, Field.Index index,
                          Field.TermVector termVector,
                          Map<Integer, LuceneFieldId> id2type,
                          Map<String, LuceneFieldId> label2type,
                          int nextId, Analyzer analyzer,
                          Normalizer normalizer, Set<String> stopwords,
                          boolean treatMultipleAddsAsContinuous, boolean offByOne) {
    this.id = nextId;
    this.label = label;
    this.store = store;
    this.index = index;
    this.termVector = termVector;
    this.normalizer = normalizer;
    this.stopwords = stopwords;
    this.treatMultipleAddsAsContinuous = treatMultipleAddsAsContinuous;
    this.offByOne = offByOne;

    this.analyzer = computeAnalyzer(index, analyzer, normalizer, stopwords, treatMultipleAddsAsContinuous);

    id2type.put(id, this);
    label2type.put(label, this);
  }

  private static final Analyzer computeAnalyzer(Field.Index index, Analyzer analyzer,
                                                Normalizer normalizer, Set<String> stopwords,
                                                boolean treatMultipleAddsAsContinuous) {
    Analyzer result = analyzer;

    if (index == Field.Index.ANALYZED) {
      if (analyzer == null) {
        result = new SdAnalyzer(normalizer, stopwords, treatMultipleAddsAsContinuous);
//        result = LuceneStore.getDefaultAnalyzer();
      }
    }
    else {
      result = null;
    }

    return result;
  }

  public String getLabel() {
    return label;
  }

  public int getId() {
    return id;
  }

  public boolean isOffByOne() {
    return offByOne;
  }

  /** Compute a hashCode for this compareType. */
  public int hashCode() {
    return getId();
  }

  /** Determine equality. */
  public boolean equals(Object other) {
    return (this == other);
  }

  /**
   * Get a string describing this compareType.
   */
  public String toString() {
    return label;
  }

  /**
   * Set the analyzer for this field id.
   * <p>
   * Note that the analyzer will be ignored if index != Field.Index.ANALYZED
   * and that a default analyzer will be computed if index == Field.Index.ANALYZED and analyzer == null.
   */
  public void setAnalyzer(Analyzer analyzer) {
    this.analyzer = computeAnalyzer(index, analyzer, normalizer, stopwords, treatMultipleAddsAsContinuous);
  }

  /**
   * Get this field id's analyzer.
   * <p>
   * The analyzer will be null unless index == Field.Index.ANALYZED, in which
   * case it will be non-null, defaulting to LuceneStore.getDefaultAnalyzer() if
   * it has never been set for this instance.
   */
  public Analyzer getAnalyzer() {
    return this.analyzer;
  }

  /**
   * Get this instance's normalizer.
   */
  public Normalizer getNormalizer() {
    return this.normalizer;
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
   * Apply this field's normalizer to the string.
   */
  public NormalizedString normalize(String string) {
    NormalizedString result = null;

    if (normalizer != null && string != null && !"".equals(string)) {
      result = normalizer.normalize(string);
    }

    return result;
  }

  /**
   * Use this field's normalizer to tokenize the string, or just
   * return the string itself lowercased with stopwords filtered
   * (as a single element in the result).
   */
  public String[] tokenize(String string) {
    String[] result = null;

    if (string != null && !"".equals(string)) {
      final NormalizedString nString = normalize(string);
      if (nString != null) {
        result = nString.split(stopwords);
      }
      else {
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
   */
  public Query getQuery(String value, Collection<String> termCollector) {
    Query result = null;

    String termValue = value;
    if(isOffByOne()){
      termValue = String.valueOf(Integer.parseInt(termValue) - 1);
    }

    if (index == Field.Index.ANALYZED || normalizer == null) {
      result = LuceneUtils.toQuery(getAnalyzer(), label, termValue, termCollector);
    }
    else {
      result = LuceneUtils.tokensToQuery(label, tokenize(termValue), termCollector);
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

    String[] termValues = values;
    if (values != null){
      if(isOffByOne()){
        for(int i = 0; i < values.length; i++){
          termValues[i] = String.valueOf(Integer.parseInt(values[i]) - 1);
        }
      }
    }

    if (index == Field.Index.ANALYZED || normalizer == null) {
      result = LuceneUtils.toQuery(getAnalyzer(), label, termValues, termCollector);
    }
    else {
      if (termValues != null && termValues.length > 0) {
        if (termValues.length == 1) {
          result = LuceneUtils.tokensToQuery(label, tokenize(termValues[0]), termCollector);
        }
        else {
          final PhraseQuery phraseQuery = new PhraseQuery();
          for (String value : termValues) {
            final String[] tokens = tokenize(value);
            if (tokens != null && tokens.length > 0) {
              for (String token : tokens) {
                phraseQuery.add(new Term(label, token));
                if (termCollector != null) termCollector.add(token);
              }
            }
          }
          result = phraseQuery;
        }
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
      document.add(new Field(label, bytes, store));
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
      result = new Field(label, data, store, index);
    }
    else {
      result = new Field(label, data, store, index, termVector);
    }

    return result;
  }
}
