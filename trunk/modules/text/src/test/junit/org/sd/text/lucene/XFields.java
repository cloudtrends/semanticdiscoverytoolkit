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


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.sd.text.IndexingNormalizer;

/**
 * A lucene field ID for testing.
 * <p>
 * @author Spence Koehler
 */
public class XFields extends LuceneFields {

  public static final String ID = "id";
  public static final String KEY = "key";
  public static final String TEXT = "text";
  public static final String TITLE = "title";
  public static final String DATE = "date";
  public static final String DOMAIN = "domain";
  public static final String URL = "url";
  public static final String EMAIL = "email";
  public static final String MONTH_NUMBER = "monthNumber";

  private static XFields INSTANCE = null;
  private static final Object INSTANCE_MUTEX = new Object();

  public static XFields getInstance() {
    synchronized (INSTANCE_MUTEX) {
      if (INSTANCE == null) {
        INSTANCE = (XFields)new XFields().
          add(ID, Field.Store.YES, 0/*just common casing*/, null, false).
          add(KEY, Field.Store.YES, 0/*just common casing*/, null, false).
          add(TEXT, Field.Store.YES, IndexingNormalizer.DEFAULT_INDEXING_OPTIONS, LuceneUtils.DEFAULT_STOPWORDS, true).
          add(TITLE, Field.Store.YES, IndexingNormalizer.DEFAULT_INDEXING_OPTIONS, LuceneUtils.DEFAULT_STOPWORDS, false).
          add(DATE, Field.Store.YES, Field.Index.NOT_ANALYZED).
          add(DOMAIN, Field.Store.YES, IndexingNormalizer.DEFAULT_WEB_OPTIONS, null, false).
          add(URL, Field.Store.YES, IndexingNormalizer.DEFAULT_WEB_OPTIONS, null, false).
          add(EMAIL, Field.Store.YES, IndexingNormalizer.DEFAULT_WEB_OPTIONS, null, false).
          add(MONTH_NUMBER, Field.Store.YES, Field.Index.NOT_ANALYZED, true);
      }
    }
    return INSTANCE;
  }
  
  public static FieldInfo getField(String fieldName) {
    return getInstance().get(fieldName);
  }

  // enforce singleton pattern
  private XFields() {
    super();
  }

  // enforce singleton pattern
  private XFields(Analyzer analyzer) {
    super(analyzer);
  }
}
