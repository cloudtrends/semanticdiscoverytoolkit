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
package org.sd.extract.datetime;


import java.util.BitSet;

/**
 * Data structure to identify presence/absence of date/time pieces.
 * <p>
 * @author Spence Koehler
 */
public class DateTimeFlags {

  private static final int YEAR = 0;
  private static final int MONTH = 1;
  private static final int DAY = 2;
  private static final int HOUR = 3;
  private static final int MINUTE = 4;
  private static final int SECOND = 5;
  private static final int AMPM = 6;
  private static final int GUESSED_YEAR = 7;
  private static final int NUM_BITS = 8;

  private BitSet flags;

  public DateTimeFlags() {
    this.flags = new BitSet(NUM_BITS);
  }

  public DateTimeFlags(DateTimeInterpretation datetime) {
    this();
    if (datetime != null) {
      if (datetime.hasYear()) flags.set(YEAR);
      if (datetime.hasMonth()) flags.set(MONTH);
      if (datetime.hasDay()) flags.set(DAY);
      if (datetime.hasHour()) flags.set(HOUR);
      if (datetime.hasMinute()) flags.set(MINUTE);
      if (datetime.hasSecond()) flags.set(SECOND);
      if (datetime.hasAmPm()) flags.set(AMPM);
      if (datetime.guessedYear()) flags.set(GUESSED_YEAR);
    }
  }

  /**
   * Copy constructor.
   */
  public DateTimeFlags(DateTimeFlags other) {
    this.flags = (BitSet)(other.flags.clone());
  }

  /**
   * Or the other flags into this.
   */
  public void or(DateTimeFlags other) {
    this.flags.or(other.flags);
  }

  /**
   * And the other flags into this.
   */
  public void and(DateTimeFlags other) {
    this.flags.and(other.flags);
  }

  public boolean hasYear() {
    return flags.get(YEAR);
  }

  public boolean hasMonth() {
    return flags.get(MONTH);
  }

  public boolean hasDay() {
    return flags.get(DAY);
  }

  public boolean hasHour() {
    return flags.get(HOUR);
  }

  public boolean hasMinute() {
    return flags.get(MINUTE);
  }

  public boolean hasSecond() {
    return flags.get(SECOND);
  }

  public boolean hasAmPm() {
    return flags.get(AMPM);
  }

  public boolean guessedYear() {
    return flags.get(GUESSED_YEAR);
  }

  public boolean equals(Object o) {
    boolean result = false;
    if (o instanceof DateTimeFlags) {
      final DateTimeFlags other = (DateTimeFlags)o;
      result = flags.equals(other.flags);
    }
    return result;
  }

  public int hashCode() {
    return flags.hashCode();
  }

  public boolean hasDateOnly() {
    return flags.get(DAY) && !flags.get(HOUR);
  }

  public boolean hasTimeOnly() {
    return !flags.get(DAY) && flags.get(HOUR);
  }

  public boolean hasDateAndTime() {
    return flags.get(DAY) && flags.get(HOUR);
  }
}
