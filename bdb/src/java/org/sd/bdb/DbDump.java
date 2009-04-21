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
package org.sd.bdb;


import org.sd.util.LineBuilder;
import org.sd.util.MathUtil;
import org.sd.util.range.IntegerRange;
import org.sd.util.range.NumericRange;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * Utility to dump a berkeley database.
 * <p>
 * @author Spence Koehler
 */
public class DbDump {

  private BerkeleyDb bdb;
  private String[] dbNames;
  private NumericRange indexRange;
  private boolean setLongKeys;
  private boolean setPublishableValues;
  private DbTraversal traversalType;
  private File outputDbPath;
  private BerkeleyDb outputDb;

  public DbDump(BerkeleyDb bdb) {
    this(bdb, null, null, false, false, false, false, null);
  }

  public DbDump(BerkeleyDb bdb, String[] dbNames, NumericRange indexRange,
                boolean setLongKeys, boolean setPublishableValues, boolean byTimestamp,
                boolean reverse, File outputDbPath) {

    this.bdb = bdb;
    this.dbNames = (dbNames == null) ? bdb.getDatabaseNames() : dbNames;
    this.indexRange = indexRange;  // ok if null.
    this.setLongKeys = setLongKeys;
    this.setPublishableValues = setPublishableValues;
    this.traversalType = (byTimestamp) ?
      (reverse ? DbTraversal.REVERSE_TIME_ORDER : DbTraversal.TIME_ORDER) :
      (reverse ? DbTraversal.REVERSE_KEY_ORDER : DbTraversal.KEY_ORDER);
    this.outputDbPath = outputDbPath;
    this.outputDb = (outputDbPath != null) ? BerkeleyDb.getInstance(outputDbPath, false) : null;
  }

  public void stats(PrintStream out) {

    out.println("BerkeleyDb Environment: " + bdb.getEnvLocation());
    out.print("             Databases:");
    for (String dbName : dbNames) out.print(" " + dbName);
    out.println("\n");

    for (String dbName : dbNames) {
      if (!dbName.endsWith("_timestamped")) {
        stats(out, dbName);
      }
    }
  }

  public void stats(PrintStream out, String dbName) {

    out.println("      Database Name: " + dbName);

    if (!bdb.hasDatabaseName(dbName)) {
      out.println("      *** Database doesn't exist !!! ***");
      return;
    }

    final DbHandle dbHandle = getDbHandle(dbName);

    out.println("        Num Records: " + dbHandle.getNumRecords());
    if (indexRange != null) out.println("        Index Range: " + indexRange.asString());
    out.println("           Key Type: " + (dbHandle.getDbInfo().hasStringKeys() ? "STRING" : "LONG") + " [NOTE: Can't AutoDetect!]");
    if (setPublishableValues) out.println("         Value Type: PUBLISHABLE");
    out.println("  Has TimestampedDb: " + (dbHandle.hasTimestampedDb()));
    out.println("     Traversal Type: " + traversalType);
    if (outputDbPath != null) out.println("  Sending Output to: " + outputDbPath);
    out.println();
  }

  public List<QueryResult> query(String queryKey) {
    final List<QueryResult> result = new ArrayList<QueryResult>();

    for (String dbName : dbNames) {
      if (!dbName.endsWith("_timestamped")) {
        final QueryResult queryResult = query(dbName, queryKey);
        if (queryResult != null) {
          result.add(queryResult);
        }
      }
    }

    return result;
  }

  public QueryResult query(String dbName, String queryKey) {
    final DbHandle dbHandle = getDbHandle(dbName);

    QueryResult result = null;
    DbValue dbValue = null;

    if (dbHandle.getDbInfo().hasStringKeys()) {
      dbValue = dbHandle.get(queryKey);
    }
    else {
      dbValue = dbHandle.get(Long.parseLong(queryKey));
    }

    if (dbValue != null) {
      result = new QueryResult(dbName, queryKey, dbValue, setPublishableValues);
    }

    return result;
  }

  public void dump(PrintStream out, boolean onlyShowStats) {
    for (String dbName : dbNames) {
      if (!dbName.endsWith("_timestamped")) {
        dump(out, dbName, onlyShowStats);
      }
    }
  }

  public void dump(PrintStream out, String dbName, boolean onlyShowStats) {
    final DbHandle dbHandle = getDbHandle(dbName);

    IterStats iterStats = null;

    DbHandle outputDbHandle = null;
    if (outputDb != null) {
      outputDbHandle = getDbHandle(outputDb, dbName);
    }

    if (dbHandle.getDbInfo().hasStringKeys()) {
      iterStats = dumpStringKeys(out, dbHandle, onlyShowStats, outputDbHandle);
    }
    else {
      iterStats = dumpLongKeys(out, dbHandle, onlyShowStats, outputDbHandle);
    }

    if (iterStats != null) {
      System.out.println("\n\titerStats=" + iterStats.toString());
    }
  }

