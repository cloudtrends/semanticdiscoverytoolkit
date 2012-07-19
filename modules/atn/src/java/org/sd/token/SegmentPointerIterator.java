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


import java.util.Iterator;

/**
 * Base class for generating segment pointers.
 * <p>
 * @author Spence Koehler
 */
public class SegmentPointerIterator implements Iterator<SegmentPointer> {
  
  public final SegmentPointerFinder pointerFinder;

  private SegmentPointer nextPtr;

  public SegmentPointerIterator(SegmentPointerFinder pointerFinder) {
    this.pointerFinder = pointerFinder;

    final int startPtr = pointerFinder.findStartPtr(0);
    this.nextPtr = pointerFinder.findSegmentPointer(startPtr);
  }

  public boolean hasNext() {
    return nextPtr != null;
  }

  public SegmentPointer next() {
    SegmentPointer result = nextPtr;

    if (nextPtr != null) {
      nextPtr = pointerFinder.findSegmentPointer(nextPtr.getEndPtr() + 1);
    }

    return result;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }
}
