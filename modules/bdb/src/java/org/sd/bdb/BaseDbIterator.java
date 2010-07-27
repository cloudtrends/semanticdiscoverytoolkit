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


import org.sd.io.FileUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryCursor;
import com.sleepycat.je.SecondaryDatabase;

/**
 * Base implementation of a db iterator.
 * <p>
 * @author Spence Koehler
 */
public abstract class BaseDbIterator<T> implements DbIterator<T> {

  public static final int DEFAULT_QUEUE_SIZE = 1;
  public static final int MARKER_BUFFER = 1000;

  protected abstract T doInitFirst(DbInfo dbInfo, boolean forward) throws DatabaseException;

  protected abstract T doGetNext(DbInfo dbInfo, boolean forward) throws DatabaseException;

  protected abstract boolean doWriteKeyToMarker(T kvPair);

  protected abstract Cursor getCursor();

  /** Return a value that represents the empty marker. */
  protected abstract T getEmptyMarker();

  /** Test whether the element is the empty marker. */
  protected abstract boolean isEmptyMarker(T marker);

  protected final DbMarker marker;

  private final DbInfo dbInfo;
  private boolean forward;
  private LinkedBlockingQueue<DbValue> seekQueue;
  private LinkedBlockingQueue<T> nextQueue;
  private int queueSize;
  private ExecutorService cursorThread;
  private CursorRunner cursorRunner;

  private SeekData seekData;
  private final Object seekDataMutex = new Object();

  protected BaseDbIterator(DbInfo dbInfo, boolean forward, String dbMarkerId) {
    this(dbInfo, forward, DEFAULT_QUEUE_SIZE, dbMarkerId);
  }

  protected BaseDbIterator(final DbInfo dbInfo, boolean forward, int queueSize, String dbMarkerId) {
    this.dbInfo = dbInfo;
    this.forward = forward;
    this.seekQueue = new LinkedBlockingQueue<DbValue>();
    this.nextQueue = new LinkedBlockingQueue<T>();
    this.queueSize = queueSize;

    if(dbMarkerId != null && !"".equals(dbMarkerId)) { 
      DbMarker dbMarker = null;
      try {
        dbMarker = new DbMarker(dbInfo, dbMarkerId, MARKER_BUFFER);
      }
      catch(IOException ioe){
        System.err.println("Unable to create marker with dbInfo=" + dbInfo + "!: " + ioe.getMessage());
      }
      finally {
        this.marker = dbMarker;
      }
    }
    else {
      this.marker = null;
    }

    this.cursorThread =
      Executors.newSingleThreadExecutor(
        new ThreadFactory() {
          public Thread newThread(Runnable r) {
            return new Thread(r, dbInfo.getDbName() + "-CursorThread");
          }
        });
    this.cursorRunner = new CursorRunner<T>(this);
    this.cursorThread.execute(cursorRunner);
  }

  public OperationStatus rollToMarker(long lastKey){
    final Cursor cursor = getCursor();
    if (cursor != null) {
      final DatabaseEntry keyEntry = dbInfo.getKeyEntry(lastKey);
      final DatabaseEntry value = new DatabaseEntry();

      try {
        final OperationStatus opStatus = cursor.getSearchKey(keyEntry, value, null);
        if (opStatus == OperationStatus.SUCCESS) {
          System.out.println("Iterator has rolled forward to long key: " + lastKey);
          increment(true);  // reset cursor so next will follow this entry.
        }
        else {
          System.err.println("Iterator failed to find long key: " + lastKey);
        }

        return opStatus;
      }
      catch (DatabaseException de) {
//        de.printStackTrace(System.err);
        throw new IllegalStateException(de);
      }
    }
    return null;
  }

  public OperationStatus rollToMarker(String lastKey){
    final Cursor cursor = getCursor();
    if (cursor != null) {
      final DatabaseEntry keyEntry = dbInfo.getKeyEntry(lastKey);
      final DatabaseEntry value = new DatabaseEntry();

      try {
        final OperationStatus opStatus = cursor.getSearchKey(keyEntry, value, null);
        if (opStatus == OperationStatus.SUCCESS) {
          System.out.println("Iterator has rolled forward to string key: " + lastKey);
          increment(true);  // reset cursor so next will follow this entry.
        }
        else {
          System.err.println("Iterator failed to find string key: " + lastKey);
        }

        return opStatus;
      }
      catch (DatabaseException de) {
//        de.printStackTrace(System.err);
        throw new IllegalStateException(de);
      }
    }
    return null;
  }

