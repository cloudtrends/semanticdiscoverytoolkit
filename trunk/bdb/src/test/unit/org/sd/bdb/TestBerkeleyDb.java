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


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.io.FileUtil;
import org.sd.util.thread.BlockingThreadPool;
import org.sd.util.thread.HookedRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * JUnit Tests for the BerkeleyDb class.
 * <p>
 * @author Spence Koehler
 */
public class TestBerkeleyDb extends TestCase {

  public TestBerkeleyDb(String name) {
    super(name);
  }
  
  /**
   * Auxiliary to create a database and fill it with some data.
   */
  private final DbHandle createDb(File dbDir, boolean readOnly, String dbName, boolean timestamped, String dbMarkerId, String[][] data) {
    dbDir.mkdirs();
    BerkeleyDb bdb = null;

    bdb = BerkeleyDb.getInstance(dbDir, readOnly);
    final DbHandle result = bdb.get(dbName, timestamped, dbMarkerId);

    // fill the database
    for (String[] kvPair : data) {
      result.put(kvPair[0], new DbValue(kvPair[1]));

      if (timestamped) {
        try {
          Thread.sleep(1);  // wait a millisecond to differentiate the values by time.
        }
        catch (InterruptedException e) {
          // ignore.
        }
      }
    }

    return result;
  }

  public void testSimpleDb() {
    final String[][] data = new String[][] {
      {"foo", "bar"},
      {"bar", "baz"},
      {"baz", "foo"},
      {"123", "456"},
    };

    final String filename = FileUtil.getFilename("/tmp/TestBerkeleyDb", "bdb/testSimpleDb");
    final File dbDir = new File(filename);

    DbHandle dbHandle = null;

    try {
      dbHandle = createDb(dbDir, false, "foo", false, null, data);

      // peek at entries
      StringKeyValuePair pPair = null;

      pPair = dbHandle.peekFirst();
      assertEquals("123", pPair.getKey());
      assertEquals("456", pPair.getValue());

      pPair = dbHandle.peekLast();
      assertEquals("foo", pPair.getKey());
      assertEquals("bar", pPair.getValue());

      // retrieve
      for (String[] kvPair : data) {
        final DbValue value = dbHandle.get(kvPair[0]);
        assertEquals(kvPair[1], value.getValue());
      }
    }
    finally {
      if (dbHandle != null) dbHandle.getDbInfo().getBerkeleyDb().close();

      // clean up junk left behind on the disk
      FileUtil.deleteDir(dbDir);
    }
  }

  public void testTimestampedQueueDb() {
    final String[][] data = new String[][] {
      {"foo", "bar"},
      {"bar", "baz"},
      {"baz", "foo"},
      {"123", "456"},
    };

    final String filename = FileUtil.getFilename("/tmp/TestBerkeleyDb", "bdb/testTimestampedQueueDb");
    final File dbDir = new File(filename);

    DbHandle dbHandle = null;

    try {
      dbHandle = createDb(dbDir, false, "foo", true, null, data);

      // make sure everything is there as expected.
      for (String[] kvPair : data) {
        final DbValue value = dbHandle.get(kvPair[0]);
        assertEquals(kvPair[1], value.getValue());
      }

      StringKeyValuePair pPair = null;

      // peek at timestamped entries.
      pPair = dbHandle.peekLatest();
      assertEquals("123", pPair.getKey());
      assertEquals("456", pPair.getValue());

      pPair = dbHandle.peekEarliest();
      assertEquals("foo", pPair.getKey());
      assertEquals("bar", pPair.getValue());

      // pop 'em all FIFO.
      for (String[] kvPair : data) {
        pPair = dbHandle.popEarliest();
        assertEquals(kvPair[0], pPair.getKey());
        assertEquals(kvPair[1], pPair.getValue());
      }
    }
    finally {
      if (dbHandle != null) dbHandle.getDbInfo().getBerkeleyDb().close();

      // clean up junk left behind on the disk
      FileUtil.deleteDir(dbDir);
    }
  }

