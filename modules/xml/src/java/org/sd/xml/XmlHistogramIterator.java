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
package org.sd.xml;


import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Utility class to iterate over the keys of an xml histogram file.
 * <p>
 * @author Spence Koehler
 */
public class XmlHistogramIterator implements Iterator<XmlKeyContainer> {
  
  private XmlTagRipper ripper;
  private XmlLite.Tag curTag;
  private XmlKeyContainer curKey;

  private XmlLite.Tag nextTag;
  private String nextKey;

  private Long total;
  private Long bins;

  public XmlHistogramIterator(File xmlHistogram) throws IOException {
    this.ripper = new XmlTagRipper(xmlHistogram);
    this.curKey = null;
    this.curTag = null;
    this.total = null;
    this.bins = null;

    prepareNext();
  }

  /**
   * Get the current key container (that which was last returned by "getNext").
   */
  public XmlKeyContainer getCurKey() {
    return curKey;
  }

  /**
   * Get the current ripped tag (that which corresponded to the last key
   * returned by getNext.
   */
  public XmlLite.Tag getCurTag() {
    return curTag;
  }

  /**
   * Get the total number of instances if available.
   * <p>
   * NOTE: This is read from the "total" attribute of the histogram key
   * container element if found.
   */
  public Long getTotal() {
    return total;
  }

  /**
   * Get the total number of bins if available.
   * <p>
   * NOTE: This is read from the "bins" attribute of the histogram key
   * container element if found.
   */
  public Long getBins() {
    return bins;
  }

  public void close() throws IOException {
    ripper.close();
  }

  public boolean hasNext() {
    return this.nextTag != null;
  }

  public XmlKeyContainer next() {

    if (this.nextTag != null) {
      this.curTag = nextTag;
      this.curKey = new XmlKeyContainer(nextKey, nextTag.attributes);
    }

    prepareNext();

    return this.curKey;
  }

  public void remove() {
    //do nothing.
  }

  private final void prepareNext() {

    this.nextTag = null;
    this.nextKey = null;

    while (ripper.hasNext()) {
      final XmlLite.Tag tag = ripper.next();

      if (total == null) {
        final String totalString = tag.getAttribute("total");
        if (totalString != null) {
          try {
            this.total = new Long(totalString);
          }
          catch (Exception e) {}
        }
      }

      if (bins == null) {
        final String binsString = tag.getAttribute("bins");
        if (binsString != null) {
          try {
            this.bins = new Long(binsString);
          }
          catch (Exception e) {}
        }
      }

      if ("key".equals(tag.name)) {
        // found the next key
        this.nextTag = tag;
        this.nextKey = ripper.ripText();
        break;
      }
    }
  }
}
