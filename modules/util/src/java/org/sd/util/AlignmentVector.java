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


import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * A container for alignment cells that itself can be aligned.
 * <p>
 * @author Spence Koehler
 */
public class AlignmentVector implements Comparable<AlignmentVector> {
  
  private List<AlignmentCell> cells;

  /**
   * Construct an empty vector.
   */
  public AlignmentVector() {
    this.cells = new ArrayList<AlignmentCell>();
  }

  /**
   * Get the size (number of cells) of this vector.
   */
  public int size() {
    return cells.size();
  }

  /**
   * Add the cell to this vector, returning its bit position.
   */
  public int add(AlignmentCell cell) {
    final int result = cells.size();
    cells.add(cell);
    return result;
  }

  /**
   * Add a new cell to this vector with the given cell data, returning the new
   * cell's bit position.
   */
  public int add(Object cellData) {
    final int result = cells.size();
    cells.add(new AlignmentCell(cellData));
    return result;
  }

  /**
   * Get the cell at the given position.
   */
  public AlignmentCell getCell(int position) {
    return cells.get(position);
  }

  /**
   * Align this vector with the other, where bits are set corresponding to cell
   * positions that are equal (compare to 0). If this and the other vectors aren't
   * the same size, then null will be returned.
   */
  public BitSet alignWith(AlignmentVector other) {
    final int size = this.size();

    if (size != other.size()) return null;

    final BitSet result = new BitSet(size);

    for (int i = 0; i < size; ++i) {
      result.set(i, this.getCell(i).compareTo(other.getCell(i)) == 0);
    }

    return result;
  }

  /**
   * Compare this vector to another such that vectors of unequal size are
   * sorted by size and vectors of equal size have their cells' comparisons
   * summed. If a sum equals zero, but not all cell comparisons are zero, then
   * the result will be the first difference encountered. Therefore, a result
   * of 0 is consistent with equality.
   */
  public int compareTo(AlignmentVector other) {
    int result = 0;

    final int size = this.size();

    if (size != other.size()) {
      result = size - other.size();
    }
    else {
      int sum = 0;
      int firstDiff = 0;

      for (int i = 0; i < size; ++i) {
        final int c = this.getCell(i).compareTo(other.getCell(i));
        sum += c;
        if (firstDiff == 0 && c != 0) firstDiff = c;
      }

      result = (sum == 0) ? firstDiff : sum;
    }

    return result;
  }
}
