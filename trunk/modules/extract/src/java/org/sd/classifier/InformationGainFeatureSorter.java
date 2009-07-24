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
import java.util.List;

import org.sd.io.FileUtil;
import org.sd.util.MathUtil;

/**
 * Scratch-pad to play with information gain, entropy, etc on features.
 * 
 * @author Dave Barney
 */
public class InformationGainFeatureSorter {
  /** Command-line usage */
  public static final String USAGE = "\nUSAGE:\n\tjava " + InformationGainFeatureSorter.class.getName() + " <in arff> <out file>\n";

  private int getVectorCount(File inArffFile) throws IOException {
    String line;
    boolean dataFound = false;
    int lineCount = 0;
    
    BufferedReader reader = FileUtil.getReader(inArffFile);
    
    while ((line = reader.readLine()) != null) {
      if (!dataFound) {
        if (line.trim().equals("@DATA")) {
          dataFound = true;
          continue;
        }
        if (!dataFound) continue;
      }
      lineCount++;
    }
    
    return lineCount;
  }
  
  public void compute(File inArffFile, File outFile) throws IOException {
    FeatureDictionary featureDictionary = new FeatureDictionary(inArffFile);
    List<FeatureAttribute> featureAttributes = featureDictionary.getFeatureAttributeList();
    
    int vectorCount = getVectorCount(inArffFile);
    String[] classification = new String[vectorCount];
    short[][] values = new short[vectorCount][featureAttributes.size()-1];
    double[] infoGain = new double[featureAttributes.size()-1];

    loadFeatures(inArffFile, values, classification);
    computeInfoGain(values, classification, infoGain);

    BufferedWriter writer = FileUtil.getWriter(outFile);
    for (int i=0; i < infoGain.length; i++) {
      writer.write(featureAttributes.get(i+1).getName() + "|" + infoGain[i]);
      writer.newLine();
    }
    writer.close();
  }

  private void computeInfoGain(short[][] values, String[] classification, double[] infoGain) {
    for (int i=0; i < infoGain.length; i++) {
      int b2bPosCount = 0;
      int b2bNegCount = 0;
      int nonPosCount = 0;
      int nonNegCount = 0;

      //compute pos/neg count for both classes
      for (int j=0; j < values.length; j++) {
        if (classification[j].charAt(0) == 'B') {
          if (values[j][i] == 1) b2bPosCount++;
          else b2bNegCount++;
        } else {
          if (values[j][i] == 1) nonPosCount++;
          else nonNegCount++;
        }
      }
      
      final int b2bTotal = b2bPosCount + b2bNegCount;
      final int nonTotal = nonPosCount + nonNegCount;
      
      infoGain[i] = MathUtil.computeEntropy(b2bTotal, nonTotal) - (((double)b2bTotal/values.length) * MathUtil.computeEntropy(b2bPosCount, b2bNegCount) + ((double)nonTotal/values.length) * MathUtil.computeEntropy(nonPosCount, nonNegCount));
    }
  }
  
  private void loadFeatures(File inArffFile, short[][] values, String[] classification) throws IOException {
    BufferedReader reader = FileUtil.getReader(inArffFile);
    String line;
    boolean dataFound = false;
    int vectorIndex = 0;

    while ((line = reader.readLine()) != null) {
      if (!dataFound) {
        if (line.trim().equals("@DATA")) {
          dataFound = true;
          continue;
        }
        if (!dataFound) continue;
      }

      final int commaIndex = line.indexOf(",");
      classification[vectorIndex] = line.substring(0, commaIndex);
      
      final int len = line.length();
      int index = 0;
      for (int i = commaIndex+1; i < len; i+=2) {
        if (line.charAt(i) == '1') {
          values[vectorIndex][index] = 1;
        } else {
          values[vectorIndex][index] = 0;
        }
        ++index;
      }
      vectorIndex++;
    }
    
    reader.close();
  }
  
  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.err.println(USAGE);
      return;
    }
    
    File inArffFile = new File(args[0]);
    File outFile = new File(args[1]);
    
    InformationGainFeatureSorter igfs = new InformationGainFeatureSorter();
    igfs.compute(inArffFile, outFile);
  }
}
