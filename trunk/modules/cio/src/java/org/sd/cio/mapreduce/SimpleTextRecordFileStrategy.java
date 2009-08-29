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
package org.sd.cio.mapreduce;


/**
 * A TextRecordFileStrategy that uses a vertical bar delimiter and html
 * escaping.
 * <p>
 * @author Spence Koehler
 */
public abstract class SimpleTextRecordFileStrategy<K, V> extends TextRecordFileStrategy<K, V> {

  /**
   * Convert the (unescaped) key string back into a key instance.
   */
  protected abstract K stringToKey(String keyString);


  protected SimpleTextRecordFileStrategy() {
    super("|");
  }
  
  /**
   * Get an escaped form of the key for safe usage of the delimiter.
   */
  protected String escapeKey(K key) {
    return key.toString().replaceAll("\\|", "&#124;");
  }

  /**
   * Unescape and convert the key string back into a key.
   */
  protected K unescapeKey(String keyString) {
    return stringToKey(keyString.replaceAll("&#124;", "|"));
  }

  /**
   * Convert the value instance to a string.
   */
  protected String valueToString(V value) {
    return value.toString();
  }
}
