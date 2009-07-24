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
package org.sd.extract;


import org.sd.io.FileChunk;
import org.sd.io.FileIterator;
import org.sd.io.FileUtil;
import org.sd.xml.XmlData;
import org.sd.xml.XmlFactory;
import org.sd.xml.XmlLite;
import org.sd.util.tree.Tree;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Implementation of a text container for text file lines.
 * <p>
 * @author Spence Koehler
 */
public class TextFileTextContainer extends BaseTextContainer<FileIterator.FileChunkImpl> {
  
  private String name;
  private FileIterator<FileIterator.FileChunkImpl> fileIterator;

  /**
   * Vanilla constructor for xml.
   */
  public TextFileTextContainer(File textFile, boolean keepDocTexts) throws IOException {
    this(textFile.getAbsolutePath(), FileUtil.getInputStream(textFile), keepDocTexts);
  }

  public TextFileTextContainer(String name, InputStream inputStream, boolean keepDocTexts) throws IOException {
    super(keepDocTexts);

    this.name = name;
    this.fileIterator = FileIterator.getLineIterator(inputStream);
  }

  /**
   * Build an iterator for this container.
   */
  protected Iterator<FileIterator.FileChunkImpl> buildIterator() {
    return fileIterator;
  }

  /**
   * Build a doc text instance for the datum.
   */
  protected DocText buildDocText(FileIterator.FileChunkImpl fileChunk, int index) {
    Tree<XmlLite.Data> xmlNode = null;

    try {
      xmlNode = XmlFactory.buildXmlTree(fileChunk.getLine(), false, false);
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return new DocText(this, new XmlData(index, xmlNode, null, null));
  }

  /**
   * Get this text container's name.
   */
  public String getName() {
    return name;
  }

  /**
   * Close this container.
   * <p>
   * It is important that this method be called when finished iterating over
   * the text (through hasNext and next) to properly release resources.
   * <p>
   * This method should typically be called in a finally block for the
   * hasNext/next iteration.
   * <p>
   * Once this has been called, hasNext() will return false and next() will
   * return null, but all other methods will retain their defined behavior.
   */
  public void close() {
    this.fileIterator.close();
  }

  /**
   * Determine whether the entire document has been visited through the
   * iterator methods (next/hasNext).
   * <p>
   * Note that once the close method has been called, this value can never
   * change from false to true.
   */
  public boolean isComplete() {
    return !fileIterator.hasNext();
  }

  /**
   * Convert a docText from this container into another container.
   * <p>
   * This is used to be able to treat each docText from a container as high-
   * level, iterable input in its own right if this makes sense for the
   * docText.
   * 
   * @return the docText as a container or null.
   */
  public TextContainer convertToTextContainer(DocText docText, boolean keepDocTexts) {

//todo: this could return a text container that iterates over the text nodes
//      of the xml tree in the doc text, but we don't have a use for that
//      right now. If/when we need it, then we can implement this accordingly.

    return null;
  }
}
