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
package org.sd.util.thread;


import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

/**
 * Class to monitor a directory for new contents to process.
 * <p>
 * As each entry in the (typically 'Ready' bucket) directory is processed,
 * it is moved from its bucket to the active and processed (or failed input)
 * buckets.
 * <p>
 * Any output created by the file processor also starts in the processing
 * bucket and is moved to the finished (or failed output) bucket.
 *
 * @author Spence Koehler
 */
public class DirectoryMonitor extends Thread {
  
  private File directory;
  private FileProcessor processor;
  private int numProcessorThreads;
  private Map<File, ProcessorThread> runningThreads;

  private BlockingThreadPool pool;
  private AtomicBoolean monitor = new AtomicBoolean(true);
  private AtomicBoolean stayAlive = new AtomicBoolean(true);

  public DirectoryMonitor(File directory, FileProcessor processor, int numProcessorThreads) {
    this.directory = directory;
    this.processor = processor;
    this.numProcessorThreads = numProcessorThreads;
    this.runningThreads = new LinkedHashMap<File, ProcessorThread>();

    if (numProcessorThreads > 0) {
      this.pool = new BlockingThreadPool("DirectoryMonitorPool-" + directory.getAbsolutePath(), numProcessorThreads, 1, -1);
    }
    else {
      this.monitor.set(false);
      this.stayAlive.set(false);
      this.pool = null;
    }
  }

  public void run() {
    while (stayAlive.get()) {
      final long startTime = System.currentTimeMillis();

      if (monitor.get()) {
        // poll the directory and process its entries.
        final File[] entries = directory.listFiles();
        for (File entry : entries) {
          pool.add(new ProcessorThread(entry), 600, TimeUnit.SECONDS);  //todo: parameterize this.
        }
      }

      final long endTime = System.currentTimeMillis();

      if (stayAlive.get()) {
        // wait for a second before polling the directory again.
        final long timeToWait = 1000 - (endTime - startTime);
        if (timeToWait > 0L) {
          try {
            Thread.sleep(timeToWait);
          }
          catch (InterruptedException e) {
            // ignore interruption.
          }
        }
      }
    }
  }

  public boolean isUp() {
    return stayAlive.get() && pool.isUp();
  }

  protected BlockingThreadPool getPool() {
    return pool;
  }

  /**
   * Start monitoring.
   */
  public void startMonitoring() {
    monitor.set(true);
  }

  /**
   * Stop monitoring.
   */
  public void stopMonitoring() {
    monitor.set(false);
  }

  /**
   * Stop processing on all active work, deleting unfinished processing output,
   * and reclaiming the input as ready for processing.
   */
  public void resetActiveWork() {
    final boolean monitoring = monitor.getAndSet(false);
    for (File workUnit : runningThreads.keySet()) {
      resetActiveWork(workUnit);
    }
    monitor.set(monitoring);
  }

  /**
   * Given a file handle to a work unit in the "active" bucket, halt processing on
   * the unit, delete its (unfinished) processing output, and reclaim the input as
   * ready for processing input.
   */
  public void resetActiveWork(File workUnit) {
    final boolean monitoring = monitor.getAndSet(false);  // stop monitor

    final ProcessorThread pThread = runningThreads.get(workUnit);
    if (pThread != null) {
      pThread.die();                            // kill active processor thread.
    }
    Bucket.PROCESSING_OUTPUT.delete(workUnit);  // delete output processing bucket contents.
    Bucket.READY_INPUT.claim(workUnit);         // reclaim active input bucket contents as ready input bucket.

    monitor.set(monitoring);  // restore monitor
  }

  /**
   * Move processed (and failed) input work back to ready input for
   * reprocessing, deleting finished (failed, or sent) output.
   */
  public void resetProcessedWork() {
//todo: if/when a forwarder is present, stop it and restore it at the end.
    final boolean monitoring = monitor.getAndSet(false);  // stop monitor

    resetProcessedWork(Bucket.PROCESSED_INPUT);
    resetProcessedWork(Bucket.FAILED_INPUT);

    monitor.set(monitoring);  // restore monitor
  }

  private final void resetProcessedWork(Bucket bucket) {
    final File bucketDir = bucket.asDirectory(directory);
    final File[] entries = bucketDir.listFiles();
    for (File entry : entries) {
      resetProcessedWork(entry);
    }
  }

  /**
   * Given a file handle to a work unit in the "processed" (or failed) bucket,
   * move it back to "ready" for reprocessing, deleting its output in finished
   * (or failed or sent).
   */
  public void resetProcessedWork(File workUnit) {
//todo: if/when a forwarder is present, stop it and restore it at the end.
    final boolean monitoring = monitor.getAndSet(false);  // stop monitor

    Bucket.FINISHED_OUTPUT.delete(workUnit);
    Bucket.FAILED_OUTPUT.delete(workUnit);
    Bucket.SENT_OUTPUT.delete(workUnit);

    Bucket.READY_INPUT.claim(workUnit);

    monitor.set(monitoring);  // restore monitor
  }

