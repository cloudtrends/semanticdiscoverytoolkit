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


/**
 * Container for comparison results between two InputContext instances.
 * <p>
 * @author Spence Koehler
 */
public class InputContextComparison {
  
  private boolean comparable;
  private InputContext referenceRoot;
  private InputContext otherRoot;

  private int refStartPos;
  private int refEndPos;
  private int otherStartPos;
  private int otherEndPos;

  public InputContextComparison(InputContext reference, int refStartIndex, int refEndIndex,
                                InputContext other, int otherStartIndex, int otherEndIndex) {

    if (reference == null || other == null) {
      this.comparable = false;
    }
    else {
      this.comparable = true;
      this.referenceRoot = reference.getContextRoot();
      this.otherRoot = other.getContextRoot();

      final int[] rStartPos = new int[]{0};
      final int[] rEndPos = new int[]{0};
      final int[] oStartPos = new int[]{0};
      final int[] oEndPos = new int[]{0};

      if (!referenceRoot.getPosition(reference, rStartPos) ||
          !referenceRoot.getPosition(reference, rEndPos) ||
          !otherRoot.getPosition(other, oStartPos) ||
          !otherRoot.getPosition(other, oEndPos)) {
        this.comparable = false;
      }
      else {
        this.refStartPos = rStartPos[0] + refStartIndex;
        this.refEndPos = rEndPos[0] + refEndIndex;
        this.otherStartPos = oStartPos[0] + otherStartIndex;
        this.otherEndPos = oEndPos[0] + otherEndIndex;
      }
    }
  }

  public boolean isComparable() {
    return comparable;
  }

  public boolean intersects() {
    return
      (refStartPos >= otherStartPos && refStartPos <= otherEndPos) ||
      (refEndPos >= otherStartPos && refEndPos <= otherEndPos) ||
      (otherStartPos >= refStartPos && otherStartPos <= refEndPos) ||
      (otherEndPos >= refStartPos && otherEndPos <= refEndPos);
  }

  public boolean matches() {
    return refStartPos == otherStartPos && refEndPos == otherEndPos;
  }

  public boolean encompasses() {
    return otherStartPos >= refStartPos && otherEndPos <= refEndPos;
  }

  public boolean isEncompassed() {
    return refStartPos >= otherStartPos && refEndPos <= otherEndPos;
  }
}
