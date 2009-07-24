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
import org.sd.text.Trie;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Utility to create a trie.
 * <p>
 * @author Spence Koehler
 */
public class TrieMaker {
  
  private Normalizer normalizer;

  public TrieMaker() {
    this(GeneralNormalizer.getCaseInsensitiveInstance());
  }

  public TrieMaker(Normalizer normalizer) {
    this.normalizer = normalizer;
  }

  public Trie makeTrie(File inputFile, File outputFile, boolean verbose) throws IOException {
    DoubleArrayTrie result = new DoubleArrayTrie();

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
      result.dump(outputFile);
    }

    return result;
  }

  private final void load(File file, DoubleArrayTrie result, boolean verbose) throws IOException {

    if (verbose) System.out.println(new Date() + ": reading '" + file + "'");

    final BufferedReader reader = FileUtil.getReader(file);

    String line = null;
    while ((line = reader.readLine()) != null) {
      final NormalizedString normalized = normalizer.normalize(line);
      if (normalized.getNormalizedLength() > 1) {
        final String[] words = normalized.split();
        for (String word : words) {
          result.add(word);
        }
      }
    }

    reader.close();
  }


  public static final void main(String[] args) throws IOException {
    // arg0: input file or dirname for trie
    // arg1: output file for trie

    final File inputFile = new File(args[0]);
    final File outputFile = new File(args[1]);


    System.out.println("Loading '" + inputFile + "'...");
    final long startTime = System.currentTimeMillis();
    final Trie trie = new TrieMaker().makeTrie(inputFile, outputFile, true);
    final long endTime = System.currentTimeMillis();

    System.out.println("Dumped '" + outputFile + "'.");

    System.out.println("\n\tmaxDepth=" + trie.getMaxDepth() +
                       "\n\tnumWords=" + trie.getNumWords() +
                       "\n\tnumEncodedChars=" + trie.getNumEncodedChars());

    System.out.println("elapsed time: " + MathUtil.timeString(endTime - startTime, false));
  }
}
