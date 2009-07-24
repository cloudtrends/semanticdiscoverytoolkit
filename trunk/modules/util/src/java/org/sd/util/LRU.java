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


import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An LRU (least recently used) cache.
 * <p>
 * @author Spence Koehler
 */
public class LRU<K, V> extends LinkedHashMap<K, V> {

  private static final long serialVersionUID = 42L;


  private int cacheSize;

  public LRU(int cacheSize) {
    super(cacheSize, 0.75F, true);
    this.cacheSize = cacheSize;
  }

  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    boolean result = size() > this.cacheSize;
    if (result) {
      eldest.setValue(null);
    }
    return result;
  }

  public void setCacheSize(int cacheSize) {
    this.cacheSize = cacheSize;
  }

  public int getCacheSize() {
    return cacheSize;
  }
}
