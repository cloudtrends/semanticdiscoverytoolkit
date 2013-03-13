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


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sd.io.FileUtil;

/**
 * An input context for a file.
 * <p>
 * @author Spence Koehler
 */
public class FileContext implements InputContext {
  
  private StringBuilder fulltext;
  private int textLength;
  private WhitespacePolicy whitespacePolicy;
  private List<ParagraphContext> paragraphContexts;
  private Map<Integer, LineContext> lineNum2Context;
  private Map<Integer, LineContext> startPos2Line;

  public FileContext(File file, WhitespacePolicy whitespacePolicy) throws IOException {
    final BufferedReader reader = FileUtil.getReader(file);
    init(reader, whitespacePolicy);
    reader.close();
  }

  public FileContext(InputStream inputStream, WhitespacePolicy whitespacePolicy) throws IOException {
    final BufferedReader reader = FileUtil.getReader(inputStream);
    init(reader, whitespacePolicy);
  }

  public FileContext(String fileText, WhitespacePolicy whitespacePolicy) {
    init(fileText.split("\n"), whitespacePolicy);
  }

  public FileContext(String[] fileLines, WhitespacePolicy whitespacePolicy) {
    init(fileLines, whitespacePolicy);
  }

  private final void init(BufferedReader reader, WhitespacePolicy whitespacePolicy) throws IOException {
    init(whitespacePolicy);

    final int[] fileLineNum = new int[]{0};
    final ParagraphContext[] curParagraphContext = new ParagraphContext[]{null};
    String line = null;
    while ((line = reader.readLine()) != null) {
      add(line, fileLineNum, curParagraphContext);
    }
  }

  private final void init(String[] fileLines, WhitespacePolicy whitespacePolicy) {
    init(whitespacePolicy);

    final int[] fileLineNum = new int[]{0};
    final ParagraphContext[] curParagraphContext = new ParagraphContext[]{null};
    for (String line : fileLines) {
      add(line, fileLineNum, curParagraphContext);
    }
  }

  private final void init(WhitespacePolicy whitespacePolicy) {
    this.fulltext = null;
    this.textLength = 0;
    this.whitespacePolicy = whitespacePolicy;
    this.paragraphContexts = new ArrayList<ParagraphContext>();
    this.lineNum2Context = new HashMap<Integer, LineContext>();
    this.startPos2Line = new HashMap<Integer, LineContext>();
  }

  private final void add(String line, int[] fileLineNum, ParagraphContext[] curParagraphContext) {
    final String text = fixLine(line, whitespacePolicy);
    final LineContext lineContext = new LineContext(text);
    lineContext.setFileContext(this, fileLineNum[0], this.textLength);

    // keep empty lines, but don't add to paragraphs
    lineNum2Context.put(fileLineNum[0]++, lineContext);

    if (lineContext.isEmpty()) {
      // use empty lines as marker between paragraphs
      curParagraphContext[0] = null;

      if (textLength > 0 || this.whitespacePolicy == WhitespacePolicy.PRESERVE) ++this.textLength;
    }
    else {
      if (this.textLength > 0) ++this.textLength;  // account for space between lines

      startPos2Line.put(this.textLength, lineContext);

      if (curParagraphContext[0] == null) {
        curParagraphContext[0] = new ParagraphContext(whitespacePolicy, this.textLength);
        curParagraphContext[0].setFileContext(this, paragraphContexts.size());
        paragraphContexts.add(curParagraphContext[0]);
      }
      curParagraphContext[0].add(lineContext);

      this.textLength += text.length();
    }
  }


  /**
   * Fix the line according to the whitespace policy.
   * <p>
   * Note that extenders may override this method.
   */
  protected String fixLine(String line, WhitespacePolicy whitespacePolicy) {
    String result = line;

    switch (whitespacePolicy) {
      case TRIM :
        result = line.trim();
        break;

      case HYPERTRIM :
        result = StringSplitter.hypertrim(line);
        break;
    }

    return result;
  }

  /**
   * Get this file's full text, where each actual line is concatenated with the
   * next non-empty line using a single space.
   */
  public String getText() {
    if (fulltext == null) {
      final char delim = (whitespacePolicy == WhitespacePolicy.PRESERVE) ? '\n' : ' ';
      boolean needsDelim = false;
      this.fulltext = new StringBuilder();
      for (LineContext lineContext : lineNum2Context.values()) {
        final String text = lineContext.getText();
        if (text.length() > 0 || whitespacePolicy == WhitespacePolicy.PRESERVE) {
          if (needsDelim) fulltext.append(delim);
          fulltext.append(text);
          needsDelim = true;
        }
      }
    }
    return fulltext.toString();
  }

