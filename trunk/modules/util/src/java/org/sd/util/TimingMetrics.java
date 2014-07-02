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
package org.sd.util;


/**
 * Utility class for collecting timing metrics.
 * <p>
 * @author Spence Koehler
 */
public class TimingMetrics {

  private StatsAccumulator stats;
  private long startTime;
  private long lastMarkTime;
  private long endTime;
  private long pauseTime;
  private long pausedTime;

  /**
   * Default constructor, where id is the empty string.
   */
  public TimingMetrics() {
    this(null);
  }

  /**
   * Construct with the given id, which represents the units being
   * measured over time.
   */
  public TimingMetrics(String id) {
    if (id == null || "".equals(id)) id = "u";
    this.stats = new StatsAccumulator(id);
    init();
  }

  private final void init() {
    stats.clear();
    this.startTime = System.currentTimeMillis();
    this.lastMarkTime = startTime;
    this.endTime = 0L;
    this.pauseTime = 0L;
    this.pausedTime = 0L;
  }

  /**
   * Start and reset the metrics as if this were a new timer.
   */
  public void start() {
    init();
  }

  /**
   * Add a mark for the current time. NOTE: This is ignored if paused or stopped.
   */
  public void mark() {
    if (lastMarkTime > 0) {
      final long curTime = System.currentTimeMillis();
      stats.add(curTime - lastMarkTime);
      lastMarkTime = curTime;
    }
  }

  /**
   * Pause such that the time from when this is called until resume
   * is ignored.
   */
  public void pause() {
    this.lastMarkTime = 0L;
    if (this.pauseTime == 0) this.pauseTime = System.currentTimeMillis();
  }

  /**
   * Resume from a pause (or stop). Note that the startTime remains unchanged.
   */
  public void resume() {
    this.lastMarkTime = System.currentTimeMillis();
    this.endTime = 0L;  // in case we're resuming after a stop.

    if (this.pauseTime > 0) {
      this.pausedTime += (lastMarkTime - pauseTime);
      this.pauseTime = 0L;
    }
  }

  /**
   * Stop collecting metrics.
   */
  public void stop() {
    resume();  // to tally paused time in case we stop while paused

    this.lastMarkTime = 0L;
    this.endTime = System.currentTimeMillis();
    this.pauseTime = endTime;  // in case we resume after all.
  }

  /**
   * Get the current timing metrics. Units are in milliseconds.
   */
  public StatsAccumulator getStats() {
    return stats;
  }


  /**
   * Get the total un-paused time that has been measured.
   */
  public long getTotalTime() {
    final long curTime = (pauseTime > 0) ? pauseTime : (endTime > 0) ? endTime : System.currentTimeMillis();
    return curTime - startTime;
  }

  /**
   * Get the average rate of marks (units) per milliseconds.
   */
  public double getAverageRate() {
    final double timePerUnit = stats.getMean();
    return (timePerUnit > 0) ? 1.0 / stats.getMean() : 0.0;
  }

  /**
   * Get the total number of marks (or units marked).
   */
  public long getTotalMarks() {
    return stats.getN();
  }

  /**
   * Get the minimum amount of time between marks.
   */
  public long getMinTime() {
    return (long)(stats.getMin() + 0.5);
  }

  /**
   * Get the maximum amount of time between marks.
   */
  public long getMaxTime() {
    return (long)(stats.getMax() + 0.5);
  }


  /**
   * Get this instance's information of the form:
   *   x.xxx u/ms (N=xxx, min=x.xxx ms, max=x.xxx ms, t=x.xxx ms)
   */
  public String toString() {
    final StringBuilder result = new StringBuilder();
    final StringBuilder units = new StringBuilder();

    // x.xxx u/ms (N=xxx, min=x.xxx ms, max=x.xxx ms, t=<time> ms)
    final String label = stats.getLabel();
    final double rate = getRate(getAverageRate(), units);

    result.
      append(MathUtil.doubleString(rate, 3)).append(" ").
      append(label).append("/").append(units).
      append(" (N=").append(getTotalMarks()).
      append(", min=").append(MathUtil.timeString(getMinTime(), false)).
      append(", max=").append(MathUtil.timeString(getMaxTime(), false)).
      append(", t=").append(MathUtil.timeString(getTotalTime(), false)).
      append(")");

    return result.toString();
  }

  /**
   * Utility to scale unitsPerMilli to unitsPerT, where T is greater than 1,
   * if possible, up to the granularity of days.
   */
  private final double getRate(double unitsPerMilli, StringBuilder perUnit) {
    double result = unitsPerMilli;
    String pu = "ms";

    if (result >= 0) {
      if (result < 1.0) {
        result *= 1000;  // 1000 ms/sec
        pu = "s";
      }
      if (result < 1.0) {
        result *= 60;    // 60 sec/min
        pu = "min";
      }
      if (result < 1.0) {
        result *= 60;    // 60 min/hr
        pu = "hr";
      }
      if (result < 1.0) {
        result *= 24;    // 24 hr/day
        pu = "d";
      }
    }

    perUnit.setLength(0);
    perUnit.append(pu);
    return result;
  }
}
