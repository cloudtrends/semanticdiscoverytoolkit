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

/**
 * To prune features.
 * 
 * @author Dave Barney
 */
public class ArffPruner {
  /** Command-line usage */
  public static final String USAGE = "\nUSAGE:\n\tjava " + ArffPruner.class.getName() + " <arff file> <pruned arff filer> <min keep count>\n";
  
  /** Minimum feature count occurance required to keep the feature */
  private int minKeepCount;
  
  
  public ArffPruner(int minKeepCount) {
    this.minKeepCount = minKeepCount;
  }
  
  public void prune(File arffFile, File newArffFile) throws IOException {
    final FeatureDictionary featureDictionary = new FeatureDictionary(arffFile);
    final List<FeatureAttribute> featureAttributes = featureDictionary.getFeatureAttributeList();
    final boolean[] isBag = computeIsBag(featureAttributes);
    final int[] counts = computeFeatureCounts(arffFile, featureAttributes, isBag);
    
    System.out.println();
    System.out.println(new Date() + "\tRemoving features from dictionary");

    featureDictionary.unlock();
    for (int i=counts.length-1; i >= 0; i--) {
      if (counts[i] < minKeepCount) {
        featureDictionary.removeFeatureAttribute(featureAttributes.get(i+1));
      }
    }
    featureDictionary.lock();
    System.out.println();

    BufferedWriter writer = FileUtil.getWriter(newArffFile);
    writer.write(featureDictionary.getArffString());
    writePrunedFeatures(writer, arffFile, counts);
    writer.close();
  }

  private void writePrunedFeatures(BufferedWriter writer, File arffFile, int[] counts) throws IOException {
    System.out.println();
    System.out.println(new Date() + "\tRemoving features from feature vectors (" + arffFile + ")");
    final BufferedReader reader = FileUtil.getReader(arffFile);
    String line;
    boolean dataFound = false;
    int lineCount = 0;
    
    while ((line = reader.readLine()) != null) {
      if (!dataFound) {
        if (line.trim().equals("@DATA")) {
          dataFound = true;
          continue;
        }
      
        if (!dataFound) continue;
      }

      lineCount++;
      if (lineCount%1000 == 0) System.out.println(new Date() + "\t\t" + lineCount + " data lines processed");

      int commaIndex = line.indexOf(",") + 1;
      writer.write(line.substring(0, commaIndex));
      
      final int len = line.length();
      int index = 0;
      for (int i = commaIndex; i < len; ++i) {
        if (counts[index] >= minKeepCount) {
          while (i < len && line.charAt(i) != ',') writer.write(line.charAt(i++));
          if (i < len) writer.write(',');
        }
        else {
          for (; i < len && line.charAt(i) != ','; ++i);
        }
        ++index;
      }
            
      writer.newLine();
    }
  }

  private boolean[] computeIsBag(final List<FeatureAttribute> featureAttributes) {
    final boolean[] result = new boolean[featureAttributes.size() - 1];

    final int len = featureAttributes.size();
    for (int i = 1; i < len; ++i) {
      result[i - 1] = (featureAttributes.get(i).getBagOfWords() != null);
    }

    return result;
  }

  private int[] computeFeatureCounts(File arffFile, final List<FeatureAttribute> featureAttributes, boolean[] isBag) throws IOException {
    System.out.println();
    System.out.println(new Date() + "\tComputing feature stats (" + arffFile + ")");
    
    final int[] counts = new int[featureAttributes.size()-1];
    for (int i=0; i < counts.length; i++) counts[i] = 0;
    
    final BufferedReader reader = FileUtil.getReader(arffFile);
    String line;
    boolean dataFound = false;
    int lineCount = 0;
    
    while ((line = reader.readLine()) != null) {
      if (!dataFound) {
        if (line.trim().equals("@DATA")) {
          dataFound = true;
          continue;
        }
      
        if (!dataFound) continue;
      }

      lineCount++;
      if (lineCount%1000 == 0) System.out.println(new Date() + "\t\t\t" + lineCount + " data lines processed");
      
      final int len = line.length();
      int index = 0;
      for (int i = line.indexOf(",")+1; i < len; ++i) {
        final char c = line.charAt(i);
        if ((!isBag[index] && c != '?') || c == '1') ++counts[index];
        ++index;

        while ((i + 1) < len && line.charAt(++i) != ',');
      }
    }
    
    return counts;
  }
  
  public static void main(String[] args) throws IOException {
    if (args.length != 3) {
      System.err.println(USAGE);
      return;
    }
    
    final File arffFile = new File(args[0]);
    final File newArffFile = new File(args[1]);
    int minKeepCount = Integer.parseInt(args[2]);
    
    final ArffPruner pruner = new ArffPruner(minKeepCount);
    pruner.prune(arffFile, newArffFile);
  }
}
