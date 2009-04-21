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


import org.sd.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Container for search resources.
 * <p>
 * @author Spence Koehler
 */
public class SearchResources {
  
  public final Directory directory;
  public final IndexReader indexReader;
  public final IndexSearcher indexSearcher;

  public final AtomicBoolean isOpen = new AtomicBoolean(true);

  public SearchResources(File dirPath) throws IOException {
    this.directory = FSDirectory.getDirectory(dirPath);

    //
    // NOTE:
    //   found that large indexes tend to cause OutOfMemory errors
    //   in SegmentTermEnum. It is exposed by searching the indexed
    //   but caused while loading index data. The solution is to
    //   call setTermInfosIndexDivisor on the underlying IndexReader.
    //   This will slow performance slightly, but not doing it kills
    //   the process.
    //
    //   It appears that 100MB is a magic number for size. Indexes
    //   above 100MB have been "blowing out" while those under have
    //   been ok. For now, we'll try the following:
    //
    //       < 100 MB  -->  N=1
    //       < 200 MB  -->  N=2
    //       ...
    //
    //   If this still yields OutOfMemory errors, let's try
    //
    //       < 100 MB  -->  N=1
    //       < 150 MB  -->  N=2
    //       < 200 MB  -->  N=3
    //       ...
    //
    final long mbytes = FileUtil.size(dirPath) / (1024 * 1024);
    final int divisor = (int)(mbytes / 100) + 1;  // 100=1, 200=2, 300=3, ...
    //final int divisor = (int)((mbytes - 100) / 50) + 1;  // 100=1, 150=2, 200=3, ...

    this.indexReader = IndexReader.open(directory);
    indexReader.setTermInfosIndexDivisor(divisor);

    this.indexSearcher = new IndexSearcher(indexReader);
  }

  public void close() throws IOException {
    if (isOpen.compareAndSet(true, false)) {
      this.indexReader.close();
      this.indexSearcher.close();
      this.directory.close();
    }
  }

  public TopDocs search(Query query, int n, Sort sort) throws IOException {
    TopDocs result = null;

    if (sort == null) {
      result = indexSearcher.search(query, n);
    }
    else {
      result = indexSearcher.search(query, null, n, sort);
    }

    return result;
  }

	public Document getDocument(int doc) throws IOException {
		return indexSearcher.doc(doc);
	}
}
