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


import java.io.IOException;
import java.util.Map;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.XmlFactory;
import org.sd.xml.XmlStringBuilder;
import org.w3c.dom.NodeList;

/**
 * Wrapper around an xml element for updating its content.
 * <p>
 * @author Spence Koehler
 */
public class XmlWrapper {
  

  private XmlStringBuilder xmlBuilder;

  /**
   * Construct instance with the given element tag (optionally with attributes).
   * <p>
   * Where tag format is xml-safe "name att1='val1' ...".
   */
  public XmlWrapper(String elementName) {
    this.xmlBuilder = new XmlStringBuilder(elementName);
  }

  /**
   * Construct instance with the given contents.
   */
  public XmlWrapper(DomElement domElement) {
    this.xmlBuilder = new XmlStringBuilder().setXmlElement(domElement);
  }

  /**
   * Construct with the given XmlStringBuilder.
   */
  public XmlWrapper(XmlStringBuilder xmlBuilder) {
    this.xmlBuilder = xmlBuilder;
  }

  public XmlStringBuilder getXmlStringBuilder() {
    return xmlBuilder;
  }

  public DomElement getDomElement() {
    return xmlBuilder.getXmlElement();
  }


  /**
   * Add (or reset) att/val pair for non-null, non-empty att/val.
   */
  public void addAttribute(String att, String val) {
    if (att != null && val != null && !"".equals(att) && !"".equals(val)) {
      final DomElement domElement = getDomElement();
      domElement.setAttribute(att, val);
    }
  }

  /**
   * Remove the specified attribute.
   */
  public void removeAttribute(String att) {
    if (att != null && !"".equals(att)) {
      final DomElement domElement = getDomElement();
      domElement.removeAttribute(att);
    }
  }

  /**
   * Add the child node.
   *
   * @return the childNode
   */
  public DomNode addChildNode(String childXml) {
    DomNode result = null;

    try {
      result = XmlFactory.buildDomNode(childXml, false);
      addChildNode(result);
    }
    catch (IOException e) {
      //result = null;
    }

    return result;
  }

  /**
   * Add the child node unless already present.
   *
   * @return the childNode
   */
  public DomNode addChildNode(DomNode child) {
    final DomElement domElement = getDomElement();

    if (!hasChild(domElement, child)) {
      domElement.getOwnerDocument().adoptNode(child);
      domElement.appendChild(child);
    }

    return child;
  }

  private final boolean hasChild(DomElement parent, DomNode child) {
    boolean result = false;

    final NodeList childNodes = parent.getChildNodes();
    final int numChildNodes = childNodes.getLength();
    for (int childNum = 0; childNum < numChildNodes; ++childNum) {
      final DomNode childNode = (DomNode)childNodes.item(childNum);
      if (child == childNode) {
        result = true;
        break;
      }
    }

    return result;
  }

  /**
   * Add the child node unless already present.
   *
   * @return the childNode
   */
  public DomNode addChildNode(XmlWrapper childWrapper) {
    if (childWrapper == null) return null;
    return addChildNode(childWrapper.getDomElement());
  }

  /**
   * Merge in element attributes and child nodes of the given element.
   */
  public void mergeIn(DomElement otherElement) {

    // append child nodes
    final NodeList childNodes = otherElement.getChildNodes();
    final int numChildNodes = childNodes.getLength();
    for (int childNum = 0; childNum < numChildNodes; ++childNum) {
      final DomNode childNode = (DomNode)childNodes.item(childNum);
      
      //todo: only append if unique?
      if (childNode.getNodeType() == DomNode.ELEMENT_NODE) {
        xmlBuilder.addElement((DomElement)childNode);
      }
    }

    // merge in unique element attributes
    if (otherElement.hasAttributes()) {
      for (Map.Entry<String, String> entry : otherElement.getDomAttributes().getAttributes().entrySet()) {
        addAttribute(entry.getKey(), entry.getValue());
      }
    }
  }
}