  /**
   * Auxiliary to create a database and fill it with some data.
   */
  private final DbHandle createDb(File dbDir, boolean readOnly, String dbName, boolean timestamped, String dbMarkerId, long[] keys, String[] values) {
    dbDir.mkdirs();
    BerkeleyDb bdb = null;

    bdb = BerkeleyDb.getInstance(dbDir, readOnly);
    final DbHandle result = bdb.get(dbName, timestamped, dbMarkerId);
    
    // fill the database
    for (int i = 0; i < keys.length; ++i) {
      final long key = keys[i];
      final String value = values[i];

      result.put(key, new DbValue(value));

      if (timestamped) {
        try {
          Thread.sleep(1);  // wait a millisecond to differentiate the values by time.
        }
        catch (InterruptedException e) {
          // ignore.
        }
      }
    }

    return result;
  }

  public void testSimpleDbWithLongKeys() {
    final long[] keys = new long[] {
      314L,
      159L,
      265L,
      358L,
    };
    final String[] values = new String[] {
      "bar",
      "baz",
      "foo",
      "456",
    };

    final String filename = FileUtil.getFilename("/tmp/TestBerkeleyDb", "bdb/testSimpleDbWithLongKeys");
    final File dbDir = new File(filename);

    DbHandle dbHandle = null;

    try {
      dbHandle = createDb(dbDir, false, "foo", false, null, keys, values);

      // retrieve
      for (int i = 0; i < keys.length; ++i) {
        final long key = keys[i];
        final String expectedValue = values[i];

        final DbValue value = dbHandle.get(key);
        assertEquals(expectedValue, value.getValue());
      }
    }
    finally {
      if (dbHandle != null) dbHandle.getDbInfo().getBerkeleyDb().close();

      // clean up junk left behind on the disk
      FileUtil.deleteDir(dbDir);
    }
  }

  public void testTimestampedQueueDbWithLongKeys() {
    final long[] keys = new long[] {
      314L,
      159L,
      265L,
      358L,
    };
    final String[] values = new String[] {
      "bar",
      "baz",
      "foo",
      "456",
    };

    final String filename = FileUtil.getFilename("/tmp/TestBerkeleyDb", "bdb/testTimestampedQueueDbWithLongKeys");
    final File dbDir = new File(filename);

    DbHandle dbHandle = null;

    try {
      dbHandle = createDb(dbDir, false, "foo", true, null, keys, values);

      // make sure everything is there as expected.
      for (int i = 0; i < keys.length; ++i) {
        final long key = keys[i];
        final String expectedValue = values[i];

        final DbValue value = dbHandle.get(key);
        assertEquals(expectedValue, value.getValue());
      }

      LongKeyValuePair pPair = null;

      // peek at timestamped entries.
      pPair = dbHandle.peekLatestLong();
      assertEquals(358L, pPair.getKey());
      assertEquals("456", pPair.getValue());

      pPair = dbHandle.peekEarliestLong();
      assertEquals(314L, pPair.getKey());
      assertEquals("bar", pPair.getValue());

      // peek at non-timestamped entries
      pPair = dbHandle.peekFirstLong();
      assertEquals(159L, pPair.getKey());
      assertEquals("baz", pPair.getValue());

      pPair = dbHandle.peekLastLong();
      assertEquals(358L, pPair.getKey());
      assertEquals("456", pPair.getValue());

      // test updating and preserving the original timestamp
      final DbValue dbValue = dbHandle.get(keys[0]);
      dbHandle.update(keys[0], new DbValue("*new!*"), dbValue.getTimestamp());
      pPair = dbHandle.peekEarliestLong();
      assertEquals(keys[0], pPair.getKey());
      assertEquals("*new!*", pPair.getValue());
      values[0] = "*new!*";

      // pop 'em all FIFO.
      for (int i = 0; i < keys.length; ++i) {
        final long key = keys[i];
        final String expectedValue = values[i];

        pPair = dbHandle.popEarliestLong();
        assertEquals(key, pPair.getKey());
        assertEquals(expectedValue, pPair.getValue());
      }
    }
    finally {
      if (dbHandle != null) dbHandle.getDbInfo().getBerkeleyDb().close();

      // clean up junk left behind on the disk
      FileUtil.deleteDir(dbDir);
    }
  }

