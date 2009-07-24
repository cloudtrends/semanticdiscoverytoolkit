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
 * A boolean feature computation that is a logic or of other feature
 * computations.
 * <p>
 * @author Spence Koehler
 */
public class HasAnyFeature implements BooleanFeatureComputation {
  
  private String type;
  private FeatureBag featureBag;
  private String[] orFeatures;

  public HasAnyFeature(String type, FeatureBag featureBag, String[] orFeatures) {
    this.type = type;
    this.featureBag = featureBag;
    this.orFeatures = orFeatures;
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
   * @return true, false, or null if this computation does not apply to the text.
   */
  public Boolean computeFeature(String text) {
    Boolean result = null;

    for (String orFeature : orFeatures) {
      result = featureBag.computeFeature(orFeature, text);
      if (result != null && result) {
        break;
      }
    }

    return result;
  }

  /**
   * Split on each orFeature until we get a non-null result.
   *
   * @return the segment sequence of the split text or null if unable or
   *         meaningless to split.
   */
  public SegmentSequence split(String text) {
    SegmentSequence result = null;

    for (String orFeature : orFeatures) {
      result = featureBag.split(orFeature, text);
      if (result != null) break;
    }

    return result;
  }
  
}
