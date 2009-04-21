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
import org.sd.extract.Extraction;
import org.sd.extract.ExtractionPipeline;
import org.sd.extract.Extractor;
import org.sd.extract.TextContainer;

import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A classification framework feature extractor wrapper around a
 * org.sd.extract.Extractor or ExtractionPipeline.
 * <p>
 * @author Spence Koehler
 */
public class PrimaryFeatureExtractor extends FeatureExtractor {

  private ExtractionPipeline extractor;      
  private Map<String, AttributeConstraints> extractionType2constraints;

  public PrimaryFeatureExtractor() {
    super();

    this.extractor = new ExtractionPipeline() {
        protected TextContainer buildTextContainer(String streamId, InputStream inputStream, boolean keepDocTexts, boolean keepEmpties, AtomicBoolean die) throws IOException {
          return null;
        }
      };
    this.extractionType2constraints = new HashMap<String, AttributeConstraints>();
  }

//todo: make properties-based constructor.

  public final void addExtractor(Extractor extractor, AttributeConstraints constraints) {
    this.extractor.addExtractor(extractor);
    final String extractionType = extractor.getExtractionType();

    if (!needsDocTextCache()) {
      if (extractor.needsDocTextCache()) setNeedsDocTextCache(true);
    }

    if (constraints != null) {
      extractionType2constraints.put(extractionType, constraints);
      constraints.addExtractionType(extractor.getExtractionType());  // make sure constraints knows this type.
    }
  }

  /**
   * Process an element, setting attribute(s) value(s) as warranted on the feature vector.
   */
  public boolean processElement(FeatureVector result, DocText element, FeatureDictionary featureDictionary) {
    boolean rv = true;

    for (Extractor e : extractor.getExtractors()) {
      if (e.shouldExtract(element)) {
        final List<Extraction> extractions = e.extract(element, null);  //todo: use 'die' and set rv by it.

        if (extractions != null) {
          for (Extraction extraction : extractions) {
            setAttributeValue(result, extraction, featureDictionary);
          }
        }
      }
    }

    return rv;
  }

  /**
   * Utility to set the appropriate feature attribute's value on the feature
   * vector from the extraction if the constraints are met.
   */
  protected boolean setAttributeValue(FeatureVector result, Extraction extraction, FeatureDictionary featureDictionary) {
    final AttributeConstraints attributeConstraints = getAttributeConstraints(extraction.getExtractionType());
    return setAttributeValue(result, attributeConstraints, extraction.asString(), featureDictionary);
  }

  /**
   * Get the attribute constraints object for the extraction type.
   */
  private final AttributeConstraints getAttributeConstraints(String extractionType) {
    return extractionType2constraints.get(extractionType);
  }
}
