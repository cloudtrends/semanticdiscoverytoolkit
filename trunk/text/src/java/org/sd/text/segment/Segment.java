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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Container class for text 'segments' associated with boolean feature
 * computations whose results for the text are cached in this class.
 * <p>
 * @author Spence Koehler
 */
public class Segment {

  private FeatureBag featureBag;
  private String text;

  private Map<String, Boolean> feature2computation;  // cache
  private Set<String> manualFeatures;

  /**
   * Construt a segment with an empty feature bag and the given text.
   */
  public Segment(String text) {
    this(null, text);
  }

  /**
   * Construct a segment with the feature bag and text.
   */
  public Segment(FeatureBag featureBag, String text) {
    this.featureBag = featureBag == null ? new FeatureBag() : featureBag;
    this.text = text;
    this.feature2computation = new HashMap<String, Boolean>();
    this.manualFeatures = new HashSet<String>();
  }

  /**
   * Get this segment's feature bag.
   */
  public FeatureBag getFeatureBag() {
    return featureBag;
  }

  /**
   * Set this segment's feature bag.
   */
  public void setFeatureBag(FeatureBag featureBag) {
    this.featureBag = featureBag;
  }

  /**
   * Add the feature to this segment's feature bag.
   */
  public void addFeature(BooleanFeatureComputation bfc) {
//todo: need to be careful with overwriting an existing feature in a shared bag! throw exception?
    featureBag.add(bfc);
  }

  /**
   * Get this segment's text.
   */
  public String getText() {
    return text;
  }

  /**
   * Append the text to the end of this segment's text, resetting all features.
   */
  public void append(String text) {
    this.text = this.text + text;

    feature2computation.clear();
    manualFeatures.clear();
  }

  /**
   * Append the other segment to this one, discarding the other's
   * features while keeping this segment's features.
   */
  public void append(Segment other) {
    append(other.getText());
  }

  /**
   * Determine whether this segment knows about the feature.
   */
  public boolean isKnownFeature(String feature) {
    return featureBag.hasKey(feature) || manualFeatures.contains(feature);
  }

  /**
   * Check for the presence of the given feature and value without
   * trying to compute it.
   */
  public boolean checkFeature(String feature, Boolean value) {
    boolean result = false;

    final Boolean booleanResult = feature2computation.get(feature);

    if (value == null) {
      result = (booleanResult == null);
    }
    else {
      result = value.equals(booleanResult);
    }

    return result;
  }

  /**
   * Determine whether this segment's computation of the feature equals the
   * value.
   * <p>
   * Note that a manual feature is not considered if it has not been set
   * and the result will be false regardless of the value, but all feature
   * computations will be executed as needed.
   * <p>
   *
   * @return true if the value matches the feature's value;
   *         otherwise, false.
   */
//   * If the feature is unknown, an illegal argument exception is thrown.
  public boolean hasFeature(String feature, Boolean value) {
    boolean result = false;

    Boolean booleanResult = null;

    if (manualFeatures.contains(feature)) {
      booleanResult = feature2computation.get(feature);
    }
    else {
      booleanResult = getFeature(feature);
    }

    if (value == null) {
      result = (booleanResult == null);
    }
    else {
      result = value.equals(booleanResult);
    }

    return result;
  }

  /**
   * Get the computed value for the given feature over this segment's text.
   * <p>
   *
   * @return the computed boolean feature.
   */
//    * If the feature is not known to this segment's feature bag or manual features,
//    * an IllegalArgumentException is thrown.
  public Boolean getFeature(String feature) {
    Boolean result = null;

//     if (!isKnownFeature(feature)) {
//       throw new IllegalArgumentException("Unkown feature '" + feature + "'!");
//     }

    if (feature2computation.containsKey(feature)) {
      result = feature2computation.get(feature);
    }
    else if (manualFeatures.contains(feature)) {
      result = feature2computation.get(feature);
    }
    else {
      result = featureBag.computeFeature(feature, text);
      feature2computation.put(feature, result);
    }

    return result;
  }

  /**
   * Split this segment using the feature's split function.
   */
  public SegmentSequence split(String feature) {
//     if (!featureBag.hasKey(feature)) {
//       throw new IllegalArgumentException("Unkown feature '" + feature + "'!");
//     }

    return featureBag.split(feature, text);
  }

  /**
   * Set a manual feature in this segment.
   */
  public void setManualFeature(String feature, Boolean value) {
    this.manualFeatures.add(feature);
    feature2computation.put(feature, value);
  }

  public String toString() {
    return "'" + text + "'";
  }
}
