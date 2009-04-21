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
package org.sd.extract;


import java.util.Comparator;

/**
 * A comparator for sorting extractions into document order.
 * <p>
 * @author Spence Koehler
 */
public class DocumentOrderExtractionComparator implements Comparator<Extraction> {

  private static final DocumentOrderExtractionComparator INSTANCE = new DocumentOrderExtractionComparator();

  public static final DocumentOrderExtractionComparator getInstance() {
    return INSTANCE;
  }

  private DocumentOrderExtractionComparator() {
  }

  /**
   * Sort from lower path index to higher path index of underlying docText.
   * <p>
   * When path indeces are tied, sort from higher weight to lower weight.
   */
  public int compare(Extraction e1, Extraction e2) {
    // sort from lower to higher pathIndex
    int result = e1.getDocText().getPathIndex() - e2.getDocText().getPathIndex();

    if (result == 0) {
      // when tied, sort from higher to lower weight
      result = (e1.getWeight() > e2.getWeight()) ? -1 : (e1.getWeight() < e2.getWeight()) ? 1 : 0;
    }

    return result;
  }

  public boolean equals(Object o) {
    return this == o;
  }
}
