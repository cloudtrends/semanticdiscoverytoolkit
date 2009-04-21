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
package org.sd.util;


import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility base class for 'rolling' through a sequence of files (dirs).
 * <p>
 * @author Spence Koehler
 */
public abstract class RollingStore <T> {
  
  /**
   * Interface for a store.
   */
  public static interface Store <T> {
    public void open() throws IOException;
    public File getDirPath();
    public void close(boolean closingInThread) throws IOException;
    public boolean isClosed();

    /**
     * Add an element to the current store, returning true to force a rollover;
     * When false is returned, the "shouldRoll" method will be checked for
     * rollover.
     */
    public boolean addElement(T element) throws IOException;

    public boolean shouldResume();
    public boolean shouldRoll();
  }

  /**
   * Interface for a hook called when done closing a store.
   */
  public static interface ClosedHook {
    public boolean handle(File storePath);
  }


  /**
   * Get the next available store file as the last element of the list (and all
   * existing store files as prior elements in the list).
   */
  protected abstract List<File> nextAvailableFile();

  /**
   * Get the root location of this store's elements
   */
  public abstract File getStoreRoot();

  /**
   * Build an instance of a store at the given location.
   */
  protected abstract Store <T> buildStore(File storeFile);

  /**
   * Hooked called while (after) opening.
   */
  protected abstract void afterOpenHook(boolean firstTime, List<File> nextAvailable, File justOpenedStore);


  private ClosedHook closedHook;
  private boolean verbose;
  private boolean waitToFinishClose;
  private boolean openNext;
  private final AtomicInteger lockCount = new AtomicInteger(0);
  private final AtomicBoolean isClosing = new AtomicBoolean(false);

  private Store<T> currentStore;

  public RollingStore(ClosedHook closedHook, boolean verbose) {
    this(closedHook, verbose, false, true);
  }

  public RollingStore(ClosedHook closedHook, boolean verbose, boolean waitToFinishClose, boolean openNext) {
    this.closedHook = closedHook;
    this.verbose = verbose;
    this.waitToFinishClose = waitToFinishClose;
    this.openNext = openNext;
  }

  protected final void initialize(File override) {
    openNext(true, override);
  }

  public final Store getStore() {
    return currentStore;
  }

  public File getCurrentStoreFile() {
    return currentStore == null ? null : currentStore.getDirPath();
  }

  public void addLock() {
    while (isClosing.get()) {
//      Thread.yield();
      try {
        Thread.sleep(100);
      }
      catch (InterruptedException ignore) {}
    }

    lockCount.incrementAndGet();
  }

  public void removeLock() {
    lockCount.decrementAndGet();
  }

  /**
   * Add the element to this store.
   *
   * @return true if the store was automatically rolled after adding this element;
   *         otherwise, false.
   */
  public boolean addElement(T element) throws IOException {
    boolean result = false;

    if (currentStore != null) {
      boolean rollover = false;

      synchronized (currentStore) {
        rollover = currentStore.addElement(element);
      }

      if (!rollover) rollover = currentStore.shouldRoll();

      if (rollover) {
        roll(null, true); // rolling with add
        result = true;
      }
    }

    return result;
  }

  public void roll() {
    roll(null, false);
  }

  public void roll(File override) {
    roll(override, false);
  }

  private final void roll(File override, boolean rollingWithAdd) {
    if (closeCurrent(rollingWithAdd) && this.openNext) {
      openNext(false, override);
    }
  }

  public void setWaitToFinishClose(boolean waitToFinishClose) {
    this.waitToFinishClose = waitToFinishClose;
  }

  public void close() {
    closeCurrent(false);
  }

