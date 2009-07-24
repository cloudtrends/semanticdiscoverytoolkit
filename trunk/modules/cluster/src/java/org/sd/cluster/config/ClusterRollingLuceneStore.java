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
package org.sd.cluster.config;


import org.sd.text.lucene.DocumentConverter;
import org.sd.text.lucene.RollingLuceneStore;

import java.io.File;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;

/**
 * A RollingLuceneStore in the cluster environment.
 * <p>
 * @author Spence Koehler
 */
public class ClusterRollingLuceneStore extends RollingLuceneStore {

  private Config config;
  private String jobId;
  private String dataDirName;
  private String dataDirParent;
  private String indexId;

  public ClusterRollingLuceneStore(String luceneId, int docLimit, long rollTimer, boolean openSearchers, Config config,
                                   String jobId, String dataDirName, String indexId,
                                   RollingLuceneStore.ClosedHook doneClosingHook,
                                   Analyzer analyzer, File override, boolean optimizeIndexesOnClose,
                                   DocumentConverter retrievedToIndexableConverter) {
    this(luceneId, docLimit, rollTimer, openSearchers, config, jobId, null, dataDirName, indexId, 
         doneClosingHook, analyzer, override, null, 0L, optimizeIndexesOnClose,
         retrievedToIndexableConverter);
  }

  public ClusterRollingLuceneStore(String luceneId, int docLimit, long rollTimer, boolean openSearchers, Config config,
                                   String jobId, String dataDirParent, String dataDirName, String indexId,
                                   RollingLuceneStore.ClosedHook doneClosingHook,
                                   Analyzer analyzer, File override, String keyField,
                                   long ramBufferSize, boolean optimizeIndexesOnClose,
                                   DocumentConverter retrievedToIndexableConverter) {
    this(luceneId, docLimit, rollTimer, openSearchers, config, jobId, dataDirParent, dataDirName, indexId, 
         doneClosingHook, analyzer, override, keyField, ramBufferSize, optimizeIndexesOnClose, 
         false/* do not wait to finish ClosedHook before creating new store element */,
         true/* open next store elements automatically when docLimit is reached */,
         retrievedToIndexableConverter);
  }

  public ClusterRollingLuceneStore(String luceneId, int docLimit, long rollTimer, boolean openSearchers, Config config,
                                   String jobId, String dataDirParent, String dataDirName, String indexId,
                                   RollingLuceneStore.ClosedHook doneClosingHook,
                                   Analyzer analyzer, File override, String keyField,
                                   long ramBufferSize, boolean optimizeIndexesOnClose,
                                   boolean waitToFinishCloseThreads, boolean openNextElementAutomatically,
                                   DocumentConverter retrievedToIndexableConverter) {
    super(luceneId, docLimit, rollTimer, openSearchers, doneClosingHook, analyzer, override, keyField, 
          ramBufferSize, optimizeIndexesOnClose, waitToFinishCloseThreads, openNextElementAutomatically,
          retrievedToIndexableConverter);

    this.config = config;
    this.jobId = jobId;
    this.dataDirName = dataDirName;
    this.dataDirParent = dataDirParent;
    this.indexId = indexId;
    
    super.initialize(override);
  }

  /**
   * Get the next available index dir.
   */
  protected List<File> nextAvailableIndexDir() {
    if(this.dataDirParent == null) return config.nextAvailableIndexDir(jobId, dataDirName, indexId, "Index");
    return config.nextAvailableIndexDir(jobId, dataDirParent, dataDirName, indexId, "Index");
  }

  public File getStoreRoot(){
    final StringBuilder result = new StringBuilder();

    result.
      append(config.getOutputDataRoot(jobId)).
      append((dataDirParent == null ? "" : dataDirParent)).append('/');

    return new File(result.toString());
  }
}
