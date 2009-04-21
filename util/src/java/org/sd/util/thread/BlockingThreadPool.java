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


import org.sd.util.MathUtil;
import org.sd.util.StatsAccumulator;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A fixed thread pool that accepts runnables to execute when threads are
 * available.
 * <p>
 * The fastest path to shutting down the pool is: pool.shutdown(true).
 * Any items remaining on the queue and any items being worked on are clipped
 * and terminated as quickly as possible.
 * <p>
 * The cleanest (but longest) path to shutdown is the following sequence:
 * <ul>
 * <li>pool.stopAcceptingWork();
 * <li>pool.waitForQueueToEmpty(timeout, waitInterval);
 * <li>pool.shutdown(false);
 * <li>pool.waitUntilDone(timeout, timeUnit);
 * </ul>
 * This will finish all submitted and running work before terminating.
 *
 * @author Spence Koehler
 */
public class BlockingThreadPool {
  
  private static final long SHUTDOWN_LATENCY = 100L;  // in milliseconds

  private String poolId;
  private int numThreads;
  private int maxQueueSize;
  private int maximumErrors;         // <=0 ==> unlimited.
  private StatsAccumulator opTimes;  // accumulate stats for operations/millis
  private Date startDate;
  private Date stopDate;
  private Set<WorkerRunnable> runningWorkers;

  private final Object opTimesMutex = new Object();

  private BlockingQueue<HookedRunnable> queue;
  private ExecutorService xferThread;  // thread to transfer from queue to worker thread.
  private ExecutorService workerPool;  // pool of worker threads.

  private final AtomicInteger workerIds = new AtomicInteger(0);
  private final AtomicInteger numErrors = new AtomicInteger(0);
  private final AtomicBoolean isShutdown = new AtomicBoolean(false);
  private final AtomicBoolean stayAlive = new AtomicBoolean(true);
  private final AtomicBoolean acceptingWork = new AtomicBoolean(true);
  private final AtomicBoolean transferingWork = new AtomicBoolean(true);

  /**
   * Construct pool with the number of threads to use and the maximum queue
   * size. Once numThreads are all executing AND maxQueueSize is met, the
   * pool will block on adding new runnables until a thread finishes running
   * and pulls a runnable from the queue.
   *
   * @param poolId        Identifier for the pool (thread names).
   * @param numThreads    The number of threads to run simultaneously.
   * @param maxQueueSize  The maximum number of runnables to hold before
   *                      waiting for a thread to become available.
   * @param maximumErrors The maximum number of errors to accept before
   *                      automatically shutting down (&lt;=0 means to never
   *                      shut down automatially due to errors).
   */
  public BlockingThreadPool(final String poolId, int numThreads, int maxQueueSize, int maximumErrors) {
    this.poolId = poolId;
    this.numThreads = numThreads;
    this.maxQueueSize = maxQueueSize;
    this.maximumErrors = maximumErrors;
    this.opTimes = new StatsAccumulator(poolId + "-operation-times");
    this.startDate = new Date();
    this.stopDate = null;
    this.runningWorkers = new HashSet<WorkerRunnable>();

    this.queue = new LinkedBlockingQueue<HookedRunnable>(maxQueueSize);
    this.xferThread = Executors.newSingleThreadExecutor(
      new ThreadFactory() {
        public Thread newThread(Runnable r) {
          return new Thread(r, poolId + "-XferThread");
        }
      });
    final AtomicInteger workerThreadId = new AtomicInteger(0);
    this.workerPool = Executors.newFixedThreadPool(
        numThreads,
        new ThreadFactory() {
          public Thread newThread(Runnable r) {
            return new Thread(r, poolId + "-Worker-" + workerThreadId.getAndIncrement());
          }
        });

    // start up the xfer thread
    xferThread.execute(new XferRunnable());
  }

  /**
   * Get this pool's id.
   */
  public String getPoolId() {
    return poolId;
  }

  /**
   * Get the number of threads that this pool can run.
   */
  public int getNumThreads() {
    return numThreads;
  }

  /**
   * Get the number of threads that are currently running in this pool.
   */
  public int getNumRunningThreads() {
    int result = 0;
    synchronized (runningWorkers) {
      result = runningWorkers.size();
    }
    return result;
  }

