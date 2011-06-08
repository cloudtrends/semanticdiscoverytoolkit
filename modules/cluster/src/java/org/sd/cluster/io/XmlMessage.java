/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.cluster.io;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.sd.io.DataHelper;
import org.sd.xml.DomElement;
import org.sd.xml.XmlStringBuilder;
import org.sd.xml.XmlFactory;

/**
 * Generic message containing xml data.
 * <p>
 * @author Spence Koehler
 */
public abstract class XmlMessage implements Message {
  
  private String xmlDataString;
  private DomElement _xmlElement;
  private IOException _error;

  public XmlMessage() {
  }

  /**
   * Construct with the given xml data string.
   */
  public XmlMessage(String xmlDataString) {
    this.xmlDataString = xmlDataString;
    this._xmlElement = null;
  }

  /**
   * Construct with the given xml element.
   */
  public XmlMessage(DomElement xmlElement) {
    this.xmlDataString = null;
    this._xmlElement = xmlElement;
  }

  /**
   * Get this instance's xml element.
   */
  public String getXmlDataString() {
    if (xmlDataString == null && _xmlElement != null) {
      xmlDataString = _xmlElement.asFlatString(null).toString();
    }
    return xmlDataString;
  }

  /**
   * Get this instance's xml element.
   */
  public DomElement getXmlElement() {
    if (_xmlElement == null && xmlDataString != null) {
      try {
        _xmlElement = (DomElement)XmlFactory.buildDomNode(xmlDataString, false);
      }
      catch (IOException e) {
        // null result
        this._error = e;
      }
    }
    return _xmlElement;
  }

  /**
   * Get this instance's xml data as an XmlStringBuilder.
   */
  public XmlStringBuilder getXmlStringBuilder() {
    return new XmlStringBuilder().setXmlString(getXmlDataString());
  }

  /**
   * Get the error (if it exists).
   * <p>
   * This will exist if there was a problem building the xmlElement from the
   * xmlDataString.
   */
  public IOException getError() {
    return _error;
  }


  public void write(DataOutput dataOutput) throws IOException {
    DataHelper.writeString(dataOutput, getXmlDataString());
  }

  public void read(DataInput dataInput) throws IOException {
    this.xmlDataString = DataHelper.readString(dataInput);
  }
}
