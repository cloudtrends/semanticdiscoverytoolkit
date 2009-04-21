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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility to lookup terms in a dumped set.
 * <p>
 * @author Spence Koehler
 */
public class SetLookup {

  private Set<String> set;
  private Normalizer normalizer;

  public SetLookup(File setFile) throws IOException {
    this(setFile, GeneralNormalizer.getCaseInsensitiveInstance());
  }

  public SetLookup(File setFile, Normalizer normalizer) throws IOException {
    this.set = loadSet(setFile);
    this.normalizer = normalizer;
  }

  // load already normalized words.
  private final Set<String> loadSet(File file) throws IOException {
    final Set<String> result = new HashSet<String>();

    final BufferedReader reader = FileUtil.getReader(file);
    String line = null;
    while ((line = reader.readLine()) != null) {
      result.add(line);
    }
    reader.close();

    return result;
  }

  /**
   * @return true if the set contains all normalized words in the string of length 2 or more.
   */
  public boolean contains(String string) {
    return contains(normalizer.normalize(string));
  }

  /**
   * @return true if the set contains all words in the normalized string, ignoring strings of length less than 1.
   */
  public boolean contains(NormalizedString nstring) {
    return contains(nstring.split());
  }

  /**
   * @return true if the set contains all the words, ignoring strings of length less than 2.
   */
  public boolean contains(String[] words) {
    boolean result = true;

    for (String word : words) {
      if (word.length() > 1 && !set.contains(word)) {
        result = false;
        break;
      }
    }

    return result;
  }

  public static final void main(String[] args) throws IOException {
    //arg0: set data file
    //args1+: strings to test for containment in the set

    final SetLookup lookup = new SetLookup(new File(args[0]));

    for (int i = 1; i < args.length; ++i) {
      final String string = args[i];
      System.out.println(string + "|" + lookup.contains(string));
    }
  }
}
