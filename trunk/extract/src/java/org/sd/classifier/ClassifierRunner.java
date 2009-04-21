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


import org.sd.extract.TextContainer;

import java.io.IOException;
import java.util.Properties;

/**
 * Utility class for loading and running a classifer, including feature extraction.
 * <p>
 * @author Spence Koehler
 */
public class ClassifierRunner extends BaseClassifierRunner {

  private Classifier classifier;

  public ClassifierRunner(FeatureDictionary featureDictionary, FeatureExtractor featureExtractor, Classifier classifier) {
    super(featureDictionary, featureExtractor);

    this.classifier = classifier;

    verify();
  }

  public ClassifierRunner(Properties properties) throws IOException {
    super(properties);

//todo: implement this!  load the classifier from the properties
    this.classifier = null;  // = ClassifierFactory.buildClassifier(properties)

    verify();
  }

  private final void verify() {
    if (!getFeatureDictionary().isLocked()) {
      throw new IllegalStateException("Can't run with an unlocked dictionary!");
    }
  }

  /**
   * Classify an input.
   */
  public ClassificationResult classify(TextContainer input) {
    final FeatureVector fv = extract(input);
    return classifier.classify(fv, getFeatureDictionary());
  }
}
