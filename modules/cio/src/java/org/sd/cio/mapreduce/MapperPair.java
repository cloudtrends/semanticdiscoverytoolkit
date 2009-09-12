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


import org.sd.util.KVPair;

/**
 * Container for a key/value pair that also retains the pair's action key.
 * <p>
 * @author Spence Koehler
 */
public abstract class MapperPair<K, V, A> extends KVPair<K, V> {
  
  /** Construct with the given key and value. */
  protected MapperPair(K key, V value) {
    super(key, value);
  }

  /**
   * Get this mapped pair's action key
   * <p>
   * The action key is used to retrieve the MapperAction instance that is
   * relevant to this pair (see the AbstractMapperActionFactory) where the
   * MapperAction performs the map operation on this pair, queuing the result
   * for output through the MapperAction's FlushAction.
   */
  public abstract A getActionKey();

}
