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

import org.sd.util.MathUtil;
import org.sd.util.StatsAccumulator;

/**
 * Container and communications medium for counting units of work.
 * <p>
 * The UnitCounter is intended to be used in long-running repetitive tasks as
 * a means of communicating with another process (running in another thread).
 * <p>
 * The communication from the task using a counter includes progress toward
 * completion while the communication to the task through the counter is for
 * flow control. Specifically, for pausing, resuming, and/or terminating the
 * task.
 * <p>
 * Usage:
 * <pre>
 *   this.uc = new UnitCounter(toBeDone);  // note: provide an accessor for consumers to get this instance
 *   uc.markStartNow();
 *   while (iterating) {
 *     if (uc.isTimeToQuit()) {  // checks for die and waits for pause to resume
 *       //...;  // clean up for clean quit
 *       break;
 *     }
 *
 *     //...;  // do unit processing; can send uc.die() down into stack for checking
 *
 *     if (!uc.inc()) {  // inc will count, but will return false if time to die
 *       //...;  // clean up for clean quit
 *       break;
 *     }
 *   };
 *   uc.markEndNow();
 * </pre>
 *
 * @author Spence Koehler
 */
public class UnitCounter {

  public static enum Status {IDLE, RUNNING, PAUSED, KILLED, DONE};


  private long toBeDone;
  private StatsAccumulator timePerUnit;
  private long startTime;
  private AtomicBoolean paused;
  private long pauseTime;
  private long pauseCheckInterval;
  private long incTime;
  private final AtomicBoolean die;
  private long endTime;

  /**
   * Construct with an unknown number of units to be done.
   */
  public UnitCounter() {
    this(-1L);
  }

  /**
   * Construct with the number of units to be done.
   * 
   * @param toBeDone  the number of units to be done (-1 if unknown.)
   */
  public UnitCounter(long toBeDone) {
    this.toBeDone = toBeDone;
    this.timePerUnit = new StatsAccumulator("timePerUnit");
    this.startTime = -1L;
    this.paused = new AtomicBoolean(false);
    this.pauseTime = -1L;
    this.pauseCheckInterval = 100;
    this.incTime = -1L;
    this.die = new AtomicBoolean(false);
    this.endTime = -1L;
  }
  
  /**
   * Get this instance's current status.
   * <ul>
   * <li>IDLE -- Waiting to be started.</li>
   * <li>RUNNING -- Actively processing.</li>
   * <li>PAUSED -- Has been started but is currently paused.</li>
   * <li>KILLED -- Has been killed.</li>
   * <li>DONE -- Has run to completion and is finished.</li>
   * </ul>
   */
  public Status getStatus() {
    Status result = Status.IDLE;

    if (endTime >= 0) {
      result = die.get() ? Status.KILLED : Status.DONE;
    }
    else if (startTime >= 0) {
      result = paused.get() ? Status.PAUSED : Status.RUNNING;
    }

    return result;
  }

  /**
   * Get the total number of units of work to be done.
   *
   * @return the total number of units of work to be done or -1 if unknown.
   */
  public long toBeDone() {
    return toBeDone;
  }

  /**
   * Reset the number of units to be done.
   */
  public void setToBeDone(long toBeDone) {
    this.toBeDone = toBeDone;
  }

  /**
   * Get the number of units of work done so far.
   *
   * @return  the total number of units of work done so far or -1 if counting has not been started.
   */
  public long doneSoFar() {
    return startTime < 0 ? -1 : (long)timePerUnit.getN();
  }

  /**
   * Mark the starting time as now.
   */
  public void markStartNow() {
    markStart(System.currentTimeMillis());
  }

  /**
   * Mark the starting time as the given startTime.
   */
  public void markStart(long startTime) {
    this.startTime = startTime;
    if (this.incTime < 0) this.incTime = System.currentTimeMillis();
  }

  /**
   * Get the start time.
   *
   * @return the startTime or -1 if it has not been set.
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Get the amount of time that the counter has been running.
   */
  public long getTime() {
    return (startTime <= 0) ? 0L : System.currentTimeMillis() - startTime;
  }

  /**
   * Mark the ending time as now.
   */
  public void markEndNow() {
    markEnd(System.currentTimeMillis());
  }

  /**
   * Mark the ending time as the given endTime.
   */
  public void markEnd(long endTime) {
    this.endTime = endTime;
  }

  /**
   * Get the end time.
   *
   * @return the endTime or -1 if it has not been set.
   */
  public long getEndTime() {
    return endTime;
  }

