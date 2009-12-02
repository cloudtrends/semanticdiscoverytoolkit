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


import org.sd.util.MathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

/**
 * Utility to iterate over the documents of an index.
 * <p>
 * @author Spence Koehler
 */
public class IndexIterator implements Iterator<Document> {

  private File indexDir;
  private boolean readOnly;
  private IndexReader indexReader;
  private int numDocs;
  private int nextDocNum;
  private long starttime;
  private long endtime;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);

  public IndexIterator(File indexDir) {
    this(indexDir, true);
  }

  public IndexIterator(File indexDir, boolean readOnly) {
    this.indexDir = indexDir;
    this.readOnly = readOnly;
    try {
      this.indexReader = IndexReader.open(FSDirectory.open(indexDir), readOnly);
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
    this.numDocs = indexReader.numDocs();
    this.nextDocNum = 0;
    this.starttime = 0L;
    this.endtime = 0L;
  }

  /**
   * Close resources associated with this instance.
   */
  public void close() throws IOException {
    if (isClosed.compareAndSet(false, true)) {
      indexReader.close();
      markEnd();
    }
  }

  private final void markEnd() {
    if (endtime == 0L) {
      endtime = System.currentTimeMillis();
      if (starttime == 0L) starttime = endtime;
    }
  }

  /**
   * Determine whether there is a 'next' document.
   */
  public boolean hasNext() {
    final boolean result = nextDocNum < numDocs;

    if (!result) {
      markEnd();
      try {
        close();
      }
      catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    return result;
  }

  /**
   * Get the next document. Thread-safe.
   */
  public Document next() {
    Document result = null;

    synchronized (indexReader) {
      if (hasNext()) {
        if (starttime == 0L) starttime = System.currentTimeMillis();
        try {
          result = indexReader.document(nextDocNum++);
        }
        catch (Exception e) {
          throw new IllegalStateException(e);
        }
      }
    }

    return result;
  }

  /**
   * Delete the last document returned by next.
   * <p>
   * Note that in threaded use, this may delete an unexpected record due to
   * other threads having potentially moved the pointers forward. In such
   * cases as remove will be used, it is recommended to synchronize on the
   * iterator instance if not used in a single thread.
   */
  public void remove() {
    if (readOnly) throw new IllegalStateException("Can't delete when readOnly");
    try {
      indexReader.deleteDocument(nextDocNum - 1);
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }


  /**
   * Get the number of documents iterated over so far.
   */
  public int getCount() {
    return nextDocNum;
  }

  /**
   * Get the total number of documents to iterate over.
   */
  public int getNumDocs() {
    return numDocs;
  }

  /**
   * Get the number of millis elapsed since beginning iteration.
   */
  public long getElapsed() {
    final long endref = (endtime > 0L) ? endtime : System.currentTimeMillis();
    final long startref = (starttime > 0L) ? starttime : endref;
    return endref - startref;
  }

  /**
   * Get the rate of iteration in Documents per Second.
   */
  public double getRate() {
    final double docs = (double)getCount();
    final double millis = (double)getElapsed();

    return millis > 0 ? docs * 1000 / millis : 0.0;
  }

  /**
   * Estimate the time at which iteration will be complete based on the
   * current rate.
   *
   * @return the estimated date iteration will finish, or the date actually
   *         finished if already done, or null if there is insufficient
   *         information to compute (i.e. iteration has not yet begun.)
   */
  public Date getETA() {
    Date result = null;

    if (endtime > 0L) {
      result = new Date(endtime);
    }
    else {
      final double curRate = getRate();
      if (curRate > 0) {
        final int todo = numDocs - nextDocNum;
        final double secondstogo = ((double)todo) / curRate;
        final long endtime = System.currentTimeMillis() + (long)(secondstogo * 1000.0);
        result = new Date(endtime);
      }
    }

    return result;
  }


  //./run org.sd.lucene.IndexIterator /home/newmedia/cluster/jvm-0/data/output/RollupJob/Rollup_0_postsIndex/nausican-1.out
  public static final void main(String[] args) throws IOException {
    // arg0: indexDir

    System.out.println(new Date() + ": Iterating over '" + args[0] + "'");
    
    long nextCheck = 60000;  // check rates in 1 minute
    final IndexIterator iter = new IndexIterator(new File(args[0]));
    while (iter.hasNext()) {
      final Document doc = iter.next();

      final long elapsed = iter.getElapsed();
      if (elapsed > nextCheck) {
        System.out.println(new Date() + ": Iterated over " + iter.getCount() + " documents in " +
                           MathUtil.timeString(elapsed, false) + " at " +
                           MathUtil.doubleString(iter.getRate(), 3) + " docs/sec  ETA=" +
                           iter.getETA());
        nextCheck += 60000;
      }
    }
    iter.close();

    System.out.println(new Date() + ": Iterated over " + iter.getCount() + " documents in " +
                       MathUtil.timeString(iter.getElapsed(), false) + " at " +
                       MathUtil.doubleString(iter.getRate(), 3) + " docs/sec");
  }
}
