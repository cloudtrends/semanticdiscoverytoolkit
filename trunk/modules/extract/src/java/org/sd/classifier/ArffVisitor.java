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


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Base utility for visiting the attributes and instances in an arff.
 * <p>
 * This class is thread safe (at this level).
 *
 * @author Spence Koehler
 */
public abstract class ArffVisitor {
  
  protected ArffVisitor() {
  }

  public void visit(File arffFile) throws IOException {
    final FeatureDictionary featureDictionary = new FeatureDictionary(arffFile);
    final List<FeatureAttribute> featureAttributes = featureDictionary.getFeatureAttributeList();

    if (runPreAttributeVisitHook(featureDictionary, featureAttributes)) {
      int attributeIndex = 0;
      for (FeatureAttribute featureAttribute : featureAttributes) {
        if (!visitFeatureAttribute(featureDictionary, featureAttribute, attributeIndex)) {
          break;
        }
        ++attributeIndex;
      }
      runPostAttributeVisitHook(featureDictionary, featureAttributes, attributeIndex);
    }

    if (runPreVectorVisitHook(featureDictionary, featureAttributes)) {
      final BufferedReader reader = FileUtil.getReader(arffFile);
      spinToFeatureVectors(reader);

      int instanceNum = 0;
      String line = null;
      while ((line = reader.readLine()) != null) {
        if (!visitFeatureVector(featureDictionary, featureAttributes, line, instanceNum)) {
          break;
        }
        ++instanceNum;
      }
      reader.close();

      runPostVectorVisitHook(featureDictionary, featureAttributes, instanceNum);
    }
  }

  protected final void spinToFeatureVectors(BufferedReader arffReader) throws IOException {
    boolean dataFound = false;
    String line = null;

    while ((line = arffReader.readLine()) != null) {
      if (line.trim().equals("@DATA")) {
        dataFound = true;
        break;
      }
    }
  }

  /**
   * Hook to run (override) for performing operations before iterating over attributes.
   *
   * @return true to iterate over attributes, false to skip.
   */
  protected boolean runPreAttributeVisitHook(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes) {
    return true;
  }


  /**
   * Visit the given featureAttribute.
   *
   * @return true to keep iterating, false to stop iterating over attributes.
   */
  protected abstract boolean visitFeatureAttribute(FeatureDictionary featureDictionary, FeatureAttribute featureAttribute, int attributeIndex);


  /**
   * Hook to run (override) for performing operations after iterating over attributes.
   * <p>
   * This is only called if runPreAttributeVisitHook returned true.
   */
  protected void runPostAttributeVisitHook(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes, int lastAttributeIndex) {
  }

  /**
   * Hook to run (override) for performing operations before iterating over vectors.
   *
   * @return true to iterate over vectors, false to skip.
   */
  protected boolean runPreVectorVisitHook(FeatureDictionary featureDictionary, final List<FeatureAttribute> featureAttributes) {
    return true;
  }


  /**
   * Visit the given feature vector's instance line.
   * <p>
   * An implementation can call the visitVectorValues method and override the visitVectorValue method.
   *
   * @return true to keep iterating, false to stop iterating over attributes.
   */
  protected abstract boolean visitFeatureVector(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes, String instanceLine, int instanceNum);


  /**
   * Hook to run (override) for performing operations after iterating over vector instances.
   * <p>
   * This is only called if runPreVectorVisitHook returned true.
   */
  protected void runPostVectorVisitHook(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes, int lastInstanceNum) {
  }

  /**
   * Utility method to call to visit each value in a vector instance.
   * <p>
   * Override the visitVectorValue method to handle each value.
   *
   * @return true if all values were visited; otherwise, false.
   */
  protected final boolean visitVectorValues(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes, String instanceLine, int instanceNum) {
    boolean rv = true;

    final int len = instanceLine.length();
    int lineCharIndex = 0;
    int attributeNum = 0;
    char lineChar = 0;

    final StringBuilder valueBuffer = new StringBuilder();

    for (lineCharIndex = 0; lineCharIndex < len; ++lineCharIndex) {
      while (lineCharIndex < len) {
        final char c = instanceLine.charAt(lineCharIndex);
        if (c != ',') {
          valueBuffer.append(c);
          ++lineCharIndex;
        }
        else break;
      }

      if (!visitVectorValue(featureDictionary, featureAttributes, instanceLine, instanceNum, attributeNum, valueBuffer.toString())) {
        rv = false;
        break;
      }

      valueBuffer.setLength(0);
      ++attributeNum;
    }

    return rv;
  }

  /**
   * Method to override to handle each value in a vector instance.
   *
   * @return true to keep visiting values; otherwise, false.
   */
  protected boolean visitVectorValue(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes, String instanceLine, int instanceNum, int attributeNum, String attributeValue) {
    return true;
  }
}
