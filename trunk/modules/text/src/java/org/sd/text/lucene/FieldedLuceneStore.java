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
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.lucene.document.Document;


/**
 * Container for a lucene store and its lucene fields instance.
 * <p>
 * Extenders would typically implement methods to iterate through data
 * and/or load documents into the store through the asDocument and addDocument
 * methods.
 *
 * @author Spence Koehler
 */
public abstract class FieldedLuceneStore {

  protected final LuceneFields luceneFields;
  protected final LuceneStore luceneStore;
  private AtomicBoolean opened = new AtomicBoolean(false);

  public FieldedLuceneStore(LuceneFields luceneFields, File storeDir) {
    this.luceneFields = luceneFields;
    this.luceneStore = luceneFields.getLuceneStore(storeDir);
  }

  public void open() throws IOException {
    synchronized (luceneStore) {
      luceneStore.open();
      opened.set(true);
    }
  }

  public void close() throws IOException {
    if (opened.compareAndSet(true, false)) {
      System.err.println(new Date() + ": closing and optimizing store");
      luceneStore.close(true);
      System.err.println(new Date() + ": store closed and optimized.");
    }
  }

  public void addDocument(Document document) throws IOException {
    if (document != null) {
      luceneStore.addDocument(document);
    }
  }

  /**
   * Create a document from the args.
   * <p>
   * Typical implementations would be of the form:
   * <pre>
   * public Document asDocument(String fieldValue1, String fieldValue2, ...) {
   *   boolean result = false;
   *   final Document document = new Document();
   *
   *   //...
   *   result |= luceneFields.get(XFields.FIELD-I).addField(document, fieldValueI)
   *   //...
   *
   *   return result ? document : null;
   * }
   * </pre>
   *
   * @return a non-empty document or null if no fields were added to the document.
   */
  protected abstract Document asDocument(String... fieldValues);
}
