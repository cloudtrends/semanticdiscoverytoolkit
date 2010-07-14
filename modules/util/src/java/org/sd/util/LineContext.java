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


/**
 * Input context wrapper around a line of text, optionally belonging to a
 * paragraph and/or a file.
 * <p>
 * @author Spence Koehler
 */
public class LineContext implements InputContext {
  
  private String text;
  private ParagraphContext paragraphContext;
  private int paragraphLineNum;
  private int paragraphStartPos;
  private FileContext fileContext;
  private int fileLineNum;
  private int fileStartPos;

  public LineContext(String text) {
    this.text = text;
  }

  /**
   * Get this context's text.
   */
  public String getText() {
    return text;
  }

  /**
   * Determine whether this context's text is empty. For the purposes of this
   * test, text that only only contains whitespace or has no characters is empty.
   */
  public boolean isEmpty() {
    boolean result = ("".equals(text));

    if (!result) {
      result = "".equals(text.trim());
    }

    return result;
  }

  /**
   * Get an ID that identifies this InputContext within its container.
   */
  public int getId() {
    return fileLineNum;
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
      startPosition[0] = 0;
      result = true;
    }

    return result;
  }

  /**
   * Get the paragraphContext containing this line, or null.
   */
  public ParagraphContext getParagraphContext() {
    return paragraphContext;
  }

  /**
   * Get this line's number with respect to its containing paragraph.
   */
  public int getParagraphLineNum() {
    return paragraphLineNum;
  }

  /**
   * Get this line's start position within its paragraph's full text.
   */
  public int getParagraphStartPos() {
    return paragraphStartPos;
  }

  void setParagraphContext(ParagraphContext paragraphContext, int paragraphLineNum, int paragraphStartPos) {
    this.paragraphContext = paragraphContext;
    this.paragraphLineNum = paragraphLineNum;
    this.paragraphStartPos = paragraphStartPos;
  }

  /**
   * Get the fileContext containing this line, or null.
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

  /**
   * Get this line's number with respect to its containing file.
   */
  public int getFileLineNum() {
    return fileLineNum;
  }

  /**
   * Get this line's start position within its file's full text.
   */
  public int getFileStartPos() {
    return fileStartPos;
  }

  void setFileContext(FileContext fileContext, int fileLineNum, int fileStartPos) {
    this.fileContext = fileContext;
    this.fileLineNum = fileLineNum;
    this.fileStartPos = fileStartPos;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    final int numChars = Math.min(text.length(), 40);
    result.append('l').append(fileLineNum).append(':').append(text.substring(0, numChars));
    if (numChars < text.length()) result.append("...");

    return result.toString();
  }
}
