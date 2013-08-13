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
package org.sd.token;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Container for multiple features.
 * <p>
 * @author Spence Koehler
 */
public class Features {
  
		// NOTE: This implementation scans a flat list and will be inefficient
		//       if/when there are large numbers of features. If this becomes
		//       a problem, I recommend an implementation of Features and
		//       FeatureConstraint that allow for smarter indexing and retrieval
		//       for efficiency.  -Spence

  private LinkedList<Feature> theFeatures;

  public Features() {
    this.theFeatures = new LinkedList<Feature>();
  }

  /**
   * Get the number of features.
   */
  public int size() {
    return theFeatures.size();
  }

  /**
   * Add the feature.
   * <p>
   * Note that this method adds a feature with a lesser priority than all
   * prior features, which is consistent with typical functionality where
   * higher priority features are set before lower priority features. To
   * prioritize a feature differently, use the AddFirst method instead.
   * <p>
   * Don't add the feature if it is a duplicate.
   *
   * @return true if the feature was added; otherwise, false.
   */
  public boolean add(Feature feature) {
    boolean result = false;

    if (!hasFeature(feature)) {
      theFeatures.addLast(feature);
      result = true;
    }

    return result;
  }

  /**
   * Add the feature with a greater priority than all other features. Note that
   * GetFirst would return this feature over others matching a constraint, unless
   * another is injected later.
   * <p>
   * Don't add the feature if it is a duplicate.
   *
   * @return true if the feature was added; otherwise, false.
   */
  public boolean addFirst(Feature feature) {
    boolean result = false;

    if (!hasFeature(feature)) {
      theFeatures.addFirst(feature);
      result = true;
    }

    return result;
  }

  /**
   * Determine whether a feature matching the constraint is present.
   */
  public boolean hasFeature(FeatureConstraint constraint) {
    boolean result = false;

    for (Feature feature : theFeatures) {
      if (constraint.includes(feature)) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Get the first Feature that matchs the constraint or null.
   */
  public Feature getFirst(FeatureConstraint constraint) {
    Feature result = null;

    for (Feature feature : theFeatures) {
      if (constraint.includes(feature)) {
        result = feature;
        break;
      }
    }

    return result;
  }

  public boolean hasFeatureType(String featureType) {
    boolean result = false;

    if (featureType != null) {
      for (Feature feature: theFeatures) {
        if (featureType.equals(feature.getType())) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  /**
   * Get all features matching the constraint.
   *
   * @returns The features matching the constraint, or null if none do.
   */
  public List<Feature> getFeatures(FeatureConstraint constraint) {
    List<Feature> result = null;

    for (Feature feature : theFeatures) {
      if (constraint.includes(feature)) {
        if (result == null) result = new ArrayList<Feature>();
        result.add(feature);
      }
    }

    return result;
  }

  public List<Feature> getFeatures() {
    return theFeatures;
  }

  public boolean hasFeature(Feature feature) {
    boolean result = false;

    if (theFeatures != null) {
      for (Feature aFeature : theFeatures) {
        if (includes(aFeature.getType(), feature.getType()) &&
            includes(aFeature.getValue(), feature.getValue()) &&
            aFeature.getP() == feature.getP()) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  private final boolean includes(Object target, Object candidate) {
    boolean result = target == candidate;

    if (!result && target != null) {
      result = target.equals(candidate);
    }

    return result;
  }
}