  public boolean hasNext() {
    return nextQueue.size() > 0 || cursorRunner.hasNext();
  }

  public T next() {
    T result = null;

    if (hasNext()) {
      try {
        result = nextQueue.poll(5, TimeUnit.SECONDS);

        // recognize and discard the empty marker
        if (result != null && isEmptyMarker(result)) {
          result = null;
        }
      }
      catch(InterruptedException inte){
        System.err.println("Seek queue interrupted while waiting for poll data: " + inte.getMessage());
      }
    }

    // write to marker
    if(this.marker != null){
      doWriteKeyToMarker(result);
    }

    return result;
  }

  public void remove() {
    throw new UnsupportedOperationException("Can't remove with a DbIterator!");
  }

  public final void close() {
//seem to intermittently get cursor closed exceptions while iterating. uncomment the following to debug.
//new RuntimeException("closing!").printStackTrace(System.err);
    cursorRunner.close();
    cursorThread.shutdownNow();
    if(marker != null) marker.close();
  }

  /**
   * Move the cursor to the given key such that next() will return
   * the next record after the first with the key.
   * <p>
   * NOTE: Current implementation assumes we seek in a single thread
   *       before allowing access/iteration by multiple threads.
   *
   * @return the matching key value pair if the key was found and
   *         the iterator is positioned at its record such that next()
   *         will return the following record; otherwise, return null
   *         and leave the position of the cursor unchanged.
   */
  public LongKeyValuePair seek(long key) {
    return seek(key, null);
  }

  public LongKeyValuePair seek(long key, DbValue value) {
    LongKeyValuePair result = null;

    if (!cursorRunner.isAlive()) return result;

    synchronized (seekDataMutex) {
      this.seekData = new SeekData(key, null, value);
    }
    
    try {
      final DbValue dbValue = seekQueue.poll(5, TimeUnit.SECONDS);
      if (dbValue != null && dbValue != DbValue.EMPTY) {
        result = (LongKeyValuePair)dbValue;
      }
    }
    catch(InterruptedException inte){
      System.err.println("Seek queue interrupted while waiting for poll data: " + inte.getMessage());
    }

    return result;
  }

  public StringKeyValuePair seek(String key) {
    return seek(key, null);
  }

  public StringKeyValuePair seek(String key, DbValue value) {
    StringKeyValuePair result = null;

    if (!cursorRunner.isAlive()) return result;

    synchronized (seekDataMutex) {
      this.seekData = new SeekData(null, key, value);
    }

    try {
      final DbValue dbValue = seekQueue.poll(5, TimeUnit.SECONDS);
      if (dbValue != null && dbValue != DbValue.EMPTY) {
        result = (StringKeyValuePair)dbValue;
      }
    }
    catch(InterruptedException inte){
      System.err.println("Seek queue interrupted while waiting for poll data: " + inte.getMessage());
    }

    return result;
  }

  private boolean hasSeekData() {
    return (seekData != null);
  }

  private final void doSeek() {
    if (seekData != null) {
      synchronized (seekDataMutex) {
        if (seekData.seekLongKey != null) {
          if (seekData.seekValue != null) {
            doSeekLong(seekData.seekLongKey, seekData.seekValue);
          }
          else {
            doSeekLong(seekData.seekLongKey);
          }
        }
        else if (seekData.seekStringKey != null) {
          if (seekData.seekValue != null) {
            doSeekString(seekData.seekStringKey, seekData.seekValue);
          }
          else {
            doSeekString(seekData.seekStringKey);
          }
        }
        this.seekData = null;
      }
    }
  }

