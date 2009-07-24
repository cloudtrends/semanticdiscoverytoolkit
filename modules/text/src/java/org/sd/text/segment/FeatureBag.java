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


import java.util.HashMap;
import java.util.Map;

/**
 * Structure to hold boolean features with their associated computations.
 * <p>
 * @author Spence Koehler
 */
public class FeatureBag {

  private Map<String, BooleanFeatureComputation> key2bfc;

  public FeatureBag() {
    this.key2bfc = new HashMap<String, BooleanFeatureComputation>();
  }

  public FeatureBag(BooleanFeatureComputation[] bfcs) {
    this();

    for (BooleanFeatureComputation bfc : bfcs) {
      add(bfc);
    }
  }

  public FeatureBag add(BooleanFeatureComputation bfc) {
//todo: do the right thing with conflicts. (add only if not there? throw exception?)
    key2bfc.put(bfc.getFeatureType(), bfc);
    return this;
  }

  public boolean hasKey(String feature) {
    return key2bfc.containsKey(feature);
  }

  public Boolean computeFeature(String key, String text) {
    Boolean result = null;

    final BooleanFeatureComputation bfc = key2bfc.get(key);
    if (bfc != null) {
      result = bfc.computeFeature(text);
    }

    return result;
  }

  public SegmentSequence split(String key, String text) {
    SegmentSequence result = null;

    final BooleanFeatureComputation bfc = key2bfc.get(key);
    if (bfc != null) {
      result = bfc.split(text);
      
      if (result != null) {
        result.injectFeatureBag(this);
      }
    }

    return result;
  }
}
