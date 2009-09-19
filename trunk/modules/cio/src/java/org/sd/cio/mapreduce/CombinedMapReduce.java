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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sd.io.DirectorySelector;
import org.sd.io.FileRecordIterator;
import org.sd.io.MultiFileIterator;
import org.sd.io.MultiPartRecordFactory;
import org.sd.util.KVPair;
import org.sd.util.KVPairLoader;
import org.sd.util.NameGenerator;
import org.sd.util.thread.Governable;
import org.sd.util.thread.UnitCounter;

/**
 * Utility to map arbitrary input, then reduce until done.
 * <p>
 * @author Spence Koehler
 */
public abstract class CombinedMapReduce<K extends Comparable<K>, V, A, R> implements Governable {

  public static final int DEFAULT_MAX_PAIRS = 10000000;
  public static final int DEFAULT_NUM_DIGITS = 3;
  public static final Integer DEFAULT_MAX_SIMULTANEOUS_REDUCER_FILES = null;


  // mapper
  protected abstract boolean getVerboseFlag();
  protected abstract File getInputDir();
  protected abstract File getMapOutputDir();
  protected abstract DirectorySelector.Action selectInputDirectory(File dir);
  protected abstract boolean selectInputFile(File file);
  protected abstract FileRecordIterator<R> buildInputFileRecordIterator(File file) throws IOException;
  protected abstract List<MapperPair<K, V, A>> transformInputRecord(R record);
  protected abstract void map(MapperPair<K, V, A> mapperPair);  // ==>operate

  // reducer
  protected abstract File getReduceOutputDir();
  protected abstract MapperPair<K, V, A> merge(List<KVPair<K, V>> pairs, A actionKey);

  // both
  protected abstract A extractActionKey(File file);  // reverse of generateOutputFilePrefix but with potentially non-applicable input or input with extra postfix chars.
  protected abstract String generateOutputFilePrefix(A actionKey); //return new StringBuilder().append(actionKey).append("gms").toString();
  protected abstract SimpleTextRecordFileStrategy<K, V> buildSimpleTextRecordFileStrategy();


  private UnitCounter uc;
  private Mapper<K, V, A, R> mapper;
  private Reducer<K, V, A, R> reducer;

  protected final Map<A, Map<K, V>> actionMaps;
  private SimpleTextRecordFileStrategy<K, V> _strfs;

  protected CombinedMapReduce() {
    this.uc = new UnitCounter();
    this.actionMaps = new HashMap<A, Map<K, V>>();
  }

  /**
   * Get this instance's unit counter.
   */
  public UnitCounter getUnitCounter() {
    return uc;
  }

  public void run() {
//todo: add some logging and control to this through overridable hooks.
    initialize();
    if (preMapHook()) {
      mapper.run();
    }
    if (preReduceHook()) {
      reducer.run();
    }
  }

  /**
   * Hook executed prior to running the mapper.
   * <p>
   * Intended for extenders to override. Default is true.
   *
   * @return true to run the mapper; false to skip running the mapper.
   */
  protected boolean preMapHook() {
    return true;
  }

  /**
   * Hook executed prior to running the reducer.
   * <p>
   * Intended for extenders to override. Default is true.
   *
   * @return true to run the reducer; false to skip running the reducer.
   */
  protected boolean preReduceHook() {
    return true;
  }

  /**
   * Get the maximum number of pairs to write to a file during the 'chainNum'
   * phase.
   * <p>
   * Intended for extenders to override. Default is 10,000,000.
   * <p>
   * Where chainNum is
   * <ul>
   * <li>null or 0 for map phase</li>
   * <li>N &gt; 0 for Nth reduce phase</li>
   * </ul>
   */
  protected int getMaxPairs(Integer chainNum) {
    return DEFAULT_MAX_PAIRS;
  }

  /**
   * Get the number of digits necessary for rolling output files during the
   * 'chainNum' phase.
   * <p>
   * Intended for extenders to override. Default is 3.
   * <p>
   * Where chainNum is
   * <ul>
   * <li>null or 0 for map phase</li>
   * <li>N &gt; 0 for Nth reduce phase</li>
   * </ul>
   */
  protected int getNumDigits(Integer chainNum) {
    return DEFAULT_NUM_DIGITS;
  }

  protected Integer getMaxSimultaneousReducerFiles(Integer chainNum) {
    return DEFAULT_MAX_SIMULTANEOUS_REDUCER_FILES;
  }

