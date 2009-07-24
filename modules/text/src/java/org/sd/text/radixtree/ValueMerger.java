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
 * An interface for resolving conflicts while building a radix tree.
 * <p>
 * This will be called in the event that there is an exact match to
 * an existing "real" node being added to the trie.
 *
 * @author Spence Koehler
 */
public interface ValueMerger<T> {
  
  /**
   * Merge the new value into the existing data or throw an
   * IllegalStateException if not possible.
   *
   * @param existingData  The radix data on the existing node.
   * @param newValue      The new conflicting value to be added.
   */
  public void merge(RadixData<T> existingData, T newValue);
}
