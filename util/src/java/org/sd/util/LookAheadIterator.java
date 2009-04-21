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


import java.util.Iterator;
import java.util.LinkedList;

/**
 * A wrapper for an iterator that adds look ahead capabilities.
 * <p>
 * @author Spence Koehler
 */
public class LookAheadIterator<T> implements Iterator<T> {

  private Iterator<T> iterator;
  private int position;
  private boolean allowNegative;
  private LinkedList<T> cache;
  private int firstCachePosition;
  private T curElement;

  /**
   * Construct a look ahead iterator around another iterator.
   * <p>
   * Default constructor does NOT allow for negative lookAhead.
   */
  public LookAheadIterator(Iterator<T> iterator) {
    this(iterator, false);
  }

  /**
   * Construct a look ahead iterator around another iterator.
   * <p>
   * If allowNegative, all elements will be cached. Beware of memory
   * limitations over large collections!
   */
  public LookAheadIterator(Iterator<T> iterator, boolean allowNegative) {
    this.iterator = iterator;
    this.position = -1;
    this.allowNegative = allowNegative;
    this.cache = null;
    this.firstCachePosition = -1;  // nothing in cache.
    this.curElement = null;
  }

  /**
   * Look ahead to the element that would be returned after 'numForward' next
   * calls.  The current (or last returned) element is 0 forward, the next
   * element is 1 forward, etc. Negative values are supported only if specified
   * on construction.
   * <p>
   * If beyond the range of elements to iterate over, return null.
   */
  public T lookAhead(int numForward) {
    return getElement(position + numForward);
  }

  /**
   * Get the position of the element last returned by 'next', where -1 is returned
   * before 'next' has been called; 0 is returned after the first 'next', etc.
   */
  public int getPosition() {
    return position;
  }

  /**
   * Determine whether there is a next element from the current position.
   */
  public boolean hasNext() {
    boolean result = iterator.hasNext();

    if (!result) {
      if (cache != null) {
        if (allowNegative) {
          result = (position + 1) < cache.size();
        }
        else {
          result = cache.size() > 0;
        }
      }
    }
    
    return result;
  }

  /**
   * Get the next element.
   */
  public T next() {
    T result = null;

    if (allowNegative) {
      if (ensureCacheHasElement(position + 1)) {
        result = cache.get(++position);
      }
    }
    else {
      if (cache != null && cache.size() > 0) {
        result = cache.removeFirst();
        ++firstCachePosition;
        ++position;
      }
      else {
        if (iterator.hasNext()) {
          result = iterator.next();
          ++position;
        }
      }
    }

    this.curElement = result;

    return result;
  }

  /**
   * Remove is not supported by this type of iterator and calling this method
   * throws an UnsupportedOperationException.
   */
  public void remove() {
    throw new UnsupportedOperationException("Can't remove elements through a LookAheadIterator!");
  }

  /**
   * Get the element at the given absolute index, or null if out of cache range.
   */
  private final T getElement(int absoluteIndex) {
    T result = null;

    if (absoluteIndex == position) {
      result = curElement;
    }
    else if (absoluteIndex < position) {  // looking backward
      if (allowNegative && absoluteIndex >= 0) {
        result = cache.get(absoluteIndex);
      }
    }
    else {  // absoluteIndex > position ==> looking forward
      final int cacheIndex = allowNegative ? absoluteIndex : absoluteIndex - position - 1;
      if (ensureCacheHasElement(cacheIndex)) {
        result = cache.get(cacheIndex);
      }
    }

    return result;
  }

  private final boolean ensureCacheHasElement(int index) {

    int numElements = (cache == null) ? 0 : cache.size();
    while (iterator.hasNext() && numElements <= index) {
      if (cache == null) {
        cache = new LinkedList<T>();
        firstCachePosition = position + 1;
      }
      cache.add(iterator.next());
      ++numElements;
    }
    
    return numElements > index;
  }
}
