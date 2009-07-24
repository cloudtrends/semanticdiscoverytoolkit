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
package org.sd.text.segment;


/**
 * Utility to generate a starting set of segments.
 * <p>
 * @author Spence Koehler
 */
public class SegmentFactory {
  
  /**
   * Generate a sequence by splitting the string on multiple delims and then
   * splitting each segment on single delims.
   */
  public static final SegmentSequence splitMultiAndSingle(FeatureBag featureBag, String string) {
    return splitMultiAndSingle(featureBag, string, SingleDelimSplitFunction.getDefaultInstance());
  }

  /**
   * Generate a sequence by splitting the string on multiple delims and then
   * splitting each segment on single delims.
   */
  public static final SegmentSequence splitMultiAndSingle(FeatureBag featureBag, String string, SplitFunction singleSplitter) {
    final SegmentSequence result = MultiDelimSplitFunction.getDefaultInstance().split(string);
    if (featureBag != null) result.injectFeatureBag(featureBag);

    final SegmentMachine machine = new SegmentMachine(result);
    while (machine.getCurSegment() != null) {
      if (!machine.splitAndPositionAfter(singleSplitter)) {
        machine.forward(1);
      }
    }

    return result;
  }

}
