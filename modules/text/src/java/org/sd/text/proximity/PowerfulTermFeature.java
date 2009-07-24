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
package org.sd.text.proximity;


import org.sd.text.segment.BooleanFeatureComputation;
import org.sd.text.segment.SegmentSequence;

/**
 * A boolean feature computation to determine whether a powerful keyword term
 * is found in text.
 * <p>
 * @author Spence Koehler
 */
public class PowerfulTermFeature implements BooleanFeatureComputation {

  private String type;
  private double minPower;
  private Keywords keywords;
  private String splitRegex;
  private boolean searchBackward;
  private int maxLookups;

  public PowerfulTermFeature(String type, double minPower, Keywords keywords, String splitRegex, boolean searchBackward, int maxLookups) {
    this.type = type;
    this.minPower = minPower;
    this.keywords = keywords;
    this.splitRegex = splitRegex;
    this.searchBackward = searchBackward;
    this.maxLookups = maxLookups;
  }

  public double getMinPower() {
    return minPower;
  }

  public Keywords getKeywords() {
    return keywords;
  }

  /**
   * Get the feature type (or key).
   */
  public String getFeatureType() {
    return type;
  }

  /**
   * Compute the boolean feature value for the text.
   *
   * @return true if keyword power meets or exceeds the minimum,
   *         false if the keyword power is lower than the minimum,
   *         or null if the text is not a keyword.
   */
  public Boolean computeFeature(String text) {
    Boolean result = null;

    final String[] terms = text.split(splitRegex);
    int numLookups = 0;

    if (searchBackward) {
      for (int i = terms.length - 1; i >= 0 && (maxLookups < 0 || numLookups < maxLookups); --i) {
        final String term = terms[i];
        result = isPowerfulTerm(term, result);

        if (result != null && result) {
          break;
        }
        ++numLookups;
      }
    }
    else {
      for (int i = 0; i < terms.length && (maxLookups < 0 || numLookups < maxLookups); ++i) {
        final String term = terms[i];
        result = isPowerfulTerm(term, result);

        if (result != null && result) {
          break;
        }
        ++numLookups;
      }
    }

    return result;
  }

  private final Boolean isPowerfulTerm(String term, Boolean curResult) {
    Boolean result = curResult;

    final Keyword keyword = keywords.findKeyword(term, false);
    if (keyword != null) {
      result = keyword.getPower() >= minPower;
    }

    return result;
  }

  /**
   * Split the string to data into alternating sequences of consecutive powerful
   * and non-powerful terms.
   *
   * @return the segment sequence of the split text or null if there are no
   *         powerful terms in the text.
   */
  public SegmentSequence split(String text) {
//todo: implement this.
    return null;
  }
}
