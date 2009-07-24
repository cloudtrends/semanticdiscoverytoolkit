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


import org.sd.io.FileUtil;

import java.io.BufferedReader;
import java.io.File;

/**
 * Container class for managing word net's nouns as a bag of words.
 * <p>
 * @author Spence Koehler
 */
public class NounBag {

  private WordNetIndex nounIndex;

  public NounBag() {
    this.nounIndex = WordNetIndex.getInstance((File)null, POS.NOUN);
  }

  /**
   * Determine whether the word could be a noun.
   *
   * @param word          The word to test.
   */
  public boolean isNoun(String word) {
    boolean result = false;

    //todo: use onomasticon and/or capitalization to recognize proper nouns?

    for (WordForm wordForm = new WordForm(null, word); !result && wordForm != null; wordForm = wordForm.getNext()) {
      if (!wordForm.canBe(POS.NOUN)) continue;
      result = (nounIndex.lookup(wordForm.word)) != null;
    }

    return result;
  }
}