  /**
   * Get a map of the runnables that are being processed in this pool, mapped
   * by the length of time they have been running.
   */
  public Map<Long, HookedRunnable> getRunning() {
    final Map<Long, HookedRunnable> result = new LinkedHashMap<Long, HookedRunnable>();

    synchronized (runningWorkers) {
      for (WorkerRunnable wr : runningWorkers) {
        result.put(wr.getRunTime(), wr.hr);
      }
    }

    return result;
  }

  /**
   * Get the maximum size of the queue.
   */
  public int getMaxQueueSize() {
    return maxQueueSize;
  }

  /**
   * Get the number of runnables queued in this pool.
   */
  public int getNumQueued() {
    return queue.size();
  }

  /**
   * Determine whether this queue is currently full.
   */
  public boolean isFull() {
    return maxQueueSize == queue.size();
  }

  /**
   * Get the stats accumulator for how long operations are taking in millis.
   */
  public StatsAccumulator getOperationTimes() {
    return opTimes;
  }

  /**
   * Get the timestamp for when this thread pool was started.
   */
  public Date getStartDate() {
    return startDate;
  }

  /**
   * Get the timestamp for when this thread pool was shut down.
   *
   * @return the shutdown time or null if still alive.
   */
  public Date getStopDate() {
    return stopDate;
  }

  /**
   * Get the total number of runnables that have (not necessarily successfully)
   * been run by this pool.
   */
  public int getNumRun() {
    return opTimes.getN();
  }

  /**
   * Get the number of errors that have been caught by threads in this pool.
   */
  public int getNumErrors() {
    return numErrors.get();
  }

  /**
   * Get the maximum number of errors that will be caught before automatically
   * shutting down.
   * <p>
   * @return the maximum, where &lt;= 0 indicates to never shutdown due to errors.
   */
  public int getMaximumErrors() {
    return maximumErrors;
  }

  /**
   * Determine whether this pool is up.
   * <p>
   * Note that once a pool is shutdown, it cannot be brought up again; a new
   * instance will need to be created.
   */
  public boolean isUp() {
    return stayAlive.get();
  }

  /**
   * Determine whether this pool has been shutdown.
   */
  public boolean isShutdown() {
    return isShutdown.get();
  }

  /**
   * Shut this pool down if it is still up.
   * <p>
   * Drain any runnables on the queue and, if 'now', also attempt to reclaim
   * elements submitted to the thread pool, but not running yet.
   */
  public List<HookedRunnable> shutdown(boolean now) {
    List<HookedRunnable> result = null;

    if (isShutdown.compareAndSet(false, true)) {

      // log shutdown
      System.out.println(new Date() + ": BlockingThreadPool." + poolId + ".shutdown(" +
                         now + ") invoked!");

      stayAlive.set(false);

      result = drainQueue();
      this.stopDate = new Date();

      if (now) {
        xferThread.shutdownNow();
        final List<Runnable> awaiting = workerPool.shutdownNow();
        for (Runnable r : awaiting) {
          final WorkerRunnable wr = (WorkerRunnable)r;
          result.add(wr.hr);
        }
      }
      else {
        xferThread.shutdown();
        workerPool.shutdown();
      }
    }
    else {
      System.out.println(new Date() + ": BlockingThreadPool." + poolId + ".shutdown(" +
                         now + ") ignored! (Already shutdown!)");
    }

    return result;
  }

  /**
   * Add more work to the queue.
   * <p>
   * If this pool has been shutdown, an IllegalStateException will be thrown.
   * If this queue is not accepting work or if the timeout is reached before
   * the queue empties enough to accept the work, false will be returned.
   *
   * @return true if the work was added to the queue.
   */
  public boolean add(HookedRunnable runnable, long timeout, TimeUnit unit) {
    if (!stayAlive.get()) {
      // note: the pool could shutdown without the consumer knowing due to
      //       interruptions or rejected executions. This exception will alert
      //       the consumer to these conditions.
//      throw new IllegalStateException(poolId + " pool has shutdown -- can't add more work!");
      System.err.println(new Date() + ": ***WARNING: Trying to add more work to a pool that has been shutdown! " + runnable);
      return false;
    }

    boolean result = false;

    if (acceptingWork.get()) {
      try {
        result = queue.offer(runnable, timeout, unit);
      }
      catch (InterruptedException e) {
        // nothing to do, result already false.
        System.err.println(new Date() + ": ***WARNING: Interrupted while offering work to queue! " + runnable);
      }
    }
    else {
      System.err.println(new Date() + ": ***WARNING: Pool is no longer accepting work! " + runnable);
    }

    return result;
  }

