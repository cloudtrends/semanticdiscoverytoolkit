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


import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * A container holding an ordered map whose entries are written to a file.
 * <p>
 * @author Spence Koehler
 */
public class MapContainer<K, V> {
  
  /** Map to be accessed and updated by extending classes (always non-null). */
  protected final TreeMap<K, V> curmap;

  public MapContainer() {
    this.curmap = new TreeMap<K,V>();
  }

  /** Get this container's map. */
  public final Map<K, V> getMap() {
    return curmap;
  }
}
