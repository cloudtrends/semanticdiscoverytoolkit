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


import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.TypeInfo;

/**
 * Wrapper for a dom attribute node as provided through XmlLite.Tag.attributes.
 * <p>
 * @author Spence Koehler
 */
public class DomAttribute extends DomNode implements Attr {
  
  private DomElement containingNode;

  DomAttribute(String attrName) {
    this(null, attrName, "");
  }

  DomAttribute(DomElement containingNode, String attrName, String attrValue) {
    super(null, attrName, attrName, attrValue);

    this.containingNode = containingNode;

/*
    if (containingNode != null) {
      final XmlLite.Data backref = containingNode.getBackReference();
      if (backref != null) {
        final XmlLite.Tag tag = backref.asTag();
        if (tag != null) {
          tag.setAttribute(attrName, attrValue);
        }
      }
    }
*/
  }

  public String getTextContent() {
    return nodeValue;
  }

  public String getLocalName() {
    String result = nodeName;

    if (result != null) {
      int cPos = result.indexOf(':');
      if (cPos >= 0) {
        result = result.substring(cPos + 1);
      }
    }

    return result;
  }

  public short getNodeType() {
    return ATTRIBUTE_NODE;
  }

  public String getPrefix() {
    return getDefaultNamespacePrefix();
  }

  public String getName() {
    return nodeName;
  }

  public Element getOwnerElement() {
    return containingNode;
  }

  public TypeInfo getSchemaTypeInfo() {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public boolean getSpecified() {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public String getValue() {
    return nodeValue;
  }

  public boolean isId() {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public void setValue(String value) {
    this.nodeValue = value;
    containingNode.getBackReference().asTag().attributes.put(nodeName, value);
  }
}
