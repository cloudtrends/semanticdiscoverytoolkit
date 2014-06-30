/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
 * Implementation for collection of stats through a rolling window of time.
 * <p>
 * @author Spence Koehler
 */
public class RollingStats {

  public static final int DEFAULT_WINDOW_WIDTH = 300000;  // 5 minute window
  public static final int DEFAULT_SEGMENT_WIDTH = 5000;   // 5 seconds

  //
  // Algorithm:
  // - Accumulate stats for a segment of time
  // - Keep a rolling queue of segments comprising the window
  // - Rollup and report stats by aggregating all segments
  //
  // Implementation:
  // - Keep a native array of n=windowWidth/segmentWidth StatsAccumulator instances
  // - point to that which corresponds to the current time
  //   - clear out the stats when we switch to it from prior
  //

  private int windowWidth;
  private int segmentWidth;
  private int numSegments;

  private StatsAccumulator cumulativeStats;  // keep track of overall cumulative stats
  private StatsAccumulator[] segmentStats;   // keep track of rolling window stats by segment
  private long starttime;
  private long reftime;
  private int curSegment;

  private final Object addMutex = new Object();

  /**
   * Construct with the default time widths in ms.
   */
  public RollingStats() {
    this(DEFAULT_WINDOW_WIDTH, DEFAULT_SEGMENT_WIDTH);
  }

  /**
   * Construct with the given time widths in ms.
   *
   * @param windowWidth the overall width of the time window.
   * @param segmentWidth the time for collecting each segment.
   */
  public RollingStats(int windowWidth, int segmentWidth) {
    this.windowWidth = windowWidth;
    this.segmentWidth = segmentWidth;
    this.cumulativeStats = new StatsAccumulator("cumulative");

    this.numSegments = (int)(0.5 + (double)windowWidth / (double)segmentWidth);
    this.segmentStats = new StatsAccumulator[numSegments];
    for (int i = 0; i < numSegments; ++i) {
      segmentStats[i] = new StatsAccumulator("segment-" + i);
    }
    this.starttime = System.currentTimeMillis();
    this.reftime = starttime;
    this.curSegment = 0;
  }

  /**
   * Get the number of segments.
   */
  public int getNumSegments() {
    return numSegments;
  }

  /**
   * Get the (up-to-date) current segment.
   */
  public int getCurSegment() {
    int result = -1;
    synchronized (addMutex) {
      incToCurrentSegment();
      result = curSegment;
    }
    return result;
  }

  /**
   * Get the curSegment that was applicable at the last refTime.
   */
  public int getLastSegmentNum() {
    return curSegment;
  }

  /**
   * Get the start time for this instance.
   */
  public long getStartTime() {
    return starttime;
  }

  /**
   * Get the time of the last add or getWindowStats.
   */
  public long getRefTime() {
    return reftime;
  }

  /**
   * Reset these stats, including the rolling window start time and cumulative
   * stats.
   */
  public void reset() {
    synchronized (addMutex) {
      this.starttime = System.currentTimeMillis();
      this.reftime = starttime;
      this.curSegment = 0;
      for (StatsAccumulator segment : segmentStats) {
        segment.clear();
      }
      synchronized (cumulativeStats) {
        cumulativeStats.clear();
      }
    }
  }

  /**
   * Add the current value.
   * <p>
   * @return the segment number the value was added to (useful for testing).
   */
  public int add(double value) {
    int result = -1;

    synchronized (addMutex) {
      incToCurrentSegment();
      segmentStats[curSegment].add(value);
      result = curSegment;
      synchronized (cumulativeStats) {
        cumulativeStats.add(value);
      }
    }

    return result;
  }

  /**
   * Get the cumulative stats.
   */
  public StatsAccumulator getCumulativeStats() {
    return cumulativeStats;
  }

  /**
   * Get the stats for the current window.
   */
  public StatsAccumulator getWindowStats() {
    StatsAccumulator result = null;
    synchronized (addMutex) {
      incToCurrentSegment();
      result = StatsAccumulator.combine("Window-" + (reftime - windowWidth) + "-" + reftime, segmentStats);
    }
    return result;
  }

  private final void incToCurrentSegment() {
    final long result = System.currentTimeMillis();  // return the reference time
    final int segNum = (int)((double)((result - starttime) % windowWidth) / (double)segmentWidth);
    
    final long diff = result - reftime;
    if (segNum != curSegment || diff > segmentWidth /*wrapped around*/) {
      if (segmentStats.length == 1) {
        // special case: wrapped around one and only segment in window
        segmentStats[0].clear();
      }
      else {
        // walk up to and including new current segment, clearing each
        final int nextSegNum = (segNum + 1) % segmentStats.length;
        for (int i = (curSegment + 1) % segmentStats.length; i != nextSegNum; i = (i + 1) % segmentStats.length) {
          segmentStats[i].clear();
        }
        this.curSegment = segNum;
      }
    }

    this.reftime = result;
  }
}
