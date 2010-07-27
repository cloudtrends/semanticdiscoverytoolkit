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


import java.util.Comparator;

/**
 * A container for data to be aligned in an AlignmentVector.
 * <p>
 * @author Spence Koehler
 */
public class AlignmentCell implements Comparable<AlignmentCell> {
  
  private Object data;
  private Comparator<? super Object> alignmentFunction;

  /**
   * Construct a cell with the given data and no alignment function.
   * <p>
   * Note that the alignment function can be set later. If no alignment
   * function is ever set, then When this cell is compared against another, the
   * cell's data must be a Comparable and will be compared directly against the
   * other cell's data.
   */
  public AlignmentCell(Object data) {
    this(data, null);
  }

  /**
   * Construct with the given data and alignmentFunction that will be used to
   * compare data object to data object (not alignment cell to cell).
   */
  public AlignmentCell(Object data, Comparator<? super Object> alignmentFunction) {
    this.data = data;
    this.alignmentFunction = alignmentFunction;
  }

  /**
   * Get this cell's data.
   */
  public Object getData() {
    return data;
  }

  /**
   * Set this cell's data.
   */
  public void setData(Object data) {
    this.data = data;
  }

  /**
   * Get the alignment function used to compare this cell's data.
   */
  public Comparator getAlignmentFunction() {
    return alignmentFunction;
  }

  /**
   * Set this cell's alignment function for comparing data to data.
   */
  public void setAlignmentFunction(Comparator<? super Object> alignmentFunction) {
    this.alignmentFunction = alignmentFunction;
  }

  /**
   * Compare cells by comparing the contained data.
   */
  @SuppressWarnings("unchecked")
  public int compareTo(AlignmentCell other) {
    int result = 0;

    if (alignmentFunction != null) {
      result = alignmentFunction.compare(this.data, other.getData());
    }
    else if (data instanceof Comparable) {
      result = ((Comparable<? super Object>)data).compareTo(other.getData());
    }
    else {
      throw new IllegalStateException("Unable to compare cells! (this=" + this.toString() + ", other=" + other.toString() + ")");
    }

    return result;
  }
}
