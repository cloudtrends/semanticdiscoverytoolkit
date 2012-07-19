/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.token;


import java.io.Serializable;

/**
 * Container for a pointer into a segment.
 * <p>
 * @author Spence Koehler
 */
public class SegmentPointer implements Serializable {

  private String input;
  private String label;
  private int seqNum;
  private int startPtr;
  private int endPtr;
  private int numWords;
  private String _text;
  private WordCharacteristics _wc;

  public SegmentPointer(String input, String label, int seqNum, int startPtr, int endPtr) {
    this.input = input;
    this.label = label;
    this.seqNum = seqNum;
    this.startPtr = startPtr;
    this.endPtr = endPtr;
    this.numWords = -1;
    this._text = null;
    this._wc = null;
  }

  public String getInput() {
    return input;
  }

  public int getStartPtr() {
    return startPtr;
  }

  public int getEndPtr() {
    return endPtr;
  }

  public String getText() {
    if (_text == null) {
      _text = input.substring(startPtr, endPtr);
    }
    return _text;
  }

  public WordCharacteristics getWordCharacteristics() {
    if (_wc == null) {
      _wc = new WordCharacteristics(getText());
    }
    return _wc;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public int getSeqNum() {
    return seqNum;
  }

  public void setSeqNum(int seqNum) {
    this.seqNum = seqNum;
  }

  public int getNumWords() {
    if (numWords < 0) {
      numWords = computeNumWords();
    }
    return numWords;
  }

  public void setNumWords(int numWords) {
    this.numWords = numWords;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append('(').append(seqNum).append(") ").
      append(label).append(": " ).append(getText());

    return result.toString();
  }

  private final int computeNumWords() {
    int result = 1;

    boolean lastWasWhite = true;
    for (int i = startPtr; i < endPtr; ++i) {
      final char c = input.charAt(i);
      if (Character.isWhitespace(c)) {
        if (!lastWasWhite) {  // ignore consecutive whitespace
          ++result;
        }
        lastWasWhite = true;
      }
      else {
        lastWasWhite = false;
      }
    }

    return result;
  }
}
