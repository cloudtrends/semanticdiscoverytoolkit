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
package org.sd.bdb;


import org.sd.io.Publishable;

import com.sleepycat.je.DatabaseEntry;

/**
 * Base container class for a db value.
 * <p>
 * The value can take the form of a string, byte array, and/or publishable
 * object. A timestamp can be associated with a value. An initial timestamp
 * of -1L indicates that the timestamp should be initialized from the
 * system clock at the time the timestamp is first accessed from the instance
 * (which typically will occur at the time of serialization). An initial
 * timestamp of 0 indicates that the timestamp is and will remain undefined
 * until or unless explicitly set.
 *
 * @author Spence Koehler
 */
public class DbValue {

  public static final DbValue EMPTY = new DbValue("");


  private byte[] valueBytes;
  private String value;
  private Publishable publishable;
  private long timestamp;

  public DbValue(String value) {
    this(value, 0L);
  }

  public DbValue(byte[] valueBytes) {
    this(valueBytes, 0L);
  }

  public DbValue(Publishable publishable) {
    this(publishable, 0L);
  }

  public DbValue(String value, long timestamp) {
    this.value = value;
    this.valueBytes = null;
    this.publishable = null;
    this.timestamp = timestamp;
  }

  public DbValue(byte[] valueBytes, long timestamp) {
    this.valueBytes = valueBytes;
    this.value = null;
    this.publishable = null;
    this.timestamp = timestamp;
  }

  public DbValue(Publishable publishable, long timestamp) {
    this.value = null;
    this.valueBytes = null;
    this.publishable = publishable;
  }

  public boolean isEmpty() {
    boolean result = (value == null && valueBytes == null && publishable == null);

    if (!result) {
      result = (value != null && value.length() == 0) || (valueBytes != null && valueBytes.length == 0);
      // note: assume the publishable is 'non-empty'.
    }

    return result;
  }

  public String getValue() {
    if (value == null) {
      value = DbUtil.getString(getValueBytes());
    }
    return value;
  }

  public byte[] getValueBytes() {
    if (valueBytes == null) {
      if (value != null) {
        valueBytes = DbUtil.getBytes(value);
      }
      else if (publishable != null) {
        valueBytes = DbUtil.getBytes(publishable);
      }
    }
    return valueBytes;
  }

  public Publishable getPublishable() {
    if (publishable == null) {
      publishable = DbUtil.getPublishable(getValueBytes());
    }
    return publishable;
  }

  public String toString() {
    String result = null;

    if (publishable != null) {
      result = publishable.toString();
    }
    else if (value != null) {
      result = value;
    }
    else {
      try {
        final Publishable publishable = getPublishable();
        result = publishable.toString();
      }
      catch (Exception e) {
        result = getValue();
      }
    }

    return result;
  }

  public boolean hasTimestamp() {
    return timestamp != 0L;  //note: -1 is defined, we just don't know what it is yet.
  }

  public long getTimestamp() {
    if (timestamp < 0) setTimestamp();
    return timestamp;
  }

  /**
   * Set this record's timestamp to the current time.
   */
  public void setTimestamp() {
    setTimestamp(System.currentTimeMillis());
  }

  /**
   * Set this record's timestamp to the given value.
   */
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * Get a database entry for the value, adding a timestamp if indicated.
   * <p>
   * Note that a timestamp will be added regardless of whether this value is
   * already a timestamped value if addTimestamp is true.
   */
  final DatabaseEntry getValueEntry(boolean addTimestamp) {
    DatabaseEntry result = null;

    final byte[] bytes = getValueBytes();
    if (addTimestamp) {
      result = TimestampedValue.getDbEntry(bytes);
    }
    else {
      result = new DatabaseEntry(bytes);
    }

    return result;
  }
}
