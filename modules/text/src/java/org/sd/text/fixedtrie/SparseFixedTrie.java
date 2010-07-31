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


import org.sd.io.Publishable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.sd.util.BitVector;

/**
 * A fixed-depth trie implementation for a fixed number of values that sparsely
 * populates values in the problem space as needed.
 * <p>
 * @author Spence Koehler
 */
public class SparseFixedTrie implements Publishable {

  private int depth;      // or fixed value string length
  private int numValues;  // where values range from 0 to numValues - 1

  private int size;           // num bits with 1 bit for each value
  private int[] rowEndInds;   // bit indexes
  private BitVector bits;     // bits

  public SparseFixedTrie() {
  }

  public SparseFixedTrie(int depth, int numValues) {
    this(depth, numValues, 0);
  }

  public SparseFixedTrie(int depth, int numValues, int initialSize) {
    this.depth = depth;
    this.numValues = numValues;

    allocate(initialSize);
  }

  private final void allocate(int initialSize) {
    rowEndInds = new int[depth];

    size = numValues;
    rowEndInds[0] = size;   // first row has all values

    for (int row = 1; row < depth; ++row) {
      rowEndInds[row] = rowEndInds[row - 1];  // each row zero length until populated
    }

    bits = new BitVector(initialSize <= 0 ? rowEndInds[depth - 1] / 8 + 1 : initialSize);
//    bits = new BitVector(1073741824);  // 8589934592 == 1 Gig. bits  1073741824 == 1 Gig. bytes
  }

  public int getDepth() {
    return depth;
  }

  public int getNumValues() {
    return numValues;
  }

  public int getSize() {
    return size;
  }

  public void add(int[] valueSequence) {
    int startBitInd = 0;        // start of cur row
    int bitOffsetOnRow = 0;     // bit offset for beginning of group of numValues bits

    for (int row = 0; row < valueSequence.length && row < depth; ++row) {
      final int curValue = valueSequence[row];
      final int bitOffset = startBitInd + bitOffsetOnRow + curValue;
      final int numSetBits = bits.countSetBits(startBitInd, bitOffset - 1);  // inclusive

      // note: newly setting a bit on this row will insert numValues bits to the next
      setBit(bitOffset, curValue, bitOffsetOnRow, startBitInd, row, numSetBits);

      // increment pointers
      startBitInd = rowEndInds[row];  // use cache to avoid a multiply
      bitOffsetOnRow = numSetBits * numValues;
    }
  }

  public boolean contains(int[] valueSequence) {
    boolean result = true;

    int startBitInd = 0;        // start of cur row
    int bitOffsetOnRow = 0;

    for (int row = 0; row < valueSequence.length && row < depth; ++row) {
      final int curValue = valueSequence[row];
      final int bitOffset = startBitInd + bitOffsetOnRow + curValue;
      final int numSetBits = bits.countSetBits(startBitInd, bitOffset - 1);  // inclusive

      if (!bitIsSet(bitOffset)) {
        result = false;
        break;
      }

      // increment pointers
      startBitInd = rowEndInds[row];  // use cache to avoid a multiply
      bitOffsetOnRow = numSetBits * numValues;
    }

    return result;
  }

  private final void setBit(int bitOffset, int groupInd, int rowGroupInd, int rowStartInd, int row, int numSetBits) {
    //note: bitOffset == rowStartInd + rowGroupInd + groupInd

    if (!bits.get(bitOffset)) {
      bits.set(bitOffset);

      // newly setting the bit requires creating space for its children on the next row
      if (row < depth - 1) {  // no need to insert beyond last row

        // find place to insert numValues bits on next row
        final int rowInsertOffset = numSetBits * numValues;
        final int insertOffset = rowEndInds[row] + rowInsertOffset;

        // insert numValues bits
        insertBlock(insertOffset, row + 1);
      }
    }
  }

  private final void insertBlock(int insertOffset, int row) {

    // shift tail over by numValues after insertOffset
    bits.shiftRight(insertOffset, numValues);

    // shift over all rowEndInds
    for (int i = row; i < depth; ++i) rowEndInds[i] += numValues;
    size += numValues;
  }

  /**
   * Determine whether the given bit is set.
   */
  private final boolean bitIsSet(int bitOffset) {
    return bits.get(bitOffset);
  }

  /**
   * Write this instance to the dataOutput stream such that it
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    dataOutput.writeInt(depth);
    dataOutput.writeInt(numValues);
    dataOutput.writeInt(size);
    dataOutput.writeInt(rowEndInds.length);
    for (int rowEndInd : rowEndInds) {
      dataOutput.writeInt(rowEndInd);
    }
    bits.write(dataOutput);
  }

  /**
   * Read this instance's contents from the dataInput stream that was written by
   * this.write(dataOutput).
   * <p>
   * NOTE: this requires all implementing classes to have a default constructor
   *       with no args.
   *
   * @param dataInput  the data output to write to.
   */
  public void read(DataInput dataInput) throws IOException {
    this.depth = dataInput.readInt();
    this.numValues = dataInput.readInt();
    this.size = dataInput.readInt();
    this.rowEndInds = new int[dataInput.readInt()];
    for (int i = 0; i < rowEndInds.length; ++i) {
      rowEndInds[i] = dataInput.readInt();
    }
    this.bits = new BitVector();
    bits.read(dataInput);
  }
}
