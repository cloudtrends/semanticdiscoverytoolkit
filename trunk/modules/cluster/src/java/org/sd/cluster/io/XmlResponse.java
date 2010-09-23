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

/**
 * Generic response containing xml data.
 * <p>
 * @author Spence Koehler
 */
public class XmlResponse extends Response {
  
  private XmlStringBuilder xml;

  /**
   * Empty constructor for publishable reconstruction.
   */
  public XmlResponse() {
    this.xml = null;
  }

  /**
   * Construct with the given xmlStringBuilder.
   */
  public XmlResponse(XmlStringBuilder xmlStringBuilder) {
    this.xml = xmlStringBuilder;
  }

  /**
   * Get this instance's xmlStringBuilder, creating if necessary.
   */
  public XmlStringBuilder getXmlStringBuilder() {
    if (xml == null) {
      xml = new XmlStringBuilder("response");
    }
    return this.xml;
  }

  public String getXmlString() {
    return xml == null ? null : xml.getXmlString();
  }

  public DomElement getXmlElement() {
    return xml == null ? null : xml.getXmlElement();
  }

  public void write(DataOutput dataOutput) throws IOException {
    final String xmlString = getXmlString();
    DataHelper.writeString(dataOutput, xmlString);
  }

  public void read(DataInput dataInput) throws IOException {
    this.xml = null;
    final String xmlString = DataHelper.readString(dataInput);
    if (xmlString != null) {
      this.xml = new XmlStringBuilder().setXmlString(xmlString);
    }
  }
}