  /**
   * Drain the and return the current contents of the queue.
   * <p>
   * Current queue contents are those runnables that have not yet been
   * submitted to threads for execution.
   *
   * @return the unexecuted runnables from the queue.
   */
  public List<HookedRunnable> drainQueue() {
    final List<HookedRunnable> result = new ArrayList<HookedRunnable>();
    queue.drainTo(result);
    return result;
  }

  /**
   * Stop accepting more items added to the queue.
   */
  public void stopAcceptingWork() {
    acceptingWork.set(false);
  }

  /**
   * Resume accepting items to be added to the queue.
   */
  public void resumeAcceptingWork() {
    acceptingWork.set(true);
  }

  /**
   * Determine whether this instance is currently accepting work.
   */
  public boolean isAcceptingWork() {
    return acceptingWork.get();
  }

  /**
   * Stop transfering more items added to the queue.
   */
  public void stopTransferingWork() {
    transferingWork.set(false);
  }

  /**
   * Resume transfering items to be added to the queue.
   */
  public void resumeTransferingWork() {
    transferingWork.set(true);
  }

  /**
   * Determine whether this instance is currently transfering work.
   */
  public boolean isTransferingWork() {
    return transferingWork.get();
  }

  /**
   * Wait for the queue's contents to be transfered to worker threads.
   * <p>
   * Note that this method does nothing to block other threads from continuing
   * to place work on the queue. Consumers should usually call 'stopAcceptingWork'
   * before calling this method.
   *
   * @param timeout       maximum time to wait in milliseconds.
   * @param waitInterval  time to sleep between checking.
   *
   * @return true to confirm completion; false if timed out or interrupted.
   */
  public boolean waitForQueueToEmpty(long timeout, int waitInterval) {
    boolean result = false;

    final long starttime = System.currentTimeMillis();
    long curtime = 0L;
    while ((queue.size() > 0) &&
           (((curtime = System.currentTimeMillis()) - starttime) < timeout)) {
      try {
        Thread.sleep(waitInterval);
      }
      catch (InterruptedException e) {
        // stop waiting.
        break;
      }
    }

    return (queue.size() > 0);
  }

  /**
   * Wait for an opening in the queue.
   * <p>
   * Note that this method does nothing to block other threads from continuing
   * to place work on the queue. Consumers should usually call 'stopAcceptingWork'
   * before calling this method.
   *
   * @param timeout       maximum time to wait in milliseconds.
   * @param waitInterval  time to sleep between checking.
   *
   * @return true to confirm completion; false if timed out or interrupted.
   */
  public boolean waitForAvailableSlot(long timeout, int waitInterval, AtomicBoolean pause) {
    boolean result = false;

    final long starttime = System.currentTimeMillis();
    long curtime = 0L;
    while (isFull() &&
           (((curtime = System.currentTimeMillis()) - starttime) < timeout)) {
      if (pause != null && pause.get()) break;
      try {
        Thread.sleep(waitInterval);
      }
      catch (InterruptedException e) {
        // stop waiting.
        break;
      }
    }

    return !isFull();
  }

  /**
   * Assuming a shutdown command has already been issued, wait until all
   * running threads are finished.
   *
   * @return true to confirm completion; false if timed out or interrupted.
   */
  public boolean waitUntilDone(long timeout, TimeUnit unit) {
    boolean result = false;
    try {
      result = workerPool.awaitTermination(timeout, unit);
      result &= xferThread.awaitTermination(timeout, unit);
    }
    catch (InterruptedException e) {
      // nothing to do. result is already false.
    }
    return result;
  }

  public void killLongestRunningThread() {
    long longestTime = 0L;
    WorkerRunnable wrToKill = null;
    for (WorkerRunnable wr : runningWorkers) {
      final long curTime = wr.getRunTime();
      if (curTime > longestTime) {
        longestTime = curTime;
        wrToKill = wr;
      }
    }
    if (wrToKill != null) {
      runningWorkers.remove(wrToKill);
      wrToKill.die();
    }
  }

