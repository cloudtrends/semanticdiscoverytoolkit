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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Base utility for translating one arff to another.
 * <p>
 * This class is NOT thread safe at this level. An instance per file to translate is necessary.
 *
 * @author Spence Koehler
 */
public abstract class ArffTranslator extends ArffVisitor {

  private File newArffFile;
  private BufferedWriter arffWriter;

  protected ArffTranslator() {
    super();
  }
  
  protected final File getNewArffFile() {
    return newArffFile;
  }

  protected final BufferedWriter getArffWriter() {
    return arffWriter;
  }

  public void translate(File arffFile, File newArffFile) throws IOException {
    this.newArffFile = newArffFile;
    super.visit(arffFile);
    if (arffWriter != null) arffWriter.close();
  }

  /**
   * Hook to run (override) for performing operations before iterating over attributes.
   *
   * @return true to iterate over attributes, false to skip.
   */
  protected final boolean runPreAttributeVisitHook(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes) {
    try {
      this.arffWriter = FileUtil.getWriter(newArffFile);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return runPreAttributeTranslateHook(featureDictionary, featureAttributes);
  }

  /**
   * Visit the given featureAttribute. For a translator, this means to alter attributes
   * in the feature dictionary as appropriate for the translated file as pertains to
   * the current featureAttribute (i.e. remove, rename, change range, etc.).
   *
   * @return true to keep iterating, false to stop iterating over attributes.
   */
  protected abstract boolean visitFeatureAttribute(FeatureDictionary featureDictionary, FeatureAttribute featureAttribute, int attributeIndex);

  /**
   * Hook to run (override) for performing operations after iterating over attributes.
   * <p>
   * This is only called if runPreAttributeVisitHook returned true.
   */
  protected final void runPostAttributeVisitHook(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes, int lastAttributeIndex) {

    // unlock the dictionary
    featureDictionary.unlock();

    if (runPreDictionaryWriteHook(featureDictionary, featureAttributes, lastAttributeIndex)) {
      // write new arff's header
      try {
        arffWriter.write(featureDictionary.getArffString());
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }


  /**
   * Hook to run (override) for cleaning up after all attributes have been visited,
   * but before the dictionary is written to the new arff file.
   * <p>
   * For example, if the values for an attribute have changed or been reduced, here
   * would be the place to fix them.
   *
   * @return true to write the heading; false to skip writing.
   */
  protected boolean runPreDictionaryWriteHook(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes, int lastAttributeIndex) {
    return true;
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
  protected final boolean visitFeatureVector(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes, String instanceLine, int instanceNum) {

    // log status
    if (instanceNum > 0 && instanceNum % 1000 == 0) {
      System.out.println(new Date() + "\t\t" + instanceNum + " data lines processed.");
    }

    visitVectorValues(featureDictionary, featureAttributes, instanceLine, instanceNum);
    try {
      arffWriter.newLine();
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return true;
  }

  /**
   * Hook to run (override) for performing operations after iterating over vector instances.
   * <p>
   * This is only called if runPreVectorVisitHook returned true.
   */
  protected final void runPostVectorVisitHook(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes, int lastInstanceNum) {
    // log completion
    System.out.println(new Date() + "\t\t" + lastInstanceNum + " data lines processed. Done.");

    try {
      arffWriter.close();
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }


  /**
   * Hook to run before beginning attribute translation.
   *
   * @return true to iterate over attributes, false to skip.
   */
  protected abstract boolean runPreAttributeTranslateHook(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes);


  /**
   * Method to override to handle each value in a vector instance.
   *
   * @return true to keep visiting values; otherwise, false.
   */
  protected final boolean visitVectorValue(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes, String instanceLine, int instanceNum, int attributeNum, String attributeValue) {
    final String translatedValue = getTranslatedVectorValue(featureDictionary, featureAttributes, instanceLine, instanceNum, attributeNum, attributeValue);

    if (translatedValue == null) {
      // we're removing attribute value, don't even put the comma
    }
    else {
      // we have a value to write
      try {
        if (attributeNum > 0) arffWriter.write(',');
        arffWriter.write(translatedValue);
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    return true;
  }


  /**
   * Get the translated vector value, where null indicates that the value is to be removed.
   *
   * @return the translated value, possibly null to signal deletion.
   */
  protected abstract String getTranslatedVectorValue(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes, String instanceLine, int instanceNum, int attributeNum, String attributeValue);
}
