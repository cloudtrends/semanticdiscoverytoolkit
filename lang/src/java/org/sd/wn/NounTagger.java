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
package org.sd.wn;


import java.io.File;
import java.io.IOException;

/**
 * Utility to identify words that could be a noun (ignoring context).
 * <p>
 * @author Spence Koehler
 */
public class NounTagger {
  
  final WordNetIndex nounIndex;

  public NounTagger(File dictDir) throws IOException {
    this.nounIndex = WordNetIndex.getInstance(dictDir, POS.NOUN);
  }

  /**
   * Determine whether the (unnormalized) word could be a noun.
   *
   * @param word          The word to test.
   */
  public boolean isNoun(String word) {
    return isNoun(word, false);
  }

  /**
   * Determine whether the word could be a noun.
   *
   * @param word          The word to test.
   * @param isNormalized  True if the word is already lowercase with underscores in place of spaces.
   */
  public boolean isNoun(String word, boolean isNormalized) {
    return nounIndex.lookup(isNormalized ? word : WordNetUtils.normalize(word)) != null;
  }


  //java -Xmx640m org.sd.wn.NounTagger /usr/local/share/download/wordnet/WordNet-3.0/dict "hydraulic" "pump" "hydraulic pump"
  public static final void main(String[] args) throws IOException {
    //arg0: wordnet dict dir (i.e. /usr/local/share/download/wordnet/WordNet-3.0/dict)
    //args1+: words to test for being nouns.

    try {
      final NounTagger nounTagger = new NounTagger(args[0].length() > 1 ? new File(args[0]) : null);
      for (int i = 1; i < args.length; ++i) {
        final boolean isNoun = nounTagger.isNoun(args[i]);
        System.out.println(args[i] + " -- " + (isNoun ? "" : "!") + "NOUN");
      }
    }
    finally {
      WordNetUtils.closeAll();
    }
  }
}