  /**
   * Must call at instantiation or within a synchronized(storeMutex) block for thread safety.
   * @return false if waitToFinishClose is flagged and ClosedHook.handle() returns false, otherwise true
   */
  private boolean closeCurrent(boolean rollingWithAdd) {
    boolean result = true;
    if (currentStore != null) {

      synchronized (currentStore) {
        if (!isClosing.compareAndSet(false, true) || currentStore.isClosed()) {
          // already closed or closing
          if (verbose) {
            System.out.println(new Date() + ": *** NOTE: Store '" + currentStore.getDirPath() +
                               "' is already closed or closing. " + currentStore.toString() +
                               " isClosed=" + currentStore.isClosed());

            // todo: should we handle this case differently?  it may be better to throw an error in this case,
            // even if the code runs smoothly, as this state is indicative of a race condition
          
            return true;
          }
        }
      }

      // wait until locks are released
      final int minLockCount = rollingWithAdd ? 1 : 0;

      //NOTE: In the case of "rolling with add",
      //      we're checking for > 1 here because we expect the lock to be 1 if we're using
      //      locks. This happens because calling code will add a lock, then add a document,
      //      which calls add element, which checks to roll and then rolls if necessary
      //      before returning back control from addDocument, after which the lock is released
      //      by the calling code.

      if (lockCount.get() > minLockCount) {
        final Timer timer = new Timer(120000);
        System.out.println(new Date() + ": *** NOTE: Waiting to closeCurrent for locks to be released! (" + lockCount.get() + ")");

        while (lockCount.get() > minLockCount) {
//          Thread.yield();
          try {
            Thread.sleep(100);
          }
          catch (InterruptedException e) {
            System.err.println(new Date() + ": WARNING: RollingStore Interrupted while waiting for locks to be released! (" + lockCount.get() + "). Still choosing to wait!");
            e.printStackTrace(System.err);
          }

          if (timer.reachedTimerMillis()) {
            System.out.println(new Date() + ": *** NOTE: Still waiting to closeCurrent for locks to be released! (" + lockCount.get() + ")");
          }
        }
      }

      if (verbose) {
        System.out.println(new Date() + ": *** NOTE: Closing store '" + currentStore.getDirPath() +
                           "' for rollover " + currentStore.toString());
      }
      CloseStoreThread thread = new CloseStoreThread(currentStore, closedHook, verbose, isClosing);
      if(waitToFinishClose){
        thread.run();
        result = thread.isHandled();
      }
      else {
        thread.start();
      }
    }
 
    return result;
  }

  /**
   * Must call at instantiation or within a synchronized(luceneStoreMutex) block for thread safety.
   */
  private final void openNext(boolean firstTime, File override) {
    File storeFile = override;
    List<File> storeFiles = null;

    if (storeFile == null) {
      storeFiles = nextAvailableFile();
      int dirNum = storeFiles.size() - 1;

      if (firstTime && dirNum > 0) {
        // on the first open, skip back to the prior store to see if we can fill it more.
        --dirNum;
      }

      storeFile = storeFiles.get(dirNum);
      currentStore = buildStore(storeFile);
    
      try {
        currentStore.open();

        if (firstTime) {
          if (!currentStore.shouldResume() && dirNum + 1 < storeFiles.size()) {
            // can't resume this store
            currentStore.close(false);
            storeFile = storeFiles.get(++dirNum);
            currentStore = buildStore(storeFile);
            currentStore.open();
          }
        }

        if (verbose) {
          System.out.println(new Date() + ": *** NOTE: opened rolling store '" + storeFile + "'. " + currentStore.toString());
        }
      }
      catch (IOException e) {
        System.err.println(new Date() + ": *** ERROR! Can't open store at '" + storeFile + "'");
        e.printStackTrace(System.err);
        return;
      }
    }
    else {
      currentStore = buildStore(storeFile);
    
      try {
        currentStore.open();

        if (verbose) {
          System.out.println(new Date() + ": *** NOTE: opened rolling store '" + storeFile + "'. " + currentStore.toString());
        }
      }
      catch (IOException e) {
        System.err.println(new Date() + ": *** ERROR! Can't open store at '" + storeFile + "'");
        e.printStackTrace(System.err);
        return;
      }
    }
    
    afterOpenHook(firstTime, storeFiles, storeFile);
  }

  public static abstract class CountingStore <T> implements Store <T> {
    private File storeDir;
    private int docLimit;
    private int docCount;
    private long rollTimer;
    private long rollTimerMark;
    private boolean closed;

    /**
     * Do operation of adding the element.
     *
     * @return true to force a rollover; otherwise, false.
     */
    protected abstract boolean doAddElement(T element) throws IOException;

