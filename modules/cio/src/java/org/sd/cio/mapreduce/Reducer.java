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
package org.sd.cio.mapreduce;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sd.io.FileRecordIterator;
import org.sd.io.FileUtil;
import org.sd.io.MultiFileIterator;
import org.sd.io.MultiFileIteratorFactory;
import org.sd.io.MultiPartRecordFactory;
import org.sd.io.RecordMerger;
import org.sd.io.SimpleFileCollector;
import org.sd.util.KVPair;
import org.sd.util.KVPairLoader;

/**
 * Reduce process definition utility.
 * <p>
 * In addtion to the abstract methods from MapReduceBase for specifying the
 * processing input and output, extenders of this class must implement the
 * following:
 * <ul>
 * <li>getRootOutputFile -- to specify the root output directory.</li>
 * <li>buildMultiPartRecordFactory -- to specify groups of files over which to co-iterate.</li>
 * <li>getCoIterator -- to co-iterate over a file group.</li>
 * <li>buildRecordComparer -- for sorting and collecting like records for reduction.</li>
 * <li>buildOutputFinalizer -- for finalizing each output file when filled.</li>
 * <li>reduce -- to do the actual work of reducing equal pairs into one.</li>
 * </ul>
 *
 * @author Spence Koehler
 */
public abstract class Reducer<K, V, A, R> extends MapReduceBase<K, V, A, R> {
  
  /**
   * Build a factory for specifying and collecting groups of files over which
   * to co-iterate.
   *
   * @return the MultiPartRecordFactory for grouping files.
   */
  protected abstract MultiPartRecordFactory<File, List<File>> buildFileCollector(); //todo: create special abstract class for this and just reference its method(s)?

  /**
   * Get a co-iterator over the key/value pairs from the files in a group of
   * files as returned from the multi-part record factory.
   *
   * @param files  The group of files over which to co-iterate.
   * @param recordComparer  The comparator for sorting and collecting pairs.
   *
   * @return a MultiFileIterator for the files.
   */
  protected abstract MultiFileIterator<KVPair<K, V>> getCoIterator(List<File> files, Comparator<KVPair<K, V>> recordComparer) throws IOException;

  /**
   * Build the comparer for sorting and collecting like key/value pair records
   * during co-iteration.
   *
   * @return the comparator.
   */
  protected abstract Comparator<KVPair<K, V>> buildRecordComparer();

  /**
   * Build an output finalizer for finalizing each output file when filled.
   *
   * @param faf  The flush action factory from which the output finalizer will
   *             select the appropriate flush action for the context of a group
   *             of files.
   *
   * @return the output finalizer.
   */
  protected abstract OutputFinalizer<List<File>> buildOutputFinalizer(FlushActionFactory<K, V, A> faf);

  /**
   * Do the work of reducing the pairs into one pair that can be correlated to
   * the appropriate flush action through its action key.
   *
   * @param pairs  A set of equal pairs (according to the record comparator)
   *               to be combined.
   * @param context  The group of files from which the pairs were collected.
   *
   * @return a single combined pair.
   */
  protected abstract MapperPair<K, V, A> reduce(List<KVPair<K, V>> pairs, List<File> context);


  // flag to track whether the output needs to be reduced again
  private final AtomicBoolean needsReduce = new AtomicBoolean(false);
  private int chainNum;

  /**
   * Default constructor.
   */
  protected Reducer() {
    super();
    this.chainNum = 1;
  }

  /**
   * Determine whether the output of this reducer needs to be reduced again.
   */
  public boolean needsReduce() {
    return needsReduce.get();
  }

  /**
   * Get the current chain num (initially 1).
   */
  public int getChainNum() {
    return chainNum;
  }

  /**
   * Reset this reducer for another chained phase.
   */
  public void reset(int nextChainNum) {
    this.chainNum = nextChainNum;
    this.needsReduce.set(false);
  }

