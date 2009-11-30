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
 * Structure to hold a key/value pair.
 * <p>
 * @author Spence Koehler
 */
public class KVPair<K, V> {

  public final K key;
  public final V value;

  public KVPair(K key, V value) {
    this.key = key;
    this.value = value;
  }

  public boolean equals(Object o) {
    boolean result = false;

    if (o instanceof KVPair) {
      final KVPair other = (KVPair)o;

      if (key == null || value == null) {
        if (key == null && value != null) {
          result = (other.key == null) && value.equals(other.value);
        }
        else if (value == null && key != null) {
          result = other.value == null && key.equals(other.key);
        }
        else {
          result = (other.key == null && other.value == null);
        }
      }
      else {
        result = key.equals(other.key) && value.equals(other.value);
      }
    }

    return result;
  }

  public int hashCode() {
    int result = 1;

    if (key != null) {
      result = (result * 17) + key.hashCode();
    }
    if (value != null) {
      result = (result * 17) + value.hashCode();
    }

    return result;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append('(').append(key).append(':').append(value).append(')');
    return result.toString();
  }
}
