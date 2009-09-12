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


import java.io.File;
import java.io.IOException;
import java.util.List;

import org.sd.io.FileRecordIterator;
import org.sd.io.FileRecordIteratorFactory;
import org.sd.io.RecordOperator;

/**
 * Map process definition utility.
 * <p>
 * In addition to the abstract methods from MapReduceBase for specifying the
 * processing input and output, extenders of this class must implement the
 * following:
 * <ul>
 * <li>buildFileRecordIterator -- for iterating over the input records from a
 *                                selected file.</li>
 * <li>transformRecord -- for transforming input records into mapper pairs that
 *                        include a key/value pair to represent the data as well
 *                        as an actionKey for identification of the appropriate
 *                        output through the flush action.</li>
 * <li>operate -- do the work of mapping the pair into flushable output,
 *                combining if warranted. (NOTE: this is to implement
 *                FlushAction.AddStrategy.)
 * </ul>
 *
 * @author Spence Koehler
 */
public abstract class Mapper<K, V, A, R> extends MapReduceBase<K, V, A, R> implements FlushAction.AddStrategy<K, V, A> {


  /**
   * Build a file record iterator instance for iterating over each input record
   * of type R.
   */
  protected abstract FileRecordIterator<R> buildFileRecordIterator(File file) throws IOException;

  /**
   * Transform a record of type R as returned by the file record iterator into
   * a list of mapper pairs, each of which associates a key of type K with a
   * value of type V. Additionally, each mapper pair will have an action key of
   * type A for identifying the appropriate output through the flush action.
   * <p>
   * Note that records are sorted by key (K) on output, so K should have a
   * meaningful natural ordering.
   * <p>
   * Note that each flush action (as identified by an actionKey) can be thought
   * of as a distinct output channel in which key/value pairs are collected,
   * merging where necessary, and periodically flushed to an output file that
   * will become one of multiple input files (<b>from the same channel</b>)
   * to be co-iterated during the reduce phase. Across flush actions (channels),
   * there will typically be no combining as elements from these sets are
   * incompatible or mutually exclusive.
   * <p>
   * As an example, consider the task of mapping N-grams to their frequencies.
   * Each N, would constitute a separate flush action output channel and would
   * have its own action key (like N) because it does not make sense to combine
   * m-grams with n-grams where m != n. Assuming that N-grams for any N could
   * be generated from the input, the action key allows us to process all of
   * these in a single pass.
   */
  protected abstract List<MapperPair<K, V, A>> transformRecord(R record);


  /**
   * Default constructor.
   */
  protected Mapper() {
    super();
  }

  /**
   * Build a "mapper" directory walker using the abstract methods to fill in
   * the details.
   */
  protected final DirWalker buildWalker() {
     return DirWalker.mapper(
       getVerbose(), getUnitCounter(), getCurrentInputFile(),
       buildDirectorySelector(),
       buildFileSelector(),
      new FileRecordIteratorFactory<R>() {
        public FileRecordIterator<R> getFileRecordIterator(File file) throws IOException {
          return buildFileRecordIterator(file);
        }
      },
      new RecordOperator<R, File>() {
        public boolean operate(R record, File context) {
          boolean result = false;
          try {
            result = processRecord(record, context);
          }
          catch (IOException e) {
            throw new IllegalStateException(e);
          }
          return result;
        }
      });
  }

  /**
   * Process a record encountered during iteration, sending it through
   * transformRecord to generate key/value pairs and then sending each
   * generated pair through the appropriate flush action according to
   * their action key.
   */
  private final boolean processRecord(R record, File context) throws IOException {
    final List<MapperPair<K, V, A>> pairs = transformRecord(record);
    if (pairs != null) {
      for (MapperPair<K, V, A> pair : pairs) {
        final FlushAction<K, V, A> flushAction = getFlushActionFactory().getFlushAction(pair);
        flushAction.add(pair, this);
      }
    }
    return true;
  }
}
