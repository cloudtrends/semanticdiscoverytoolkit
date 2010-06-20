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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;

/**
 * Wrapper for a dom element node as provided through XmlLite.Tag.
 * <p>
 * @author Spence Koehler
 */
public class DomElement extends DomNode implements Element {
  
  private String _localName;
  private DomNamedNodeMap _attributes;

  DomElement(XmlLite.Tag tagData) {
    super(tagData, tagData.name, tagData.name, null);

    this._localName = null;
    this._attributes = null;
  }

  public NamedNodeMap getAttributes() {
    return getDomAttributes();
  }

  private DomNamedNodeMap getDomAttributes() {
    if (_attributes == null) {
      _attributes = new DomNamedNodeMap(this);
    }
    return _attributes;
  }

  public String getLocalName() {
    if (_localName == null) {
      _localName = nodeName;

      if (_localName != null) {
        int cPos = _localName.indexOf(':');
        if (cPos >= 0) {
          _localName = _localName.substring(cPos + 1);
        }
      }
    }

    return _localName;
  }

  public boolean hasAttributes() {
    return backref.asTag().attributes.size() > 0;
  }

  public short getNodeType() {
    return ELEMENT_NODE;
  }

  public String getPrefix() {
    return getDefaultNamespacePrefix();
  }

  public String getAttribute(String name) {
    String result = null;

    final Attr attr = getAttributeNode(name);
    if (attr != null) {
      result = attr.getValue();
    }

    return result;
  }

  public Attr getAttributeNode(String name) {
    DomAttribute result = null;

    if (hasAttributes()) {
      final DomNamedNodeMap attributes = getDomAttributes();
      result = (DomAttribute)attributes.getNamedItem(name);
    }

    return result;
  }

  public Attr getAttributeNodeNS(String namespaceURI, String localName) {
    DomAttribute result = null;

    if (hasAttributes()) {
      final DomNamedNodeMap attributes = getDomAttributes();
      result = (DomAttribute)attributes.getNamedItemNS(namespaceURI, localName);
    }

    return result;
  }

  public String getAttributeNS(String namespaceURI, String localName) {
    String result = null;

    final Attr attr = getAttributeNodeNS(namespaceURI, localName);
    if (attr != null) {
      result = attr.getValue();
    }

    return result;
  }

  public NodeList getElementsByTagName(String tagname) {
    return doGetElementsByTagName(tagname);
  }

  public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
    return doGetElementsByTagNameNS(namespaceURI, localName);
  }

  public TypeInfo getSchemaTypeInfo() {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public String getTagName() {
    return nodeName;
  }

  public boolean hasAttribute(String name) {
    boolean result = false;

    if (hasAttributes()) {
      // NOTE: go through NamedNodeMap to properly handle namespacing in 'name'
      final DomNamedNodeMap attributes = getDomAttributes();
      result = attributes.containsKey(name);
    }

    return result;
  }

  public boolean hasAttributeNS(String namespaceURI, String localName) {
    boolean result = false;

    if (hasAttributes()) {
      // NOTE: go through NamedNodeMap to properly handle namespacing in 'name'
      final DomNamedNodeMap attributes = getDomAttributes();
      result = attributes.containsKey(namespaceURI, localName);
    }

    return result;
  }

  public void removeAttribute(String name) {
    if (hasAttributes()) {
      final DomNamedNodeMap attributes = getDomAttributes();
      attributes.removeNamedItem(name);
    }
  }

  public Attr removeAttributeNode(Attr oldAttr) {
    Attr result = null;

    if (hasAttributes()) {
      final DomNamedNodeMap attributes = getDomAttributes();
      result = (Attr)attributes.removeNamedItem(oldAttr.getName());
    }

    return result;
  }

  public void removeAttributeNS(String namespaceURI, String localName) {
    if (hasAttributes()) {
      final DomNamedNodeMap attributes = getDomAttributes();
      attributes.removeNamedItemNS(namespaceURI, localName);
    }
  }

  public void setAttribute(String name, String value) {
    final DomAttribute attr = (DomAttribute)(getOwnerDocument().createAttribute(name));
    attr.setValue(value);
    setAttributeNode(attr);
  }

  public Attr setAttributeNode(Attr newAttr) {
    //NOTE: newAttr's values are used and a new Attr instance is created!
    final DomNamedNodeMap attributes = getDomAttributes();
    return (Attr)attributes.setNamedItem(newAttr);
  }

  public Attr setAttributeNodeNS(Attr newAttr) {
    //NOTE: newAttr's values are used and a new Attr instance is created!
    final DomNamedNodeMap attributes = getDomAttributes();
    return (Attr)attributes.setNamedItemNS(newAttr);
  }

  public void setAttributeNS(String namespaceURI, String qualifiedName, String value) {
    final DomAttribute attr = (DomAttribute)(getOwnerDocument().createAttributeNS(namespaceURI, qualifiedName));
    attr.setValue(value);
    setAttributeNodeNS(attr);
  }

  public void setIdAttribute(String name, boolean isId) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public void setIdAttributeNode(Attr idAttr, boolean isId) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) {
    throw new UnsupportedOperationException("Implement when needed.");
  }
}
