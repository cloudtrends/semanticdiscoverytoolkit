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


import java.util.ArrayList;
import java.util.List;
import org.sd.util.SentenceIterator;

/**
 * 
 * <p>
 * @author Spence Koehler
 */
public class SequenceSegmenter {
  
  public SequenceSegmenter() {
  }

  public SegmentPointer[] segment(SegmentPointerFinderFactory ptrFactory, String input) {
    final List<SegmentPointer> result = new ArrayList<SegmentPointer>();

    int seqNum = 0;
    for (SentenceIterator iter = new SentenceIterator(input).setDetectAbbrev(true).setGreedy(false); iter.hasNext(); ) {
      final String sentence = iter.next();

      final SegmentPointerFinder ptrFinder = ptrFactory.getSegmentPointerFinder(sentence);
      for (SegmentPointerIterator iter2 = new SegmentPointerIterator(ptrFinder); iter2.hasNext(); ) {
        final SegmentPointer ptr = iter2.next();
        ptr.setSeqNum(seqNum++);
        result.add(ptr);
      }
    }

    return result.toArray(new SegmentPointer[result.size()]);
  }
}
