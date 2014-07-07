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
import java.util.ArrayList;
import java.util.List;

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
  private List<InnerSegment> innerSegments;
  private String _text;
  private String _wordText;
  private WordCharacteristics _wc;
  private Integer _textStart;
  private Integer _textEnd;

  public SegmentPointer(String input, String label, int seqNum, int startPtr, int endPtr) {
    this.input = input;
    this.label = label;
    this.seqNum = seqNum;
    this.startPtr = startPtr;
    this.endPtr = endPtr;
    this.numWords = -1;
    this.innerSegments = null;
    this._text = null;
    this._wordText = null;
    this._wc = null;
    this._textStart = null;
    this._textEnd = null;
  }

  public String getInput() {
    return input;
  }

  public char getChar(int idx) {
    return input.charAt(idx);
  }

  public int getStartPtr() {
    return startPtr;
  }

  /**
   * Get the startPtr, incrementing over symbols if necessary.
   */
  public int getTextStart() {
    if (_textStart == null) {
      _textStart = startPtr + getWordCharacteristics().skip(WordCharacteristics.Type.OTHER, 0);
    }
    return _textStart;
  }

  public void setStartPtr(int startPtr) {
    this.startPtr = startPtr;
  }

  public int getEndPtr() {
    return endPtr;
  }

  /**
   * Determine whether there is input beyond this pointer's end.
   */
  public boolean hasNext() {
    return input.length() > endPtr;
  }

  /**
   * Get the endPtr, decrementing over symbols if necessary.
   */
  public int getTextEnd() {
    if (_textEnd == null) {
      final WordCharacteristics wc = getWordCharacteristics();
      if (wc.hasEndDelims()) {
        _textEnd = startPtr + wc.skipBack(WordCharacteristics.Type.OTHER) + 1;
      }
      else {
        _textEnd = endPtr;
      }
    }
    return _textEnd;
  }

  public void setEndPtr(int endPtr) {
    this.endPtr = endPtr;
  }

  public String getText() {
    if (_text == null) {
      _text = input.substring(startPtr, endPtr);
    }
    return _text;
  }
  public String getWordText() {
    if (_wordText == null) {
      final int textStart = getTextStart();
      final int textEnd = getTextEnd();
      _wordText = (textStart < textEnd) ? input.substring(textStart, textEnd) : "";
    }
    return _wordText;
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

  public boolean hasInnerSegments() {
    return innerSegments != null && innerSegments.size() > 0;
  }

  public List<InnerSegment> getInnerSegments() {
    return innerSegments;
  }

  public void addInnerSegment(int startPtr, int endPtr) {
    if (startPtr < endPtr && startPtr >= 0) {
      addInnerSegment(new InnerSegment(startPtr, endPtr));
    }
  }

  public void addInnerSegment(InnerSegment innerSegment) {
    if (innerSegments == null) innerSegments = new ArrayList<InnerSegment>();
    innerSegments.add(innerSegment);
  }

  public void setInnerSegments(List<InnerSegment> innerSegments) {
    this.innerSegments = innerSegments;
  }

  public String getText(InnerSegment innerSegment) {
    return input.substring(innerSegment.getStartPtr(), innerSegment.getEndPtr());
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append('(').append(seqNum).append(") ").
      append(label).append(": " ).append(getText());

    if (innerSegments != null) {
      for (InnerSegment innerSegment : innerSegments) {
        result.append("\n\t\t").append(getText(innerSegment));
      }
    }

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


  public static class InnerSegment {
    private int startPtr;
    private int endPtr;

    public InnerSegment(int startPtr, int endPtr) {
      this.startPtr = startPtr;
      this.endPtr = endPtr;
    }

    /** Get the inner segment's start pointer relative to the full input */
    public int getStartPtr() {
      return startPtr;
    }

    /** Set the inner segment's start pointer relative to the full input */
    public void setStartPtr(int startPtr) {
      this.startPtr = startPtr;
    }

    /** Get the inner segment's end pointer relative to the full input */
    public int getEndPtr() {
      return endPtr;
    }

    /** Set the inner segment's end pointer relative to the full input */
    public void setEndPtr(int endPtr) {
      this.endPtr = endPtr;
    }
  }
}
