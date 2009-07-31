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

/**
 * A TextSegmenter that uses an nlp.SentenceSplitter.
 * <p>
 * @author Spence Koehler
 */
public class SentenceSegmenter implements TextSegmenter {
  
  private SentenceSplitter splitter;

  private SentenceSplitter.SplitInfo[] sentences;
  private int index;

  private String text;
  private int lastStart;
  private int lastEnd;

  public SentenceSegmenter(String text) {
    this(new SentenceSplitter(), text);
  }

  protected SentenceSegmenter(SentenceSplitter splitter, String text) {
    this.splitter = splitter;
    this.doSetText(text);
  }

  /**
   * Get the full text being iterated over.
   */
  public String getText() {
    return text;
  }

  /**
   * Set the text to be iterated over, resetting iteration to the
   * beginning of the text.
   */
  private final void doSetText(String text) {
    this.sentences = splitter.splitInfo(text);
    this.index = 0;

    this.text = text;
    this.lastStart = 0;
    this.lastEnd = 0;
  }

  /**
   * Set the text to be iterated over, resetting iteration to the
   * beginning of the text.
   */
  public void setText(String text) {
    doSetText(text);
  }

  /**
   * Get the starting character index (inclusive) in the input text of the
   * last string returned by 'next'.
   */
  public int getStartIndex() {
    return lastStart;
  }

  /**
   * Get the ending character index (exclusive) in the input text of the
   * last string returned by 'next'.
   */
  public int getEndIndex() {
    return lastEnd;
  }

  /**
   * Determine whether there is a next sentence.
   */
  public boolean hasNext() {
    return index < sentences.length;
  }

  /**
   * Get the next sentence.
   */
  public String next() {
    String result = null;

    if (hasNext()) {
      final SentenceSplitter.SplitInfo sentence = sentences[index];
      result = sentence.sentence;
      lastStart = sentence.startIndex;
      lastEnd = sentence.endIndex;
      ++index;
    }

    return result;
  }

  /**
   * Remove the last text returned by 'next'.
   * <p>
   * Not implemented!
   *
   * @throws UnsupportedOperationException
   */
  public void remove() {
    throw new UnsupportedOperationException("Not implemented!");
  }

  /**
   * Determine whether the last segment returned by 'next' should be flushed.
   * <p>
   * When segments are not flushed, repeated word N-Grams only count as
   * a single instance for purposes of tallying frequencies.
   * <p>
   * This implementation always returns true.
   */
  public boolean shouldFlush() {
    return true;
  }
}
