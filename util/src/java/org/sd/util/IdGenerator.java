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


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class to generate unique ids for data based on data equality.
 * <p>
 * @author Spence Koehler
 */
public class IdGenerator <T> {

  private AtomicInteger nextId;
  private Map<T, Integer> data2id;

  public IdGenerator() {
    this.nextId = new AtomicInteger(0);
    this.data2id = new HashMap<T, Integer>();
  }

  /**
   * Get the id of the data.
   * <p>
   * If the data is null, then the id is null; otherwise, the id
   * is a unique id for the data.
   */
  public Integer getId(T data) {
    if (data == null) return null;

    Integer result = data2id.get(data);
    if (result == null) {
      result = nextId.getAndIncrement();
      data2id.put(data, result);
    }
    return result;
  }
}