  private final void doSeekLong(long seekKey) {
    final Cursor cursor = getCursor();
    if (cursor != null) {
      final DatabaseEntry keyEntry = dbInfo.getKeyEntry(seekKey);
      final DatabaseEntry value = new DatabaseEntry();

      try {
        final OperationStatus opStatus = cursor.getSearchKey(keyEntry, value, null);
        if (opStatus == OperationStatus.SUCCESS) {
          final DbValue dbValue = dbInfo.getDbValue(value);
          final LongKeyValuePair result = new LongKeyValuePair(seekKey, dbValue.getValueBytes(), dbValue.getTimestamp());
          seekQueue.add(result); //todo: change this to a key->value map if multiple seeker threads are expected.
          increment(true);  // reset cursor so next will follow this entry.
        }
        else {
          seekQueue.add(DbValue.EMPTY);
        }
      }
      catch (DatabaseException de) {
//        de.printStackTrace(System.err);
        throw new IllegalStateException(de);
      }
    }
  }

  /**
   * Move the cursor to the given key/value pair such that next() will
   * return the next record after the first with the key and value.
   *
   * @return the matching key value pair if the key was found and
   *         the iterator is positioned at its record such that next()
   *         will return the following record; otherwise, return null
   *         and leave the position of the cursor unchanged.
   */
  private final void doSeekLong(long seekKey, DbValue value) {
    final Cursor cursor = getCursor();
    if (cursor != null) {
      final DatabaseEntry keyEntry = dbInfo.getKeyEntry(seekKey);
      final DatabaseEntry data = dbInfo.getValueEntry(value);

      try {
        final OperationStatus opStatus = cursor.getSearchBoth(keyEntry, data, null);
        if (opStatus == OperationStatus.SUCCESS) {
          final LongKeyValuePair result = new LongKeyValuePair(seekKey, value.getValueBytes(), value.getTimestamp());
          seekQueue.add(result); //todo: change this to a key->value map if multiple seeker threads are expected.
          increment(true);  // reset cursor so next will follow this entry.
        }
        else {
          seekQueue.add(DbValue.EMPTY);
        }
      }
      catch (DatabaseException de) {
//        de.printStackTrace(System.err);
        throw new IllegalStateException(de);
      }
    }
  }

  /**
   * Move the cursor to the given key such that next() will return
   * the next record after the first with the key.
   *
   * @return the matching key value pair if the key was found and
   *         the iterator is positioned at its record such that next()
   *         will return the following record; otherwise, return null
   *         and leave the position of the cursor unchanged.
   */
  private final void doSeekString(String seekKey) {
    final Cursor cursor = getCursor();
    if (cursor != null) {
      final DatabaseEntry keyEntry = dbInfo.getKeyEntry(seekKey);
      final DatabaseEntry value = new DatabaseEntry();

      try {
        final OperationStatus opStatus = cursor.getSearchKey(keyEntry, value, null);
        if (opStatus == OperationStatus.SUCCESS) {
          final DbValue dbValue = dbInfo.getDbValue(value);
          final StringKeyValuePair result = new StringKeyValuePair(seekKey, dbValue.getValueBytes(), dbValue.getTimestamp());
          seekQueue.add(result); //todo: change this to a key->value map if multiple seeker threads are expected.
          increment(true);  // reset cursor so next will follow this entry.
        }
        else {
          seekQueue.add(DbValue.EMPTY);
        }
      }
      catch (DatabaseException de) {
//        de.printStackTrace(System.err);
        throw new IllegalStateException(de);
      }
    }
  }

  /**
   * Move the cursor to the given key/value pair such that next() will
   * return the next record after the first with the key and value.
   *
   * @return the matching key value pair if the key was found and
   *         the iterator is positioned at its record such that next()
   *         will return the following record; otherwise, return null
   *         and leave the position of the cursor unchanged.
   */
  private final void doSeekString(String seekKey, DbValue value) {
    final Cursor cursor = getCursor();
    if (cursor != null) {
      final DatabaseEntry keyEntry = dbInfo.getKeyEntry(seekKey);
      final DatabaseEntry data = dbInfo.getValueEntry(value);

      try {
        final OperationStatus opStatus = cursor.getSearchBoth(keyEntry, data, null);
        if (opStatus == OperationStatus.SUCCESS) {
          final StringKeyValuePair result = new StringKeyValuePair(seekKey, value.getValueBytes(), value.getTimestamp());
          seekQueue.add(result); //todo: change this to a key->value map if multiple seeker threads are expected.
          increment(true);  // reset cursor so next will follow this entry.
        }
        else {
          seekQueue.add(DbValue.EMPTY);
        }
      }
      catch (DatabaseException de) {
//        de.printStackTrace(System.err);
        throw new IllegalStateException(de);
      }
    }
  }