  public void testIteratorSeek() {
    final String[][] data = new String[][] {
      {"foo", "bar"},
      {"bar", "baz"},
      {"baz", "foo"},
      {"123", "456"},
    };

    final String filename = FileUtil.getFilename("/tmp/TestBerkeleyDb", "bdb/testIteratorSeek");
    final File dbDir = new File(filename);

    DbHandle dbHandle = null;
    DbIterator<StringKeyValuePair> iter = null;

    try {
      dbHandle = createDb(dbDir, false, "foo", false, null, data);

      iter = dbHandle.iterator(DbTraversal.KEY_ORDER);

      StringKeyValuePair pPair = null;

      // peek at first entry
      pPair = iter.next();
      assertEquals("123", pPair.getKey());
      assertEquals("456", pPair.getValue());
      assertTrue(iter.hasNext());

      // seek non-existent
      pPair = iter.seek("abcd");
      assertNull(pPair);

      // make sure next is second entry
      pPair = iter.next();
      assertEquals("bar", pPair.getKey());
      assertEquals("baz", pPair.getValue());

      // seek to last entry
      pPair = iter.seek("foo");
      assertEquals("foo", pPair.getKey());
      assertEquals("bar", pPair.getValue());

      // make sure there is no next
      assertTrue(!iter.hasNext() || iter.next() == null);

      // seek back to first entry
      pPair = iter.seek("123");

      int count =0;
      while (iter.hasNext()) {
        pPair = iter.next();
        if (pPair != null) {
          ++count;
//          System.out.println(pPair.getKey() + "," + pPair.getValue() );
        }
      }

      // make sure we visited remaining 3 entries
      assertEquals(3, count);
    }
    finally {
      if (iter != null) iter.close();
      if (dbHandle != null) dbHandle.getDbInfo().getBerkeleyDb().close();

      // clean up junk left behind on the disk
      FileUtil.deleteDir(dbDir);
    }
  }

  public void testMultiThreadedIteration() {
    final String filename = FileUtil.getFilename("/tmp/TestBerkeleyDb", "bdb/testMultiThreadedIteration");
    final File dbDir = new File(filename);

    final int num = 1000;
    final long[] keys = new long[num];
    final String[] values = new String[num];
    for (int i = 0; i < num; ++i) {
      keys[i] = (long)i;
      values[i] = Integer.toString(i);
    }

    DbHandle dbHandle = null;
    DbIterator<LongKeyValuePair> iter = null;
    BlockingThreadPool threadPool = null;

    final List<String> gotValues = new ArrayList<String>();

    try {
      dbHandle = createDb(dbDir, false, "multi", false, null, keys, values);

      iter = dbHandle.iteratorLong(DbTraversal.KEY_ORDER);

      threadPool = new BlockingThreadPool("testMultiThreadedIteration", 20, 1, 1);

      for (int i = 0; i < 20; ++i) {
        threadPool.add(new IterThread<LongKeyValuePair>(iter, gotValues), 5, TimeUnit.SECONDS);
      }

      // wait for threads to finish iterating
      while (iter.hasNext()) {}
    }
    finally {
      if (threadPool != null) threadPool.shutdown(false);
      if (iter != null) iter.close();
      if (dbHandle != null) dbHandle.getDbInfo().getBerkeleyDb().close();

      // clean up junk left behind on the disk
      FileUtil.deleteDir(dbDir);
    }

    assertEquals(num, gotValues.size());
    final Set<String> uniqueValues = new HashSet<String>(gotValues);
    assertEquals(num, uniqueValues.size());
  }

