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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * A lucene database.
 * <p>
 * @author Spence Koehler
 */
public class LuceneStore {

//todo: remove this flag. should always be true.
  private static final boolean DISABLE_AUTO_FLUSH = true;


  private final File dirPath;

  private Directory directory;
  private IndexWriter indexWriter;

  protected Analyzer analyzer;
  protected static final Analyzer DEFAULT_ANALYZER = new StandardAnalyzer();

  private long ramBufferSize;                 // flush when ramSizeInBytes() returns value larger than this buffer size
  private String keyField;                      // the field label of the Field to be used in de-duping.  the field label passed must be an untokenized field

  public static final Analyzer getDefaultAnalyzer() {
    return DEFAULT_ANALYZER;
  }

  /**
   * Construct a lucene store.
   *
   * @param dirPath  The path to the lucene store directory.
   */
  public LuceneStore(String dirPath) {
    this(new File(dirPath));
  }

  /**
   * Construct a lucene store.
   *
   * @param dirPath  The path to the lucene store directory.
   */
  public LuceneStore(String dirPath, String keyField) {
    this(new File(dirPath), keyField);
  }

  /**
   * Construct a lucene store.
   *
   * @param dirPath  The path to the lucene store directory.
   */
  public LuceneStore(File dirPath) {
    this(dirPath, null);
  }
  
  /**
   * Construct a lucene store.
   *
   * @param dirPath  The path to the lucene store directory.
   */
  public LuceneStore(File dirPath, String keyField) {
    this.dirPath = dirPath;
    this.keyField = keyField;

    this.ramBufferSize = 16777216;     // 16M
  }
  
  public File getDirPath() {
    return dirPath;
  }

  /**
   * Set this lucene store's analyzer (before opening) to override the default
   * (which is a StopAnalyzer with ENGLISH_STOP_WORDS).
   */
  public void setAnalyzer(Analyzer analyzer) {
    this.analyzer = analyzer;
  }

  /**
   * Get the default stopwords.
   */
  public static final String[] getStopWords() {
    return StopAnalyzer.ENGLISH_STOP_WORDS;
  }

  public void setBufferSize(long size) {
    this.ramBufferSize = size;
  }

  public long getBufferSize() {
    return this.ramBufferSize;
  }

  /**
   * Open this lucene store, creating necessary components if they do not yet
   * exist.
   */
  public void open() throws IOException {
    final boolean create = !dirPath.exists();
    if (this.analyzer == null) this.analyzer = new StopAnalyzer();
    this.directory = FSDirectory.getDirectory(dirPath);
//todo: parameterize MaxFieldLength
    this.indexWriter = new IndexWriter(directory, analyzer, create, IndexWriter.MaxFieldLength.UNLIMITED);
    this.indexWriter.setMergeFactor(1000);
    this.indexWriter.setMaxBufferedDocs(100000);
    // Disable flushing
    if (DISABLE_AUTO_FLUSH) {
      this.indexWriter.setRAMBufferSizeMB(IndexWriter.DISABLE_AUTO_FLUSH);
    }
    else {
      if (ramBufferSize > 0) {
        this.indexWriter.setRAMBufferSizeMB(ramBufferSize);
      }
    }
  }

	/**
	 * Get this instance's indexWriter (null if not opened yet.)
	 */
	public IndexWriter getIndexWriter() {
		return indexWriter;
	}

  /**
   * Close and optimize this lucene store.
   */
  public void close() throws IOException {
    close(true);
  }

  /**
   * Optimize this index, leaving it open.
   */
  public void optimize() throws IOException {
    if (indexWriter != null) {
      indexWriter.commit();
      indexWriter.optimize();
    }
  }

  /**
   * Close this lucene store, optimizing if specified.
   */
  public void close(boolean optimize) throws IOException {
    commit();
    if (indexWriter != null) {
      if (optimize) indexWriter.optimize();
      indexWriter.close();
    }
    if (directory != null) directory.close();
  }

  /**
   * Flush the contents of RAM into the directory
   */
  private void commit() throws IOException {
    if(DISABLE_AUTO_FLUSH && indexWriter != null){
      indexWriter.commit();
    }
  }

  public void addDocument(Document document) throws IOException {
    if(indexWriter.ramSizeInBytes() >= ramBufferSize){
      commit();
    }
    if(this.keyField != null){
      String value = document.get(keyField);
      if(value != null){
        Term key = new Term(keyField, value);
        indexWriter.updateDocument(key, document);
        return;
      }
    }
    indexWriter.addDocument(document);
  }

  public void addDocument(Document document, Analyzer analyzer) throws IOException {
    if(indexWriter.ramSizeInBytes() >= ramBufferSize){
      commit();
    }
    if(this.keyField != null){
      String value = document.get(keyField);
      if(value != null){
        Term key = new Term(keyField, value);
        indexWriter.updateDocument(key, document, analyzer);
        return;
      }
    }
    indexWriter.addDocument(document, analyzer);
  }

  public void addIndex(File index, boolean optimize) throws IOException {
    FSDirectory[] dirs = new FSDirectory[1];
    dirs[0] = FSDirectory.getDirectory(index);
      
    indexWriter.addIndexesNoOptimize(dirs);
		if (optimize) optimize();
  }

  public void addIndexes(File[] indexes, boolean optimize) throws IOException {
    FSDirectory[] dirs = new FSDirectory[indexes.length];

    for (int i = 0; i < indexes.length; ++i) {
      dirs[i] = FSDirectory.getDirectory(indexes[i]);
    }
      
    indexWriter.addIndexesNoOptimize(dirs);
		if (optimize) optimize();
  }

  public int docCount() {
		int result = -1;
		try {
			result =indexWriter.numDocs();
		}
		catch (IOException ignore) {}  // return value of -1 indicates problem

		return result;
  }
}
