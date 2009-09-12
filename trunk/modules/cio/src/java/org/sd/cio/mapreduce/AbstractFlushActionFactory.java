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
import java.util.HashMap;
import java.util.Map;


/**
 * Abstract FlushActionFactory base class.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractFlushActionFactory<K, V, A> implements FlushActionFactory<K, V, A> {

  protected abstract FlushAction<K, V, A> buildFlushAction(MapperPair<K, V, A> mapperPair);

  private Map<A, FlushAction<K, V, A>> flushActions;

  protected AbstractFlushActionFactory() {
    this.flushActions = new HashMap<A, FlushAction<K, V, A>>();
  }

  /** Get (or build) the action relevant to the mapper pair. */
  public final FlushAction<K, V, A> getFlushAction(MapperPair<K, V, A> mapperPair) {
    final A actionKey = mapperPair.getActionKey();
    FlushAction<K, V, A> result = flushActions.get(actionKey);

    if (result == null) {
      // create flush action for the key
      result = buildFlushAction(mapperPair);
      // cache the flush action by the key
      flushActions.put(actionKey, result);
    }

    return result;
  }

  /** Get the action for the given key or null if non-existent */
  public FlushAction<K, V, A> getFlushAction(A actionKey) {
    return flushActions.get(actionKey);
  }

  /**
   * Finalize when usage of this instance is complete.
   * <p>
   * This will flush all of the flush action instances.
   */
  public final void finalize() throws IOException {
    for (FlushAction<K, V, A> flushAction : flushActions.values()) {
      flushAction.flush();
    }
  }
}
