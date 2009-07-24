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


import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;

/**
 * Wrapper for a timestamped value.
 * <p>
 * @author Spence Koehler
 */
public class TimestampedValue extends DbValue {

  private static final TsTupleBinding TUPLE_BINDING = new TsTupleBinding();
  private static final TupleBinding<Long> LONG_TUPLE_BINDING = TupleBinding.getPrimitiveBinding(Long.class);
  public static final TsKeyCreator KEY_CREATOR = new TsKeyCreator(TUPLE_BINDING);


  /**
   * Get the TimestampedValue from the DatabaseEntry.
   */
  public static final TimestampedValue getTsRecord(DatabaseEntry dbEntry) {
    return TUPLE_BINDING.entryToObject(dbEntry);
  }

  /**
   * Turn the TimestampedValue into a DatabaseEntry.
   */
  public static final DatabaseEntry getDbEntry(TimestampedValue tsValue) {
    final DatabaseEntry result = new DatabaseEntry();
    TUPLE_BINDING.objectToEntry(tsValue, result);
    return result;
  }

  /**
   * Turn the timestamp and dbValue into a DatabaseEntry.
   */
  public static final DatabaseEntry getDbEntry(long timestamp, DbValue dbValue) {
    return getDbEntry(new TimestampedValue(timestamp, dbValue.getValueBytes()));
  }

//   /**
//    * Turn the value into a TimestampedValue DatabaseEntry.
//    */
//   public static final DatabaseEntry getDbEntry(String value) {
//     return getDbEntry(new TimestampedValue(value));
//   }

  /**
   * Turn the value into a TimestampedValue DatabaseEntry.
   */
  public static final DatabaseEntry getDbEntry(byte[] valueBytes) {
    return getDbEntry(new TimestampedValue(valueBytes));
  }


  /**
   * Construct with the given value.
   */
  public TimestampedValue(String value) {
    super(value, -1L);
  }

  /**
   * Construct with the given value.
   */
  public TimestampedValue(byte[] valueBytes) {
    super(valueBytes, -1L);
  }

  /**
   * Construct with timestamp and value.
   */
  private TimestampedValue(long timestamp, byte[] valueBytes) {
    super(valueBytes, timestamp);
  }

  /**
   * Helper class for serializing/deserializing a record.
   */
  private static final class TsTupleBinding extends TupleBinding<TimestampedValue> {
    public void objectToEntry(TimestampedValue tsValue, TupleOutput to) {
      to.writeLong(tsValue.getTimestamp());

      final byte[] bytes = tsValue.getValueBytes();
      to.writeInt(bytes.length);
      for (byte b : bytes) to.writeUnsignedByte(b);
    }

    public TimestampedValue entryToObject(TupleInput ti) {
      final long timestamp = ti.readLong();
      final int numBytes = ti.readInt();
      final byte[] valueBytes = new byte[numBytes];

      for (int i = 0; i < numBytes; ++i) {
        valueBytes[i] = (byte)ti.readUnsignedByte();
      }

      return new TimestampedValue(timestamp, valueBytes);
    }
  }

  public static final class TsKeyCreator implements SecondaryKeyCreator {
    private TupleBinding<TimestampedValue> tupleBinding;

    public TsKeyCreator(TupleBinding<TimestampedValue> tupleBinding) {
      this.tupleBinding = tupleBinding;
    }

    public boolean createSecondaryKey(SecondaryDatabase secondaryDatabase,
                                      DatabaseEntry keyEntry,
                                      DatabaseEntry dataEntry,
                                      DatabaseEntry resultEntry) {
      final TimestampedValue tsValue = tupleBinding.entryToObject(dataEntry);
      final long timestamp = tsValue.getTimestamp();
      LONG_TUPLE_BINDING.objectToEntry(timestamp, resultEntry);
      return true;
    }
  }
}