  private final boolean shouldIncrement(){
    return nextQueue.size() < queueSize && !dbInfo.isClosed();
  }

  private final void initFirst() {
    try {
      final T next = doInitFirst(dbInfo, forward);
      if (next != null) {
        nextQueue.add(next);
      }
    }
    catch (DatabaseException de) {
//      de.printStackTrace(System.err);
      throw new IllegalStateException(de);
    }
  }

  private final void increment(boolean reset) {
    if (reset) {
      this.seekData = null;
      this.nextQueue.clear();
    }

    try {
      final T next = doGetNext(dbInfo, forward);
      if (next != null) {
        nextQueue.add(next);
      }
      else {
        nextQueue.add(getEmptyMarker());
        cursorRunner.setHasNext(false);
      }
    }
    catch (DatabaseException de) {
//      de.printStackTrace(System.err);
      throw new IllegalStateException(de);
    }
  }

  protected final Cursor getPrimaryCursor(DbInfo dbInfo) {
    Cursor result = null;

    try {
      result = dbInfo.getDatabase().openCursor(null, null);
    }
    catch (DatabaseException de) {
//      de.printStackTrace(System.err);
      throw new IllegalStateException(de);
    }

    return result;
  }

  protected final SecondaryCursor getSecondaryCursor(DbInfo dbInfo) {
    SecondaryCursor result = null;

    final SecondaryDatabase timestampedDb = dbInfo.getTimestampedDb();

    if (timestampedDb == null) {
      throw new IllegalStateException("Can't traverse database in time order without a timestamped db!");
    }

    try {
      result = timestampedDb.openSecondaryCursor(null, null);
    }
    catch (DatabaseException de) {
//      de.printStackTrace(System.err);
      throw new IllegalStateException(de);
    }

    return result;
  }

  protected final StringKeyValuePair getNextStringPair(DbInfo dbInfo, DatabaseEntry key, DatabaseEntry value, OperationStatus opStatus) {
    StringKeyValuePair result = null;

    if (opStatus == OperationStatus.SUCCESS) {
      final DbValue dbValue = dbInfo.getDbValue(value);
      result = new StringKeyValuePair(dbInfo.getKeyString(key), dbValue.getValueBytes(), dbValue.getTimestamp());
    }

    return result;
  }

  protected final LongKeyValuePair getNextLongPair(DbInfo dbInfo, DatabaseEntry key, DatabaseEntry value, OperationStatus opStatus) {
    LongKeyValuePair result = null;

    if (opStatus == OperationStatus.SUCCESS) {
      final DbValue dbValue = dbInfo.getDbValue(value);
      result = new LongKeyValuePair(dbInfo.getKeyLong(key), dbValue.getValueBytes(), dbValue.getTimestamp());
    }

    return result;
  }

  private static final class SeekData {
    public final Long seekLongKey;
    public final String seekStringKey;
    public final DbValue seekValue;

    public SeekData(Long seekLongKey, String seekStringKey, DbValue seekValue) {
      this.seekLongKey = seekLongKey;
      this.seekStringKey = seekStringKey;
      this.seekValue = seekValue;
    }
  }

  private static final class CursorRunner <T> implements Runnable {
    private BaseDbIterator<T> dbIter;
    private final AtomicBoolean stayAlive = new AtomicBoolean(true);
    private final AtomicBoolean isOpen = new AtomicBoolean(false);
    private final AtomicBoolean hasNext = new AtomicBoolean(true);

    CursorRunner(BaseDbIterator<T> dbIter) {
      this.dbIter = dbIter;
    }