  public void testMarkerKeyOrder() throws IOException, InterruptedException{
    String[][] data = new String[136][2];
    for(int i = 0; i < 136; i++){
      data[i][0] = String.format("%03d",i);
      data[i][1] = String.valueOf(i);
    }

		final String testRoot = "/tmp/TestBerkeleyDb";
		new File(testRoot).delete();  // clean up
    final String filename = FileUtil.getFilename(testRoot, "bdb/testMarker");
    final File dbDir = new File(filename);

    DbHandle dbHandle = null;
    DbIterator<StringKeyValuePair> iter = null;

    try {
      dbHandle = createDb(dbDir, false, "keyOrder", false, "TestDbMarker", data);

      iter = dbHandle.iterator(DbTraversal.KEY_ORDER);
      for(int i = 0; i < 41; i++){
        iter.next();
      }
      iter.close();

      // delay to allow final elements to flush
      Thread.sleep(100);
      assertEquals(FileUtil.countLines(testRoot + "/bdb/testMarker/TestDbMarker.marker"),41);

      // test restart and roll forward
      iter = dbHandle.iterator(DbTraversal.KEY_ORDER);
      while(iter.hasNext()){
        iter.next();
      }
      iter.close();

      // delay to allow final elements to flush
      Thread.sleep(100);

      // check for backup file and bufferSize
      assertTrue(new File(testRoot + "/bdb/testMarker/TestDbMarker.marker").exists());
      assertEquals(FileUtil.countLines(testRoot + "/bdb/testMarker/TestDbMarker.marker"),136);

      // verify contents
      ArrayList<String> markerLines = FileUtil.readLines(testRoot + "/bdb/testMarker/TestDbMarker.marker");
      for(int i = 0; i < markerLines.size(); i++){
        String line = markerLines.get(i);
        assertEquals(i + ":" + line, data[i][0], line);
      }

      // test iterator is finished
      iter = dbHandle.iterator(DbTraversal.KEY_ORDER);
      iter.next();
      assertFalse(iter.hasNext());
      iter.close();

      // test reverse order
      iter = dbHandle.iterator(DbTraversal.REVERSE_KEY_ORDER);
      for(int i = 0; i < 30; i++){
        iter.next();
      }
      iter.close();

      // delay to allow final elements to flush
      Thread.sleep(100);
      // check for backup file and bufferSize
      assertTrue(new File(testRoot + "/bdb/testMarker/TestDbMarker.marker").exists());
      assertEquals(FileUtil.countLines(testRoot + "/bdb/testMarker/TestDbMarker.marker"),166);

      // verify contents
      ArrayList<String> reverseMarkerLines = FileUtil.readLines(testRoot + "/bdb/testMarker/TestDbMarker.marker");
      int dataIndex = 0;
      for(int i = 0; i < 136; i++){
        String line = reverseMarkerLines.get(i);
        assertEquals(i + ":" + line, data[dataIndex][0], line);
        dataIndex++;
      }
      // first position before last recorded position
      dataIndex = 134;
      for(int i = 136; i < reverseMarkerLines.size(); i++){
        String line = reverseMarkerLines.get(i);
        assertEquals(i + ":" + line, data[dataIndex][0], line);
        dataIndex--;
      }

      // test roll
      iter = dbHandle.iterator(DbTraversal.REVERSE_KEY_ORDER);
      while(iter.hasNext()){
        iter.next();
      }
      iter.close();

      // delay to allow final elements to flush
      Thread.sleep(100);
      // check for backup file and bufferSize
      assertTrue(new File(testRoot + "/bdb/testMarker/TestDbMarker.marker").exists());
      assertEquals(FileUtil.countLines(testRoot + "/bdb/testMarker/TestDbMarker.marker"),271);
    }
    finally {
      if (iter != null) iter.close();
      // delay to allow final elements to flush
      Thread.sleep(100);
      if (dbHandle != null) dbHandle.getDbInfo().getBerkeleyDb().close();

      // clean up junk left behind on the disk
      FileUtil.deleteDir(dbDir);
    }
  }