  protected FlushActionFactory<K, V, A> doBuildFlushActionFactory(final File outDir, final Integer chainNum) {
    if (chainNum == null || chainNum.equals(0)) {
      // mapper flush
      return new CommonFlushActionFactory<K, V, A>(outDir, getMaxPairs(chainNum), chainNum, getNumDigits(chainNum)) {
        protected String getOutputFilePrefix(A actionKey) {
          return generateOutputFilePrefix(actionKey);
        }
        protected SimpleTextRecordFileStrategy<K, V> buildFlushFileStrategy() {
          return getSimpleTextRecordFileStrategy();
        }
        protected FlushAction<K, V, A> buildFlushAction(A actionKey, File flushDir, NameGenerator nameGenerator, int maxPairs, SimpleTextRecordFileStrategy<K, V> flushFileStrategy) {
          final MapContainer<K, V> mapContainer = new MapContainer<K, V>();
          actionMaps.put(actionKey, mapContainer.getMap());  // keep a handle on the flush action's map
          return new SimpleFlushAction<K, V, A>(flushDir, nameGenerator, maxPairs, flushFileStrategy, mapContainer);
        }
      };
    }
    else {
      // reducer flush
      return new CommonFlushActionFactory<K, V, A>(outDir, getMaxPairs(chainNum), chainNum, getNumDigits(chainNum)) {
        protected String getOutputFilePrefix(A actionKey) {
          return generateOutputFilePrefix(actionKey);
        }
        protected SimpleTextRecordFileStrategy<K, V> buildFlushFileStrategy() {
          return getSimpleTextRecordFileStrategy();
        }
        protected FlushAction<K, V, A> buildFlushAction(A actionKey, File flushDir, NameGenerator nameGenerator, int maxPairs, SimpleTextRecordFileStrategy<K, V> flushFileStrategy) {
          return new DirectFlushAction<K, V, A>(flushDir, nameGenerator, maxPairs, flushFileStrategy);
        }
      };
    }
  }

  protected A computeActionKey(List<File> context) {
    A result = null;

    if (context != null && context.size() > 0) {
      final File exampleFile = context.get(0);
      result = extractActionKey(exampleFile);
    }

    return result;
  }


  private final void initialize() {
    this.mapper = buildMapper();
    this.reducer = buildReducer(mapper);
  }

  private final Mapper<K, V, A, R> buildMapper() {
    final Mapper<K, V, A, R> result = new Mapper<K, V, A, R>() {
      protected final boolean getVerbose() { return getVerboseFlag(); }
      protected final File getRootInputFile() { return getInputDir(); }
      protected final File getRootOutputFile() { return getMapOutputDir(); }
      protected final DirectorySelector.Action selectDirectory(File dir) { return selectInputDirectory(dir); }
      protected final boolean selectFile(File file) { return selectInputFile(file); }
      protected final FileRecordIterator<R> buildFileRecordIterator(File file) throws IOException { return buildInputFileRecordIterator(file); }
      protected final FlushActionFactory<K, V, A> buildFlushActionFactory(File outDir) { return doBuildFlushActionFactory(outDir, 0); }
      protected final List<MapperPair<K, V, A>> transformRecord(R record) { return transformInputRecord(record); }
      public final void operate(MapperPair<K, V, A> mapperPair) { map(mapperPair); }
    };        

    result.setUnitCounter(uc.registerSubsidiary());
    return result;
  }

  private final Reducer<K, V, A, R> buildReducer(Mapper<K, V, A, R> mapper) {
    final int chainNum = 1;

    final Reducer<K, V, A, R> result = new RepeatingReducer<K, V, A, R>(mapper) {
      protected final boolean getVerbose() { return getVerboseFlag(); }
      protected final File getRootInputFile() { return getMapOutputDir(); }  // reduce input is map output
      protected final File getRootOutputFile() { return getReduceOutputDir(); }
      protected final A getActionKey(File file) { return extractActionKey(file); }
      protected final MultiPartRecordFactory<File, List<File>> buildFileCollector() { return super.buildSimpleFileCollector(getMaxSimultaneousReducerFiles(chainNum)); }
      protected final MultiFileIterator<KVPair<K, V>> getCoIterator(List<File> files, Comparator<KVPair<K, V>> recordComparer) throws IOException {
        return simpleTextFileCoIterator(files, recordComparer, new KVPairLoader<K, V, String>() {
            public KVPair<K, V> buildKVPair(String line) {
              return getSimpleTextRecordFileStrategy().decodeLine(line);
            }
          });
      }
      protected final Comparator<KVPair<K, V>> buildRecordComparer() {
        // sort records by key to merge those where key is equal and to co-iterate over sorted-by-key input files
        return new Comparator<KVPair<K, V>>() {
          public int compare(KVPair<K, V> p1, KVPair<K, V> p2) {
            return p1.key.compareTo(p2.key);
          }
        };
      }
      protected final OutputFinalizer<List<File>> buildOutputFinalizer(final FlushActionFactory<K, V, A> faf) {
        return new OutputFinalizer<List<File>>() {
          public void finalize(List<File> context) throws IOException {
            final A actionKey = computeActionKey(context);
            if (actionKey != null) {
              final FlushAction<K, V, A> flushAction = faf.getFlushAction(actionKey);
              if (flushAction != null) {
                flushAction.flush();
              }
            }
          }
        };
      }
      protected final MapperPair<K, V, A> reduce(List<KVPair<K, V>> pairs, List<File> context) {
        MapperPair<K, V, A> result = null;

        final A actionKey = computeActionKey(context);
        if (actionKey != null) {
          result = merge(pairs, actionKey);
        }
    
        return result;
      }
      protected final FlushActionFactory<K, V, A> buildFlushActionFactory(File outDir) {
        return doBuildFlushActionFactory(outDir, chainNum);
      }
    };

    result.setUnitCounter(uc.registerSubsidiary());
    return result;
  }

  private final SimpleTextRecordFileStrategy<K, V> getSimpleTextRecordFileStrategy() {
    if (_strfs == null) {
      _strfs = buildSimpleTextRecordFileStrategy();
    }
    return _strfs;
  }
}
