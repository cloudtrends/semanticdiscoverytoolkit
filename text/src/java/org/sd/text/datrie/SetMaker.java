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
package org.sd.text.datrie;


import org.sd.io.FileUtil;
import org.sd.nlp.GeneralNormalizer;
import org.sd.nlp.NormalizedString;
import org.sd.nlp.Normalizer;
import org.sd.util.MathUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility to create a hash set.
 * <p>
 * @author Spence Koehler
 */
public class SetMaker {
  
  private Normalizer normalizer;

  public SetMaker() {
    this(GeneralNormalizer.getCaseInsensitiveInstance());
  }

  public SetMaker(Normalizer normalizer) {
    this.normalizer = normalizer;
  }

  public Map<String, Integer> makeSet(File inputFile, File outputFile, boolean verbose) throws IOException {
    Map<String, Integer> result = new TreeMap<String, Integer>();

    if (inputFile.isDirectory()) {
      // load each file in the directory
      final File[] files = inputFile.listFiles();
      for (File file : files) {
        if (!file.isDirectory()) {
          load(file, result, verbose);
        }
      }
    }
    else {
      // load the file
      load(inputFile, result, verbose);
    }

    if (outputFile != null) {
      final BufferedWriter writer = FileUtil.getWriter(outputFile);
      for (Map.Entry<String, Integer> entry : result.entrySet()) {
        final String word = entry.getKey();
        final Integer freq = entry.getValue();
        writer.write(word + "|" + freq + "\n");
      }
      writer.close();
    }

    return result;
  }

  private final void load(File file, Map<String, Integer> result, boolean verbose) throws IOException {

    if (verbose) System.out.println(new Date() + ": reading '" + file + "'");

    final BufferedReader reader = FileUtil.getReader(file);

    String line = null;
    while ((line = reader.readLine()) != null) {
      final NormalizedString normalized = normalizer.normalize(line);
//      if (normalized.getNormalizedLength() > 1) {
        final String[] words = normalized.split();
        for (String word : words) {
          final Integer freq = result.get(word);
          result.put(word, freq == null ? 1 : freq + 1);
        }
//      }
    }

    reader.close();
  }


  public static final void main(String[] args) throws IOException {
    // arg0: input file or dirname for set
    // arg1: output file for set

    final File inputFile = new File(args[0]);
    final File outputFile = new File(args[1]);


    System.out.println("Loading '" + inputFile + "'...");
    final long startTime = System.currentTimeMillis();
    final Map<String, Integer> set = new SetMaker().makeSet(inputFile, outputFile, true);
    final long endTime = System.currentTimeMillis();

    System.out.println("Dumped '" + outputFile + "'.");

    System.out.println("\n\tnumWords=" + set.size());

    long total = 0L;
    for (Integer value : set.values()) total += value;
    System.out.println("\tnumValues=" + total);

    System.out.println("elapsed time: " + MathUtil.timeString(endTime - startTime, false));
  }
}