  private final IterStats dumpStringKeys(PrintStream out, DbHandle dbHandle, boolean onlyShowStats, DbHandle outputDbHandle) {
    final IterStats result = new IterStats();

    final DbIterator<StringKeyValuePair> iter = dbHandle.iterator(traversalType);

    final LineBuilder lineBuilder = new LineBuilder();
    int iterIndex = 0;
    int numOut = 0;
    while (iter.hasNext()) {
      final StringKeyValuePair kvPair = iter.next();
      
      final int curIterIndex = iterIndex++;

      if (kvPair == null) {
        System.err.println("WARNING: Null kvPair at index=" + curIterIndex + "!");
        continue;
      }

      if (indexRange != null) {
        if (!indexRange.includes(curIterIndex)) {
          if (indexRange.size() != null && numOut >= indexRange.size()) break;
          else continue;
        }
        else {
          if (!result.isStarted()) result.start();
        }
      }

      if (!onlyShowStats) {
        if (outputDbHandle != null) {
          outputDbHandle.put(kvPair.getKey(), kvPair);
        }
        else {
          lineBuilder.
            append(dbHandle.getDbInfo().getDbName()).
            append(iterIndex).
            append(kvPair.getTimestamp()).
            append(kvPair.getKey()).
            append(getValueString(kvPair, setPublishableValues));

          out.println(lineBuilder.toString());

          lineBuilder.reset();
        }
      }

      result.inc();
      ++numOut;
    }

    result.stop();
    iter.close();

    return result;
  }

  private final IterStats dumpLongKeys(PrintStream out, DbHandle dbHandle, boolean onlyShowStats, DbHandle outputDbHandle) {
    final IterStats result = new IterStats();

    final DbIterator<LongKeyValuePair> iter = dbHandle.iteratorLong(traversalType);

    final LineBuilder lineBuilder = new LineBuilder();
    int iterIndex = 0;
    int numOut = 0;
    while (iter.hasNext()) {
      final LongKeyValuePair kvPair = iter.next();
      
      final int curIterIndex = iterIndex++;
      if (indexRange != null) {
        if (!indexRange.includes(curIterIndex)) {
          if (indexRange.size() != null && numOut >= indexRange.size()) break;
          else continue;
        }
        else {
          if (!result.isStarted()) result.start();
        }
      }

      if (!onlyShowStats) {
        if (outputDbHandle != null) {
          outputDbHandle.put(kvPair.getKey(), kvPair);
        }
        else {
          lineBuilder.
            append(dbHandle.getDbInfo().getDbName()).
            append(iterIndex).
            append(kvPair.getTimestamp()).
            append(kvPair.getKey()).
            append(getValueString(kvPair, setPublishableValues));

          out.println(lineBuilder.toString());

          lineBuilder.reset();
        }
      }

      result.inc();
      ++numOut;
    }

    result.stop();
    iter.close();

    return result;
  }

  private final DbHandle getDbHandle(String dbName) {

    if (!bdb.hasDatabaseName(dbName)) {
      throw new IllegalArgumentException("Database '" + dbName + "' doesn't exist at '" + bdb.getEnvLocation() + "'!");
    }

    return getDbHandle(bdb, dbName);
  }

  private final DbHandle getDbHandle(BerkeleyDb bdb, String dbName) {
    final DbHandle dbHandle = bdb.get(dbName, null);

    if (setLongKeys) dbHandle.getDbInfo().setHasLongKeys(true);
    
    return dbHandle;
  }

  private static final String getValueString(DbValue dbValue, boolean setPublishableValues) {
    String result = null;

    if (setPublishableValues) {
      result = dbValue.getPublishable().toString();
    }
    else {
      result = dbValue.toString();
    }

    return result;
  }


  /**
   * Container class for a query result.
   */
  public static final class QueryResult {

    public final String dbName;
    public final String queryKey;
    public final DbValue dbValue;
    private final boolean setPublishableValues;
    private Integer index;

    public QueryResult(String dbName, String queryKey, DbValue dbValue, boolean setPublishableValues) {
      this.dbName = dbName;
      this.queryKey = queryKey;
      this.dbValue = dbValue;
      this.setPublishableValues = setPublishableValues;
      this.index = null;
    }

    public void setIndex(int index) {
      this.index = index;
    }

    public int getIndex() {
      return index;
    }

    public String toString() {
      final LineBuilder result = new LineBuilder();

      result.
        append(dbName).
        append(index == null ? "" : index.toString()).
        append(dbValue.getTimestamp()).
        append(queryKey).
        appendNoFix(getValueString(dbValue, setPublishableValues));

      return result.toString();
    }
  }

  private static final class IterStats {

    private boolean started;
    private long starttime;
    private long endtime;
    private int count;

    IterStats() {
      this.started = false;
      this.starttime = System.currentTimeMillis();
      this.endtime = 0L;
      this.count = 0;
    }

    boolean isStarted() {
      return started;
    }

    void start() {
      started = true;
      this.starttime = System.currentTimeMillis();
    }

    void inc() {
      ++count;
    }

    void stop() {
      this.endtime = System.currentTimeMillis();
    }