  /**
   * Reset all active and processed work.
   */
  public void resetAllWork() {
    final boolean monitoring = monitor.getAndSet(false);  // stop monitor

    resetActiveWork();
    resetProcessedWork();

    monitor.set(monitoring);  // restore monitor
  }

  /**
   * Reset all active and processed work for the given unit.
   */
  public void resetAllWork(File workUnit) {
    final boolean monitoring = monitor.getAndSet(false);  // stop monitor

    resetActiveWork(workUnit);
    resetProcessedWork(workUnit);

    monitor.set(monitoring);  // restore monitor
  }

  /**
   * Forward the output to each host's corresponding baseJobPath.
   *
   * @param destHostnames            The hosts to which the output should be
   *                                 forwarded.
   * @param destBaseJobPaths         The base paths of the job on the destination
   *                                 machines that will be processing the
   *                                 forwarded results as input. These paths
   *                                 correspond to the job's working directory
   *                                 that is the parent to all of the bucket
   *                                 directories.
   * @param localRestorations        Buckets from which results should be moved
   *                                 into the finished output bucket before
   *                                 forwarding. Usually, data that is being
   *                                 resent would be moved from the "sent"
   *                                 bucket back into the finished bucket.
   * @param remoteRestorations       Buckets from which forwarded results should
   *                                 first be copied into the destination's
   *                                 receiving area for taking advantage of
   *                                 rsync'ing instead of retransferring all
   *                                 data.
   * @param remoteDeletions          Buckets from which forwarded results will
   *                                 be deleted. This is intended for clearing
   *                                 results that should be recomputed from
   *                                 the incoming data.
   * @param moveToSentBucket         If true, each sent result will be moved
   *                                 to the sent bucket.
   * @param optionalSpecificResults  If non-null, then just these results will
   *                                 be forwarded; otherwise, all results in
   *                                 the local "sent" bucket will be forwarded.
   */
  public void forwardOutput(String[] destHostnames, String[] destBaseJobPaths,
                            Bucket[] localRestorations, Bucket[] remoteRestorations,
                            Bucket[] remoteDeletions, boolean moveToSentBucket,
                            File[] optionalSpecificResults) {
//todo: implement this when we finally get around to having true multi-stage processing.
    // move all local 'restorations' (i.e. "sent" back to "finished") through finding and claiming
    // for each "finished": (using remote shell commands)
    //   move corresponding remote "ready", "failed"?, "active"? or "processed"? back to "receiving";
    //     delete remote processing?, finished?, failed?, or sent?
    // final String absoluteSourcePath = ...
    // DataPusher.sendFileToNode(absoluteSourcePath, hostname, absoluteDestPath)
  }

  /**
   * Shutdown this directory monitor.
   */
  public void shutdown(boolean now) {
    monitor.set(false);
    stayAlive.set(false);
    if (pool != null && pool.isUp()) {
      if (!now) {
        pool.stopAcceptingWork();
        pool.waitForQueueToEmpty(5000, 50);  //todo: parameterize these values.
        pool.shutdown(false);
        pool.waitUntilDone(5, TimeUnit.SECONDS);  //todo: parameterize these values.
      }
      else {
        pool.shutdown(true);
      }
    }
  }
  
  private final class ProcessorThread implements HookedRunnable {

    private File workUnit;
    private File outputUnit;
    private AtomicBoolean die = new AtomicBoolean(false);

    public ProcessorThread(File workUnit) {
      this.workUnit = workUnit;
      this.outputUnit = Bucket.PROCESSING_OUTPUT.asFile(workUnit);
    }

    /**
     * Hook to be executed just before the 'run' method when a thread handles
     * this runnable.
     */
    public void preRunHook() {
      if (!die.get()) {
        // rename input workUnit to "active" bucket
        workUnit = Bucket.ACTIVE_INPUT.claim(workUnit);
      }
    }

    /**
     * Hook to be executed just after the 'run' method when a thread handles
     * this runnable.
     */
    public void postRunHook() {
      if (!die.get()) {
        // rename active workUnit to "processed" bucket
        workUnit = Bucket.PROCESSED_INPUT.claim(workUnit);
        outputUnit = Bucket.FINISHED_OUTPUT.claim(outputUnit);
      }
    }
    
    /**
     * Hook to be executed if there is an Exception thrown while running.
     * <p>
     * This hook is called if there is an exception thrown during preRunHook,
     * run, or postRunHook.
     */
    public void exceptionHook(Throwable t) {
      if (!die.get()) {
        // rename active workUnit to "error" bucket
        workUnit = Bucket.FAILED_INPUT.claim(workUnit);
        outputUnit = Bucket.FAILED_OUTPUT.claim(outputUnit);
      }
    }
    
    public void die() {
      die.set(true);
    }

    public void run() {
      if (workUnit.exists() && !die.get()) {
        runningThreads.put(workUnit, this);
        processor.processFile(workUnit, outputUnit, die);
        runningThreads.remove(workUnit);
      }
    }
  }
}