  /**
   * Determine whether this counter has ended (as indicated by an endTime
   * having been set.)
   */
  public boolean hasEnded() {
    return endTime >= 0;
  }

  /**
   * Increment this counter.
   * <p>
   * Note that a counter is not thread-safe. It is assumed to be counting the
   * units of work performed in a single thread!
   */
  public boolean inc() {
    return inc(false);
  }

  /**
   * Increment this counter.
   * <p>
   * Note that a counter is not thread-safe. It is assumed to be counting the
   * units of work performed in a single thread!
   */
  public boolean inc(boolean enforceCountLimit) {

    final long curTime = System.currentTimeMillis();
    timePerUnit.add(curTime - incTime);
    this.incTime = curTime;

    if (enforceCountLimit && toBeDone > 0 && doneSoFar() >= toBeDone) {
      markEndNow();
    }

    return getStatus() == Status.RUNNING;
  }

  /**
   * Mark counting as being paused and set the time to sleep between checking
   * for resume to the given checkInterval.
   */
  public void pause(long checkInterval) {
    if (this.paused.compareAndSet(false, true)) {
      this.pauseTime = System.currentTimeMillis();
    }
    this.pauseCheckInterval = checkInterval;
  }

  /**
   * Determine whether this counter is paused.
   */
  public boolean isPaused() {
    return this.paused.get();
  }

  /**
   * Determine whether this counter is in an interrupted state.
   * <p>
   * @return true if this counter has been paused or killed; otherwise, false.
   */
  public boolean interrupted() {
    return this.paused.get() || this.die.get();
  }

  /**
   * Check for die and wait for pause to resume.
   */
  public boolean isTimeToQuit() {
    if (die.get()) return true;

    boolean result = false;

    while (paused.get()) {  // didn't get resumed yet
      if (die.get()) {
        result = true;  // time to die
        break;
      }

      // sleep for a spell
      try {
        Thread.sleep(pauseCheckInterval);
      }
      catch (InterruptedException e) {
        result = true;  // interrupted without resume == quit
        break;
      }
    }

    return result;
  }

  /**
   * Un-pause this counter.
   */
  public void resume() {
    if (this.paused.compareAndSet(true, false) && pauseTime > 0) {
      //// adjust startTime as if we hadn't paused
      final long curTime = System.currentTimeMillis();
      //this.startTime += curTime - pauseTime;
      this.pauseTime = -1L;
      this.incTime = curTime;
    }
  }

  /**
   * Get the handle to the AtomicBoolean that indicates that processing should
   * terminate, tying up loose ends and providing partial results if possible.
   * <p>
   * Consumers should monitor (and/or set) this variable.
   */
  public AtomicBoolean die() {
    return die;
  }

  /**
   * Send a signal to die through this instance.
   * <p>
   * NOTE: This simply sets the die flag to true.
   */
  public void kill() {
    final boolean hasEnded = hasEnded();
    die.set(true);
    if (!hasEnded) markEndNow();
  }

  /**
   * Get the ratio of completion.
   * <ul>
   * <li>doneSoFar -- the total number of units of work done so far or -1 if
   *                  counting has not been started.</li>
   * <li>toBeDone -- the total number of units of work to be done or -1 if
   *                 unknown.</li>
   * </ul>
   *
   * @return {doneSoFar, toBeDone}
   */
  public long[] getCompletionRatio() {
    return new long[]{doneSoFar(), toBeDone()};
  }

  /**
   * Convenience method to interpret the completion ratio and get the
   * percentage of completion if available.
   *
   * @return -1 if not started, -2 if unknown, or the current percentage.
   */
  public double getPercentComplete() {
    final long[] completionRatio = getCompletionRatio();
    return getPercentComplete(completionRatio);
  }

  /**
   * Convenience method to interpret the completion ratio and get the
   * percentage of completion if available.
   *
   * @return -1 if not started, -2 if unknown, or the current percentage.
   */
  public double getPercentComplete(long[] completionRatio) {
    double result = 0.0;

    final long doneSoFar = completionRatio[0];
    final long toBeDone = completionRatio[1];

    if (toBeDone > 0) {
      if (doneSoFar >= 0) {
        result = (double)doneSoFar / (double)toBeDone;
      }
      else {
        result = -1;  // not started if doneSoFar < 0
      }
    }
    else {
      result = -2;  // unknown if toBeDone < 0
    }

    return result;
  }

