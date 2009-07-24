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
package org.sd.io;


import java.io.File;
import java.io.IOException;
 
/**
 * A class to encapsulate locking a file while reading/writing.
 * <p>
 * @author Spence Koehler
 */
public class FileLock <T> {
  
  private String filename;
  private int lockWaitTimeout;
  private String lockfile;

  /**
   * Construct a file lock for the given filename.
   *
   * @param filename         the file for which to obtain a lock.
   * @param lockWaitTimeout  the maximum time to wait (in millis) to acquire a
   *                         lock on the file.
   */
  public FileLock(String filename, int lockWaitTimeout) {
    this.filename = filename;
    this.lockWaitTimeout = lockWaitTimeout;
    this.lockfile = buildLockFilename();
  }

  private final String buildLockFilename() {
    final String basePath = FileUtil.getBasePath(filename);
    return basePath + "_LOCK_";
  }

  public String getFilename() {
    return filename;
  }

  public int getLockWaitTimeout() {
    return lockWaitTimeout;
  }

  public String getLockFile() {
    return lockfile;
  }

  /**
   * Obtain a lock for the file, perform the operation while locked, then
   * release the lock.
   *
   * @param lockOperation  the operation to perform while locked.
   *
   * @return an operation result if succeeded; null if not (i.e. timeout or operate returned false)
   */
  public T operateWhileLocked(LockOperation<T> lockOperation) {
    T result = null;

    try {
      // obtain lock
      if (obtainLock(lockWaitTimeout)) {
        result = lockOperation.operate(filename);
      }
    }
    catch (IOException e) {
      // ok to eat -- problems'll be indicated by null result.
      e.printStackTrace(System.err);
    }
    finally {
      // release lock
      try {
        releaseLock();
      }
      catch (IOException e) {
        //todo: log message that we couldn't release the lock.
        e.printStackTrace(System.err);
      }
    }

    return result;
  }

  /**
   * @return true if lock was obtained; otherwise, false.
   */
  private final boolean obtainLock(int timeout) throws IOException {
    final File file = new File(lockfile);
    
    createDirIfNeeded(file);

    boolean gotLock = false;
    final long starttime = System.currentTimeMillis();

    while (!gotLock && System.currentTimeMillis() - starttime < timeout) {
      gotLock = file.createNewFile();

      if (!gotLock) {
        try {
          Thread.sleep(50);
        }
        catch (InterruptedException e) {
          break;
        }
      }
    }
    
    return gotLock;
  }

  private final void releaseLock() throws IOException {
    final File file = new File(lockfile);
    file.delete();
//todo: if !file.delete() log a message that we couldn't delete the lock
  }

  private final void createDirIfNeeded(File file) throws IOException {
    File dir = file.getParentFile();
    if (!dir.exists()) {
      dir.mkdirs();
    }
  }

  public static interface LockOperation <T> {
    public T operate(String filename) throws IOException;
  }
}
