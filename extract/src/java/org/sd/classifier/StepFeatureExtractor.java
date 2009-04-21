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
import org.sd.extract.TextContainer;

/**
 * A feature extractor that steps down into its input to apply another extractor.
 * <p>
 * @author Spence Koehler
 */
public class StepFeatureExtractor extends FeatureExtractor {

  private FeatureExtractor extractor;

  public StepFeatureExtractor() {
    this.extractor = null;
  }

//todo: make properties-based constructor.

  public StepFeatureExtractor(FeatureExtractor featureExtractor) {
    this();
    setFeatureExtractor(featureExtractor);
  }

  public final void setFeatureExtractor(FeatureExtractor featureExtractor) {
    this.extractor = featureExtractor;
    setNeedsDocTextCache(extractor.needsDocTextCache());
  }
  
  /**
   * Process an element, setting attribute(s) value(s) as warranted on the feature vector.
   */
  protected boolean processElement(FeatureVector result, DocText element, FeatureDictionary featureDictionary) {
    boolean rv = true;

    final TextContainer stepContainer = element.asTextContainer(needsDocTextCache());
    if (stepContainer != null) {
      rv = extractor.process(result, stepContainer, featureDictionary);
    }

    return rv;
  }
}
