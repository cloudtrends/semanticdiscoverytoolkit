/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.token;


/**
 * A simple SegmentPointerFinder that segments input into whitespace-based
 * words.
 * <p>
 * @author Spence Koehler
 */
public class WordFinder extends BaseSegmentPointerFinder {
  
  public static final String WORD_LABEL = "w";


  private int seqNum;

  public WordFinder(String input) {
    super(input);
    this.seqNum = -1;
  }

  public int getSeqNum() {
    return seqNum;
  }

  public void setSeqNum(int seqNum) {
    this.seqNum = seqNum;
  }

  /**
   * Finds the first non-white at or beyond fromPos.
   */
  public int findStartPtr(int fromPos) {
    ++seqNum;
    return skipToNonWhite(fromPos);
  }

  /**
   * Find the segment (word) beginning at the startPtr.
   * <p>
   * NOTES:
   * <ul>
   * <li>All segments will all be labeled with WORD_LABEL.</li>
   * <li>Segments will be numbered in the order requested by findStartPtr.</li>
   * </ul>
   *
   * @return the segment or null if there is no segment to be found.
   */
  public SegmentPointer findSegmentPointer(int startPtr) {
    SegmentPointer result = null;

    final int endPtr = skipToWhite(startPtr);

    if (endPtr > startPtr) {
      result = new SegmentPointer(input, WORD_LABEL, seqNum, startPtr, endPtr);
    }

    return result;
  }

  protected boolean isWhitespace(char c) {
    return c == '-' || Character.isWhitespace(c);
  }
}
