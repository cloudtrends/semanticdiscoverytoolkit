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


/**
 * Container class for a key/value pair.
 * <p>
 * @author Spence Koehler
 */
public class LongKeyValuePair extends DbValue {

  private long key;

  public LongKeyValuePair(long key, String value) {
    this(key, value, 0L);
  }

  public LongKeyValuePair(long key, String value, long timestamp) {
    super(value, timestamp);
    this.key = key;
  }

  public LongKeyValuePair(long key, byte[] valueBytes) {
    this(key, valueBytes, 0L);
  }

  public LongKeyValuePair(long key, byte[] valueBytes, long timestamp) {
    super(valueBytes, timestamp);
    this.key = key;
  }

  public long getKey() {
    return key;
  }
}
