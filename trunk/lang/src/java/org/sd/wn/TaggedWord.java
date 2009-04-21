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


import java.util.Set;

/**
 * Structure to hold a word with its possible parts of speech.
 * <p>
 * @author Spence Koehler
 */
public class TaggedWord {
  
  public final String word;
  public final POS[] partsOfSpeech;

  public TaggedWord(String word, POS[] partsOfSpeech) {
    this.word = word;
    this.partsOfSpeech = WordNetUtils.trim(partsOfSpeech);
  }

  public TaggedWord(String word, Set<POS> partsOfSpeech) {
    this.word = word;
    this.partsOfSpeech = new POS[partsOfSpeech.size()];

    int index = 0;
    for (POS pos : partsOfSpeech) {
      this.partsOfSpeech[index++] = pos;
    }
  }
}
