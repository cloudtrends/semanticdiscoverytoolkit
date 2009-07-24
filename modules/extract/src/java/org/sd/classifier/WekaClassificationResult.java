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
package org.sd.classifier;

import org.sd.util.MathUtil;

/**
 * Implementation specific to a {@link WekaClassifier}.
 * 
 * @author Dave Barney
 */
public class WekaClassificationResult implements ClassificationResult {
  
  /** Value value/result of the classifier (if nominal, then index into label) */
  private double value;
  
  /** Weight weight/confidence of the classification */
  private double weight;
  
  /** String representation of classification result */
  private String label;
  
  /** The feature vector that produced this result */
  private FeatureVector featureVector;

  
  /**
   * Constructor requiring all components upon instantiation.
   * 
   * @param value value/result of the classifier (if nominal, then index into label)
   * @param weight weight/confidence of the classification
   * @param label String representation of classification result
   * @param featureVector the feature vector that produced this result
   */
  public WekaClassificationResult(double value, double weight, String label, FeatureVector featureVector) {
    this.value = value;
    this.weight = weight;
    this.label = label;
    this.featureVector = featureVector;
  }
  
  /**
   * Currently does nothing for weka classifiers.
   * 
   * @return "Weka is unexaplainable!" every time
   */
  public String getExplanation() {
    return "Weka is unexplainable!";
  }

  /**
   * Access to the {@link FeatureVector} on this {@link ClassificationResult}.
   * 
   * @return the feature vector 
   */
  public FeatureVector getFeatureVector() {
    return featureVector;
  }

  /**
   * For {@link NominalFeatureAttribute}s, this is the string representation
   * of the classification.  Otherwise, it is the numeric result as a string.
   * 
   * @return the string representation of the classification
   */
  public String getLabel() {
    return label;
  }

  /**
   * The classification answer/result itself. For {@link NominalFeatureAttribute}s,
   * this is the index into the nominal values.
   */
  public double getValue() {
    return value;
  }

  /**
   * The weight (confidence) of the value return bu {@link #getValue()}.
   */
  public double getWeight() {
    return weight;
  }
  
  /**
   * Formats the classification result in a human readable format for debugging purposes.
   */
  public String toString() {
    return label + "(" + MathUtil.doubleString(value, 1) + ")\tweight: " + MathUtil.doubleString(weight, 2) + "\tFeatureVector: " + featureVector.toString();
  }
}
