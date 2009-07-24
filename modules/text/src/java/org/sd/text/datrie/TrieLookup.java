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


import org.sd.nlp.GeneralNormalizer;
import org.sd.nlp.NormalizedString;
import org.sd.nlp.Normalizer;
import org.sd.text.Trie;

import java.io.File;
import java.io.IOException;

/**
 * Utility to lookup terms in a dumped trie.
 * <p>
 * @author Spence Koehler
 */
public class TrieLookup {

  private Trie trie;
  private Normalizer normalizer;

  public TrieLookup(File datFile) throws IOException {
    this(datFile, GeneralNormalizer.getCaseInsensitiveInstance());
  }

  public TrieLookup(File datFile, Normalizer normalizer) throws IOException {
    this.trie = DoubleArrayTrie.loadTrie(datFile);
    this.normalizer = normalizer;
  }

  /**
   * @return true if the trie contains all normalized words in the string of length 2 or more.
   */
  public boolean contains(String string) {
    return contains(normalizer.normalize(string));
  }

  /**
   * @return true if the trie contains all words in the normalized string, ignoring strings of length less than 1.
   */
  public boolean contains(NormalizedString nstring) {
    return contains(nstring.split());
  }

  /**
   * @return true if the trie contains all the words, ignoring strings of length less than 2.
   */
  public boolean contains(String[] words) {
    boolean result = true;

    for (String word : words) {
      if (word.length() > 1 && !trie.contains(word)) {
        result = false;
        break;
      }
    }

    return result;
  }

  public static final void main(String[] args) throws IOException {
    //arg0: trie data file
    //args1+: strings to test for containment in the trie

    final TrieLookup lookup = new TrieLookup(new File(args[0]));

    for (int i = 1; i < args.length; ++i) {
      final String string = args[i];
      System.out.println(string + "|" + lookup.contains(string));
    }
  }
}
