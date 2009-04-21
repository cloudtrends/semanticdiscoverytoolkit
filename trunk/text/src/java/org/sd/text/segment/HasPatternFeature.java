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
import org.sd.text.TermFinder;

/**
 * A boolean feature computation to find a pattern in text.
 * <p>
 * @author Spence Koehler
 */
public class HasPatternFeature implements BooleanFeatureComputation {
  
  private PatternFinder patternFinder;
  private SplitFunction splitFunction;
  private int acceptPartial;  

  public HasPatternFeature(PatternFinder patternFinder, int acceptPartial) {
    this(patternFinder, new PatternExtractorSplitFunction(patternFinder), acceptPartial);
  }

  public HasPatternFeature(PatternFinder patternFinder, SplitFunction splitFunction, int acceptPartial) {
    this.patternFinder = patternFinder;
    this.splitFunction = splitFunction;
    this.acceptPartial = acceptPartial;
  }

  public HasPatternFeature(String type, String[] termsToFind, SplitFunction splitFunction,
                        boolean caseSensitive, int acceptPartial) {
    this.patternFinder = new TermFinder(type, caseSensitive, termsToFind);
  }

//todo: add a constructor to load a file of patterns if/when needed.  

  /**
   * Get the feature type (or key).
   */
  public String getFeatureType() {
    return patternFinder.getType();
  }

  /**
   * Compute the boolean feature value for the text.
   *
   * @return true if the pattern is found; otherwise, false.
   */
  public Boolean computeFeature(String text) {
    return patternFinder.hasPattern(text, acceptPartial);
  }

  /**
   * Split the text according to this instance's pattern finder split function.
   *
   * @return the split text sequence or null if there is no split function or
   *         the text doesn't have a recognized pattern.
   */
  public SegmentSequence split(String text) {
    return splitFunction == null ? null : splitFunction.split(text);
  }
}
