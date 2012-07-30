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
package org.sd.util;


import java.text.BreakIterator;
import java.util.Iterator;

/**
 * A naive iterator over portions of a string based on java.text.BreakIterator.
 * <p>
 * @author Spence Koehler
 */
public class BaseTextIterator implements TextIterator {
  
  private BreakIterator breakIterator;
  private boolean canSkip;
  private String text;
  private int start;
  private int end;
  private String next;

  private int lastStart;
  private int lastEnd;

  private boolean initialized;

  /**
   * Construct with the string whose text is to be iterated over.
   */
  public BaseTextIterator(BreakIterator breakIterator, boolean canSkip) {
    this.breakIterator = breakIterator;
    this.canSkip = canSkip;
    this.initialized = false;
  }

  /**
   * Set the text to be iterated over, resetting iteration to the
   * beginning of the text.
   */
  public final void setText(String text) {
    this.text = text;
    this.start = 0;
    this.end = 0;
    this.initialized = false;
  }

  private final void init() {
    if (!initialized) {
      if (text != null && !"".equals(text)) {
        breakIterator.setText(text);
        computeNext(true);
      }
      else {
        this.next = null;
        this.end = BreakIterator.DONE;
      }
      initialized = true;
    }
  }

  /**
   * Get the full text being iterated over.
   */
  public final String getText() {
    return text;
  }

  /**
   * Get the starting character index (inclusive) in the input text of the
   * last string returned by 'next'.
   */
  public final int getStartIndex() {
    return lastStart;
  }

  /**
   * Get the ending character index (exclusive) in the input text of the
   * last string returned by 'next'.
   */
  public final int getEndIndex() {
    return lastEnd;
  }

  /**
   * Get the next text.
   */
  public String next() {
    init();

    final String result = next;
    lastStart = start;
    lastEnd = end;

    computeNext(false);
    return result;
  }

  /**
   * Determine whether there is a next text.
   */
  public boolean hasNext() {
    init();
    return text != null && !"".equals(text) && next != null;
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
   * Determine whether the the substring should be accepted as text to return.
   * <p>
   * This default implementation accepts text if there is a letter or
   * digit codepoint within the span.
   */
  protected boolean accept(String text, int start, int end) {
    boolean result = false;

    for (int i = start; i < end; ++i) {
      if (Character.isLetterOrDigit(text.codePointAt(i))) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Get the next text.
   */
  private void computeNext(boolean isFirst) {
    String result = null;

    start = isFirst ? breakIterator.first() : end;
    final int theStart = start;
    boolean acceptedOne = false;

    while (result == null && (isFirst || end != BreakIterator.DONE)) {

      if (canSkip && !isFirst) start = end;
      end = breakIterator.next();

      if (end != BreakIterator.DONE) {
        if (accept(text, start, end)) {
          acceptedOne = true;
          result = text.substring(start, end).trim();
        }
      }
      else {
        break;
      }

      if (canSkip) isFirst = false;
    }

    if (result == null && !acceptedOne && !canSkip && theStart != BreakIterator.DONE && theStart < text.length()) {
      result = text.substring(theStart, text.length()).trim();
      if (!"".equals(result)) {
        start = theStart;
        end = text.length();
      }
      else result = null;
    }

    this.next = result;
  }
}
