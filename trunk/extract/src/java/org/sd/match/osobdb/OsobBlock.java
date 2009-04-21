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
package org.sd.match.osobdb;


import org.sd.match.Osob;

import java.util.Iterator;

/**
 * Container for a block of osobs.
 * <p>
 * @author Spence Koehler
 */
class OsobBlock {

  private Osob[] osobs;
  private int baseConceptId;
  private int numOsobs;

  OsobBlock(int blockSize, int baseConceptId) {
    this.osobs = new Osob[blockSize];
    this.baseConceptId = baseConceptId;
    this.numOsobs = 0;
  }
  
  public boolean addOsob(Osob osob) {
    final int index = osob.getConceptId() - baseConceptId;
    final boolean result = osobs[index] == null;

    osobs[index] = osob;

    if (result) ++numOsobs;

    return result; // return true if added new osob.
  }

  public Osob getOsob(int conceptId) {
    return osobs[conceptId - baseConceptId];
  }

  /**
   * Get the actual number of osobs in this block (not the size of the block).
   */
  public int getNumOsobs() {
    return numOsobs;
  }

  /**
   * Get an iterator over the (non-null) osobs in this block.
   */
  public Iterator<Osob> iterator() {
    return new OsobIterator();
  }

  private class OsobIterator implements Iterator<Osob> {

    private int nextIndex;

    OsobIterator() {
      this.nextIndex = 0;
      increment();  // spin up to a non-null position
    }

    public boolean hasNext() {
      return nextIndex < osobs.length;
    }

    public Osob next() {
      final Osob result = osobs[nextIndex++];
      increment();
      return result;
    }

    public void remove() {
      //do nothing.
    }

    private final void increment() {
      while ((nextIndex < osobs.length) && (osobs[nextIndex] == null)) {
        ++nextIndex;
      }
    }
  }
}
