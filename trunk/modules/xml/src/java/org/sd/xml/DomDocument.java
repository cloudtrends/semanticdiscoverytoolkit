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


import java.io.BufferedWriter;
import java.io.IOException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.sd.util.tree.Tree;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

/**
 * Wrapper for a dom document as provided through XmlLite.Tag.
 * <p>
 * @author Spence Koehler
 */
public class DomDocument extends DomNode implements Document {
  
  private DomElement documentElement;
  private String documentUri;
  private String xmlEncoding;
  private boolean xmlStandalone;
  private String xmlVersion;
  private DataProperties dataProperties;
  private DomContext _domContext;

  protected static final XPathFactory XPATH_FACTORY = javax.xml.xpath.XPathFactory.newInstance();

  protected static final XPath newXPath() { return XPATH_FACTORY.newXPath(); }

  DomDocument(XmlLite.Tag rootTag) {
    super(rootTag, "#document", "#document", null);

    this.documentElement = rootTag == null ? null : (DomElement)rootTag.asDomNode();
    this.documentUri = null;
    this.xmlEncoding = null;
    this.xmlStandalone = false;
    this.xmlVersion = null;
    this.dataProperties = null;

    setOwnerDocument(this);
  }

  public void writeTo(BufferedWriter writer) throws IOException {
    if (documentElement != null) {
      final Tree<XmlLite.Data> tree = documentElement.asTree();
      if (tree != null) {
        XmlLite.writeXml(tree, writer);
      }
    }
  }

  DataProperties doGetDataProperties() {
    return this.dataProperties;
  }

  void doSetDataProperties(DataProperties dataProperties) {
    if (this.dataProperties == null) this.dataProperties = dataProperties;
  }

  public short getNodeType() {
    return DOCUMENT_NODE;
  }

  public Node adoptNode(Node source) {
    Node result = null;

    if (source instanceof DomNode) {
      ((DomNode)source).setOwnerDocument(this);
      result = source;
    }

    return result;
  }

  public Attr createAttribute(String name) {
    final DomAttribute result = new DomAttribute(name);
    result.setOwnerDocument(this);
    return result;
  }

  public DomAttribute createDomAttribute(DomElement containingNode, String name, String value) {
    final DomAttribute result = new DomAttribute(containingNode, name, value);
    result.setOwnerDocument(this);
    return result;
  }

  public Attr createAttributeNS(String namespaceURI, String qualifiedName) {
    final NSContainer nsContainer = new NSContainer(namespaceURI, qualifiedName);

    nsContainer.addUri(this);

    final DomAttribute result = new DomAttribute(qualifiedName);
    result.setNamespaceURI(namespaceURI);
    result.setOwnerDocument(this);

    return result;
  }

  public CDATASection createCDATASection(String data) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public Comment createComment(String data) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public DocumentFragment createDocumentFragment() {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public Element createElement(String tagName) {
    final XmlLite.Tag elementTag = new XmlLite.Tag(tagName, commonCase());
    final Tree<XmlLite.Data> elementNode = new Tree<XmlLite.Data>(elementTag);
    elementTag.setContainer(elementNode);
    final DomElement result = (DomElement)elementTag.asDomNode();
    result.setOwnerDocument(this);
    return result;
  }

  public Element createElementNS(String namespaceURI, String qualifiedName) {
    final NSContainer nsContainer = new NSContainer(namespaceURI, qualifiedName);

    nsContainer.addUri(this);

    final XmlLite.Tag elementTag = new XmlLite.Tag(qualifiedName, commonCase());
    final Tree<XmlLite.Data> elementNode = new Tree<XmlLite.Data>(elementTag);
    elementTag.setContainer(elementNode);
    final DomElement result = (DomElement)elementTag.asDomNode();

    result.setNamespaceURI(namespaceURI);
    result.setOwnerDocument(this);

    return result;
  }

  public EntityReference createEntityReference(String name) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public ProcessingInstruction createProcessingInstruction(String target, String data) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public Text createTextNode(String data) {
    final XmlLite.Text text = new XmlLite.Text(data);
    final Tree<XmlLite.Data> textNode = new Tree<XmlLite.Data>(text);
    text.setContainer(textNode);
    final DomText result = (DomText)text.asDomNode();
    result.setOwnerDocument(this);
    return result;
  }

  public DocumentType getDoctype() {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public Element getDocumentElement() {
    return documentElement;
  }

  public DomElement getDocumentDomElement() {
    return documentElement;
  }

  public DomContext getDomContext() {
    return documentElement.getDomContext();
  }

  public String getDocumentURI() {
    return documentUri;
  }

  public DOMConfiguration getDomConfig() {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public Element getElementById(String elementId) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public NodeList getElementsByTagName(String tagname) {
    NodeList result = null;

    if (documentElement != null) {
      result = documentElement.doGetElementsByTagName(tagname);
    }

    return result;
  }

  public NodeList getElementsByTagNameNS(String namespaceURI, String localName) {
    NodeList result = null;

    if (documentElement != null) {
      result = documentElement.doGetElementsByTagNameNS(namespaceURI, localName);
    }

    return result;
  }

  public DOMImplementation getImplementation() {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public String getInputEncoding() {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public boolean getStrictErrorChecking() {
    return false;
  }

  public String getXmlEncoding() {
    return xmlEncoding;
  }

  public void setXmlEncoding(String xmlEncoding) {
    this.xmlEncoding = xmlEncoding;
  }

  public boolean getXmlStandalone() {
    return this.xmlStandalone;
  }

  public String getXmlVersion() {
    return this.xmlVersion;
  }

  public Node importNode(Node importedNode, boolean deep) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public void normalizeDocument() {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public Node renameNode(Node n, String namespaceURI, String qualifiedName) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public void setDocumentURI(String documentURI) {
    this.documentUri = documentURI;
  }

  public void setStrictErrorChecking(boolean strictErrorChecking) {
    if (strictErrorChecking) {
      throw new UnsupportedOperationException("Implement when needed.");
    }
  }

  public void setXmlStandalone(boolean xmlStandalone) {
    this.xmlStandalone = xmlStandalone;
  }

  public void setXmlVersion(String xmlVersion) {
    this.xmlVersion = xmlVersion;
  }

  //NOTE: when creating a node through this class, call DomNode.setOwnerDocument(this)


  private static final class NSContainer {

    String namespaceURI;
    String qualifiedName;
    String localName;
    String prefix;

    NSContainer(String namespaceURI, String qualifiedName) {
      this.namespaceURI = namespaceURI;
      this.qualifiedName = qualifiedName;
      this.localName = qualifiedName;
      this.prefix = null;

      final int cPos = qualifiedName.indexOf(':');
      if (cPos >= 0) {
        this.prefix = qualifiedName.substring(0, cPos);
        this.localName = qualifiedName.substring(cPos + 1);
      }
    }

    void addUri(DomNode node) {
      node.addUri(namespaceURI, prefix);
    }
  }
}
