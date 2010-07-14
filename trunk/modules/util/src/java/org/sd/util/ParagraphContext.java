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


import java.util.ArrayList;
import java.util.List;

/**
 * Input context wrapper around multiple lines of text forming a paragraph,
 * optionally belonging to a file.
 * <p>
 * @author Spence Koehler
 */
public class ParagraphContext implements InputContext {
  
  private StringBuilder fulltext;
  private int textLength;
  private WhitespacePolicy whitespacePolicy;
  private FileContext fileContext;
  private List<LineContext> lines;
  private int fileStartPos;
  private int paragraphNum;

  public ParagraphContext(WhitespacePolicy whitespacePolicy, int fileStartPos) {
    this.fulltext = null;
    this.textLength = 0;
    this.whitespacePolicy = whitespacePolicy;
    this.fileContext = null;
    this.lines = new ArrayList<LineContext>();
    this.fileStartPos = fileStartPos;
  }

  public void add(LineContext lineContext) {
    if (textLength > 0) ++textLength;  // account for space between lines
    lineContext.setParagraphContext(this, this.lines.size(), this.textLength);
    this.lines.add(lineContext);
    this.textLength += lineContext.getText().length();
  }

  /**
   * Get this paragraph's start position within its file's full text.
   */
  public int getFileStartPos() {
    return fileStartPos;
  }

  /**
   * Get this paragraph's full text.
   */
  public String getText() {
    if (fulltext == null) {
      //NOTE: this impl assumes no blank lines are in a paragraph
      final char delim = (whitespacePolicy == WhitespacePolicy.PRESERVE) ? '\n' : ' ';
      this.fulltext = new StringBuilder();
      for (LineContext lineContext : lines) {
        if (fulltext.length() > 0) fulltext.append(delim);
        fulltext.append(lineContext.getText());
      }
    }
    return fulltext.toString();
  }

  /**
   * Get the length of this paragraph's full text.
   */
  public int getTextLength() {
    return this.textLength;
  }

  /**
   * Get an ID that identifies this InputContext within its container.
   */
  public int getId() {
    return paragraphNum;
  }

  /**
   * Get the character startPosition of the other context's text within
   * this context or return false if the other context is not contained
   * within this context.
   *
   * @param other  The other input context
   * @param startPosition a single element array holding the return value
   *        of the start position -- only set when returning 'true'.
   *
   * @result true and startPosition[0] holds the value or false.
   */
  public boolean getPosition(InputContext other, int[] startPosition) {
    boolean result = false;

    if (other == this) {
      result = true;
      startPosition[0] = 0;
    }
    else if (other instanceof LineContext) {
      final LineContext lineContext = (LineContext)other;
      if (lineContext.getParagraphContext() == this) {
        startPosition[0] = lineContext.getParagraphStartPos();
        result = true;
      }
    }

    return result;
  }

  /**
   * Get the fileContext containing this paragraph, or null.
   */
  public FileContext getFileContext() {
    return fileContext;
  }

  /**
   * Get this context's ultimate container or itself if it is not contained
   * by anything.
   */
  public InputContext getContextRoot() {
    return fileContext;
  }

  void setFileContext(FileContext fileContext, int paragraphNum) {
    this.fileContext = fileContext;
    this.paragraphNum = paragraphNum;
  }

  public int getNumLines() {
    return lines.size();
  }

  public LineContext getLine(int index) {
    return lines.get(index);
  }

  public LineContext[] getLineEnds(int startPos, int endPos, int[] lineStartPos, int[] lineEndPos) {
    return fileContext.getLineEnds(fileStartPos + startPos, fileStartPos + endPos, lineStartPos, lineEndPos);
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    final int numChars = Math.min(getTextLength(), 40);
    result.append('p').append(paragraphNum).append(':').append(getText().substring(0, numChars));
    if (numChars < getTextLength()) result.append("...");

    return result.toString();
  }
}
