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
package org.sd.cio;


import org.sd.io.FileUtil;
import org.sd.util.ExecUtil;

import java.io.File;
import java.util.Date;

/**
 * Utility class to pull data from another node.
 * <p>
 * @author Spence Koehler
 */
public class DataBroker {
  
  public static final int MAX_TRIES = 100;
  public static final int WAIT_INTERVAL = 1000;  // 1 second.
  public static final int NO_SUCH_FILE_CODE = 23;  // rsync error code for no such file
  public static final int CONNECTION_CLOSED = 12;  // too many open connections


  private final int maxTries;
  private final int waitInterval;

  public DataBroker() {
    this(MAX_TRIES, WAIT_INTERVAL);
  }

  public DataBroker(int maxTries, int waitInterval) {
    this.maxTries = maxTries;
    this.waitInterval = waitInterval;
  }

  /**
   * Execute "rsync -Laz <remoteName> <localName>". If necessary, the directories
   * leading to the local file will be created. This will wait and retry a number
   * of times if the rsync fails unless the failure is because of the remote file
   * not existing.
   *
   * @param remoteName  of the form machine:path
   * @param localFile  path to local destination.
   */
  public final boolean pullWithRsync(String remoteName, File localFile) {
    return pullWithRsync(remoteName, localFile, null);
  }

  public final boolean pullWithRsync(String remoteName, File localFile, String rsyncArgs) {
    return pullWithRsync(remoteName, localFile, rsyncArgs, false);
  }

  public final boolean pullWithRsync(String remoteName, File localFile, String rsyncArgs, boolean verbose) {

    boolean result = false;
    final String localName = localFile.getParent();

    // make parent directories if needed.
    if (!FileUtil.makeParentDirectories(localFile)) {
      System.err.println("***WARNING: mkParentDirs '" + localFile + "' failed!  localName=" + localName);
    }

    final StringBuilder command = new StringBuilder();

    command.append("rsync -Laz ");
    if (rsyncArgs != null) command.append(rsyncArgs).append(' ');
    command.append(remoteName).append(' ').append(localName);

    int numTries = 0;

    if (verbose) {
      System.out.println(new Date() + " invoking DataBroker.pullWithRsync(" + command.toString() + ")");
    }

    while (!localFile.exists() && numTries++ < maxTries) {
      final ExecUtil.ExecResult execResult = ExecUtil.executeProcess(command.toString());

      if (execResult == null) {
        break;  // unrecoverable error.
      }
      else if (execResult.failed()) {
        if (execResult.exitValue == NO_SUCH_FILE_CODE) {
          // source doesn't exist to copy. exit.
          break;
        }
        else {
          if (localFile.exists()) break;

          // otherwise, wait and try again.
          if (verbose) {
            System.err.println(numTries + " DataBroker.pullWithRsync(" + remoteName + ", " + localFile + ") FAILED: (" + execResult.exitValue + ") " + execResult.output);
          }

          try {
            Thread.sleep(waitInterval);
          }
          catch (InterruptedException e) {
            // stop waiting.
            break;
          }
        }
      }
    } 

    if (numTries >= maxTries) {
      System.err.println("*** WARNING: DataBroker.pullWithRsync(" + remoteName + ", " + localFile + ") FAILED after " + numTries + " tries.");
    }

    return localFile.exists();
  }

  /**
   * Push the localFile to the remoteDestination, including making any dirs on
   * the destination if needed.
   */
  public final boolean pushWithRsync(File localFile, String remoteName) {
    return pushWithRsync(localFile, remoteName, null);
  }

  public final boolean pushWithRsync(File localFile, String remoteName, String rsyncArgs) {

    boolean result = localFile.exists() && mkRemoteDir(remoteName);

    if (result) {
      final StringBuilder command = new StringBuilder();
      final String localName = localFile.getAbsolutePath();

      command.append("rsync -Laz ");
      if (rsyncArgs != null) command.append(rsyncArgs).append(' ');
      command.append(localName).append(' ').append(remoteName);

      final String rsyncCommand = command.toString();
      ExecUtil.ExecResult execResult = null;
      int numTries = 0;

      while (numTries++ < maxTries) {
        execResult = ExecUtil.executeProcess(rsyncCommand);

        if (execResult == null) {
          break;  // unrecoverable error.
        }
        else if (execResult.failed()) {
          // wait and try again.
          try {
            Thread.sleep(waitInterval);
          }
          catch (InterruptedException e) {
            // stop waiting.
            break;
          }
        }
        else {
          // did it! time to kick out.
          break;
        }
      }

      if (execResult == null || execResult.failed()) {
        System.err.println("*** WARNING: DataBroker.pushWithRsync(" + localName + ", " + remoteName + ") FAILED after " + numTries + " tries.");
      }
    }

    return localFile.exists();
  }

  /**
   * Make the remote directory for the file including necessary parents.
   *
   * @param remoteName is a file in the directory to be made of the form machine:path
   *
   * @return true if successful; false if failed.
   */
  public final boolean mkRemoteDir(String remoteName) {
    boolean result = false;

    final int cPos = remoteName.indexOf(':');
    if (cPos > 0) {
      final String destMachine = remoteName.substring(0, cPos).toLowerCase();
      String destDir = remoteName.substring(cPos + 1);
      final int lastSlashPos = destDir.lastIndexOf('/');
      if (lastSlashPos >= 0) {
        destDir = destDir.substring(0, lastSlashPos);
      }

      final String sshCommand = "ssh " + destMachine + " mkdir -p " + destDir;
      ExecUtil.ExecResult execResult = null;
      int numTries = 0;

      while (numTries++ < maxTries) {
        execResult = ExecUtil.executeProcess(sshCommand);

        if (execResult == null) {
          break;  // unrecoverable error.
        }
        else if (execResult.failed()) {
          // wait and try again.

          try {
            Thread.sleep(waitInterval);
          }
          catch (InterruptedException e) {
            // stop waiting
            break;
          }
        }
        else {
          // did it! time to kick out.
          break;
        }
      }

      if (execResult == null || execResult.failed()) {
        System.err.println("*** WARNING: DataBroker.mkRemoteDir(" + remoteName + ") FAILED after " + numTries + " tries.");
      }
      else {
        result = true;
      }
    }

    return result;
  }

  public static final void main(String[] args) {
    //arg0: remoteName
    //arg1: localName
    //arg2: rsyncArgs (optional)

    final String remoteName = args[0];
    final File localFile = new File(args[1]);
    final String rsyncArgs = args.length > 2 ? args[2] : null;

    System.out.println("remoteName=" + remoteName);
    System.out.println("localFile=" + localFile);
    System.out.println("rsyncArgs=" + rsyncArgs);

    new DataBroker().pullWithRsync(remoteName, localFile, rsyncArgs);
      
    System.out.println("rsync'd '" + remoteName + "' to '" + localFile + "' " + (localFile.exists() ? "SUCCEEDED" : "FAILED"));
  }
}
