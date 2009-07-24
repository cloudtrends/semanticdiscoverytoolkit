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


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A specialized thread that runs and provides access to a Governable instance.
 * <p>
 * (See BaseGovernable for more info.)
 * <p>
 * <b>Usage examples:</b>
 * <p>
 * An example to perform an operation for a number of times or a certain length
 * of time, whichever comes first, would be:
 * <pre>
 * GovernableThread.newGovernableThread(new BaseGovernable(numTimes) {
 *     protected boolean doOperation(long workUnit, AtomicBoolean die) {
 *       //...do operation number workUnit here...
 *       return true;  // or false to end the thread, not counting the last op.
 *     }
 *   }, true).runFor(duration, waitMillis, interrupt);
 * </pre>
 * <p>
 * Or, for an unlimited number of times, leave off 'numTimes':
 * <pre>
 * GovernableThread.newGovernableThread(new BaseGovernable() {
 *     protected boolean doOperation(long workUnit, AtomicBoolean die) {
 *       //...do operation number workUnit here...
 *       //do 'die.set(true)' to explicitly force an end where this operation <b>is<b> counted.
 *       //return false to explicitly force an end where this operation is <b>NOT<b> counted.
 *       return true;
 *     }
 *   }, true).runFor(duration, waitMillis, interrupt);
 * </pre>
 *
 * @author Spence Koehler
 */
public class GovernableThread extends Thread {

  /**
   * Enumeration of possible run results returned by the runFor and waitFor
   * methods.
   */
  public static enum RunResult {
    COMPLETED,  // ran to completion
    PARTIAL,    // attempted to kill and succeeded
    FAILED,     // attempted to kill and failed
    RUNNING;    // process is still running
  };


  /**
   * Create a new GovernableThread for the Governable instance.
   *
   * @param start  If true, then start the thread before returning it.
   *
   * @return a new GovernableThread for the Governable instance.
   */
  public static GovernableThread newGovernableThread(Governable governable, boolean start) {
    final GovernableThread result = new GovernableThread(governable);
    if (start) result.start();
    return result;
  }


  private Governable governable;
  private ScheduledExecutorService resumeScheduler;

  /**
   * Construct with the given governable.
   */
  public GovernableThread(Governable governable) {
    super(governable);

    this.governable = governable;
    this.resumeScheduler = Executors.newScheduledThreadPool(1);
  }
  
  /**
   * Accessor for this thread's governable.
   */
  public Governable getGovernable() {
    return governable;
  }

  /**
   * Convenience accessor for this thread's governable's unit counter
   * through which to communicate with the governable instance.
   */
  public UnitCounter getUnitCounter() {
    return governable.getUnitCounter();
  }

  /**
   * Pause the governable for the given time duration (in millis).
   * <p>
   * NOTE: To pause without a specific duration, access the UnitCounter
   *       directly.
   */
  public void pauseFor(long duration, long checkInterval) {
    final UnitCounter uc = governable.getUnitCounter();
    uc.pause(checkInterval);
    resumeScheduler.schedule(new Runnable() {
        public void run() { uc.resume(); }
      }, duration, TimeUnit.MILLISECONDS);
  }

  /**
   * Let this thread run for the given duration (or until finished or
   * interrupted) before killing it if it hasn't already ended on its
   * own. Block until the operation has finished.
   * <p>
   * This essentially puts a time limit on the running of an operation.
   * <p>
   * NOTE: If the thread has not been started yet, this method will start
   *       the thread.
   *
   * @param duration  The maximum number of millis to run (0 means forever.)
   * @param waitMillis  The time to wait for the process to die after the
   *                    duration has expired.
   * @param interrupt  If true, then send an interrupt signal to the thread
   *                   if it is not dying nicely.
   *
   * @return the result of the run (completed, partial, or failed).
   */
  public RunResult runFor(long duration, long waitMillis, boolean interrupt) {
    RunResult result = RunResult.COMPLETED;  // ran to completion

    waitFor(duration);  // start (if needed) and wait for duration

    // didn't finish in the specified time
    if (this.isAlive()) {
      result = RunResult.PARTIAL;  // attempted to kill and succeeded
      if (!this.kill(true, waitMillis, interrupt)) {
        result = RunResult.FAILED;  // attempted to kill and failed
      }
    }

    return result;
  }

  /**
   * Let this thread run for the given duration (or until finished or
   * interrupted) before returning its status as a run result.
   * <p>
   * NOTE: If the thread has not been started yet, this method will start
   *       the thread.
   *
   * @param duration  The maximum length of millis to let the thread run.
   *
   * @return the current status of the run (completed, running).
   */
  public RunResult waitFor(long duration) {
    if (!this.isAlive()) this.start();

    try {
      this.join(duration);  // wait for thread to finish on its own.
    }
    catch (InterruptedException ignore) {}  // time to stop waiting to join

    return this.isAlive() ? RunResult.RUNNING : RunResult.COMPLETED;
  }

  /**
   * Kill the governable instance.
   *
   * @param nice  If true, then first try to be nice by communicating with
   *              the process through the UnitCounter for at least waitMillis.
   *              If false, then interrupt the governable thread according to
   *              the value of interrupt.
   * @param waitMillis  The length of time to wait while being nice and to
   *                    wait again for the thread to join. Note that in the
   *                    case of waiting to join, 0 means forever.
   * @param interrupt  If true, then after being nice (if we are being nice,)
   *                   interrupt the running thread.
   *
   * @return whether the governable appears to have stopped
   */
  public boolean kill(boolean nice, long waitMillis, boolean interrupt) {
    final UnitCounter uc = governable.getUnitCounter();

    uc.kill();  // be nice no matter what

    if (nice && waitMillis > 0) {  // only wait to be nice if asked to
      for (long i = 0; isAlive() && i < waitMillis; ++i) {
        try {
          Thread.sleep(1);
        }
        catch (InterruptedException ignore) {break;}
      }
    }
    if (interrupt) {
      this.interrupt();
    }

    if (this.isAlive()) {
      try {
        this.join(waitMillis);
      }
      catch (InterruptedException ignore) {}
    }

    return !this.isAlive();
  }
}
