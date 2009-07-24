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
package org.sd.cluster.config;


import org.sd.cio.DataBroker;
import org.sd.io.FileUtil;
import org.sd.util.ExecUtil;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to push or spray data from one node to others.
 * <p>
 * @author Spence Koehler
 */
public class DataPusher {

  private static final AtomicInteger THREAD_POOL_NUM = new AtomicInteger(0);
  private static String _curMachine = null;

  public static String getCurrentMachine() {
    if (_curMachine == null) {
      _curMachine = ExecUtil.getMachineName().toLowerCase();
    }
    return _curMachine;
  }

  public static boolean isCurrentMachine(String destMachine) {
    final String curMachine = getCurrentMachine();
    final int atPos = destMachine.indexOf('@');
    if (atPos >= 0) {
      destMachine = destMachine.substring(atPos + 1).toLowerCase();
    }
    else {
      destMachine = destMachine.toLowerCase();
    }

    return curMachine.equals(destMachine);
  }

  /**
   * Send the given file (possibly a directory) to the identified node.
   *
   * @param absoluteSourcePath  The file to send.
   * @param destMachine         The machine to send to (with optional "user@" prepended).
   * @param absoluteDestPath    The destination path of the file.
   */
  public static void sendFileToNode(String absoluteSourcePath, String destMachine, String absoluteDestPath) {
    if (isCurrentMachine(destMachine)) {
      if (absoluteSourcePath.indexOf("/mnt/") < 0) {
        if (!symLinkFile(absoluteSourcePath, absoluteDestPath)) {
          throw new IllegalStateException("Couldn't create symlink to '" + absoluteSourcePath + "' at '" + absoluteDestPath + "'!");
        }
      }
      else {
        // copy over mount to local drive
        if (!copyLocalFile(absoluteSourcePath, absoluteDestPath, false)) {
          throw new IllegalStateException("Couldn't copy over mount from '" + absoluteSourcePath + "' to '" + absoluteDestPath + "'!");
        }
      }
    }
    else {
      // rsync -Laz absoluteSourcePath destMachine:absoluteDestPath
      final String destDir = FileUtil.getBasePath(absoluteDestPath);
      final String destination = destMachine + ":" + absoluteDestPath;
      final String command = "rsync -Laz --delete " + absoluteSourcePath + " " + destination;

      // log the transfer
      System.out.println(new Date() + ": rsyncing '" + absoluteSourcePath + "' to '" + destination + "'.");

      // do the transfer
      ExecUtil.executeProcess("ssh " + destMachine + " mkdir -p " + destDir);
      ExecUtil.executeProcess(command);
//todo: detect/report problems with transfer.
    }
  }

  /**
   * Create a symlink to the source at the dest.
   * <p>
   * Auto-create dest dirs.
   */
  public static boolean symLinkFile(String absoluteSourcePath, String absoluteDestPath) {
    final String destDir = FileUtil.getBasePath(absoluteDestPath);
    final String command = "ln -s " + absoluteSourcePath + " " + absoluteDestPath;

    ExecUtil.executeProcess("mkdir -p " + destDir);
    ExecUtil.executeProcess(command);

    return new File(absoluteDestPath).exists();
  }

  /**
   * Copy (or move) local file (or directory) from source to dest.
   * <p>
   * Auto-create dest dirs. If moveIt, then rename the file; otherwise, force recursive copy.
   */
  public static boolean copyLocalFile(String source, String dest, boolean moveIt) {
    final String destDir = FileUtil.getBasePath(dest);
    String command = null;

    if (moveIt) {
      command = "mv -f " + source + " " + dest;
    }
    else {
      command = "cp -rf " + source + " " + dest;
    }

    ExecUtil.executeProcess("mkdir -p " + destDir);
    ExecUtil.executeProcess(command);

    return new File(dest).exists();
  }

  /**
   * Get the job directory postfix suitable for the destDirPostfix arg of
   * sendDataToNodes.
   */
  public static String getJobDirPostfix(String jobIdString, String dataDirName) {
    return "data/job/" + jobIdString + "/" + dataDirName + "/";
  }

