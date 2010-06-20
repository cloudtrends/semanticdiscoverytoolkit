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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.sd.util.tree.Tree;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

/**
 * Wrapper for a dom node as provided through XmlLite.Data.
 * <p>
 * @author Spence Koehler
 */
public abstract class DomNode implements Node {
  
  protected XmlLite.Data backref;
  protected String data;
  protected String nodeName;
  protected String nodeValue;

  private DomDocument _ownerDocument;
  private List<DomNode> _domChildNodes;

  private boolean computedXmlns;
  private String _xmlns;
  private Map<String, String> nsUri2Prefix;

  DomNode(XmlLite.Data backref, String data, String nodeName, String nodeValue) {
    this.backref = backref;
    this.data = data;

    this.nodeName = nodeName;
    this.nodeValue = nodeValue;
    this._domChildNodes = null;

    this.computedXmlns = false;
    this._xmlns = null;
    this.nsUri2Prefix = null;
  }

  XmlLite.Data getBackReference() {
    return backref;
  }

  public Tree<XmlLite.Data> asTree() {
    return backref.getContainer();
  }

  public String getNamespacePrefix(String namespaceURI) {
    String result = null;

    decodeXmlns();
    if (nsUri2Prefix != null && namespaceURI != null) {
      result = nsUri2Prefix.get(namespaceURI);
    }

    if (result == null && getParent() != null) {
      result = getParent().getNamespacePrefix(namespaceURI);
    }

    return result;
  }

  DomNode getParent() {
    DomNode result = null;

    Tree<XmlLite.Data> parentTree = asTree().getParent();
    if (parentTree != null) {
      result = parentTree.getData().asDomNode();
    }

    return result;
  }

  public String getDefaultNamespacePrefix() {
    String result = null;

    final String uri = getNamespaceURI();
    if (uri != null) {
      result = getNamespacePrefix(uri);
    }

    return result;
  }


  public Node appendChild(Node newChild) {
    DomNode theNewChild = null;
    short errorCode = DOMException.NOT_SUPPORTED_ERR;

    final short myNodeType = this.getNodeType();
    if (myNodeType != DOCUMENT_NODE && myNodeType != ELEMENT_NODE) {
      errorCode = DOMException.HIERARCHY_REQUEST_ERR;
    }
    else {
      if (newChild instanceof DomNode) {
        theNewChild = (DomNode)newChild;
        final short childNodeType = theNewChild.getNodeType();

        if (childNodeType != DOCUMENT_NODE &&
            (childNodeType == myNodeType ||
             myNodeType == DOCUMENT_NODE && childNodeType == ELEMENT_NODE)) {

          if (getOwnerDocument() != theNewChild.getOwnerDocument()) {
            theNewChild = null;
            errorCode = DOMException.WRONG_DOCUMENT_ERR;
          }
        }
        else {
          theNewChild = null;
          errorCode = DOMException.HIERARCHY_REQUEST_ERR;
        }
      }
    }

    if (theNewChild != null) {
      addChild(theNewChild);
    }
    else {
      throw new DOMException(errorCode, "Can't append child.");
    }

    return theNewChild;
  }

  void addChild(DomNode childNode) {
    this.asTree().addChild(childNode.asTree());

    if (_domChildNodes != null) {
      _domChildNodes.add(childNode);
    }
  }

  public Node cloneNode(boolean deep) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public short compareDocumentPosition(Node other) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public NamedNodeMap getAttributes() {
    // NOTE: only Element returns non-null.
    return null;
  }

