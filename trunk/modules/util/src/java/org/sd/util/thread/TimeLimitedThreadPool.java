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


import org.sd.util.StatsAccumulator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility to execute tasks using a fixed thread pool, collecting those results
 * that are generated within a time limit.
 * <p>
 * @author Spence Koehler
 */
public class TimeLimitedThreadPool<T> {

  private String poolId;
  private int numThreads;
  private ExecutorService workerPool;  // pool of worker threads.
  private final AtomicInteger workerIds = new AtomicInteger(0);


  public TimeLimitedThreadPool(final String poolId, int numThreads) {
    this.poolId = poolId;
    this.numThreads = numThreads;

    this.workerPool = Executors.newFixedThreadPool(
      numThreads,
      new ThreadFactory() {
        public Thread newThread(Runnable r) {
          return new Thread(r, poolId + "-Worker-" + workerIds.getAndIncrement());
        }
      });
  }

  public void shutdown() {
    workerPool.shutdownNow();
  }

  public ExecutionInfo<T> execute(Collection<Callable<T>> callables, long timeLimit) {
    return execute(callables, timeLimit, false);
  }

  public ExecutionInfo<T> execute(Collection<Callable<T>> callables, long timeLimit, boolean takeInsteadOfPoll) {
    final int num = callables.size();
    final boolean[] gotFuture = new boolean[num];
    final ExecutionInfo<T> result = new ExecutionInfo<T>(timeLimit, num);

    final CompletionService<T> ecs = new ExecutorCompletionService<T>(workerPool);
    final List<Future<T>> futures = new ArrayList<Future<T>>(num);

    try {
      for (Callable<T> callable : callables) {
//System.out.println("TimeLimitedThreadPool(" + poolId + ").submitted callable: " + callable + " num=" + num);
        futures.add(ecs.submit(callable));
      }

      int numGot = 0;
      long remainingTime = result.getRemainingTime();
      
      long waitEach = remainingTime / num;

      while (remainingTime > 0 && numGot < num) {
        for (int i = 0; i < num; ++i) {
          if (gotFuture[i]) continue;

          try {
            remainingTime = result.getRemainingTime();
            if (remainingTime <= 0) {
              // time to go
//System.out.println("TimeLimitedThreadPool(" + poolId + ") outOfTime! remainingTime=" + remainingTime);
              result.close();
              break;
            }
            else {
              final Future<T> future = (takeInsteadOfPoll) ? ecs.take() :
                ecs.poll(Math.min(remainingTime, waitEach), TimeUnit.MILLISECONDS);

//               if (future == null) {
//                 // nothing finished in time!
//                 --i;  // reset for another go'round
// //System.out.println("TimeLimitedThreadPool(" + poolId + ").pollTimeOut! remainingTime=" + remainingTime);
//               }
//               else {

              if (future != null) {
                T computed = future.get();
//System.out.println("TimeLimitedThreadPool(" + poolId + ").gotFutureValue! " + computed);
                if (computed != null) {
                  gotFuture[i] = true;
                  ++numGot;
                  if (result.addComputedResult(computed)) {
                    break;
                  }
                }
              }
            }
          }
          catch (ExecutionException ignoreEE) {}
        }
      }
    }
    catch (InterruptedException ignore) {}
    finally {
      for (Future<T> future : futures) {
        future.cancel(true);
      }
      result.close();
    }

    return result;
  }


  /**
   * Container class for information collected during execution.
   */
  public static final class ExecutionInfo<T> {
    private long timeLimit;
    private int num;

    private long startTime;
    private long endTime;
    private long expirationTime;
    private StatsAccumulator opTimes;  // accumulate stats for operations/millis

    private List<T> computedResults;

    public ExecutionInfo(long timeLimit, int num) {
      this.timeLimit = timeLimit;
      this.num = num;
      this.startTime = System.currentTimeMillis();
      this.endTime = -1;
      this.expirationTime = startTime + timeLimit;
      this.opTimes = new StatsAccumulator("opTimes");
      this.computedResults = new ArrayList<T>(num);
    }

    public long getRemainingTime() {
      return (expirationTime - System.currentTimeMillis());
    }

    public void close() {
      if (this.endTime < 0) this.endTime = System.currentTimeMillis();
    }

    /**
     * @return true if there are more results expected AND there is still time
     *         left to compute them; otherwise, false.
     */
    public boolean addComputedResult(T computedResult) {
      boolean result = true;

      computedResults.add(computedResult);
      final long curTime = System.currentTimeMillis();
      opTimes.add(curTime - startTime);

      if (computedResults.size() == num) {
        // got all results, we're done!
        endTime = curTime;
        result = false;
      }
      else if (curTime > startTime + timeLimit) {
        endTime = curTime;
        result = false;
      }

      return result;
    }

    public boolean timedOut() {
      return computedResults.size() < num;
    }

    public StatsAccumulator getOperationTimes() {
      return opTimes;
    }

    public long getStartTime() {
      return startTime;
    }

    public long getEndTime() {
      return endTime;
    }

    public long getExpirationTime() {
      return expirationTime;
    }

    public int getNum() {
      return num;
    }

    public List<T> getComputedResults() {
      return computedResults;
    }
  }
}
