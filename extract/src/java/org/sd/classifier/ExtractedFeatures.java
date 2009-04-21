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
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sd.io.FileUtil;
import org.sd.util.StringUtil;

/**
 * Container for attributes (FeatureDictionary) and instances (FeatureVectors).
 *
 * @author Dave Barney
 */
public class ExtractedFeatures {

  private FeatureDictionary featureDictionary;
  private List<FeatureVector> featureVectors;
  private List<String> sourceLines;

  public ExtractedFeatures(FeatureDictionary featureDictionary) {
    this.featureDictionary = featureDictionary;
    this.featureVectors = new ArrayList<FeatureVector>();
    this.sourceLines = new ArrayList<String>();
  }

  public ExtractedFeatures(File arffFile, File srcFile) throws IOException {
    this.featureDictionary = new FeatureDictionary(arffFile);
    this.featureVectors = FeatureVector.loadFeatureVectors(arffFile, featureDictionary);

    this.sourceLines = new ArrayList<String>();
    loadSourceLines(srcFile);
  }
  
  public void merge(File arffFile, File srcFile) throws IOException {
    FeatureDictionary localFeatureDictionary = featureDictionary.appendArffFile(arffFile);
    this.featureVectors.addAll(FeatureVector.loadFeatureVectors(arffFile, localFeatureDictionary));
    loadSourceLines(srcFile);
  }

  private final void loadSourceLines(File srcFile) throws IOException {
    // line number index
    int index = sourceLines.size()+1;
    
    if (srcFile != null) {
      final BufferedReader reader = FileUtil.getReader(srcFile);
      String line = null;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split("\\|");
        parts[0] = "" + (index++);
        String newLine = StringUtil.join(parts, "|");
        sourceLines.add(newLine);
      }
      reader.close();
    }
  }

  public void add(FeatureVector featureVector, LabeledInput labeledInput) {
    this.featureVectors.add(featureVector);
    this.sourceLines.add(labeledInput.getSourceLine(featureVectors.size()));
  }

  /**
   * Get the feature vectors.
   */
  public List<FeatureVector> getFeatureVectors() {
    return featureVectors;
  }

  /**
   * Get the source lines corresponding to the vectors.
   */
  public List<String> getSourceLines() {
    return sourceLines;
  }
  
  public FeatureDictionary getFeatureDictionary() {
	  return this.featureDictionary;
  }

  /**
   * Writes to SD-Enhances ARFF file.
   * 
   * @param arffFile output arff file
   * @throws IOException
   */
  public void writeToArff(File arffFile) throws IOException {
    BufferedWriter writer = FileUtil.getWriter(arffFile);
    writeToArff(writer);
    writer.close();
  }

  /**
   * Writes to SD-Enhances ARFF writer.
   * 
   * @param writer output arff writer
   * @throws IOException
   */
  public void writeToArff(BufferedWriter writer) throws IOException {
    writer.write(featureDictionary.getArffString());

    List<FeatureAttribute> featureAttributes = featureDictionary.getFeatureAttributeList();
    for (FeatureVector fv : featureVectors) {
      
      for (int i=0; i < featureAttributes.size(); i++) {
        if (i > 0) writer.write(",");
        Double value = fv.getValue(featureAttributes.get(i));
        writer.write(value == null ? "?" : featureDictionary.toString(featureAttributes.get(i), value));
      }
      writer.newLine();
    }
  }

  /**
   * Writes source lines to the file in the same order as arff instances.
   *
   * @param srcFile  output source file
   * @throws IOException
   */
  public void writeToSource(File srcFile) throws IOException {
    final BufferedWriter writer = FileUtil.getWriter(srcFile);
    writeToSource(writer);
    writer.close();
  }

  /**
   * Writes source lines to the writer in the same order as arff instances.
   *
   * @param writer output source writer
   * @throws IOException
   */
  public void writeToSource(BufferedWriter writer) throws IOException {
    for (String sourceLine : sourceLines) {
      writer.write(sourceLine);
      writer.newLine();
    }
  }
}