  public String getBaseURI() {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public NodeList getChildNodes() {
    return new DomNodeList(getDomChildNodes());
  }

  public Object getFeature(String feature, String version) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public Node getFirstChild() {
    Node result = null;

    final List<DomNode> domChildNodes = getDomChildNodes();
    if (domChildNodes != null && domChildNodes.size() > 0) {
      result = domChildNodes.get(0);
    }

    return result;
  }

  public Node getLastChild() {
    Node result = null;

    final List<DomNode> domChildNodes = getDomChildNodes();
    if (domChildNodes != null && domChildNodes.size() > 0) {
      result = domChildNodes.get(domChildNodes.size() - 1);
    }

    return result;
  }

  public String getLocalName() {
    // null for anything other than Element and Attr nodes
    return null;
  }

  public String getNamespaceURI() {
    decodeXmlns();
    if (_xmlns == null && getParent() != null) {
      _xmlns = ((DomNode)getParent()).getNamespaceURI();
    }
    return _xmlns;
  }

  /**
   * Decode this node's xmlns info (non-recursive).
   */
  private void decodeXmlns() {
    // see http://www.w3.org/TR/1999/REC-xml-names-19990114/
    if (!computedXmlns) {
      if (hasAttributes()) {
        for (Map.Entry<String, String> mapEntry : backref.asTag().attributes.entrySet()) {
          final String key = mapEntry.getKey();
          final String value = mapEntry.getValue();

          if (key.startsWith("xmlns")) {
            String prefix = "";

            final int cPos = key.indexOf(':');
            if (cPos >= 0) {
              prefix = key.substring(cPos + 1);
            }
            else {
              final int vcPos = value.lastIndexOf(':');
              if (vcPos >= 0) {
                prefix = value.substring(vcPos + 1);
              }

              // set namespace URI to be that without any prefix
              _xmlns = value;
            }

            String uri = (value == null) ? "" : value;

            addUri(uri, prefix);

            // default namespace URI to be the first xmlns uri
            if (_xmlns == null) _xmlns = uri;
          }
        }
      }
      computedXmlns = true;
    }
  }

  public Node getNextSibling() {
    DomNode result = null;

    final Tree<XmlLite.Data> sibTree = asTree().getNextSibling();
    if (sibTree != null) {
      result = sibTree.getData().asDomNode();
    }
    
    return result;
  }

  public String getNodeName() {
    return nodeName;
  }

  public String getNodeValue() {
    return nodeValue;
  }

  public Document getOwnerDocument() {
    if (_ownerDocument == null) {
      final Tree<XmlLite.Data> myTree = this.asTree();

      if (myTree.getParent() != null) {
        _ownerDocument = (DomDocument)this.asTree().getRoot().getData().asDomNode().getOwnerDocument();
      }
      else {
        _ownerDocument = getOwnerDomDocument();
      }
    }
    return _ownerDocument;
  }

  DomDocument getOwnerDomDocument() {
    if (_ownerDocument == null) {
      _ownerDocument = new DomDocument(backref.asTag());
    }
    return _ownerDocument;
  }

  void setOwnerDocument(DomDocument ownerDocument) {
    // only called from DomDocument.
    this._ownerDocument = ownerDocument;
  }

  public Node getParentNode() {
    return (DomNode)getParent();
  }

  public String getPrefix() {
    // only non-null for Element and Attr nodes
    return null;
  }

  public Node getPreviousSibling() {
    DomNode result = null;

    final Tree<XmlLite.Data> sibTree = asTree().getPrevSibling();
    if (sibTree != null) {
      result = sibTree.getData().asDomNode();
    }
    
    return result;
  }

  public String getTextContent() {
    // special impls for element, attr, entity, entityReference, documentFragment; text, cdataSection, comment, processingInstruction
//todo: for 1st group implement as concatenation of textContent of every child node; for 2nd group return nodeValue
    return null;
  }

  public Object getUserData(String key) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public boolean hasAttributes() {
    // only possible for Element nodes.
    return false;
  }

  public boolean hasChildNodes() {
    return this.asTree().hasChildren();
  }

  public Node insertBefore(Node newChild, Node refChild) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public boolean isDefaultNamespace(String namespaceURI) {
    boolean result = false;

    final String defaultNamespaceURI = getNamespaceURI();
    if (namespaceURI == null) {
      result = defaultNamespaceURI == null;
    }
    else {
      result = namespaceURI.equals(defaultNamespaceURI);
    }

    return result;
  }

  public boolean isEqualNode(Node arg) {
    throw new UnsupportedOperationException("Implement whe needed.");
  }

  public boolean isSameNode(Node other) {
    return this == other;
  }

  public boolean isSupported(String feature, String version) {
    // implement when needed.
    return false;
  }

  public String lookupNamespaceURI(String prefix) {
//...todo: implement this similarly to getNamespacePrefix
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public String lookupPrefix(String namespaceURI) {
    return getNamespacePrefix(namespaceURI);
  }

  public void normalize() {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public Node removeChild(Node oldChild) {
    Node result = null;

    DomNode oldChildNode = (DomNode)oldChild;
    if (oldChildNode.getParent() == this) {
      oldChildNode.asTree().prune();

      if (_domChildNodes != null) {
        _domChildNodes.remove(oldChildNode);
      }

      result = oldChild;
    }
    
    return result;
  }

  public Node replaceChild(Node newChild, Node oldChild) {
    Node result = null;

    final Tree<XmlLite.Data> myTree = this.asTree();
    final DomNode oldChildNode = (DomNode)oldChild;
    final Tree<XmlLite.Data> oldChildTree = oldChildNode.asTree();

    if (myTree == oldChildTree.getParent()) {
      final int childPos = oldChildTree.getSiblingPosition();
      if (childPos >= 0) {
        final DomNode newChildNode = (DomNode)newChild;
        final Tree<XmlLite.Data> newChildTree = newChildNode.asTree();
        myTree.setChild(childPos, newChildTree);

        if (_domChildNodes != null) {
          _domChildNodes.set(childPos, newChildTree.getData().asDomNode());
        }

        result = oldChild;
      }
    }

    return result;
  }

  public void setNodeValue(String nodeValue) {
    this.nodeValue = nodeValue;
  }

  public void setPrefix(String prefix) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public void setTextContent(String textContent) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public Object setUserData(String key, Object data, UserDataHandler handler) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  protected void addUri(String uri, String prefix) {
    if (nsUri2Prefix == null) nsUri2Prefix = new HashMap<String, String>();
    nsUri2Prefix.put(uri, prefix);
  }

  protected void setNamespaceURI(String namespaceURI) {
    _xmlns = namespaceURI;
  }

  protected NodeList doGetElementsByTagName(String tagname) {
    final List<DomNode> domNodes = new ArrayList<DomNode>();

    final boolean selectAll = "*".equals(tagname);

    final Tree<XmlLite.Data> myTree = this.asTree();
    for (Iterator<Tree<XmlLite.Data>> it = myTree.iterator(Tree.Traversal.DEPTH_FIRST); it.hasNext(); ) {
      final Tree<XmlLite.Data> node = it.next();
      final XmlLite.Tag tag = node.getData().asTag();
      if (tag != null) {
        if (selectAll || tagname.equals(tag.name)) {
          domNodes.add(tag.asDomNode());
        }
      }
    }

    return new DomNodeList(domNodes);
  }

  protected NodeList doGetElementsByTagNameNS(String namespaceURI, String localName) {
    final List<DomNode> domNodes = new ArrayList<DomNode>();

    final boolean nsSelectAll = "*".equals(namespaceURI);
    final boolean lnSelectAll = "*".equals(localName);
    String prefix = null;

    final Tree<XmlLite.Data> myTree = this.asTree();
    for (Iterator<Tree<XmlLite.Data>> it = myTree.iterator(Tree.Traversal.DEPTH_FIRST); it.hasNext(); ) {
      final Tree<XmlLite.Data> node = it.next();
      final XmlLite.Tag tag = node.getData().asTag();
      if (tag != null) {
        final DomNode curElement = tag.asDomNode();
        if (lnSelectAll || localName.equals(curElement.getLocalName())) {
          if (prefix == null) prefix = curElement.lookupPrefix(namespaceURI);
          if (prefix != null) {
            if (nsSelectAll || prefix.equals(curElement.getPrefix())) {
              domNodes.add(curElement);
            }
          }
        }
      }
    }

    return new DomNodeList(domNodes);
  }

  protected final List<DomNode> getDomChildNodes() {
    if (_domChildNodes == null) {
      final Tree<XmlLite.Data> myTree = this.asTree();
      if (myTree.hasChildren()) {
        _domChildNodes = new ArrayList<DomNode>();
        for (Tree<XmlLite.Data> child : myTree.getChildren()) {
          _domChildNodes.add(child.getData().asDomNode());
        }
      }
    }
    return _domChildNodes;
  }
}