  /**
   * Convenience method to compute the average time per unit.
   *
   * @return the average time per unit or -1 if the process has not been started.
   */
  public double getAverageTimePerUnit() {
    return getAverageTimePerUnit(doneSoFar());
  }

  /**
   * Convenience method to compute the average time per unit.
   *
   * @return the average time per unit or -1 if the process has not been started.
   */
  public double getAverageTimePerUnit(long doneSoFar) {
    double result = 0.0;

    if (startTime <= 0 || doneSoFar <= 0) {
      result = -1;  // hasn't been started
    }
    else {
      final long markTime = (endTime > 0) ? endTime : System.currentTimeMillis();
//      result = (double)doneSoFar / (double)(markTime - startTime);
      result = (double)doneSoFar / timePerUnit.getSum();
    }

    return result;
  }

  /**
   * Convenience method to compute the estimated remaining time until completion.
   *
   * @return the estimated remaining time or -1 if not started or -2 if toBeDone is unknown.
   */
  public long getEstimatedRemainingTime() {
    long result = 0;

    final double timePerUnit = getAverageTimePerUnit();
    if (timePerUnit < 0) {
      result = -1;  // not started yet.
    }
    else {
      final long toBeDone = toBeDone();
      if (toBeDone < 0) {
        result = -2;
      }
      else {
        final long doneSoFar = doneSoFar();
        final double remainingUnits = toBeDone - doneSoFar;
        result = Math.round(remainingUnits / timePerUnit);
      }
    }

    return result;
  }

  /**
   * Heuristic to determine the amount of time a running process appears to
   * have been stalled. It assumes that each unit of work would take the same
   * amount of time (on average). It returns the time since the last increment
   * that exceeds 2 standard deviations larger than the mean time per unit
   * so far.
   *
   * @return an estimate for the time stalled or 0 if there is no apparent stall.
   */
  public long getTimeStalled() {
    if (doneSoFar() < 1) return 0;

    final long curTime = System.currentTimeMillis();
    final double cutoff = timePerUnit.getMean() + 2 * timePerUnit.getStandardDeviation();

    long result = ((incTime - curTime) - Math.round(cutoff));

    if (result <= 0) result = 0;

    return result;
  }

  /**
   * Get a string representation of this instance.
   */
  public String toString() {
    final StringBuilder result = new StringBuilder();

    // status counts runTime elapsedTime
    // [running/paused/done(STALLED stalledTime)] doneSoFar/toBeDone(%) @ rate/unit runTime=X elapsed=X()

    // status "[running/paused/done(STALLED stalledTime)] "
    final Status status = getStatus();
    result.append('[').append(status);
    final long stalledTime = getTimeStalled();
    if (stalledTime > 0) {
      result.append("(STALLED ").append(MathUtil.timeString(stalledTime, false)).append(')');
    }
    result.append("] ");
    
    // counts "doneSoFar/toBeDone(%) @ rate/unit"
    if (status != Status.IDLE) {
      final long[] completionRatio = getCompletionRatio();
      final long markTime = (endTime > 0) ? endTime : System.currentTimeMillis();
      final double pct = getPercentComplete(completionRatio);

      result.append(completionRatio[0]);
      if (completionRatio[1] > 0) {
        result.append('/').append(completionRatio[1]);
      }
      if (pct >= 0) {
        result.append('(').append(MathUtil.doubleString(pct * 100.0, 3)).append("%)");
      }
      final double rate = getAverageTimePerUnit(completionRatio[0]);
      if (rate >= 0) {
        result.append(" @ ").append(MathUtil.rateString(rate, 3));
      }

      // runTime=X (not including pause time)
      result.append(" cpu=").append(MathUtil.timeString(Math.round(timePerUnit.getSum()), false));

      // elapsed...
      result.append(" elapsed=").append(MathUtil.timeString(markTime - startTime, false));

      // elapsed=X(since Y with ERT to go)
      if (endTime > 0) {
        result.
          append("(from ").append(MathUtil.dateString(startTime)).
          append(" until ").append(MathUtil.dateString(endTime)).append(')');
      }
      else {
        // elapsed=X(from Y until Z) (includes pause time, capped when finished)
        final double ert = getEstimatedRemainingTime();

        result.append("(since ").append(MathUtil.dateString(startTime));
        if (completionRatio[1] > 0) {
          result.
            append(" with ").append(MathUtil.timeString(Math.round(ert), false)).
            append(" to go)");
        }
        else {
          result.append(')');
        }
      }
    }

    return result.toString();
  }
}
