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


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility to prune attributes by name from an arff, including whole bags.
 * <p>
 * @author Spence Koehler
 */
public class ArffAttributePruner extends ArffTranslator {
  
  private Set<String> attributeNamesToPrune;

  public ArffAttributePruner(String[] attributeNamesToPrune) {
    super();

    this.attributeNamesToPrune = new HashSet<String>(Arrays.asList(attributeNamesToPrune));
  }

  /**
   * Hook to run before beginning attribute translation.
   *
   * @return true to iterate over attributes, false to skip.
   */
  protected boolean runPreAttributeTranslateHook(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes) {
    return true;
  }


  /**
   * Visit the given featureAttribute.
   *
   * @return true to keep iterating, false to stop iterating over attributes.
   */
  protected boolean visitFeatureAttribute(FeatureDictionary featureDictionary, FeatureAttribute featureAttribute, int attributeIndex) {
    return true;
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

    // remove attributes
    for (FeatureAttribute featureAttribute : featureAttributes) {
      if (isAttributeToPrune(featureAttribute)) {
        featureDictionary.removeFeatureAttribute(featureAttribute);
      }
    }

    return true;
  }


  /**
   * Get the translated vector value, where null indicates that the value is to be removed.
   *
   * @return the translated value, possibly null to signal deletion.
   */
  protected String getTranslatedVectorValue(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes, String instanceLine, int instanceNum, int attributeNum, String attributeValue) {
    final FeatureAttribute attribute = featureAttributes.get(attributeNum);
    return isAttributeToPrune(attribute) ? null : attributeValue;
  }

  private final boolean isAttributeToPrune(FeatureAttribute featureAttribute) {
    final String[] bagWord = featureAttribute.getBagOfWords();
    final String name = (bagWord != null) ? bagWord[0] : featureAttribute.getName();
    return attributeNamesToPrune.contains(name);
  }


  public static final void main(String[] args) throws IOException {
    //arg0: comma-delimitted names list (bagNames and/or featureNames)
    //arg1: input arff
    //arg2: output arff

    final ArffAttributePruner pruner = new ArffAttributePruner(args[0].split("\\s*\\,\\s*"));
    pruner.translate(new File(args[1]), new File(args[2]));  // translate arg0 to arg1
  }
}
