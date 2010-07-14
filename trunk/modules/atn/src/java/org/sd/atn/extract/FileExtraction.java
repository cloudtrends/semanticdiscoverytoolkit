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
package org.sd.atn.extract;


import org.sd.util.FileContext;
import org.sd.util.ParagraphContext;
import org.sd.util.LineContext;

/**
 * An extraction based on input from a file.
 * <p>
 * @author Spence Koehler
 */
public class FileExtraction extends Extraction {
  
  private FileContext fileContext;
  private ParagraphContext paragraphContext;
  private LineContext lineContext;

  private LineContext startLineContext;
  private LineContext endLineContext;
  private int startLinePos;
  private int endLinePos;
  private int fileStartPos;
  private int fileEndPos;

  public FileExtraction(String type, FileContext fileContext, int startPos, int endPos) {
    super(type, fileContext.getText().substring(startPos, endPos));

    this.fileContext = fileContext;
    this.paragraphContext = null;
    this.lineContext = null;

    init(startPos, endPos);

    if (startLineContext.getParagraphContext() == endLineContext.getParagraphContext()) {
      this.paragraphContext = startLineContext.getParagraphContext();
    }

    if (startLineContext == endLineContext) {
      this.lineContext = startLineContext;
    }
  }

  public FileExtraction(String type, ParagraphContext paragraphContext, int startPos, int endPos) {
    super(type, paragraphContext.getText().substring(startPos, endPos));

    this.fileContext = paragraphContext.getFileContext();
    this.paragraphContext = paragraphContext;
    this.lineContext = null;

    final int offset = paragraphContext.getFileStartPos();
    init(offset + startPos, offset + endPos);

    if (startLineContext == endLineContext) {
      this.lineContext = startLineContext;
    }
  }

  public FileExtraction(String type, LineContext lineContext, int startPos, int endPos) {
    super(type, lineContext.getText().substring(startPos, endPos));

    this.fileContext = lineContext.getFileContext();
    this.paragraphContext = lineContext.getParagraphContext();
    this.lineContext = lineContext;

    final int offset = lineContext.getFileStartPos();
    init(offset + startPos, offset + endPos);
  }

  private final void init(int fileStartPos, int fileEndPos) {
    this.fileStartPos = fileStartPos;
    this.fileEndPos = fileEndPos;

    final int[] lineStartPos = new int[]{0};
    final int[] lineEndPos = new int[]{0};

    final LineContext[] lineContexts = fileContext.getLineEnds(fileStartPos, fileEndPos, lineStartPos, lineEndPos);

    this.startLineContext = lineContexts[0];
    this.endLineContext = lineContexts[1];
    this.startLinePos = lineStartPos[0];
    this.endLinePos = lineEndPos[0];
  }


  public FileContext getFileContext() {
    return fileContext;
  }

  public ParagraphContext getParagraphContext() {
    return paragraphContext;
  }

  public LineContext getLineContext() {
    return lineContext;
  }

  public LineContext getStartLineContext() {
    return startLineContext;
  }

  public LineContext getEndLineContext() {
    return endLineContext;
  }

  public int getStartLinePos() {
    return startLinePos;
  }

  public int getEndLinePos() {
    return endLinePos;
  }

  public int getFileStartPos() {
    return fileStartPos;
  }

  public int getFileEndPos() {
    return fileEndPos;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append("f[l").
      append(startLineContext.getFileLineNum());

    if (endLineContext != startLineContext) {
      result.
        append('-').
        append(endLineContext.getFileLineNum());
    }

    result.
      append("][").
      append(fileStartPos).
      append('-').
      append(fileEndPos).
      append("]=").
      append(getText());

    return result.toString();
  }
}
