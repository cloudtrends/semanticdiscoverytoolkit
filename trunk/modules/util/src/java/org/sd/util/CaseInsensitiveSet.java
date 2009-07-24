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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * A set of case-insensitive, order-aware, Strings.
 * <p>
 * @author Spence Koehler
 */
public class CaseInsensitiveSet extends LinkedHashSet<String> {
  
  private static final long serialVersionUID = 42L;

  private Map<String, String> insensitive2firstSensitive;

  public CaseInsensitiveSet() {
    super();

    this.insensitive2firstSensitive = new LinkedHashMap<String, String>();
  }

  public boolean add(String string) {
    boolean result = false;

    final String istring = string.toLowerCase();
    final String fstring = insensitive2firstSensitive.get(istring);
    if (fstring == null) {
      insensitive2firstSensitive.put(istring, string);
      result = super.add(istring);
    }

    return result;
  }

  public boolean contains(Object o) {
    boolean result = false;

    if (o instanceof String) {
      final String istring = ((String)o).toLowerCase();
      result = super.contains(istring);
    }

    return result;
  }

  public boolean remove(Object o) {
    boolean result = false;

    if (o instanceof String) {
      final String istring = ((String)o).toLowerCase();
      result = super.remove(istring);
      insensitive2firstSensitive.remove(istring);
    }

    return result;
  }

  public void clear() {
    super.clear();
    insensitive2firstSensitive.clear();
  }

  public Iterator<String> iterator() {
    return insensitive2firstSensitive.values().iterator();
  }
}
