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


import java.io.IOException;

/**
 * Factory class for MapperAction instances to be used by a Mapper.
 * <p>
 * @author Spence Koehler
 */
public interface FlushActionFactory<K, V, A> {

  /** Get (or build) the action relevant to the mapper pair. */
  public FlushAction<K, V, A> getFlushAction(MapperPair<K, V, A> pair);

  /** Get the action for the given key or null if non-existent */
  public FlushAction<K, V, A> getFlushAction(A actionKey);

  /**
   * Finalize when usage of this instance is complete.
   * <p>
   * Typically, this will flush all of the mapper action instances.
   */
  public void finalize() throws IOException;
}
