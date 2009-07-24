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
package org.sd.crawl;


import org.sd.xml.XmlData;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for a link tag.
 * <p>
 * @author Spence Koehler
 */
public class LinkTag {
  
  public final XmlLite.Tag tag;
  public final List<XmlData> xmlDatas;
  public final String key;
  private String linkUrl;

  private StringBuilder linkText;
  private int minIndex;
  private int maxIndex;

  public LinkTag(XmlLite.Tag tag, XmlData xmlData, String key) {
    this.tag = tag;
    this.xmlDatas = new ArrayList<XmlData>();
    xmlDatas.add(xmlData);
    this.key = key;
    this.linkUrl = tag.getAttribute("href");

    this.linkText = new StringBuilder();
    this.linkText.append(XmlTreeHelper.getAllText(xmlData.xmlNode));
    this.minIndex = xmlData.index;
    this.maxIndex = xmlData.index;
  }

  /**
   * Add the xmlData if it belongs.
   * 
   * @return true if added; otherwise, false.
   */
  public boolean addXmlData(XmlData xmlData) {
    boolean result = false;

    if (tag != null && xmlData.tagStack.hasTag(tag) >= 0) {
      result = true;

      xmlDatas.add(xmlData);
      if (linkText.length() > 0) linkText.append(' ');
      linkText.append(XmlTreeHelper.getAllText(xmlData.xmlNode));
      if (xmlData.index < minIndex) minIndex = xmlData.index;
      if (xmlData.index > maxIndex) maxIndex = xmlData.index;
    }

    return result;
  }

  /**
   * Get the link url.
   */
  public String getLinkUrl() {
    return linkUrl;
  }

  /**
   * Get the link text.
   */
  public String getLinkText() {
    return linkText.toString();
  }

  /**
   * Get the min path index covered by this tag.
   */
  public int getMinIndex() {
    return minIndex;
  }

  /**
   * Get the max path index covered by this tag.
   */
  public int getMaxIndex() {
    return maxIndex;
  }
}
