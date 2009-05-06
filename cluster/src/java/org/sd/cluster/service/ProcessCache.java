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
package org.sd.cluster.service;


import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.sd.util.LRU;

/**
 * LRU cache of ProcessHandle instances.
 * <p>
 * @author Spence Koehler
 */
public class ProcessCache {
	
  public final Map<String, ProcessHandle> key2ph;
  private int cacheId;

  private static final AtomicInteger nextId = new AtomicInteger(0);

	/**
	 * Default constructor limits cache size to 1.
	 */
  public ProcessCache() {
    this(1);

    this.cacheId = nextId.getAndIncrement();
  }

	/**
	 * Construct with the given limited cache size.
	 */
  public ProcessCache(int cacheSize) {
    this.key2ph = new LRU<String, ProcessHandle>(cacheSize);
  }

  public synchronized ProcessHandle get(String key) {
    return key2ph.get(key);
  }

  public synchronized void put(String key, ProcessHandle processHandle) {
    key2ph.put(key, processHandle);
  }

  public synchronized void remove(String key) {
    final ProcessHandle processHandle = key2ph.remove(key);
  }

  public void clear() {
    key2ph.clear();
  }
}
