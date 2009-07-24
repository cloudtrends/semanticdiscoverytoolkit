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
package org.sd.xml;


import org.sd.util.tree.Tree;

import java.util.Iterator;

/**
 * Wrapper around an xml ripper for ripping xml data from xml.
 * <p>
 * @author Spence Koehler
 */
public class XmlDataRipper implements Iterator<XmlData> {
  
  private XmlRipper xmlRipper;
  private XmlData prior;

  public XmlDataRipper(XmlRipper xmlRipper) {
    this.xmlRipper = xmlRipper;
    this.prior = null;
  }

  public boolean hasNext() {
    return xmlRipper.hasNext();
  }

  public XmlData next() {
    XmlData result = null;

    if (xmlRipper.hasNext()) {
      final Tree<XmlLite.Data> xmlNode = xmlRipper.next();
      final TagStack tagStack = xmlRipper.getTagStack();
      final int index = xmlRipper.getIndex();
      final XmlLite.Tag blockTag = computeBlockTag(tagStack);

      result = new XmlData(index, xmlNode, tagStack, blockTag);

      if (prior != null) {
        prior.setNextBlockTag(result);
      }
      prior = result;
    }

    return result;
  }

  public void remove() {
    xmlRipper.remove();
  }

  /**
   * Close this ripper, cleanly disposing of open references, etc.
   */
  public void close() {
    xmlRipper.close();
  }

  /**
   * Determine whether the stream has been read to its end.
   */
  public boolean finishedStream() {
    return xmlRipper.finishedStream();
  }

  private final XmlLite.Tag computeBlockTag(TagStack curTagStack) {
    XmlLite.Tag result = null;

    if (prior != null) {
      final TagStack priorTagStack = prior.tagStack;

      // The block tag is that which diverges from the prior tag stack
      final int blockIndex = curTagStack.findFirstDivergentTag(priorTagStack);

      if (blockIndex >= 0) {
        result = curTagStack.getTag(blockIndex);
      }
    }

    if (result == null) {
      result = curTagStack.getTag(0);
    }

    return result;
  }
}
