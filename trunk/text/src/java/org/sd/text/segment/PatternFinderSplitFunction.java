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


import org.sd.text.PatternFinder;

/**
 * An interface for specifying the splitting of text using a patternFinder.
 * <p>
 * @author Spence Koehler
 */
public abstract class PatternFinderSplitFunction implements SplitFunction {

  private PatternFinder patternFinder;

  protected PatternFinderSplitFunction(PatternFinder patternFinder) {
    this.patternFinder = patternFinder;
  }

  /**
   * Don't split delimiter segments or those that already have a 'true'
   * associated with the pattern finder's type as a feature.
   */
  public boolean shouldSplit(Segment segment) {
    boolean result = false;

    if (segment != null) {
      result =
        !segment.checkFeature(patternFinder.getType(), true) &&
        !SegmentUtils.isDelimOrWhitespace(segment);
    }
        
    return result;
  }

  /**
   * Split the text into a segment sequence.
   *
   * @return the segment sequence or null if unable or meaningless to split.
   */
  public SegmentSequence split(String text) {
    return split(patternFinder, text);
  }

  /**
   * Split the text according to properties wrt the patternFinder.
   * 
   * @return the segment sequence or null if the text has no recognized patterns.
   */
  public abstract SegmentSequence split(PatternFinder patternFinder, String text);
}
