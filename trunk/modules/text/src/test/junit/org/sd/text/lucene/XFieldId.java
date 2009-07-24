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


import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import org.apache.lucene.document.Field;

import org.sd.text.IndexingNormalizer;

/**
 * A lucene field ID for testing.
 * <p>
 * @author Spence Koehler
 */
public class XFieldId extends LuceneFieldId {

  public static final FieldAnalyzer FIELD_ANALYZER = new FieldAnalyzer(XFieldId.class);

  private static int nextId = 0;
  private static Map<Integer, LuceneFieldId> id2type = new HashMap<Integer, LuceneFieldId>();
  private static Map<String, LuceneFieldId> label2type = new HashMap<String, LuceneFieldId>();


  /** Key to identify the record */
  public static final XFieldId ID = new XFieldId("id", Field.Store.YES,
                                                 0,  // don't need anything more than common casing
                                                 null,
                                                 false);
  /** Key to identify the record */
  public static final XFieldId KEY = new XFieldId("key", Field.Store.YES,
                                                  0,  // don't need anything more than common casing
                                                  null,
                                                  false);

  /** Core Content -- core content can run together */
  public static final XFieldId TEXT = new XFieldId("text", Field.Store.YES,
                                                   IndexingNormalizer.DEFAULT_INDEXING_OPTIONS,
                                                   DEFAULT_STOPWORDS,
                                                   true);

  /** Title Text -- don't want titles running together */
  public static final XFieldId TITLE = new XFieldId("title", Field.Store.YES,
                                                    IndexingNormalizer.DEFAULT_INDEXING_OPTIONS,
                                                    DEFAULT_STOPWORDS,
                                                    false);

  /** Day Resolution Date */
  public static final XFieldId DATE = new XFieldId("date", Field.Store.YES, Field.Index.NOT_ANALYZED);

  /** Domain -- no stopwords */
  public static final XFieldId DOMAIN = new XFieldId("domain", Field.Store.YES,
                                                     IndexingNormalizer.DEFAULT_WEB_OPTIONS,
                                                     null,
                                                     false);

  /** Url -- no stopwords */
  public static final XFieldId URL = new XFieldId("url", Field.Store.YES,
                                                  IndexingNormalizer.DEFAULT_WEB_OPTIONS,
                                                  null,
                                                  false);

  /** EMail -- no stopwords */
  public static final XFieldId EMAIL = new XFieldId("email", Field.Store.YES,
                                                    IndexingNormalizer.DEFAULT_WEB_OPTIONS,
                                                    null,
                                                    false);


  /** Month Number -- off by one due to representing an index and not a ordinal */
  public static final XFieldId MONTH_NUMBER = new XFieldId("monthNumber", Field.Store.YES, Field.Index.NOT_ANALYZED, true);


  /** Always constructs a unique instance. */
  private XFieldId(String label, Field.Store store, Field.Index index) {
    this(label, store, index, false);
  }

  private XFieldId(String label, Field.Store store, Field.Index index, boolean offByOne) {
    super(label, store, index, null, id2type, label2type, nextId++, offByOne);
  }

  /** Always constructs a unique instance. */
  private XFieldId(String label, Field.Store store, Integer normalizerOptions, Set<String> stopwords, boolean treatMultipleAddsAsContinuous) {
    this(label, store, normalizerOptions, stopwords, treatMultipleAddsAsContinuous, false);
  }

  private XFieldId(String label, Field.Store store, Integer normalizerOptions, Set<String> stopwords, boolean treatMultipleAddsAsContinuous, boolean offByOne) {
    // pre-tokenize with no analyzer, an indexing normalizer
    super(label, store,  Field.Index.NOT_ANALYZED, null, id2type, label2type, nextId++, null,
          normalizerOptions != null ? IndexingNormalizer.getInstance(normalizerOptions) : null,
          stopwords, treatMultipleAddsAsContinuous, offByOne);
  }

  
  /**
   * Get the post field id instance that has the given label.
   */
  public static LuceneFieldId getLuceneFieldId(String label) {
    return label2type.get(label);
  }

  /** Get all enum instances. */
  static LuceneFieldId[] getLuceneFieldIds() {
    Collection<LuceneFieldId> values = id2type.values();
    return values.toArray(new LuceneFieldId[values.size()]);
  }
}
