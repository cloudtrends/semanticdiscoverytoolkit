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
import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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

  private Map<String, String> attributes;

  public XmlMessage() {
  }

  /**
   * Construct with the given xml data string.
   */
  public XmlMessage(String xmlDataString) {
    this.xmlDataString = xmlDataString;
    this._xmlElement = null;
    this.attributes = null;
  }

  /**
   * Construct with the given xml element.
   */
  public XmlMessage(DomElement xmlElement) {
    this.xmlDataString = null;
    this._xmlElement = xmlElement;
  }

  /**
   * Determine whether this message has any extra attributes.
   */
  public boolean hasAttributes() {
    return attributes != null && attributes.size() > 0;
  }

  /**
   * Set an extra attribute in this message.
   */
  public void setAttribute(String att, String val) {
    if (attributes == null) attributes = new HashMap<String, String>();
    attributes.put(att, val);
  }

  /**
   * Get the extra attribute's value or null.
   */
  public String getAttribute(String att) {
    return attributes == null ? null : attributes.get(att);
  }

  /**
   * Get the message attribute's value or null.
   * <p>
   * This is defined as the attribute's value, overridable by the top xml
   * element's attribute.
   */
  public String getMessageAttribute(String att) {
    return getMessageAttribute(att, null);
  }

  /**
   * Get the message attribute's value or the given default value.
   * <p>
   * This is defined as the attribute's value, overridable by the top xml
   * element's attribute.
   */
  public String getMessageAttribute(String att, String defaultValue) {
    String result = null;

    final DomElement xmlElement = getXmlElement();

    if (xmlElement != null) {
      result = xmlElement.getAttributeValue(att, null);
    }

    if (result == null) {
      result = getAttribute(att);
    }

    return result == null ? defaultValue : result;
  }

  public int getMessageAttributeInt(String att, int defaultValue) {
    int result = defaultValue;

    final String str = getMessageAttribute(att);
    if (str != null) {
      result = Integer.parseInt(str);
    }

    return result;
  }

  public long getMessageAttributeLong(String att, long defaultValue) {
    long result = defaultValue;

    final String str = getMessageAttribute(att);
    if (str != null) {
      result = Long.parseLong(str);
    }

    return result;
  }

  public boolean getMessageAttributeBoolean(String att, boolean defaultValue) {
    boolean result = defaultValue;

    final String str = getMessageAttribute(att);
    if (str != null) {
      result = "true".equalsIgnoreCase(str);
    }

    return result;
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
   * (Re-)Set this instance's payload.
   */
  public void setXmlData(XmlStringBuilder xml) {
    if (xml != null) {
      this.xmlDataString = xml.getXmlString();
      this._xmlElement = xml.getXmlElement();
    }
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

    if (attributes == null) dataOutput.writeInt(0);
    else {
      dataOutput.writeInt(attributes.size());
      for (Map.Entry<String, String> attrEntry : attributes.entrySet()) {
        DataHelper.writeString(dataOutput, attrEntry.getKey());
        DataHelper.writeString(dataOutput, attrEntry.getValue());
      }
    }
  }

  public void read(DataInput dataInput) throws IOException {
    this.xmlDataString = DataHelper.readString(dataInput);

    try {
      final int numAttrs = dataInput.readInt();
      if (numAttrs > 0) {
        this.attributes = new HashMap<String, String>();
        for (int attrNum = 0; attrNum < numAttrs; ++attrNum) {
          final String key = DataHelper.readString(dataInput);
          final String val = DataHelper.readString(dataInput);
          attributes.put(key, val);
        }
      }
    }
    catch (EOFException e) {
      // this happens with "old" XmlMessage clients that don't write attributes,
      // so there are no attributes and "eating" this exception is appropriate.
    }
  }
}