  public void testMarkerLongKeyOrder() throws IOException, InterruptedException {
    long[] keys = new long[136];
    String[] values = new String[136];
    for(int i = 0; i < 136; i++){
      keys[i] = (long)i;
      values[i] = String.valueOf(i);
    }

		final String testRoot = "/tmp/TestBerkeleyDb";
    final String filename = FileUtil.getFilename(testRoot, "bdb/testMarker");
    final File dbDir = new File(filename);

    DbHandle dbHandle = null;
    DbIterator<LongKeyValuePair> iter = null;

    try {
      dbHandle = createDb(dbDir, false, "keyOrder_id", false, "TestDbMarker", keys, values);

      iter = dbHandle.iteratorLong(DbTraversal.KEY_ORDER);
      for(int i = 0; i < 41; i++){
        iter.next();
      }
      iter.close();

      // delay to allow final elements to flush
      //Thread.sleep(100);
      //assertEquals(FileUtil.countLines(testRoot + "/bdb/testMarker/TestDbMarker.marker"),41);
      final String checkfile = testRoot + "/bdb/testMarker/TestDbMarker.marker";
      assertTrue(waitFor(1000, 100, new BooleanFunction() {
          public boolean getValue() {
            boolean result = false;
            try {
              result = (FileUtil.countLines(checkfile) == 41);
            }
            catch (IOException e) {}
            return result;
          }
        }));

      // test restart and roll forward
      iter = dbHandle.iteratorLong(DbTraversal.KEY_ORDER);
      while(iter.hasNext()){
        iter.next();
      }
      iter.close();

      // delay to allow final elements to flush
      Thread.sleep(100);

      // check for backup file and bufferSize
      assertTrue(new File(testRoot + "/bdb/testMarker/TestDbMarker.marker").exists());

      //assertEquals(FileUtil.countLines(testRoot + "/bdb/testMarker/TestDbMarker.marker"),136);
      //final String checkfile = testRoot + "/bdb/testMarker/TestDbMarker.marker";
      assertTrue(waitFor(1000, 100, new BooleanFunction() {
          public boolean getValue() {
            boolean result = false;
            try {
              result = (FileUtil.countLines(checkfile) == 136);
            }
            catch (IOException e) {}
            return result;
          }
        }));

      // verify contents
      ArrayList<String> markerLines = FileUtil.readLines(testRoot + "/bdb/testMarker/TestDbMarker.marker");
      for(int i = 0; i < markerLines.size(); i++){
        String line = markerLines.get(i);
        assertEquals(i + ":" + line, values[i], line);
      }

      // test iterator is finished
      iter = dbHandle.iteratorLong(DbTraversal.KEY_ORDER);
      iter.next();
      assertFalse(iter.hasNext());
      iter.close();

      // test reverse order
      iter = dbHandle.iteratorLong(DbTraversal.REVERSE_KEY_ORDER);
      for(int i = 0; i < 30; i++){
        iter.next();
      }
      iter.close();

      // delay to allow final elements to flush
      Thread.sleep(100);
      // check for backup file and bufferSize
      assertTrue(new File(testRoot + "/bdb/testMarker/TestDbMarker.marker").exists());
      assertEquals(FileUtil.countLines(testRoot + "/bdb/testMarker/TestDbMarker.marker"),166);

      // verify contents
      ArrayList<String> reverseMarkerLines = FileUtil.readLines(testRoot + "/bdb/testMarker/TestDbMarker.marker");
      int dataIndex = 0;
      for(int i = 0; i < 136; i++){
        String line = reverseMarkerLines.get(i);
        assertEquals(i + ":" + line, values[dataIndex], line);
        dataIndex++;
      }
      // first position before last recorded position
      dataIndex = 134;
      for(int i = 136; i < reverseMarkerLines.size(); i++){
        String line = reverseMarkerLines.get(i);
        assertEquals(i + ":" + line, values[dataIndex], line);
        dataIndex--;
      }

      // test roll
      iter = dbHandle.iteratorLong(DbTraversal.REVERSE_KEY_ORDER);
      while(iter.hasNext()){
        iter.next();
      }
      iter.close();

      // delay to allow final elements to flush
      Thread.sleep(100);
      // check for backup file and bufferSize
      assertTrue(new File(testRoot + "/bdb/testMarker/TestDbMarker.marker").exists());
      assertEquals(FileUtil.countLines(testRoot + "/bdb/testMarker/TestDbMarker.marker"),271);
    }
    finally {
      if (iter != null) iter.close();
      if (dbHandle != null) dbHandle.getDbInfo().getBerkeleyDb().close();

      // clean up junk left behind on the disk
      FileUtil.deleteDir(dbDir);
    }
  }