    public void run() {
      // initialze the cursor and put first element into the queue
      dbIter.initFirst();
      isOpen.set(true);

      // loop, adding an element to the queue if it is not full
      while (stayAlive.get()) {
        if (dbIter.hasSeekData()) {
          hasNext.set(true);
          dbIter.doSeek();
        }
        else if (hasNext.get() && dbIter.shouldIncrement()) {
          dbIter.increment(false);
        }
        else {
          if (dbIter.dbInfo.isClosed()) {
            dbIter.close();
            break;
          }
        }
      }

      stayAlive.set(false);
    }

    public boolean isAlive() {
      return stayAlive.get();
    }

    public void setHasNext(boolean hasNext) {
      this.hasNext.set(hasNext);
    }

    public boolean hasNext() {
      return hasNext.get();
    }

    public void close() {

      stayAlive.set(false);

      if (isOpen.compareAndSet(true, false)) {
        final Cursor cursor = dbIter.getCursor();
        if (cursor != null) {
          try {
            cursor.close();
          }
          catch (DatabaseException de) {
//            de.printStackTrace(System.err);
            throw new IllegalStateException(de);
          }
        }
      }
    }
  }

  protected class DbMarker {
    private static final String markerSuffix = "marker";

    private int count;
    private int bufferSize;
    private long lastLongKey;
    private String lastStringKey;

    private final String markerId;
    private File markerFile;
    private BufferedWriter markerBuffer;

    public DbMarker(DbInfo baseDbInfo, String markerId, int bufferSize) throws IOException{
      this.markerId = markerId;
      this.markerFile = new File(baseDbInfo.getBerkeleyDb().getEnvLocation(), markerId + "." + markerSuffix);

      this.bufferSize = (bufferSize < 1 ? 1: bufferSize);
      this.lastLongKey = -1L;
      this.lastStringKey = null;

      init(baseDbInfo.hasStringKeys());
    }

    public void init(boolean stringKeys) throws IOException{
//      System.out.println("Initializing marker file '" + markerFile.getAbsolutePath() + "' with markerId=" + markerId + ",bufferSize=" + bufferSize);
      if(markerFile.exists()){
        ArrayList<String> lines = FileUtil.readLines(markerFile.getAbsolutePath());
        count = lines.size();
        if(lines.size() != 0){
          if(!stringKeys){
            lastLongKey = Long.parseLong(lines.get(lines.size() - 1));
          }
          else{
            lastStringKey = lines.get(lines.size() - 1);
          }
        }

//        System.out.println("Marker file found from prior iteration: " 
//                           + count + "/" + bufferSize + " lines");
        markerBuffer = FileUtil.getWriter(markerFile, true/*append*/);
        return;
      } 
      else {
        count = 0;
        markerBuffer = FileUtil.getWriter(markerFile, true/*append*/);
      }
    }

    public long getLastLongKey(){
      return lastLongKey;
    }

    public String getLastStringKey(){
      return lastStringKey;
    }

    public boolean writeNextKey(long key) throws IOException{
      return writeNextKey(Long.toString(key));
    }

    public boolean writeNextKey(String key) throws IOException{
      if(key == null) return false;

      synchronized (markerFile) {
        if(count >= bufferSize){
          roll();
        }

        markerBuffer.write(key + "\n");
        markerBuffer.flush();
        count++;
      }

      return true;
    }

    private void roll() throws IOException{
      // copy old file to backup
      markerBuffer.flush();
      boolean success = FileUtil.copyFile(markerFile, new File(markerFile.getAbsolutePath() + ".bup"));
      if(!success){
        throw new IOException("Unable to create backup file due to low disk space!");
      }

      // clean up old file
      try{
        markerBuffer.flush();
        markerBuffer.close();
      }
      finally {
        markerFile.delete();
      }

      // start new file
      count = 0;
      markerBuffer = FileUtil.getWriter(markerFile, true/*append*/);
    }

    public void close(){ 
      if(markerBuffer != null){
        try{
          markerBuffer.flush();
          markerBuffer.close();
        }
        catch(IOException ioe){
          System.err.println("Unable to close BufferedWriter for marker file!: " + ioe.getMessage());
        }
        finally {
          markerBuffer = null;
        }
      }
    }
  }
}
