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

import org.sd.io.FileUtil;
import org.sd.xml.XmlData;
import org.sd.xml.XmlLeafNodeRipper;
import org.sd.xml.XmlDataRipper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

/**
 * Implementation of a text container for xml (or html) data.
 * <p>
 * @author Spence Koehler
 */
public class XmlTextContainer extends BaseTextContainer<XmlData> {
  
  private String name;
  private XmlDataRipper xmlDataRipper;
  private boolean keepEmpties;

  /**
   * Vanilla constructor for xml.
   */
  public XmlTextContainer(File xmlFile, boolean keepDocTexts, boolean keepEmpties) throws IOException {
    this(xmlFile, false, null, keepDocTexts, keepEmpties);
  }

  /**
   * Construct with the given settings.
   */
  public XmlTextContainer(File xmlFile, boolean isHtml, Set<String> ignoreTags, boolean keepDocTexts, boolean keepEmpties) throws IOException {
    this(xmlFile.getAbsolutePath(), FileUtil.getInputStream(xmlFile), isHtml, ignoreTags, keepDocTexts, keepEmpties);
  }

  /**
   * Tags to 'save' while ripping that are used to identify breaks in documents
   * by some heuristics over docTexts.
   */
  private static final String[] BREAKING_TAGS = new String[]{"br", "hr"};

  /**
   * Construct with the given settings.
   */
  public XmlTextContainer(String name, InputStream inputStream, boolean isHtml, Set<String> ignoreTags, boolean keepDocTexts, boolean keepEmpties) throws IOException {
    super(keepDocTexts);

    this.name = name;
    this.xmlDataRipper = new XmlDataRipper(new XmlLeafNodeRipper(inputStream, isHtml, ignoreTags, keepEmpties, BREAKING_TAGS));
    this.keepEmpties = keepEmpties;
  }

  /**
   * Build an iterator for this container.
   */
  protected Iterator<XmlData> buildIterator() {
    return xmlDataRipper;
  }

  /**
   * Build a doc text instance for the datum.
   */
  protected DocText buildDocText(XmlData xmlData, int index) {
    return new DocText(this, xmlData);
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
    this.xmlDataRipper.close();
  }

  /**
   * Determine whether the entire document has been visited through the
   * iterator methods (next/hasNext).
   * <p>
   * Note that once the close method has been called, this value can never
   * change from false to true.
   */
  public boolean isComplete() {
    return xmlDataRipper.finishedStream();
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

  /**
   * Convenience method to get the number of non-empty paths between startIndex
   * (inclusive) and endIndex (exclusive).
   */
  public int numNonEmptyPaths(int startIndex, int endIndex) {
    int result = 0;

    if (keepEmpties && isCaching()) {
      for (int i = startIndex; i < endIndex; ++i) {
        final DocText docText = getDocText(i);
        if (!"".equals(docText.getString())) ++result;
      }
    }
    else {
      result = endIndex - startIndex;
    }

    return result;
  }
}
