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
package org.sd.cio.mapreduce;


import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.sd.io.DirectorySelector;

/**
 * Simple base implementation of CombinedMapReduce.
 * <p>
 * @author Spence Koehler
 */
public abstract class SimpleMapReduce<K extends Comparable<K>, V, A, R> extends CombinedMapReduce<K, V, A, R> {
  
  private boolean verbose;
  private File inDataDir;
  private File mapOutDir;
  private File reduceOutDir;
  private int maxMapPairs;
  private int maxReducePairs;

  /**
   * Construct with an ngrams directory with its 'n' for exploding out
   * <p>
   * Properties:
   * <ul>
   * <li>verbose -- (optional, default=true) whether to be verbose while working</li>
   * <li>inDataDir -- (required) path to mapper output root directory (i.e. .../LDC2006T13.mapped.0)</li>
   * <li>mapOutDir -- (required) path to root directory for map output</li>
   * <li>reduceOutDir -- (required) path to root directory for reduce output</li>
   * <li>maxMapPairs -- (required) maximum number of entries for collecting map terms</li>
   * <li>maxReducePairs -- (required) maximum number of entries to write before rolling reduce output files</li>
   * <li>minN -- (optional, default=2) minimum N of N-grams to collect</li>
   * <li>maxN -- (optional, default=5) maximum N of N-grams to collect</li>
   * <li>normalizer -- (optional, default=IndexingNormalizer) normalizer to use on ldc ngrams</li>
   * <li>splitOnCamelCase -- (optional, default=false) whether to split on came clase</li>
   * </ul>
   */
  public SimpleMapReduce(Properties properties) {
    super();

    this.verbose = "true".equalsIgnoreCase(properties.getProperty("verbose", "true"));

    final String inDataDir = properties.getProperty("inDataDir");
    if (inDataDir == null || "".equals(inDataDir)) throw new IllegalArgumentException("Need 'inDataDir'!");
    this.inDataDir = new File(inDataDir);

    final String mapOutDir = properties.getProperty("mapOutDir");
    if (mapOutDir == null || "".equals(mapOutDir)) throw new IllegalArgumentException("Need 'mapOutDir'!");
    this.mapOutDir = new File(mapOutDir);

    final String reduceOutDir = properties.getProperty("reduceOutDir");
    if (reduceOutDir == null || "".equals(reduceOutDir)) throw new IllegalArgumentException("Need 'reduceOutDir'!");
    this.reduceOutDir = new File(reduceOutDir);

    this.maxMapPairs = Integer.parseInt(properties.getProperty("maxMapPairs", "0"));
    if (maxMapPairs <= 0) throw new IllegalArgumentException("Need 'maxMapPairs'!");

    this.maxReducePairs = Integer.parseInt(properties.getProperty("maxReducePairs", "0"));
    if (maxReducePairs <= 0) throw new IllegalArgumentException("Need 'maxReducePairs'!");
  }

  // mapper
  protected boolean getVerboseFlag() {
    return verbose;
  }

  protected File getInputDir() {
    return inDataDir;
  }

  protected File getMapOutputDir() {
    return mapOutDir;
  }

  protected DirectorySelector.Action selectInputDirectory(File dir) {
    DirectorySelector.Action result = DirectorySelector.Action.DESCEND;
        
    // Skip all disabled directories. Descend all others.
    // Check in FileSelector that the dir should have ngrams.
        
    final String name = dir.getName();
    if (name.endsWith("disabled")) {
      result = DirectorySelector.Action.IGNORE;
    }
        
    return result;
  }

  // reducer
  protected File getReduceOutputDir() {
    return reduceOutDir;
  }

  protected int getMaxPairs(Integer chainNum) {
    return (chainNum == null || chainNum.equals(0)) ? maxMapPairs : maxReducePairs;
  }
}
