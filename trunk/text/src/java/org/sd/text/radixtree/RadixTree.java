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


import java.util.List;

/**
 * Interface for a radix or Patricia trie.
 * <p>
 * @author Spence Koehler
 */
public interface RadixTree<T> {
  
  /**
   * Insert a new string key and its value to the tree. Throw an IllegalStateException
   * if there is a conflict.
   * 
   * @param key
   *            The string key of the object
   * @param value
   *            The value that need to be stored corresponding to the given
   *            key.
   */
  public void insert(String key, T value);

  /**
   * Insert a new string key and its value to the tree. If there is already
   * a conflicting value at the insertion point, resolve by calling the
   * value merger function.
   * 
   * @param key
   *            The string key of the object
   * @param value
   *            The value that need to be stored corresponding to the given
   *            key.
   * @param valueMerger
   *            The value merger to use to resolve conflicts. If null, the
   *            conflict will generate an IllegalStateException.
   * @param valueReplicator
   *            The value replicator to use to make a duplicate of a value
   *            to place into nodes inserted for splits. If null, the
   *            same instance of value will be used. For values like Strings,
   *            using the same value instance does no harm as the string's
   *            contents are immutable. A value whose object carries state
   *            that does change (i.e. when merged,) however, must be
   *            duplicated for proper function.
   */
  public void insert(String key, T value, ValueMerger<T> valueMerger, ValueReplicator<T> valueReplicator);

  /**
   * Delete a key and its associated value from the tree.
   * @param key The key of the node that need to be deleted
   * @return true if deleted
   */
  public boolean delete(String key);

  /**
   * Find a value based on its corresponding key.
   * 
   * @param key The key for which to search the tree.
   * @return The value corresponding to the key. null if iot can not find the key
   */
  public T find(String key);

  /**
   * Check if the tree contains any entry corresponding to the given key.
   * 
   * @param key The key that needto be searched in the tree.
   * @return retun true if the key is present in the tree otherwise false
   */
  public boolean contains(String key);

  /**
   * Search for all the keys that start with given prefix. limiting the results based on the supplied limit.
   * 
   * @param prefix The prefix for which keys need to be search
   * @param recordLimit The limit for the results
   * @return The list of values those key start with the given prefix
   */
  public List<T> searchPrefix(String prefix, int recordLimit);
    
  /**
   * Return the size of the Radix tree
   * @return the size of the tree
   */
  public long getSize();

}
