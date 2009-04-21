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


import org.sd.extract.DocText;

import java.util.ArrayList;
import java.util.List;

/**
 * A feature extractor that executes a pipeline of feature extractors.
 * <p>
 * @author Spence Koehler
 */
public class FeatureExtractionPipeline extends FeatureExtractor {
  
  private List<FeatureExtractor> extractors;

  public FeatureExtractionPipeline() {
    this.extractors = new ArrayList<FeatureExtractor>();
  }

//todo: make properties-based constructor.

  public final void addFeatureExtractor(FeatureExtractor featureExtractor) {
    extractors.add(featureExtractor);

    if (!needsDocTextCache()) {
      if (featureExtractor.needsDocTextCache()) setNeedsDocTextCache(true);
    }
  }

  /**
   * Process an element, setting attribute(s) value(s) as warranted on the feature vector.
   */
  protected boolean processElement(FeatureVector result, DocText element, FeatureDictionary featureDictionary) {
    boolean rv = true;

    for (FeatureExtractor extractor : extractors) {
      if (!extractor.processElement(result, element, featureDictionary)) {
        rv = false;
        break;
      }
    }

    return rv;
  }
}