  /**
   * Given a directory whose entries match the given pattern, send
   * each element to the destination cluster nodes.
   *
   * @param clusterDef       the cluster definition.
   * @param destGroupName    specifies the destination cluster ndoes.
   * @param destDirPostfix   specifies the portion of the destination directory
   *                         after the "jvm-N/" part.
   * @param partitionedDir   local directory containing partitioned files.
   * @param destPattern      pattern for directory entries to be sent.
   * @param patternGroupNum  designates the matching group that identifies the destination node.
   * @param maxThreads       the maximum number of threads to use for transfer (<=0 for unlimited).
   * @param timeout          time to wait for termination in seconds.
   */
  public static void sendDataToNodes(ClusterDefinition clusterDef, String destGroupName, String destDirPostfix,
                                     String partitionedDir, Pattern destPattern, int patternGroupNum,
                                     int maxThreads, int timeout) {

    // create a thread pool for doing transfers.
    final ExecutorService threadPool = getThreadPool(maxThreads);

    List<String> groupNodeNames = clusterDef.getGroupNodeNames(destGroupName, true);

    final File dir = new File(partitionedDir);
    final File[] files = dir.listFiles();

System.out.println("DataPusher: groupNodeNames=" + groupNodeNames + " nfiles(" + partitionedDir + ")=" + files.length);

    try {
      for (final File file : files) {
        // match filename against pattern
        final Matcher m = destPattern.matcher(file.getName());
        if (!m.matches()) {
System.out.println("NOTE: pattern failed to match '" + file.getName() + "'");
          continue;
        }
        int partitionNum = 0;

        try {
          partitionNum = (groupNodeNames != null) ? Integer.parseInt(m.group(patternGroupNum)) : 0;
        }
        catch (NumberFormatException e) {
          partitionNum = 0;
          groupNodeNames = null;
        }

        // translate "partitionNum" to machine and jvmNum
        final String destNodeName = (groupNodeNames != null) ? groupNodeNames.get(partitionNum) : destGroupName; 
        final String[] pieces = destNodeName.split("-");
        final String destMachine = pieces[0];
        final int jvmNum = Integer.parseInt(pieces[1]);

        // build and execute command
        final String destDirPrefix = clusterDef.getJvmBasePath(jvmNum);
        final String destDir = destDirPrefix + destDirPostfix;
        final String destination = destMachine + ":" + destDir;
        final String command = "rsync -Laz " + file.getAbsolutePath() + " " + destination;

        final DataBroker dataBroker = new DataBroker(100, 50);

        // log transfer
        System.out.println("Sending '" + file.getAbsolutePath() + "' to '" + destination + "'...");

        // transfer each file on its own thread.
        threadPool.execute(new Runnable() {
            public void run() {
              dataBroker.pushWithRsync(file, destination);
//System.out.println("ssh " + destMachine + " mkdir -p " + destDir);
//              ExecUtil.ExecResult result = ExecUtil.executeProcess("ssh " + destMachine + " mkdir -p " + destDir);
//System.out.println("result=" + result + " command=" + command);
//              result = ExecUtil.executeProcess(command);
//System.out.println("result=" + result);
            }
          });
      }
    }
    finally {
      // wait for transfers to finish.
      threadPool.shutdown();
      try {
        threadPool.awaitTermination(timeout, TimeUnit.SECONDS);
      }
      catch (InterruptedException e) {
      }
    }

    //todo: send a message to receiving nodes for a checksum of the file expected now to be there and verify.
  }

  /**
   * Create a thread pool with the maximum number of threads (or unlimited if maxThreads<=0).
   */
  private static ExecutorService getThreadPool(int maxThreads) {
    ExecutorService result = null;

    final AtomicInteger localId = new AtomicInteger(0);

    if (maxThreads <= 0) {
      result = Executors.newCachedThreadPool(
        new ThreadFactory() {
          public Thread newThread(Runnable r) {
            return new Thread(r, "DataPusher-" + THREAD_POOL_NUM.getAndIncrement() + "-Thread-" + localId.getAndIncrement());
          }
        });
    }
    else {
      result = Executors.newFixedThreadPool(
        maxThreads,
        new ThreadFactory() {
          public Thread newThread(Runnable r) {
            return new Thread(r, "DataPusher-" + THREAD_POOL_NUM.getAndIncrement() + "-Thread-" + localId.getAndIncrement());
          }
        });
    }

    return result;
  }
}
