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
package org.sd.text.radixtree;


/**
 * An interface for duplicating an instance of a value for placement
 * into nodes inserted for splits.
 * <p>
 * When a node is split, the existing node is inserted under a new
 * node who shares the existing node's value before receiving a new
 * child. Replicators are called to duplicate that value into the
 * new node.
 * <p>
 * A replicator is not necessary for immutable values, but is essential
 * for proper function with values that contain changing state, which
 * is often the case with values that "merge".
 *
 * @author Spence Koehler
 */
public interface ValueReplicator<T> {
  
  /**
   * Replicate the given value to place in the new "parent"
   * of a node containing the value created while splitting.
   *
   * @param value      The value to be replicated.
   */
  public T replicate(T value);
}
