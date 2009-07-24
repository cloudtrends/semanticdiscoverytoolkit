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


import org.sd.util.ReflectUtil;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

/**
 * Field-based Lucene Analyzer integrated with the LuceneFieldId class.
 * <p>
 * @author Spence Koehler
 */
public class FieldAnalyzer extends Analyzer {
  
  private Map<String, LuceneFieldId> name2inst;  // cache
  private Map<String, Analyzer> name2analyzer;   // cache

  private Class fieldIdClass;
  private Object previousTokenStream;

  public FieldAnalyzer(Class fieldIdClass) {
    super();
    this.fieldIdClass = fieldIdClass;
    this.name2inst = new HashMap<String, LuceneFieldId>();
    this.name2analyzer = new HashMap<String, Analyzer>();
  }

  /**
   * Create a token stream for the field according to the field id class with
   * the reader.
   */
  public TokenStream tokenStream(String fieldName, Reader reader) {
    return getAnalyzer(fieldName).tokenStream(fieldName, reader);
  }

  /**
   * Get the position increment gap for the field according to this Analyzer's
   * field id class.
   */
  public int getPositionIncrementGap(String fieldName) {
    final LuceneFieldId fieldId = getFieldId(fieldName);

    if (fieldId == null) {
      throw new IllegalStateException("Can't interpret fieldName '" + fieldName +
                                      "' using fieldIdClass '" + fieldIdClass);
    }

    return fieldId.treatMultipleAddsAsContinuous() ? 0 : 1;
  }

  /**
   * Get the stored previous token stream.
   */
  protected Object getPreviousTokenStream() {
    return previousTokenStream;
  }

  /**
   * Get the tokenStream for the field.
   */
  public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
    return getAnalyzer(fieldName).reusableTokenStream(fieldName, reader);
  }

  /**
   * Store the given previous token stream.
   */
  protected void setPreviousTokenStream(Object obj) {
    this.previousTokenStream = obj;
  }

  /**
   * Get the fieldId for the field.
   */
  private final LuceneFieldId getFieldId(String fieldName) {
    LuceneFieldId result = name2inst.get(fieldName);

    if (result == null) {
      result = (LuceneFieldId)ReflectUtil.getInstance(fieldIdClass, "getLuceneFieldId", fieldName);
      name2inst.put(fieldName, result);
    }

    return result;
  }

  /**
   * Get the analyzer for the field.
   */
  private final Analyzer getAnalyzer(String fieldName) {
    Analyzer result = name2analyzer.get(fieldName);

    if (result == null) {
      final LuceneFieldId luceneFieldId = getFieldId(fieldName);
      result = luceneFieldId.getAnalyzer();
      if (result == null) result = LuceneFieldId.DEFAULT_ANALYZER;
      name2analyzer.put(fieldName, result);
    }

    return result;
  }
}
