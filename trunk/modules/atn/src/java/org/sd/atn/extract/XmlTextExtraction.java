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


import org.sd.xml.DomContext;
import org.sd.xml.DomNode;
import org.sd.xml.DomUtil;

/**
 * Container class for xml-based extracted node text.
 * <p>
 * @author Spence Koehler
 */
public class XmlTextExtraction extends XmlExtraction {
  
  private int startIndex;
  /**
   * Starting index of extraction text within domContext's text (inclusive).
   */
  public int getStartIndex() {
    return startIndex;
  }
  protected void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  private int endIndex;
  /**
   * Ending index of extraction text within domContext's text (exclusive).
   */
  public int getEndIndex() {
    return endIndex;
  }
  protected void setEndIndex(int endIndex) {
    this.endIndex = endIndex;
  }


  /**
   * Construct with an domContext and the extraction's start index (inclusive)
   * and end index (exclusive) within the domContext's text.
   * 
   * Note that the domContext used for an extraction will be that which is
   * closest to the indicated text (deepest in its DOM tree) while still
   * encompassing the extraction text.
   */
  public XmlTextExtraction(String type, DomContext domContext, int startIndex, int endIndex) {
    super(type);

    // adjust domContext as tightly as possible around the extracted text.
    final int[] adjustedStartIndex = new int[] {startIndex};
    final int[] adjustedEndIndex = new int[] {endIndex};
    final DomNode adjustedXmlNode = DomUtil.getDeepestNode(domContext.getDomNode(), startIndex, endIndex, adjustedStartIndex, adjustedEndIndex);


    // set instance data
    final String fullNodeText = DomUtil.getTrimmedNodeText(adjustedXmlNode);

    setText(fullNodeText.substring(adjustedStartIndex[0], adjustedEndIndex[0]));

    setNode(adjustedXmlNode);
    this.startIndex = adjustedStartIndex[0];
    this.endIndex = adjustedEndIndex[0];
  }


  protected String asString() {
    StringBuilder result = new StringBuilder();

    result.
      append(super.asString()).
      append(" ([").
      append(startIndex).
      append(",").
      append(endIndex).
      append("] ").
      append(")");

    return result.toString();
  }
}
