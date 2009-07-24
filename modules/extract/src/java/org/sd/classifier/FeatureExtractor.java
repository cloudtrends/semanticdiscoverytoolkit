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
 * Class to process input to generate a feature vector.
 * <p>
 * @author Spence Koehler
 */
public abstract class FeatureExtractor {

  private boolean needsDocTextCache;

  /**
   * Process an element, setting attribute(s) value(s) as warranted on the feature vector.
   */
  protected abstract boolean processElement(FeatureVector result, DocText element, FeatureDictionary featureDictionary);


  public FeatureExtractor() {
    this.needsDocTextCache = false;
  }

  protected void setNeedsDocTextCache(boolean needsDocTextCache) {
    this.needsDocTextCache = needsDocTextCache;
  }

  public boolean needsDocTextCache() {
    return needsDocTextCache;
  }

  /**
   * Primary interface for extracting from input and maintaining
   * the dictionary.
   *
   * @param result      A (usually empty) feature vector to instantiate.
   * @param input       The input to iterate over and extract from.

   * @return true if input was successfully processed; otherwise, false.
   */
  public boolean process(FeatureVector result, TextContainer input, FeatureDictionary featureDictionary) {
    boolean rv = true;

    while (input.hasNext() && rv) {
      final DocText element = input.next();
      rv &= processElement(result, element, featureDictionary);
    }

    return rv;
  }

  /**
   * Utility to set the appropriate feature attribute's value on the feature
   * vector from the extraction if the constraints are met.
   */
  protected final boolean setAttributeValue(FeatureVector result, AttributeConstraints attributeConstraints, String extractedString, FeatureDictionary featureDictionary) {
    boolean rv = false;

    if (attributeConstraints != null) {
      rv = attributeConstraints.setIfValid(extractedString, featureDictionary, result);
    }

    return rv;
  }
}
