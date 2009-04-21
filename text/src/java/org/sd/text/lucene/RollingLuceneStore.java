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


import org.sd.util.RollingStore;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;

/**
 * LuceneStore that 'rolls' over at a document limit.
 * <p>
 * @author Spence Koehler
 */
public abstract class RollingLuceneStore extends RollingStore <Document> {

  /**
   * Convenience method to close a lucene index on a separate thread using
   * the rollover utilities.
   */
  public static final void closeLuceneStoreOnThread(LuceneStore luceneStore, boolean optimizeIndexesOnClose, boolean verbose) {

    final StoreWrapper store = new StoreWrapper(luceneStore.getDirPath(), null, 0, 0L, null, luceneStore.getBufferSize(), optimizeIndexesOnClose);
    final CloseStoreThread thread = new CloseStoreThread(store, verbose);
    thread.start();
  }


  /**
   * Get the next available index dir as the last element of the list (and all existing dirs
   * as prior elements).
   */
  protected abstract List<File> nextAvailableIndexDir();

  /**
   * Get the root location of this store's elements
   */
  public abstract File getStoreRoot();


  private String luceneId;
  private int docLimit;
  private long rollTimer;
  private MultiSearcher searchers;
  private Analyzer analyzer;
  private long ramBufferSize;
  private String keyField;
  private boolean optimizeIndexesOnClose;
  private DocumentConverter retrievedToIndexableConverter;

  /**
   * Constructor to be called (as super) by extending classes.
   * <p>
   * NOTE: A concrete class must call super(...), set its variables on which
   *       the implementation of "nextAvailableIndexDir" depend, and then call
   *       "super.initialize" before leaving its constructor!
   */
  protected RollingLuceneStore(String luceneId, int docLimit, long rollTimer, boolean openSearchers,
                               ClosedHook doneClosingHook, Analyzer analyzer, File override, String keyField, long ramBufferSize,
                               boolean optimizeIndexesOnClose, DocumentConverter retrievedToIndexableConverter) {
    this(luceneId, docLimit, rollTimer, openSearchers, doneClosingHook, analyzer, override, keyField, ramBufferSize,
         optimizeIndexesOnClose, false, true, retrievedToIndexableConverter);
  }

  protected RollingLuceneStore(String luceneId, int docLimit, long rollTimer, boolean openSearchers,
                               ClosedHook doneClosingHook, Analyzer analyzer, File override, String keyField, long ramBufferSize,
                               boolean optimizeIndexesOnClose, boolean waitToFinishCloseThreads, boolean openNextElementAutomatically,
                               DocumentConverter retrievedToIndexableConverter) {
    super(doneClosingHook, true/*verbose*/, waitToFinishCloseThreads, openNextElementAutomatically);

    this.luceneId = luceneId;
    this.docLimit = docLimit;
    this.rollTimer = rollTimer;
    this.searchers = openSearchers ? new MultiSearcher(luceneId, SearcherType.TRANSIENT) : null;  //todo: parameterize SearcherType
    this.analyzer = analyzer;
    this.ramBufferSize = (ramBufferSize >= 16777216 ? ramBufferSize : 16777216);
    this.keyField = keyField;
    this.optimizeIndexesOnClose = optimizeIndexesOnClose;
    this.retrievedToIndexableConverter = retrievedToIndexableConverter;
  }

  public String getLuceneId() {
    return luceneId;
  }

  public LuceneStore getLuceneStore() {
    LuceneStore result = null;

    final Store store = super.getStore();
    if (store != null) {
      result = ((RollingLuceneStore.StoreWrapper)store).getLuceneStore();
    }

    return result;
  }

  public MultiSearcher getMultiSearcher() {
    return searchers;
  }

  /**
   * Add the document.
   *
   * @return true if the store was automatically rolled after adding this document; otherwise, false.
   */
  public boolean addDocument(Document doc) throws IOException {
    return super.addElement(doc);
  }


  /**
   * Add all the documents in the store to this index.
   *
   * @return the number of rolls(indicating number of new index shards created) performed from adding the index
   * 
   * @throws IllegalStateException if unable to convert a retrieved document to an indexable document.
   */
  public int addIndex(File store) throws IOException {
    int rolls = 0;

    for (IndexIterator i = new IndexIterator(store); i.hasNext();){
      final Document retrieved = i.next();
      final Document indexable = retrievedToIndexableConverter.convert(retrieved);

      if (indexable == null) {
        throw new IllegalStateException("Can't addIndex w/out postData stored! doc=" + retrieved);
      }
      else {
        if(addDocument(indexable)) rolls++;
      }
    }

    return rolls;
  }

  /**
   * Get the next available store file as the last element of the list (and all
   * existing store files as prior elements in the list).
   */
  protected List<File> nextAvailableFile() {
    return nextAvailableIndexDir();
  }

  /**
   * Build an instance of a store at the given location.
   */
  protected Store<Document> buildStore(File storeFile) {
    return new StoreWrapper(storeFile, analyzer, docLimit, rollTimer, this.keyField, this.ramBufferSize, this.optimizeIndexesOnClose);
  }

  /**
   * Hooked called while (after) opening.
   */
  protected void afterOpenHook(boolean firstTime, List<File> nextAvailable, File justOpenedStore) {
    if (searchers != null) {
      if (searchers.size() == 0 && nextAvailable != null) {  // must be the first open. add everything
        for (File dir : nextAvailable) {
          searchers.addSearcher(dir);

          System.out.println(new Date() + ": *** NOTE: " + luceneId + " opened searcher (" + searchers.size() + ") for index '" + dir + "'.");
        }
      }
      else {  // just add the newly opened index
        searchers.addSearcher(justOpenedStore);

        System.out.println(new Date() + ": *** NOTE: " + luceneId + " opened searcher (" + searchers.size() + ") for index '" + justOpenedStore + "'.");
      }
    }
  }


  public static final class StoreWrapper extends CountingStore<Document> {
    private Analyzer analyzer;
    private LuceneStore luceneStore;
    private boolean optimizeIndexesOnClose;

    public StoreWrapper(File luceneDir, Analyzer analyzer, int docLimit, long rollTimer, String keyField, long ramBufferSize, boolean optimizeIndexesOnClose) {
      super(luceneDir, docLimit, rollTimer);
      this.analyzer = analyzer;
      this.luceneStore = new LuceneStore(luceneDir, keyField);
      if(luceneStore != null) luceneStore.setBufferSize(ramBufferSize);
      this.optimizeIndexesOnClose = optimizeIndexesOnClose;
    }

    public LuceneStore getLuceneStore() {
      return luceneStore;
    }

    public void open() throws IOException {
      luceneStore.open();
      setDocCount(luceneStore.docCount());
    }

    /**
     * Add an element to the current store, returning true to force a rollover;
     * When false is returned, the "shouldRoll" method will be checked for
     * rollover.
     */
    protected boolean doAddElement(Document element) throws IOException {
      if (analyzer == null) {
        luceneStore.addDocument(element);
      }
      else {
        luceneStore.addDocument(element, analyzer);
      }
      return false;
    }

    public void close(boolean closingInThread) throws IOException {
      luceneStore.close(closingInThread && optimizeIndexesOnClose);  // optimize if closingInThread and indicated; otherwise, don't
      super.close(closingInThread);
    }
  }
}