  private final class XferRunnable implements Runnable {
    public void run() {
      while (stayAlive.get()) {  // while pool is up
        // wait for queued work
        while (stayAlive.get() && (getNumQueued() == 0)) {
//          Thread.yield();  // CPU usage jumps if we yield instead of sleep here. (but units/sec goes way up)
            try {
              Thread.sleep(1);
            }
            catch (InterruptedException ignore) {}
        }

        // wait for an available thread or shutdown
        while (stayAlive.get() && (getNumRunningThreads() >= numThreads)) {
//          Thread.yield();
           try {
             Thread.sleep(1);
           }
           catch (InterruptedException ignore) {}
        }

        // wait for signal to initiate a transfer
        while (!transferingWork.get() && stayAlive.get()) {
          Thread.yield();   // yield here is better than sleep.
        }

        // kick out of we've been shutdown
        if (!stayAlive.get()) break;  // got shutdown while waiting for thread

        HookedRunnable r = null;

        try {
          r = queue.poll(SHUTDOWN_LATENCY, TimeUnit.MILLISECONDS);
          
          if (r != null) {  // got a runnable from the queue within the timelimit
            // transfer the runnable to the worker pool.
            final boolean ran = tryRunning(r, 100);
            if (!ran) {
              // couldn't run it! shutdown this pool.
              stayAlive.set(false);

              System.err.println(new Date() + " BlockingThreadPool -- Couldn't run '" + r + "'! Shutting down '" + poolId + "'");
            }
          }
        }
        catch (InterruptedException e) {
          stayAlive.set(false);
        }
      }
    }

    private final boolean tryRunning(HookedRunnable r, int maxTries) {
      boolean result = true;

      if (stayAlive.get()) {
        result = false;
        int tryCount = 0;
        final WorkerRunnable wr = new WorkerRunnable(r);

        while (stayAlive.get() && tryCount < maxTries) {
          try {
            workerPool.execute(wr);
            result = true;
            break;
          }
          catch (RejectedExecutionException e) {
//            Thread.yield();
            try {
              Thread.sleep(1000);
            }
            catch (InterruptedException ignore) {}
            ++tryCount;
          }
        }
      }

      return result;
    }
  }

  private final class WorkerRunnable implements Killable {
    private int id;
    public final HookedRunnable hr;
    private long starttime;

    WorkerRunnable(HookedRunnable hr) {
      this.hr = hr;
      this.id = workerIds.incrementAndGet();
    }
    public void run() {
      this.starttime = System.currentTimeMillis();
      synchronized (runningWorkers) {
        runningWorkers.add(this);
      }
      try {
        hr.preRunHook();
        hr.run();
        hr.postRunHook();
      }
      catch (Throwable t) {
        final int nerrors = numErrors.incrementAndGet();
        hr.exceptionHook(t);

        if (maximumErrors > 0 && nerrors >= maximumErrors) {
          // too many errors. time to shutdown the thread pool.
          System.err.println(new Date() + ": BlockingThreadPool.WorkerRunnable -- exceeded maximum errors (" + nerrors + "/" + maximumErrors + ")! Shutting down.");
          stayAlive.set(false);
        }
      }
      finally {
        synchronized (runningWorkers) {
          runningWorkers.remove(this);
        }
        final long endtime = System.currentTimeMillis();
        synchronized (opTimesMutex) {
          opTimes.add(endtime - starttime);
        }
      }
    }

    public void die() {
      hr.die();

      System.err.println(new Date() + " BlockingThreadPool.WorkerRunnable #" + id +
                         " -- killed '" + hr + "'! (runtime=" + MathUtil.timeString(getRunTime(), false) + ")");
    }

    public long getRunTime() {
      return System.currentTimeMillis() - starttime;
    }

    public int hashCode() {
      return id;
    }

    public boolean equals(Object o) {
      boolean result = (this == o);
      if (!result && (o instanceof WorkerRunnable)) {
        final WorkerRunnable other = (WorkerRunnable)o;
        result = (id == other.id);
      }
      return result;
    }
  }
}
