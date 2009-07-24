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
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.sd.util.MathUtil;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;

/**
 * Represents a trained Weka classifier in execution. This
 * class loads an serialized Weka classifier and applies
 * {@link ExtractedFeatures} to it, but first converting
 * to the weka libraries (e.g. {@link Instance}).
 *  
 * @author Dave Barney
 */
public class WekaClassifier implements Classifier {
  /** Internal reference to Weka classifier */
  private weka.classifiers.Classifier classifier;
  
  /**
   * Even though we are just classifying one instance as a time, Weka
   * requires a data set ({@link Instances} attached to each
   * {@link Instance} we classify.
   */
  private Instances dataSet;
  
  
  /**
   * Constructed with a serialized weka classifer, as well as the ARFF file
   * used to create the serialized weka classifer.
   * 
   * @param serializedWekaClassifier points to serialized weka classifier
   * @param arffFile                 arff used to train serialized classifier
   * @throws IOException
   */
  public WekaClassifier(File serializedWekaClassifier, File arffFile) throws IOException, ClassNotFoundException {
    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serializedWekaClassifier));
    this.classifier = (weka.classifiers.Classifier)ois.readObject();
    this.dataSet = (new ArffReader(new FileReader(arffFile))).getData();
    
    // todo: we currently have everything hard-coded to use the first attribute as the class label, but we may
    //       want to make this somehow accessible from the FeatureDictionary or something
    dataSet.setClassIndex(0);
  }
  
  public ClassificationResult classify(FeatureVector featureVector, FeatureDictionary featureDictionary) {
    double[] attrValues = featureDictionary.getFeatureVectorAsDouble(featureVector);
    
    // we are required to put a weight for each instance, so we weight them all as 1.0
    Instance instance = new Instance(1.0, attrValues);
    instance.setDataset(dataSet);
    
    ClassificationResult classificationResult = null;
    
    try {
      double[] distribution = classifier.distributionForInstance(instance);
      double value = classifier.classifyInstance(instance);
      int index = distribution.length == 1 ? 0 : MathUtil.toInt(value);
      classificationResult = new WekaClassificationResult(value, distribution[index], featureDictionary.getClassificationLabel(value), featureVector);

    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return classificationResult;
  }
}
