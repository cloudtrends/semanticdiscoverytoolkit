/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utility to compute the transitive closure of linked elements.
 * <p>
 * @author Spence Koehler
 */
public class TransitiveClosure<T> {
  
  private Map<T, Set<T>> links;

  public TransitiveClosure() {
    this.links = new HashMap<T, Set<T>>();
  }

  public void addLink(T elt1, T elt2) {
    Set<T> commonSet1 = links.get(elt1);
    Set<T> commonSet2 = links.get(elt2);

    if (commonSet1 == null && commonSet2 == null) {
      commonSet1 = new HashSet<T>();
      commonSet1.add(elt1);
      commonSet1.add(elt2);
      links.put(elt1, commonSet1);
      links.put(elt2, commonSet1);
    }
    else if (commonSet1 != commonSet2) {
      if (commonSet1 != null && commonSet2 != null) {
        commonSet1.addAll(commonSet2);
        for (T cs2elt : commonSet2) {
          links.put(cs2elt, commonSet1);
        }
      }
      else if (commonSet1 == null) {
        commonSet2.add(elt1);
        links.put(elt1, commonSet2);
      }
      else if (commonSet2 == null) {
        commonSet1.add(elt2);
        links.put(elt2, commonSet1);
      }
    }
  }

  public List<Set<T>> getClosedSets() {
    final List<Set<T>> result = new ArrayList<Set<T>>();

    final TreeSet<T> remaining = new TreeSet<T>(links.keySet());
    while (remaining.size() > 0) {
      final T first = remaining.first();
      final Set<T> curset = links.get(first);
      result.add(curset);
      remaining.removeAll(curset);
    }

    return result;
  }
}