  /**
   * Build a "reducer" directory walker using the abstract methods to fill in
   * the details.
   */
  protected final DirWalker buildWalker() {
    return DirWalker.reducer(
      getVerbose(), getUnitCounter(), getCurrentInputFile(),
      buildDirectorySelector(),
      buildFileSelector(),
      buildFileCollector(),
      buildMultiFileIteratorFactory(),
      buildRecordComparer(),
      new RecordMerger<KVPair<K, V>, List<File>>() {
        public boolean merge(List<KVPair<K, V>> records, List<File> context) {
          boolean result = false;
          try {
            result = mergeRecords(records, context);
          }
          catch (IOException e) {
            throw new IllegalStateException(e);
          }
          return result;
        }
      },
      buildOutputFinalizer(getFlushActionFactory()),
      needsReduce);
  }

  /**
   * Helper method for building a MultiFileIteratorFactory that uses this
   * class's getCoIterator method.
   */
  protected MultiFileIteratorFactory<KVPair<K, V>> buildMultiFileIteratorFactory() {
    return new MultiFileIteratorFactory<KVPair<K, V>>() {
      public MultiFileIterator<KVPair<K, V>> getMultiFileIterator(List<File> files, Comparator<KVPair<K, V>> recordComparer) throws IOException {
        return getCoIterator(files, recordComparer);
      }
    };
  }

  /**
   * Process a group of equal records collected during iteration, sending them
   * through the reduce method to generate a single mapper key/value pair that
   * is flushed through the appropriate flush action according to the flush
   * action factory.
   *
   * @param pairs  A group of equal key/value pairs to reduce.
   * @param context  The group of files from which the pairs were collected.
   *                 Note that this enables identification of the appropriate
   *                 flush action instance to retrieve from the flush action
   *                 factory.
   *
   * @return true if the reduced pair is successfully added to the flush action.
   */
  private final boolean mergeRecords(List<KVPair<K, V>> pairs, List<File> context) throws IOException {
    final MapperPair<K, V, A> reducedPair = reduce(pairs, context);
    if (reducedPair != null) {
      final FlushAction<K, V, A> flushAction = getFlushActionFactory().getFlushAction(reducedPair);
      flushAction.add(reducedPair, null);
    }
    return true;
  }


  /**
   * Utility method for extenders to call while implementing getCoIterator.
   * <p>
   * This is appropriate for cases where the input file contains key/value pairs
   * in a text file, one pair per line.
   *
   * @param files  The list of files over which to co-iterate.
   * @param recordComparer  The comparator for sorting and combining like pairs.
   * @param kvPairLoader  A strategy for transforming a file line into a mapper key/value pair.
   *
   * @return the MultiFileIterator over the files.
   */
  protected final MultiFileIterator<KVPair<K, V>> simpleTextFileCoIterator(
    List<File> files, Comparator<KVPair<K, V>> recordComparer, final KVPairLoader<K, V, String> kvPairLoader)
    throws IOException {

    return new MultiFileIterator<KVPair<K, V>>(files.toArray(new File[files.size()]), recordComparer) {
      protected FileRecordIterator<KVPair<K, V>> buildRecordIterator(File file) throws IOException {
        return new FileRecordIterator<KVPair<K, V>>(file) {
          private BufferedReader reader;

          /** Initialize this instance for reading the file. */
          protected void init(File file) throws IOException {
            this.reader = FileUtil.getReader(file);
          }

          /** Read the next record, returning null if there are no more. */
          protected KVPair<K, V> readNextRecord() throws IOException {
            KVPair<K, V> result = null;

            final String line = reader.readLine();
            if (line != null) {
              result = kvPairLoader.buildKVPair(line);
            }

            return result;
          }

          /** Close resources for this instance. */
          public void close() throws IOException {
            reader.close();
          }
        };
      }
    };
  }

  /**
   * Utility method for extenders to call while implementing buildFileCollector.
   * <p>
   * This is appropriate for cases where files to co-iterate are grouped under
   * a common parent.
   *
   * @param maxFiles  The maximum number of files to collect in a group.
   * @return a SimpleFileCollector instance.
   */
  protected final SimpleFileCollector buildSimpleFileCollector(final Integer maxFiles) {
    return new SimpleFileCollector(maxFiles);
  }
}
