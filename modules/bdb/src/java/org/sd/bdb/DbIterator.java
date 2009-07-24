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


import java.util.Iterator;

/**
 * A database iterator differs from a normal iterator in that it must be closed
 * when its use is finished.
 * <p>
 * try {
 *   DbIterator iter = ...;
 *   // do stuff
 * }
 * finally {
 *   iter.close();
 * }
 * @author Spence Koehler
 */
public interface DbIterator<T> extends Iterator<T> {

  /**
   * Close iterator's cursors in the database.
   */
  public void close();

  /**
   * Move the cursor to the given key such that next() will return
   * the next record after the first with the key.
   *
   * @return the matching key value pair if the key was found and
   *         the iterator is positioned at its record such that next()
   *         will return the following record; otherwise, return null
   *         and leave the position of the cursor unchanged.
   */
  public LongKeyValuePair seek(long key);

  /**
   * Move the cursor to the given key/value pair such that next() will
   * return the next record after the first with the key and value.
   *
   * @return the matching key value pair if the key was found and
   *         the iterator is positioned at its record such that next()
   *         will return the following record; otherwise, return null
   *         and leave the position of the cursor unchanged.
   */
  public LongKeyValuePair seek(long key, DbValue value);

  /**
   * Move the cursor to the given key such that next() will return
   * the next record after the first with the key.
   *
   * @return the matching key value pair if the key was found and
   *         the iterator is positioned at its record such that next()
   *         will return the following record; otherwise, return null
   *         and leave the position of the cursor unchanged.
   */
  public StringKeyValuePair seek(String key);

  /**
   * Move the cursor to the given key/value pair such that next() will
   * return the next record after the first with the key and value.
   *
   * @return the matching key value pair if the key was found and
   *         the iterator is positioned at its record such that next()
   *         will return the following record; otherwise, return null
   *         and leave the position of the cursor unchanged.
   */
  public StringKeyValuePair seek(String key, DbValue value);
}
