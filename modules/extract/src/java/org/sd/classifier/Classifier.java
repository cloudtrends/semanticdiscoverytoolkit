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


/**
 * Interface for running classification over a feature vector.
 * <p>
 * @author Spence Koehler
 */
public interface Classifier {

  /**
   * Predict the classification labels, using the feature vector's extracted
   * feature values.
   *
   * @param featureVector      an instantiated (filled) feature vector.
   * @param featureDictionary  the relevant feature dictionary.
   */
  public ClassificationResult classify(FeatureVector featureVector, FeatureDictionary featureDictionary);
}
