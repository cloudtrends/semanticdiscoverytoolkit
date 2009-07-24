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


import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple timer utility.
 * <p>
 * @author Spence Koehler
 */
public class Timer {

  private final AtomicLong time = new AtomicLong(0L);
  private long timerMillis;
  private Date lastCheckDate;

  /**
   * Construct to measure timerMillis.
   * <p>
   * Note that by default, timerMillis will have been reached the first
   * time reachedTimerMillis is called.
   */
  public Timer(long timerMillis) {
    this.timerMillis = timerMillis;
    this.lastCheckDate = null;
  }

  /**
   * Construct to measure timerMillis.
   * <p>
   * Note that by default, timerMillis will have been reached when the
   * designated amount of time has elapsed since 'startDate'.
   */
  public Timer(long timerMillis, Date startDate) {
    this.timerMillis = timerMillis;
    time.set(startDate.getTime());
  }

  /**
   * Determine whether timerMillis has elapsed since the last check,
   * resetting the timer if it has.
   */
  public boolean reachedTimerMillis() {
    boolean result = false;

    this.lastCheckDate = new Date();
    if ((lastCheckDate.getTime() - time.get()) > timerMillis) {
      time.set(lastCheckDate.getTime());  // reset timer when time has been reached.
      result = true;
    }

    return result;
  }

  /**
   * Get the amount of time remaining before this timer reaches its limit.
   */
  public long getRemainingTime() {
    return timerMillis - (System.currentTimeMillis() - time.get());
  }

  /**
   * Get the time this timer was last checked.
   * <p>
   * Note that null means it has never been checked.
   */
  public Date getLastCheckDate() {
    return lastCheckDate;
  }

  /**
   * Get the number of millis that this timer measures.
   */
  public long getTimerMillis() {
    return timerMillis;
  }

  /**
   * (Re-)set the number of millis that this timer measures.
   */
  public void setTimerMillis(long timerMillis) {
    this.timerMillis = timerMillis;
  }

  /**
   * Start counting from the mark of 'now'.
   */
  public void setTime() {
    setTime(new Date());
  }

  /**
   * Start counting from the date.
   */
  public void setTime(Date date) {
    time.set(date.getTime());
    this.lastCheckDate = date;
  }
}