  /**
   * Get the length of this file's full text.
   */
  public int getTextLength() {
    return this.textLength;
  }

  /**
   * Get this instance's whitespace policy.
   */
  public WhitespacePolicy getWhitespacePolicy() {
    return this.whitespacePolicy;
  }

  /**
   * Get an ID that identifies this InputContext within its container.
   */
  public int getId() {
    return 0;
  }

  /**
   * Get this context's ultimate container or itself if it is not contained
   * by anything.
   */
  public InputContext getContextRoot() {
    return this;
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
    else if (other instanceof ParagraphContext) {
      final ParagraphContext paragraphContext = (ParagraphContext)other;
      if (paragraphContext.getFileContext() == this) {
        startPosition[0] = paragraphContext.getFileStartPos();
        result = true;
      }
    }
    else if (other instanceof LineContext) {
      final LineContext lineContext = (LineContext)other;
      if (lineContext.getFileContext() == this) {
        startPosition[0] = lineContext.getFileStartPos();
        result = true;
      }
    }

    return result;
  }


  public int getNumParagraphs() {
    return paragraphContexts.size();
  }

  public ParagraphContext getParagraph(int index) {
    return paragraphContexts.get(index);
  }

  public int getNumLines() {
    return lineNum2Context.size();
  }

  public LineContext getLine(int index) {
    return lineNum2Context.get(index);
  }


  public InputContextIterator getParagraphIterator() {
    return new ParagraphIterator(this);
  }

  public InputContextIterator getLineIterator() {
    return new LineIterator(this);
  }

  /**
   * Get the start and end line contexts that hold startPos and endPos.
   */
  public LineContext[] getLineEnds(int startPos, int endPos, int[] lineStartPos, int[] lineEndPos) {
    final LineContext startLine = getLineContaining(startPos);
    final LineContext endLine = getLineContaining(endPos);

    if (startLine == null || endLine == null) return null;

    lineStartPos[0] = startPos - startLine.getFileStartPos();
    lineEndPos[0] = endPos - endLine.getFileStartPos();

    return new LineContext[]{startLine, endLine};
  }

  private final LineContext getLineContaining(int fileCharPos) {
    LineContext result = null;

    for (int lPos = fileCharPos; lPos >= 0; --lPos) {
      if (startPos2Line.containsKey(lPos)) {
        result = startPos2Line.get(lPos);
        break;
      }
    }

    return result;
  }

  /**
   * Get the start and end paragraph contexts that hold startPos and endPos.
   */
  public ParagraphContext[] getParagraphEnds(int startPos, int endPos, int[] paragraphStartPos, int[] paragraphEndPos) {
    final LineContext startLine = getLineContaining(startPos);
    final LineContext endLine = getLineContaining(endPos);

    if (startLine == null || endLine == null) return null;

    final ParagraphContext startParagraph = startLine.getParagraphContext();
    final ParagraphContext endParagraph = endLine.getParagraphContext();

    if (startParagraph == null || endParagraph == null) return null;

    paragraphStartPos[0] = startPos - startParagraph.getFileStartPos();
    paragraphEndPos[0] = endPos - endParagraph.getFileStartPos();

    return new ParagraphContext[]{startParagraph, endParagraph};
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    final int numChars = Math.min(getTextLength(), 40);
    result.append('f').append(':').append(getText().substring(0, numChars));
    if (numChars < getTextLength()) result.append("...");

    return result.toString();
  }


  private static abstract class FileContextIterator extends SimpleInputContextIterator {

    protected final FileContext fileContext;

    protected FileContextIterator(FileContext fileContext, int numItems) {
      super(numItems);
      this.fileContext = fileContext;
    }
  }

  private static final class ParagraphIterator extends FileContextIterator {
    public ParagraphIterator(FileContext fileContext) {
      super(fileContext, fileContext.getNumParagraphs());
    }

    protected final InputContext getItem(int itemNum) {
      return fileContext.getParagraph(itemNum);
    }

    public InputContextIterator broaden() {
      return new LineIterator(fileContext);
    }
  }


  private static final class LineIterator extends FileContextIterator {
    public LineIterator(FileContext fileContext) {
      super(fileContext, fileContext.getNumLines());
    }

    protected final InputContext getItem(int itemNum) {
      return fileContext.getLine(itemNum);
    }

    public InputContextIterator broaden() {
      return new SingleIterator(fileContext);
    }
  }

  private static final class SingleIterator extends FileContextIterator {
    public SingleIterator(FileContext fileContext) {
      super(fileContext, 1);
    }

    protected final InputContext getItem(int itemNum) {
      return fileContext;
    }

    public InputContextIterator broaden() {
      return null;
    }
  }
}
