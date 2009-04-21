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
package org.sd.match;


import org.sd.util.BitUtil;

import java.io.ByteArrayOutputStream;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * Class to serialize a match concept as an osob.
 * <p>
 * @author Spence Koehler
 */
public class OsobSerializer extends AbstractConceptSerializer {

  private static final InputOrderComparator inputOrderComparator = new InputOrderComparator();
  private static final OsobBitDefinition bitDefinition = OsobBitDefinition.getInstance();

  public static final InputOrderComparator getInputOrderComparator() {
    return inputOrderComparator;
  }

  private ByteArrayOutputStream byteStream;
  private BitDefinition.Marker marker;
  private TreeSet<NumberedWord> words;  // current variant's words
  private int wordNum;

  private Osob osob;

  public OsobSerializer(boolean sortWords) {
    this.byteStream = new ByteArrayOutputStream();
    this.marker = null;
    this.wordNum = 0;
    this.osob = null;

    if (sortWords) {
      this.words = new TreeSet<NumberedWord>();
    }
    else {
      this.words = new TreeSet<NumberedWord>(inputOrderComparator);
    }
  }

  /**
   * Get the serialized data as a string.
   */
  public String asString() {
    return getOsob().encode();
  }

  public Osob getOsob() {
    if (osob == null) {
      flushWords();
      osob = new Osob(byteStream.toByteArray());
    }
    return osob;
  }

  /**
   * Set the id for the match concept.
   */
  public void setConceptId(int conceptId) {
    // create header bytes (4) w/conceptId.
    final byte[] bytes = BitUtil.getBytes(conceptId);
    for (byte b : bytes) byteStream.write(b);
  }

  /**
   * @return true if there were words to flush.
   */
  protected final boolean flushWords() {
    final int numWords = words.size();
    if (numWords == 0) return false;

    // write out marker and words
    for (NumberedWord numberedWord : words) {
      marker.startNext(bitDefinition.WORD, numberedWord.word.wordType.ordinal());

      // write out marker
      final byte[] markerBytes = marker.getMarkerBytes();
      for (byte markerByte : markerBytes) byteStream.write(markerByte);

      // write out wordNum (8bits) and nChars (8bits) followed by wordChars
      byteStream.write(numberedWord.number);
      final char[] chars = numberedWord.word.word.toCharArray();
      byteStream.write(chars.length);
      for (char c : chars) byteStream.write(c);
    }

    // clear words for next variant.
    words.clear();
    wordNum = 0;

    return true;
  }

  protected void startNextForm(boolean flushedWords, Form.Type formType) {
    marker = bitDefinition.startMarker(formType.ordinal());
  }

  protected void startNextTerm(boolean flushedWords, Decomp.Type decompType) {
    marker.startNext(bitDefinition.TERM, decompType.ordinal());
  }

  protected void startNextSynonym(boolean flushedWords, Synonym.Type synonymType) {
    marker.startNext(bitDefinition.SYNONYM, synonymType.ordinal());
  }

  protected void startNextVariant(boolean flushedWords, Variant.Type variantType) {
    marker.startNext(bitDefinition.VARIANT, variantType.ordinal());
  }

  public void addTypedWord(TypedWord typedWord) {
    if (wordNum >= 255) {
      throw new IllegalStateException("Too many words! Must be below 256.");
    }
    if (typedWord.word.length() > 255) {
      throw new IllegalArgumentException("Word too large! (" + typedWord.word.length() + "). Must be below 256.");
    }

    words.add(new NumberedWord(wordNum++, typedWord));
  }


  /**
   * A comparator to preserve input order.
   */
  public static final class InputOrderComparator implements Comparator<NumberedWord> {
    private InputOrderComparator() {
    }

    public int compare(NumberedWord nw1, NumberedWord nw2) {
      return nw1.number - nw2.number;
    }
    public boolean equals(Object o) {
      return (this == o) || (o instanceof InputOrderComparator);
    }
  }
}
