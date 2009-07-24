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


import java.io.IOException;
import java.util.Properties;

/**
 * Transforms RawLabeledData into ExtractedFeatures.
 * <p>
 * @author Spence Koehler
 */
public class ClassifierTrainer extends BaseClassifierRunner {

  private ExtractedFeatures _extractedFeatures;

  public ClassifierTrainer(FeatureDictionary featureDictionary, FeatureExtractor featureExtractor) {
    super(featureDictionary, featureExtractor);

    verify();
  }

  public ClassifierTrainer(Properties properties) throws IOException {
    super(properties);

    verify();
  }

  private final void verify() {
    if (getFeatureDictionary().isLocked()) {
      throw new IllegalStateException("Can't train with a locked dictionary!");
    }
  }

  public ExtractedFeatures getExtractedFeatures() {
    if (_extractedFeatures == null) {
      _extractedFeatures = new ExtractedFeatures(getFeatureDictionary());
    }
    return _extractedFeatures;
  }

//   /**
//    * Generate features by extracting from the labeled input.
//    */
//   public ExtractedFeatures train(RawLabeledData data) {
//     while (data.hasNext()) {
//       final LabeledInput labeledInput = data.next();
//       train(labeledInput);
//     }
// 
//     return getExtractedFeatures();
//   }

  /**
   * Add a training instance to the extracted features by extracting from the
   * labeled input.
   */
  public void train(LabeledInput labeledInput) {
    final FeatureDictionary featureDictionary = getFeatureDictionary();
    final FeatureVector fv = extract(labeledInput.input);
    final ExtractedFeatures extractedFeatures = getExtractedFeatures();

    // set the training label (labeledInput.label) on the feature vector.
    if (featureDictionary.setClassificationAttribute(labeledInput.label, fv)) {
      extractedFeatures.add(fv, labeledInput);
    }
    else {
      throw new IllegalStateException("Unable to set classification label '" + labeledInput.label + "' on FeatureVector!");
    }
  }
}
