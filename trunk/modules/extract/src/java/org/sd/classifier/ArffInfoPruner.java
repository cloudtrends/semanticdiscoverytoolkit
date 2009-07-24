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
import java.util.Date;
import java.util.List;

import org.sd.io.FileUtil;
import org.sd.util.MathUtil;

/**
 * To prune features.  Very quick and dirty, but works on numeric features only for now (optimized for processing time and memory).
 * 
 * @author Dave Barney
 */
public class ArffInfoPruner {
  /** Command-line usage */
  public static final String USAGE = "\nUSAGE:\n\tjava " + ArffInfoPruner.class.getName() + " <arff file> <pruned arff filer> <min keep count> <min info gain keep value (double)>\n";
  
  /** Minimum feature count occurance required to keep the feature */
  private int minKeepCount;
  
  /** Minimum computed info gain required to keep the feature */
  private double infoGainThreshold;
  
  
  public ArffInfoPruner(int minKeepCount, double infoGainThreshold) {
    this.minKeepCount = minKeepCount;
    this.infoGainThreshold = infoGainThreshold;
  }
  
  public void prune(File arffFile, File newArffFile) throws IOException {
    System.out.println(new Date() + "\tLoading dictionary...");
    final FeatureDictionary featureDictionary = new FeatureDictionary(arffFile);
    System.out.println(new Date() + "\tGathering FeatureAttribute list...");
    final List<FeatureAttribute> featureAttributes = featureDictionary.getFeatureAttributeList();
    boolean[] fvToRemove = new boolean[featureAttributes.size()-1];
    for (int i=0; i < fvToRemove.length; i++) fvToRemove[i] = false;

    // info gain
    System.out.println(new Date() + "\tCounting FeatureVectors...");
    final int vectorCount = getVectorCount(arffFile);
    System.out.println(new Date() + "\tCreating space for data...");
    final int[] classification = new int[vectorCount];
    final int[][] data = new int[vectorCount][fvToRemove.length];
    final float[] infoGain = new float[fvToRemove.length];
    System.out.println(new Date() + "\tLoading data...");
    loadFeatures(arffFile, data, classification);
    System.out.println(new Date() + "\tComputing InfoGain...");
    computeInfoGain(data, classification, infoGain);
    
    
    // counts
    System.out.println(new Date() + "\tComputing feature counts...");
    final int[] counts = computeFeatureCounts(data);

    // remove features
    System.out.println(new Date() + "\tRemoving features...");
    for (int i=fvToRemove.length-1; i >= 0; i--) {
      FeatureAttribute featureAttribute = featureAttributes.get(i+1);
      if (featureAttribute.getBagOfWords() == null) continue;
      if (counts[i] < minKeepCount || infoGain[i] < infoGainThreshold) {
        featureDictionary.removeFeatureAttribute(featureAttribute);
        fvToRemove[i] = true;
      }
    }

    System.out.println(new Date() + "\tWriting pruned arff...");
    BufferedWriter writer = FileUtil.getWriter(newArffFile);
    writer.write(featureDictionary.getArffString());
    writePrunedFeatures(writer, classification, data, fvToRemove);
    writer.close();
  }
  
  private void loadFeatures(File inArffFile, int[][] data, int[] classification) throws IOException {
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

      String[] parts = line.split(",");
      classification[vectorIndex] = parts[0].startsWith("B") ? 0 : 1;
      
      for (int i=1; i < parts.length; i++) {
        data[vectorIndex][i-1] = Integer.parseInt(parts[i]);
      }
      vectorIndex++;
    }
    
    reader.close();
  }

  private void computeInfoGain(int[][] data, int[] classification, float[] infoGain) {
    for (int i=0; i < infoGain.length; i++) {
      int b2bPosCount = 0;
      int b2bNegCount = 0;
      int nonPosCount = 0;
      int nonNegCount = 0;

      //compute pos/neg count for both classes
      for (int j=0; j < data.length; j++) {
        if (classification[j] == 0) {
          if (data[j][i] == 1) b2bPosCount++;
          else b2bNegCount++;
        } else {
          if (data[j][i] == 1) nonPosCount++;
          else nonNegCount++;
        }
      }
      
      final int b2bTotal = b2bPosCount + b2bNegCount;
      final int nonTotal = nonPosCount + nonNegCount;
      
      infoGain[i] = (float)(MathUtil.computeEntropy(b2bTotal, nonTotal) - (((double)b2bTotal/data.length) * MathUtil.computeEntropy(b2bPosCount, b2bNegCount) + ((double)nonTotal/data.length) * MathUtil.computeEntropy(nonPosCount, nonNegCount)));
    }
  }

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
      if (lineCount % 1000 == 0) System.out.println(new Date() + "\t\t" + lineCount + " lines processed.");
    }
    reader.close();
    
    return lineCount;
  }

  private void writePrunedFeatures(BufferedWriter writer, int[] classification, int[][] data, boolean[] fvToRemove) throws IOException {
    for (int i=0; i < data.length; i++) {
      writer.write(classification[i] == 0 ? "B2B" : "NON_B2B");
      for (int j=0; j < data[0].length; j++) {
        if (!fvToRemove[j]) writer.write("," + data[i][j]);
      }
      writer.newLine();
    }
  }

  private int[] computeFeatureCounts(int[][] data) throws IOException {
    final int[] counts = new int[data[0].length];
    for (int i=0; i < counts.length; i++) counts[i] = 0;
    
    for (int i=0; i < data.length; i++) {
      for (int j=0; j < counts.length; j++) {
        if (data[i][j] == 0) continue;
        counts[j]++;
      }
    }
    
    return counts;
  }
  
  public static void main(String[] args) throws IOException {
    if (args.length != 4) {
      System.err.println(USAGE);
      return;
    }
    
    final File arffFile = new File(args[0]);
    final File newArffFile = new File(args[1]);
    int minKeepCount = Integer.parseInt(args[2]);
    double infoGainThreshold = Double.parseDouble(args[3]);
    
    final ArffInfoPruner pruner = new ArffInfoPruner(minKeepCount, infoGainThreshold);
    pruner.prune(arffFile, newArffFile);
  }
}
