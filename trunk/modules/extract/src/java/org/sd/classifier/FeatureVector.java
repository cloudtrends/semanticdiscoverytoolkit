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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sd.io.FileUtil;

/**
 * Class to associate each of a set of feature attributes to an instantiated value.
 *
 * @author Ryan McGuire, Spence Koehler
 */
public class FeatureVector {

  private Map <FeatureAttribute,Double> attr_map;
  private Boolean extractionFlag;
  private FeatureDictionary featureDictionary;
  
  public FeatureVector() {
    this.attr_map = new HashMap <FeatureAttribute,Double>();
    this.extractionFlag = null;
    this.featureDictionary = null;
  }

  /**
   * Set the value of a feature attribute in this vector.
   *
   * @param attribute  The feature attribute to set.
   * @param value      The extracted value for the attribute in this instance.
   *
   * @return true if successfully set; otherwise, false.
   */
  public boolean setValue(FeatureAttribute attribute, String value) {
    boolean result = false;

    // Ignore null as an attribute.
    if (attribute != null) {
      final Double dvalue = attribute.toDouble(value);
      if (dvalue != null) {
        addAttribute(attribute, dvalue);
        result = true;
      }
    }

    return result;
  }

  /**
   * Set the value of a feature attribute in this vector.
   *
   * @param attribute  The feature attribute to set.
   * @param value      The value for the attribute in this instance.
   *
   * @return true if successfully set; otherwise, false.
   */
  public boolean setValue(FeatureAttribute attribute, double value){
    //Ignore null as an attribute
    if (attribute == null){
      return false;
    }
    addAttribute(attribute, value);
    return true; 
  }
  
  private final void addAttribute(FeatureAttribute attribute, double value) {
    attr_map.put(attribute, value);
    if (featureDictionary == null) {
      featureDictionary = attribute.getFeatureDictionary();
    }
  }

  /**
   * Get the value of a feature attribute in this vector.
   * <p>
   * Here is where special "bag" logic is applied such that if the attribute
   * is from a bag of words and this vector has not recorded a value for the
   * attribute, then the "not present" value will be returned. This is intended
   * to exhibit the behavior that words not present are known to be not present
   * as opposed to unknown or uninstantiated. If it is being asked for (because
   * the dictionary knows about it) and this vector doesn't have it, then it
   * is a word known not to be present.
   *
   * @return the set value, default value, or null if not set.
   */
  public Double getValue(FeatureAttribute attribute){
    Double result = attr_map.get(attribute);
    
    if (result == null) {
      result = attribute.getDefaultValue();
    }

    return result;
  }

  /**
   * Get the feature attributes in this vector
   */
  public Set<FeatureAttribute> getAttributes(){
    return attr_map.keySet();
  }

  /**
   * Set the extraction flag on this vector.
   */
  public void setExtractionFlag(Boolean extractionFlag) {
    this.extractionFlag = extractionFlag;
  }

  /**
   * Get the extraction flag from this vector.
   * <p>
   * The extraction flag allows us to test the status of the features on this vector.
   * <p>
   * If null, then an extractor has not been run.
   * <p>
   * If false, then the extractor has been run, but failed.
   * <p>
   * If true, then the extractor has been run and succeeded.
   *
   * @return the extractionFlag.
   */
  public Boolean getExtractionFlag() {
    return extractionFlag;
  }

  /**
   * Get this instance's feature's values as strings in dictionary order.
   * <p>
   * If there are no attributes set on this vector, then null will be returned.
   * <p>
   * Note that values not recorded in this vector for attributes known by the
   * dictionary are also included, but may be null.
   */
  public String[] getValuesStrings() {
    if (featureDictionary == null) return null;
    final List<FeatureAttribute> featureAttributes = featureDictionary.getFeatureAttributeList();
    String[] result = new String[featureAttributes.size()];

    int index = 0;
    for (FeatureAttribute featureAttribute : featureAttributes) {
      Double value = getValue(featureAttribute);
      result[index++] = value == null ? null : featureDictionary.toString(featureAttribute, value);
    }

    return result;
  }

  /**
   * Get this instance's feature's values as doubles in dictionary order.
   * <p>
   * If there are no attributes set on this vector, then null will be returned.
   * <p>
   * Note that values not recorded in this vector for attributes known by the
   * dictionary are also included, but may be null.
   */
  public Double[] getValuesValues() {
    if (featureDictionary == null) return null;
    final List<FeatureAttribute> featureAttributes = featureDictionary.getFeatureAttributeList();
    final Double[] result = new Double[featureAttributes.size()];

    int index = 0;
    for (FeatureAttribute featureAttribute : featureAttributes) {
      Double value = getValue(featureAttribute);
      result[index++] = value;
    }

    return result;
  }

  public static List<FeatureVector> loadFeatureVectors(File arffFile, FeatureDictionary localFeatureDictionary) throws IOException {
    final List<FeatureVector> featureVectors = new ArrayList<FeatureVector>();
    final List<FeatureAttribute> featureAttributeList = localFeatureDictionary.getFeatureAttributeList();
    
    // read ARFF
    final BufferedReader reader = FileUtil.getReader(arffFile);
    boolean dataFlag = false;
    String line;
    int lineCount = 0;
    
    while ((line = reader.readLine()) != null) {
      lineCount++;

      // if we have not yet reached the data header, then skip
      if (!dataFlag) {
        if (line.startsWith("@DATA")) {
          dataFlag = true;
          continue;
        }
          if (!dataFlag) continue;
      }
      
      // load feature vector values
      final String[] values = line.split(",");
      final FeatureVector fv = new FeatureVector();
      if (values.length != featureAttributeList.size()) {
        System.err.println("\tInvalid ARFF line from " + arffFile + " - Attribute list and feature vector do not align " + (values.length + ":" + featureAttributeList.size()) + "\tLine: " + lineCount);
        continue;
      }
      
      for (int i=0; i < values.length; i++) {
        // skip empty values
        if (values[i].equals("?")) continue;
        
        FeatureAttribute curr = featureAttributeList.get(i);
        if (curr.asNominal() != null) {
          fv.setValue(curr, values[i]);
        } else {
          fv.setValue(curr, Double.parseDouble(values[i]));
        }
      }
      
      featureVectors.add(fv);
    }
    
    reader.close();
    
    return featureVectors;
  }
}
