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


import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class to run an arbitrary process until time runs out.
 * <p>
 * @author Spence Koehler
 */
public class TimeoutThread {

  private RunnerThread runnerThread;
  private boolean interrupted;
  private long totalRunTime;
  private Throwable error;

  /**
   * Initialize with the given runnable, but don't run it yet.
   */
  public TimeoutThread(Thread r) {
    this.runnerThread = new RunnerThread(r);
    this.interrupted = false;
    this.totalRunTime = 0L;
    this.error = null;
  }

  /**
   * Run the process until it is finished or the time limit is hit,
   * whichever comes first.
   *
   * @return true if the process finished.
   */
  public boolean run(long timeLimit) {
    boolean result = true;

    this.error = null;
    runnerThread.start();
    final long startTime = System.currentTimeMillis();

    while (runnerThread.isRunning()) {
      final long curTime = System.currentTimeMillis();
      final long runTime = curTime - startTime;
      if (timeLimit > 0 && runTime >= timeLimit) {
        runnerThread.interrupt();
        interrupted = true;
        result = false;
        break;
      }
      else {
        long remainingTime = timeLimit > 0 ? timeLimit - runTime : 1000;
        try {
          Thread.sleep(Math.min(1000, remainingTime));
        }
        catch (InterruptedException e) {
          break;
        }
      }
    }

    this.totalRunTime = System.currentTimeMillis() - startTime;

    return result;
  }

  private final void setError(Throwable t) {
    this.error = t;
  }

  public Throwable getError() {
    return error;
  }

  public boolean wasInterrupted() {
    return interrupted;
  }

  public long getTotalRunTime() {
    return totalRunTime;
  }

  private class RunnerThread extends Thread {
    private Thread r;
    private AtomicBoolean running = new AtomicBoolean(true);

    public RunnerThread(Thread r) {
      this.r = r;
    }

    public void run() {
      try {
        r.run();
      }
      catch (Throwable t) {
        setError(t);
      }
      running.set(false);
    }

    public boolean isRunning() {
      return running.get();
    }
  }
}