    protected CountingStore(File storeDir, int docLimit, long rollTimer) {
      this.storeDir = storeDir;
      this.docLimit = docLimit;
      this.docCount = 0;
      this.rollTimer = rollTimer;
      this.rollTimerMark = 0;
      this.rollTimerMark = rollTimer > 0 ? System.currentTimeMillis() : 0;

      this.closed = false;
    }

    public boolean isClosed(){
      return closed;
    }

    public void close(boolean inThread) throws IOException{
      // todo: delay setting closed to true until thread completes?
      this.closed = true;
    }

    public File getDirPath() {
      return storeDir;
    }

    public boolean addElement(T element) throws IOException {

      boolean rollover = doAddElement(element);
      ++docCount;

      if (!rollover) {
        // check other conditions for roll
        rollover = shouldRoll();
      }

      return rollover;
    }

    public boolean shouldResume() {
      boolean shouldRoll = false;

      if (!shouldRoll) {
        // check whether need to roll on doc count
        shouldRoll = (docLimit > 0 && docCount >= 0.9 * docLimit);
      }

      if (!shouldRoll) {
        // check whether need to roll on roll timer
        shouldRoll = (rollTimer > 0 && System.currentTimeMillis() - rollTimerMark >= rollTimer);
      }

      return !shouldRoll;
    }

    public boolean shouldRoll() {
      boolean rollover = false;

      if (!rollover) {
        // check whether need to roll on doc count
        rollover = (docLimit > 0 && docCount >= docLimit);
      }

      if (!rollover) {
        // check whether need to roll on roll timer
        rollover = (rollTimer > 0 && System.currentTimeMillis() - rollTimerMark >= rollTimer);
      }

      return rollover;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.append('(').append("docCount=").append(docCount).append(')');
      return result.toString();
    }

    protected int getDocCount() {
      return docCount;
    }

    protected void setDocCount(int docCount) {
      this.docCount = docCount;

      if (rollTimer > 0 && storeDir.exists()) {
        this.rollTimerMark = storeDir.lastModified();
      }
    }

    protected int getDocLimit() {
      return docLimit;
    }

    protected void setDocLimit(int docLimit) {
      this.docLimit = docLimit;
    }

    protected void setRollTimer(long rollTimer) {
      this.rollTimer = rollTimer;

      if (rollTimer > 0) {
        if (storeDir.exists()) {
          this.rollTimerMark = storeDir.lastModified();
        }
        else {
          this.rollTimerMark = System.currentTimeMillis();
        }
      }
    }
  }

  public static final class CloseStoreThread extends Thread {
    private final Store currentStore;
    private final ClosedHook doneClosingHook;
    private final boolean verbose;
    private final AtomicBoolean isClosing;

    private boolean handled;

    public CloseStoreThread(Store currentStore, boolean verbose) {
      this(currentStore, null, verbose, null);
    }

    public CloseStoreThread(Store currentStore, ClosedHook doneClosingHook, boolean verbose, AtomicBoolean isClosing) {
      super(currentStore.getDirPath() + " closing thread.");
      this.currentStore = currentStore;
      this.doneClosingHook = doneClosingHook;
      this.verbose = verbose;
      this.isClosing = isClosing;

      this.handled = false;
    }

    public boolean isHandled() {
      return this.handled;
    }

    public void run() {
      try {
        final long starttime = System.currentTimeMillis();

        try {
          currentStore.close(true);
        }
        catch (IOException ioe) {
          System.err.println("Unable to close store! '" + currentStore.getDirPath() + "'");
          ioe.printStackTrace();
        }

        if (verbose) {
          final long endtime = System.currentTimeMillis();
          System.out.println(new Date() + " *** NOTE: finished closing store '" + currentStore.getDirPath() +
                             "'! totalCloseTime=" + MathUtil.timeString(endtime - starttime, false));
        }

        final long startHooktime = System.currentTimeMillis();
        if (doneClosingHook != null) {
          this.handled = doneClosingHook.handle(currentStore.getDirPath());

          if (verbose) {
            final long endHooktime = System.currentTimeMillis();
            System.out.println(new Date() + " *** NOTE: finished running ClosedHook on store '" + currentStore.getDirPath() +
                               "'! totalCloseTime=" + MathUtil.timeString(endHooktime - startHooktime, false));
          }
        }
      }
      finally {
        if (isClosing != null) isClosing.set(false);
      }
    }
  }
}
