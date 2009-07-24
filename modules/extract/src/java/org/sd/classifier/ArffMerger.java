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
import org.sd.util.StringUtil;

/**
 * Utility that merges .arff and .src files for the same
 * data-set, but which have been created via the cluster
 * in a distributed manner.
 * 
 * @author Dave Barney
 */
public class ArffMerger {
  /** Command-line usage */
  public static final String USAGE = "\nUSAGE:\n\tjava " + ArffMerger.class.getName() + " <output name (no extension)> <dir containing .arff and .src files>\n";

  /** The directory where all the input .arff and .src files exist */
  private File inDir;
  
  
  /**
   * Constructor.
   * 
   * @param inDir the directory where all the input .arff and .src files exist
   */
  public ArffMerger(File inDir) throws IOException {
    this.inDir = inDir;
  }

  /**
   * Performs the actual merge of the input .arff and .src file.
   * 
   * @param outArffFile the merged .arff file
   * @param outSrcFile the marged .src file
   * @throws IOException
   */
  public void merge(File outArffFile, File outSrcFile) throws IOException {
    // arff stuff
    final FeatureDictionary masterFeatureDictionary = mergeDict();
    final BufferedWriter arffWriter = FileUtil.getWriter(outArffFile);
    arffWriter.write(masterFeatureDictionary.getArffString());
    
    
    // src stuff
    int srcLineCount = 1;
    final BufferedWriter srcWriter = FileUtil.getWriter(outSrcFile);
    
    
    System.out.println();
    System.out.println(new Date() + "\tMerging data...");
    final File[] files = inDir.listFiles();
    for (File arffFile : files) {
      if (!arffFile.getName().endsWith(".arff") && !arffFile.getName().endsWith(".arff.gz")) continue;
      
      File srcFile = null;
      if (arffFile.getName().endsWith(".arff")) {
        srcFile = new File(inDir, arffFile.getName().substring(0, arffFile.getName().length()-".arff".length())+".src");
      } else if (arffFile.getName().endsWith(".arff.gz")) {
        srcFile = new File(inDir, arffFile.getName().substring(0, arffFile.getName().length()-".arff.gz".length())+".src.gz");
      }
      if (!srcFile.exists()) {
        System.err.println(new Date() + "\tMissing SRC file: " + srcFile);
        continue;
      }
      System.out.println(new Date() + "\tProcessing " + arffFile + " and " + srcFile);

      
      // arff stuff
      final FeatureDictionary localFeatureDictionary = masterFeatureDictionary.appendArffFile(arffFile);
      final List<FeatureVector> featureVectors = FeatureVector.loadFeatureVectors(arffFile, localFeatureDictionary);
      final List<FeatureAttribute> featureAttributes = masterFeatureDictionary.getFeatureAttributeList();
      
      for (FeatureVector fv : featureVectors) {
        int index=0;
        for (FeatureAttribute featureAttribute : featureAttributes) {
          if (index > 0) arffWriter.write(",");
          else index++;
          
          final Double value = fv.getValue(featureAttribute);
          arffWriter.write(value == null ? "?" : masterFeatureDictionary.toString(featureAttribute, value));
        }
        arffWriter.newLine();
      }
      
      
      // src stuff
      final BufferedReader reader = FileUtil.getReader(srcFile);
      String line = null;
      while ((line = reader.readLine()) != null) {
        final String[] parts = line.split("\\|");
        parts[0] = "" + (srcLineCount++);
        srcWriter.write(StringUtil.join(parts, "|"));
        srcWriter.newLine();
      }
    }
   
    
    arffWriter.close();
    srcWriter.close();
  }
  
  private FeatureDictionary mergeDict() throws IOException {
    System.out.println();
    System.out.println(new Date() + "\tMerging dictionaries...");
    final File[] files = inDir.listFiles();
    FeatureDictionary masterFeatureDictionary = null;
    
    for (File arffFile : files) {
      if (!arffFile.getName().endsWith(".arff") && !arffFile.getName().endsWith(".arff.gz")) continue;
      
      System.out.println(new Date() + "\tProcessing " + arffFile);
      if (masterFeatureDictionary == null) {
        masterFeatureDictionary = new FeatureDictionary(arffFile);
      } else {
        try {
          masterFeatureDictionary.appendArffFile(arffFile);
        }
        catch (IllegalArgumentException e) {
          // ignore empty file.
          System.err.println("*** WARNING: Ignoring empty arffFile '" + arffFile + "'");
        }
      }
    }

    return masterFeatureDictionary;
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.err.println(USAGE);
      return;
    }
    
    final String outputName = args[0];
    final File outArffFile = new File(outputName + ".arff.gz");
    final File outSrcFile = new File(outputName + ".src.gz");
    final File inDir = new File(args[1]);
    
    final ArffMerger arffMerger = new ArffMerger(inDir);
    arffMerger.merge(outArffFile, outSrcFile);
  }
}
