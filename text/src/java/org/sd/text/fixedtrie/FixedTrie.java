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
package org.sd.text.fixedtrie;


/**
 * A fixed-depth trie implementation for a fixed number of values.
 * <p>
 * @author Spence Koehler
 */
public class FixedTrie {

  private int depth;      // or fixed value string length
  private int numValues;  // where values from 0 to numValues - 1

  private int size;           // num bits with 1 bit for each value
  private int[] rowEndInds;   // bit indexes
  private byte[] bits;        // bits

  public FixedTrie(int depth, int numValues) {
    this.depth = depth;
    this.numValues = numValues;

    allocate();
  }

  private final void allocate() {
    rowEndInds = new int[depth];

    size = numValues;
    rowEndInds[0] = size;

    for (int row = 1; row < depth; ++row) {
      size *= numValues;
      rowEndInds[row] = rowEndInds[row - 1] + size;
    }

    bits = new byte[rowEndInds[depth - 1] / 8 + 1];
  }

  public void add(int[] valueSequence) {
    int startBitInd = 0;        // start of cur row
    int bitOffsetOnRow = 0;     // bit offset for beginning of group of numValues bits

    for (int row = 0; row < valueSequence.length && row < depth; ++row) {
      final int curValue = valueSequence[row];
      final int bitOffset = startBitInd + bitOffsetOnRow + curValue;

      setBit(bitOffset);

      // increment pointers
      startBitInd = rowEndInds[row];  // use cache to avoid a multiply
      bitOffsetOnRow = (bitOffsetOnRow + curValue) * numValues;
    }
  }

  public boolean contains(int[] valueSequence) {
    boolean result = true;

    int startBitInd = 0;        // start of cur row
    int bitOffsetOnRow = 0;

    for (int row = 0; row < valueSequence.length && row < depth; ++row) {
      final int curValue = valueSequence[row];
      final int bitOffset = startBitInd + bitOffsetOnRow + curValue;

      if (!bitIsSet(bitOffset)) {
        result = false;
        break;
      }

      // increment pointers
      startBitInd = rowEndInds[row];  // use cache to avoid a multiply
      bitOffsetOnRow = (bitOffsetOnRow + curValue) * numValues;
    }

    return result;
  }

  private final void setBit(int bitOffset) {
    final int index = bitOffset / 8;
    final int bit = bitOffset % 8;
    bits[index] |= (1 << bit);
  }

  private final boolean bitIsSet(int bitOffset) {
    final int index = bitOffset / 8;
    final int bit = bitOffset % 8;
    return (bits[index] & (1 << bit)) != 0;
  }

  //todo: read/write dataInput/dataOutput
}
