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
 * Utility class to run a killable process until time runs out.
 * <p>
 * @author Spence Koehler
 */
public class TimeLimitedThread {

  private RunnerThread runnerThread;
  private boolean interrupted;
  private long totalRunTime;
  private Throwable error;
  private int checkInterval;

  /**
   * Initialize with the given runnable, but don't run it yet.
   */
  public TimeLimitedThread(Killable r) {
    this(r, 1000);
  }

  public TimeLimitedThread(Killable r, int checkInterval) {
    this.runnerThread = new RunnerThread(r);
    this.interrupted = false;
    this.totalRunTime = 0L;
    this.error = null;
    this.checkInterval = checkInterval;
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
    final long startTime = System.currentTimeMillis();
    runnerThread.start();

    while (runnerThread.isRunning()) {
      final long curTime = System.currentTimeMillis();
      final long runTime = curTime - startTime;
      if (timeLimit > 0 && runTime >= timeLimit) {
        runnerThread.kill();
        interrupted = true;
        result = false;
      }
      else {
        final long remainingTime = timeLimit > 0 ? timeLimit - runTime : checkInterval;
        try {
          Thread.sleep(Math.min(checkInterval, remainingTime));
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
    private Killable r;
    private AtomicBoolean running = new AtomicBoolean(true);

    public RunnerThread(Killable r) {
      this.r = r;
    }

    public void kill() {
      r.die();
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