    public String toString() {
      long endtime = (this.endtime == 0L) ? System.currentTimeMillis() : this.endtime;
      final long deltatime = endtime - starttime;

      final double rate = (double)(count * 1000) / (double)deltatime;

      final StringBuilder builder = new StringBuilder();

      // <count> entries in <time> seconds at <rate> entries/sec
      builder.
        append(count).
        append(" entries in ").
        append(MathUtil.doubleString((double)deltatime / 1000.0, 3)).
        append(" seconds at ").
        append(MathUtil.doubleString(rate, 3)).
        append(" entries/sec");

      return builder.toString();
    }
  }


  public static final void main(String[] args) throws ParseException {
    //
    //Output:
    //
    // dbName | iterIndex | timestamp | key | value
    //

    // -path=    [reqd] path to bdb
    // -name=    [opt default=first] name of db to dump
    // -range    [opt] "a-b" just dumps records from a (inclusive) to b (exclusive) in traversal order, 0-based.
    // -longkeys [opt default=false, unless name.endsWith("_id")]
    // -pvals    [opt default=false] values are publishables instead of strings
    // -time     [opt default=false] traverse time stamps
    // -reverse  [opt default=false] reverse traversal
    // -stats    [opt default=false] just show stats
    // -query=   [opt] key to query for
    // -output=  [opt] path to a (different) bdb to which the range will be dumped (way of creating a subset db)

    final Options options = new Options();

    // define options
    options.addOption(OptionBuilder.withArgName("path").withLongOpt("dbPath").hasArg().isRequired(true).create('d'));
    options.addOption(OptionBuilder.withArgName("name").withLongOpt("dbName").hasArg().isRequired(false).create('n'));
    options.addOption(OptionBuilder.withArgName("range").withLongOpt("indexRange").hasArg().isRequired(false).create('i'));
    options.addOption(OptionBuilder.withArgName("long").withLongOpt("longKeys").isRequired(false).create('l'));
    options.addOption(OptionBuilder.withArgName("pval").withLongOpt("publishableValues").isRequired(false).create('p'));
    options.addOption(OptionBuilder.withArgName("time").withLongOpt("byTimestamp").isRequired(false).create('t'));
    options.addOption(OptionBuilder.withArgName("reverse").withLongOpt("reverseOrder").isRequired(false).create('r'));
    options.addOption(OptionBuilder.withArgName("stats").withLongOpt("showStats").isRequired(false).create('s'));
    options.addOption(OptionBuilder.withArgName("metrics").withLongOpt("showMetrics").isRequired(false).create('m'));
    options.addOption(OptionBuilder.withArgName("query").withLongOpt("queryKey").hasArg().isRequired(false).create('q'));
    options.addOption(OptionBuilder.withArgName("output").withLongOpt("outputDB").hasArg().isRequired(false).create('o'));

    // initialize parameters
    final CommandLineParser parser = new PosixParser();
    final CommandLine commandLine = parser.parse(options, args);

    // get args
    final String dbPath = commandLine.getOptionValue('d');
    final String dbName = commandLine.getOptionValue('n');
    final String indexRange = commandLine.getOptionValue('i');
    final boolean longKeys = commandLine.hasOption('l');
    final boolean publishableValues = commandLine.hasOption('p');
    final boolean byTimestamp = commandLine.hasOption('t');
    final boolean reverse = commandLine.hasOption('r');
    final boolean stats = commandLine.hasOption('s');
    final boolean metrics = commandLine.hasOption('m');
    final String queryKey = commandLine.getOptionValue('q');
    final String outputDB = commandLine.getOptionValue('o');

    // compute args
    final File envLocation = new File(dbPath);
    final String[] dbNames = (dbName == null) ? null : dbName.split("\\s*,\\s*");
    final NumericRange range = (indexRange == null) ? null : new IntegerRange(indexRange);
    File outputDbPath = null;
    
    if (outputDB != null) {
      if (range == null) {
        throw new IllegalStateException("Must define a range (-i) when specifying an output DB (-o) ... otherwise, just copy the directory!");
      }
      outputDbPath = new File(outputDB);

      if (outputDbPath.equals(dbPath)) {
        throw new IllegalStateException("Can't send output to database being read! output=" + outputDbPath + " reading=" + envLocation);
      }
    }

    final DbDump dbDump = new DbDump(BerkeleyDb.getInstance(envLocation, true), dbNames, range, longKeys, publishableValues, byTimestamp, reverse, outputDbPath);

    if (stats || outputDbPath != null) {
      dbDump.stats(System.out);
      if (metrics) {
        System.out.print("Computing metrics...");
        dbDump.dump(System.out, metrics);
      }
    }

    if (!stats && (queryKey == null || outputDbPath != null)) {
      dbDump.dump(System.out, metrics);
    }

    if (queryKey != null) {
      final List<QueryResult> queryResults = dbDump.query(queryKey);

      if (queryResults.size() > 0) {
        int index = 0;
        for (QueryResult queryResult : queryResults) {
          queryResult.setIndex(index++);
          System.out.println(queryResult);
        }
      }
      else {
        System.err.println("No results for '" + queryKey + "'!");
      }
    }
  }
}
