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


import java.io.File;
import java.io.IOException;

import org.sd.util.KVPair;

/**
 * Strategy to map key/value pair input out to structured, numbered files.
 * <p>
 * @author Spence Koehler
 */
public abstract class MapperAction<K, V> implements FlushAction.AddStrategy<K, V> {

  /**
   * Perform the mapping action on the key and value.
   */
  public abstract void doAdd(KVPair<K, V> kvPair);


  private FlushAction<K, V> flushAction;

  /**
   * Construct an instance to flush mapped data into the given directory
   * with generated names.
   *
   * @param flushAction  Action for flushing data.
   */
  protected MapperAction(FlushAction<K, V> flushAction) {
    this.flushAction = flushAction;
  }

  /**
   * Add a mapping for the key/value pair, flushing afterwards if warranted.
   *
   * @return true if flushed; otherwise, false.
   */
  public final boolean map(KVPair<K, V> kvPair) throws IOException {
    return flushAction.add(kvPair, this);
  }

  /**
   * Add a mapping for the key/value pair, flushing afterwards if warranted.
   *
   * @return true if flushed; otherwise, false.
   */
  public final boolean map(K key, V value) throws IOException {
    return flushAction.add(new KVPair<K, V>(key, value), this);
  }

  /**
   * Force a flush to the next file.
   *
   * @return true if flushed; otherwise, false.
   */
  public final boolean flush() throws IOException {
    return flushAction.flush();
  }
}
