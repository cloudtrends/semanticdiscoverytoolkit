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
package org.sd.text;


import org.sd.nlp.SentenceSplitter;
import org.sd.util.MappedString;

/**
 * A sentence segmenter over a mapped string.
 * <p>
 * This is used to run sentence segmentation algorithm over the mapped version
 * of a string while returning values as if from the original string.
 *
 * @author Spence Koehler
 */
public class MappedSentenceSegmenter extends SentenceSegmenter {
  
  private MappedString mappedString;

  /**
   * Construct with the given mapped string.
   * <p>
   * Underlying sentence segmentation will proceed over the "mapped" string
   * while results will be returned from the "original" string.
   */
  public MappedSentenceSegmenter(MappedString mappedString) {
    super(mappedString.getMappedString());
    this.mappedString = mappedString;
  }

  /**
   * Construct with the given splitter and mapped string.
   */
  protected MappedSentenceSegmenter(SentenceSplitter splitter, MappedString mappedString) {
    super(splitter, mappedString.getMappedString());
    this.mappedString = mappedString;
  }

  /**
   * Get this instance's mapped string.
   */
  public MappedString getMappedString() {
    return mappedString;
  }


  /**
   * Get the full (original) text being iterated over.
   */
  public String getText() {
    return mappedString.getOriginalString();
  }

  /**
   * Set the text to be iterated over, resetting iteration to the
   * beginning of the text.
   */
  public final void setText(String text) {
    throw new IllegalStateException("Can't setText(String) in a MappedSentenceSegmenter! Use setText(MappedString) instead!");
  }

  /**
   * Set the text to be iterated over, resetting iteration to the
   * beginning of the text.
   */
  public void setText(MappedString mappedString) {
    super.setText(mappedString.getMappedString());
    this.mappedString = mappedString;
  }

  /**
   * Get the starting character index (inclusive) in the input text of the
   * last string returned by 'next'.
   */
  public int getStartIndex() {
    return mappedString.convertIndex(super.getStartIndex());
  }

  /**
   * Get the ending character index (exclusive) in the input text of the
   * last string returned by 'next'.
   */
  public int getEndIndex() {
    return mappedString.convertIndex(super.getEndIndex());
  }

  /**
   * Get the next sentence.
   */
  public String next() {
    String result = null;

    final String mappedResult = super.next();
    if (mappedResult != null) {
      result = mappedString.originalSubstring(super.getStartIndex(), super.getEndIndex());
    }

    return result;
  }
}
