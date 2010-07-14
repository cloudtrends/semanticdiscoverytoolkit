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


import java.io.File;
import java.io.IOException;
import org.sd.util.ReflectUtil;

/**
 * DOM-backed properties container.
 * <p>
 * @author Spence Koehler
 */
public class DomDataProperties extends BaseDataProperties {


  private DomElement domElement;

  public DomDataProperties() {
    this.domElement = null;
  }

  public DomDataProperties(File xmlFile) throws IOException {
    final DomDocument domDocument = XmlFactory.loadDocument(xmlFile, false);
    this.domElement = domDocument.getDocumentDomElement();
  }

  public DomDataProperties(DomElement domElement) {
    this.domElement = domElement;
  }


  /**
   * Build an instance of the domNode's class specified by its relative
   * classXPath that takes the domNode object as its sole construction
   * parameter.
   */
  public Object buildInstance(DomNode domNode, String classXPath) {
    Object result = null;

    try {
      final DomNode classnameNode = domNode.selectSingleNode(classXPath);
      if (classnameNode == null) {
        throw new IllegalArgumentException("Required xpath '" + classXPath +
                                           "' not found (relative to '" +
                                           domNode.getLocalName() + "' node)!");
      }
      final String classname = classnameNode.getTextContent().trim();
      final Class theClass = Class.forName(classname);
      result = ReflectUtil.constructInstance(theClass, domNode);
    }
    catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    }

    return result;
  }


  public DomElement getDomElement() {
    return domElement;
  }


  protected String getValueString(String xpath) {
    String result = null;

    DomNode domNode = domElement == null ? null : domElement.selectSingleNode(xpath);

    if (domNode != null) {
      result = domNode.getTextContent();
    }

    return result;
  }
}
