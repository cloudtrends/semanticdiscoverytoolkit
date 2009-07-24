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


/**
 * Structure to uniquely identify a word.
 * <p>
 * @author Spence Koehler
 */
public class WordId {

  public final POS partOfSpeech;   // distinguishes words by part of speech
  public final long synsetOffset;  // offset of word's synset in its part of speech file.
  public final int wordNum;        // number of word in synset (1-based; 0 means all).

  public WordId(POS partOfSpeech, long synsetOffset, int wordNum) {
    this.partOfSpeech = partOfSpeech;
    this.synsetOffset = synsetOffset;
    this.wordNum = wordNum;
  }

  public int hashCode() {
    int result = 7;

    result = result * 31 + partOfSpeech.ordinal();
    result = result * 31 + (int)(synsetOffset^(synsetOffset>>>32));
    result = result * 31 + wordNum;

    return result;
  }

  public boolean equals(Object o) {
    boolean result = (this == o);

    if (!result && o instanceof WordId) {
      final WordId other = (WordId)o;
      result =
        (this.partOfSpeech == other.partOfSpeech) &&
        (this.synsetOffset == other.synsetOffset) &&
        (this.wordNum == other.wordNum);
    }

    return result;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append('[').
      append(partOfSpeech.name().toLowerCase()).append(',').
      append(synsetOffset).append(',').
      append(wordNum).
      append(']');

    return result.toString();
  }
}
