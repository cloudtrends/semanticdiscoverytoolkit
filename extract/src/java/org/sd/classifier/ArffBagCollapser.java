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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An arff visitor for finding words exclusively in each bag.
 * <p>
 * @author Spence Koehler
 */
public class ArffBagCollapser extends ArffTranslator {

  private Map<String, Set<String>> bagName2words;
  private Set<String> attributesToRemove;

  public ArffBagCollapser() {
    super();
  }

  /**
   * Hook to run before beginning attribute translation.
   *
   * @return true to iterate over attributes, false to skip.
   */
  protected boolean runPreAttributeTranslateHook(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes) {
    this.bagName2words = new HashMap<String, Set<String>>();
    return true;
  }


  /**
   * Visit the given featureAttribute.
   *
   * @return true to keep iterating, false to stop iterating over attributes.
   */
  protected boolean visitFeatureAttribute(FeatureDictionary featureDictionary, FeatureAttribute featureAttribute, int attributeIndex) {
    final String[] nameWord = featureAttribute.getBagOfWords();
    if (nameWord != null) {
      final String bagName = nameWord[0];
      final String bagWord = nameWord[1];

      Set<String> bagAttributes = bagName2words.get(bagName);
      if (bagAttributes == null) {
        bagAttributes = new HashSet<String>();
        bagName2words.put(bagName, bagAttributes);
      }
      bagAttributes.add(bagWord);
    }

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
    this.attributesToRemove = new HashSet<String>();

    // remove words that are common among bags.
    for (Map.Entry<String, Set<String>> entry : bagName2words.entrySet()) {
      final String bagName = entry.getKey();
      final Set<String> bagWords = entry.getValue();

      for (Iterator<String> iter = bagWords.iterator(); iter.hasNext(); ) {
        final String word = iter.next();
        boolean foundRepeat = false;

        for (Map.Entry<String, Set<String>> inner : bagName2words.entrySet()) {
          final String innerName = inner.getKey();
          final Set<String> innerWords = inner.getValue();

          if (innerWords != bagWords) {
            if (innerWords.contains(word)) {
              foundRepeat = true;
              innerWords.remove(word);

              // remove feature from dictionary  (innerName + word)
              attributesToRemove.add("_" + innerName + "_" + word);
            }
          }
        }
        if (foundRepeat) {
          iter.remove();

          // remove feature from dictionary  (bagName + word)
          attributesToRemove.add("_" + bagName + "_" + word);
        }
      }
    }

    // remove attributes
    for (FeatureAttribute featureAttribute : featureAttributes) {
      if (attributesToRemove.contains(featureAttribute.getName())) {
        featureDictionary.removeFeatureAttribute(featureAttribute);
      }
    }

//     // dump bags
//     for (Map.Entry<String, Set<String>> entry : bagName2words.entrySet()) {
//       final String bagName = entry.getKey();
//       final Set<String> bagWords = entry.getValue();

//       for (String word : bagWords) {
//         System.out.println(bagName + "|" + word);
//       }
//     }

    return true;
  }


  /**
   * Get the translated vector value, where null indicates that the value is to be removed.
   *
   * @return the translated value, possibly null to signal deletion.
   */
  protected String getTranslatedVectorValue(FeatureDictionary featureDictionary, List<FeatureAttribute> featureAttributes, String instanceLine, int instanceNum, int attributeNum, String attributeValue) {
    final FeatureAttribute attribute = featureAttributes.get(attributeNum);
    return attributesToRemove.contains(attribute.getName()) ? null : attributeValue;
  }


  public static final void main(String[] args) throws IOException {
    final ArffBagCollapser collapser = new ArffBagCollapser();
    collapser.translate(new File(args[0]), new File(args[1]));  // translate arg0 to arg1
  }
}