  public void testMarkerBuffer() throws IOException, InterruptedException {
    long[] keys = new long[1136];
    String[] values = new String[1136];
    for(int i = 0; i < 1136; i++){
      keys[i] = (long)i;
      values[i] = String.valueOf(i);
    }

		final String testRoot = "/tmp/TestBerkeleyDb";
    final String filename = FileUtil.getFilename(testRoot, "bdb/testMarker");
    final File dbDir = new File(filename);

    DbHandle dbHandle = null;
    DbIterator<LongKeyValuePair> iter = null;

    try {
      dbHandle = createDb(dbDir, false, "keyOrder_id", false, "TestDbMarker", keys, values);

      iter = dbHandle.iteratorLong(DbTraversal.KEY_ORDER);
      while(iter.hasNext()){
        iter.next();
      }
      iter.close();

      // delay to allow final elements to flush
      Thread.sleep(100);

      // check for backup file and bufferSize
      assertTrue(new File(testRoot + "/bdb/testMarker/TestDbMarker.marker").exists());
      assertTrue(new File(testRoot + "/bdb/testMarker/TestDbMarker.marker.bup").exists());
      assertEquals(FileUtil.countLines(testRoot + "/bdb/testMarker/TestDbMarker.marker"),136);
      assertEquals(FileUtil.countLines(testRoot + "/bdb/testMarker/TestDbMarker.marker.bup"),1000);

      // verify contents
      ArrayList<String> markerLinesBup = FileUtil.readLines(testRoot + "/bdb/testMarker/TestDbMarker.marker.bup");
      int dataIndex = 0;
      for(int i = 0; i < markerLinesBup.size(); i++){
        String line = markerLinesBup.get(i);
        assertEquals(i + ":" + line, values[dataIndex], line);
        dataIndex++;
      }
      ArrayList<String> markerLines = FileUtil.readLines(testRoot + "/bdb/testMarker/TestDbMarker.marker");
      for(int i = 0; i < markerLines.size(); i++){
        String line = markerLines.get(i);
        assertEquals(i + ":" + line, values[dataIndex], line);
        dataIndex++;
      }
    }
    finally {
      if (iter != null) iter.close();
      if (dbHandle != null) dbHandle.getDbInfo().getBerkeleyDb().close();

      // clean up junk left behind on the disk
      FileUtil.deleteDir(dbDir);
    }
  }

  private static final class IterThread <T> implements HookedRunnable {
    
    private final DbIterator<T> iter;
    private final Collection<String> values;

    IterThread(DbIterator<T> iter, Collection<String> values) {
      this.iter = iter;
      this.values = values;
    }

    public void run() {
      while (iter.hasNext()) {
        final DbValue dbValue = (DbValue)iter.next();
        if (dbValue != null) {
          values.add(dbValue.getValue());
        }
      }
    }

    public void preRunHook() {}
    public void postRunHook() {}
    public void exceptionHook(Throwable t) {}
    public void die() {}
  }

//todo: test push and iterators


  private final boolean waitFor(long maxWait, long checkInterval, BooleanFunction fn) {

    final long starttime = System.currentTimeMillis();
    final long exptime = maxWait + starttime;
    try {
      while (!fn.getValue() && System.currentTimeMillis() < exptime) {
        Thread.sleep(checkInterval);
      }
    }
    catch (InterruptedException e) {
    }

    return fn.getValue();
  }

  private static interface BooleanFunction {
    public boolean getValue();
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestBerkeleyDb.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
