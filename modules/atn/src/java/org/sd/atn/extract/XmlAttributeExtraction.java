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

/**
 * Container class for xml-based extracted attribute text.
 * <p>
 * @author Spence Koehler
 */
public class XmlAttributeExtraction extends XmlExtraction {
  
  private String attribute;
  /**
   * The name of the attribute hosting the xml context's extracted text.
   */
  public String getAttribute() {
    return attribute;
  }
  protected void setAttribute(String attribute) {
    this.attribute = attribute;
  }

  private int startIndex;
  /**
   * Starting index of extraction text within the xmlContext's attribute's value
   * (inclusive).
   */
  public int getStartIndex() {
    return startIndex;
  }
  protected void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  private int endIndex;
  /**
   * Ending index of extraction text within the xmlContext's attribute's value
   * (exclusive).
   */
  public int getEndIndex() {
    return endIndex;
  }
  protected void setEndIndex(int endIndex) {
    this.endIndex = endIndex;
  }


  /**
   * Construct covering the entire text (value) of the attribute in the
   * given context.
   */
  public XmlAttributeExtraction(String type, DomContext domContext, String attribute) {
    super(type, domContext);
    init(domContext, attribute, -1, -1);
  }

  /**
   * Construct covering the portion of the text (value) of the attribute
   * in the given context.
   */
  public XmlAttributeExtraction(String type, DomContext domContext, String attribute, int startIndex, int endIndex) {
    super(type, domContext);
    init(domContext, attribute, startIndex, endIndex);
  }

  private void init(DomContext domContext, String attribute, int startIndex, int endIndex) {
    this.attribute = attribute;
    final String attributeText = getNode().getAttributeValue(attribute);

    if (startIndex == -1) startIndex = 0;
    if (endIndex == -1) endIndex = attributeText.length();

    setText(attributeText.substring(startIndex, endIndex));
    this.startIndex = startIndex;
    this.endIndex = endIndex;
  }


  protected String asString() {
    final StringBuilder result = new StringBuilder();

    result.
      append(super.asString()).
      append(" (").
      append(getNode().getLocalName()).
      append("[@").
      append(attribute).
      append(":").
      append(startIndex).
      append(",").
      append(endIndex).
      append("] ").
      append(getNode().getAttributeValue(attribute)).
      append(")");

    return result.toString();
  }
}
