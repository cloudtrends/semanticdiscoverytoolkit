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


import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;
import org.sd.util.tree.Tree;

/**
 * Wrapper for a dom element node as provided through XmlLite.Tag.
 * <p>
 * @author Spence Koehler
 */
public class DomElement extends DomNode implements Element {
  
  private String _localName;
  private DomNamedNodeMap _attributes;
  private String _textContent;

  DomElement(XmlLite.Tag tagData) {
    super(tagData, tagData.name, tagData.name, null);

    this._localName = null;
    this._attributes = null;
    this._textContent = null;
  }

  /**
   * Safely, efficiently downcast this node to a DomElement if it is one.
   */
  public DomElement asDomElement() {
    return this;
  }

  /**
   * Get this element's text content.
   *
   * NOTE: Original whitespace formatting is *NOT* preserved through this
   *       implementation. Whitespace across nodes is normalized to a single
   *       space between nodes (even when there was no whitespace between the
   *       nodes when the DOM is loaded. Whitespace within nodes is left as
   *       found. That is, it is *not*  hypertrimmed.
   */
  public String getTextContent() {
    if (_textContent == null) {
      final Tree<XmlLite.Data> myTree = asTree();
      if (myTree != null) {
        final StringBuilder result = new StringBuilder();
        final List<Tree<XmlLite.Data>> leaves = myTree.gatherLeaves();
        for (Tree<XmlLite.Data> leaf : leaves) {
          final XmlLite.Text text = leaf.getData().asText();
          if (text != null && text.text != null && !"".equals(text.text)) {
            if (result.length() > 0) result.append(' ');
            result.append(text.text);
          }
        }
        _textContent = result.toString();
      }
      else {
        _textContent = getNodeValue();
      }
    }
    return _textContent;
  }

  public StringBuilder asFlatString(StringBuilder result) {
    if (result == null) result = new StringBuilder();
    final Tree<XmlLite.Data> xmlTree = asTree();
    asString(xmlTree, result, -1, -1);
    return result;
  }

  public StringBuilder asPrettyString(StringBuilder result, int indentLevel, int indentSpaces) {
    if (result == null) result = new StringBuilder();
    final Tree<XmlLite.Data> xmlTree = asTree();
    asString(xmlTree, result, indentLevel, indentSpaces);
    return result;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();
    asPrettyString(result, 0, 2);
    return result.toString();
  }

  private final void asString(Tree<XmlLite.Data> xmlTree, StringBuilder result, int indentLevel, int indentSpaces) {
    if (xmlTree == null) return;

    final XmlLite.Tag tag = xmlTree.getData().asTag();
    if (tag != null) {
      final int indent = (indentLevel >= 0) ? indentLevel * indentSpaces : -1;
      if (indent >= 0) {
        if (result.length() > 0 && result.charAt(result.length() - 1) != '\n') result.append('\n');
        addIndent(result, indent);
      }
      result.append(tag.toString());

      if (indent >= 0 && tag.isSelfTerminating()) result.append('\n');

      // recurse on children
      if (xmlTree.hasChildren()) {
        final int childIndentLevel = (indentLevel < 0) ? indentLevel : indentLevel + 1;
        for (Tree<XmlLite.Data> child : xmlTree.getChildren()) {
          asString(child, result, childIndentLevel, indentSpaces);
        }
      }

      // close tag (if necessary)
      if (!tag.isSelfTerminating()) {
        if (indent >= 0 && result.charAt(result.length() - 1) == '\n') addIndent(result, indent);
        result.append("</").append(tag.name).append('>');
        if (indent >= 0) result.append('\n');
      }
    }
    else {
      final XmlLite.Text text = xmlTree.getData().asText();
      if (text != null) {
        // add a space between e.g., consecutive text nodes
        final int resultLen = result.length();
        if (resultLen > 0) {
          final char lastc = result.charAt(resultLen - 1);
          if (lastc != '>' && lastc != ' ') {
            result.append(' ');
          }
        }

        // add the node's text
        result.append(XmlUtil.escape(text.text));
      }
    }
  }

  private final void addIndent(StringBuilder result, int indent) {
    for (int i = 0; i < indent; ++i) result.append(' ');
  }

  public NamedNodeMap getAttributes() {
    return getDomAttributes();
  }

  public DomNamedNodeMap getDomAttributes() {
    if (_attributes == null) {
      _attributes = new DomNamedNodeMap(this);
    }
    return _attributes;
  }

  /**
   * Special utility for getting a string representing this element's node name
   * and attributes, suitable for use in, e.g. XmlStringBuilder.
   */
  public String getTagAndAttributeString() {
    return getTagAndAttributeString(null);
  }

  /**
   * Special utility for getting a string representing this element's attributes
   * and a new node name, suitable for use in, e.g. XmlStringBuilder.
   */
  public String getTagAndAttributeString(String newNodeName) {
    final StringBuilder result = new StringBuilder();
    if (newNodeName == null || "".equals(newNodeName)) newNodeName = getNodeName();
    
    result.append(newNodeName);
    if (hasAttributes()) {
      for (Map.Entry<String, String> entry : getDomAttributes().getAttributes().entrySet()) {
        result.
          append(' ').
          append(entry.getKey()).
          append("=\"").
          append(StringEscapeUtils.escapeXml(entry.getValue())).
          append('"');
      }
    }

    return result.toString();
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

  public String getAttributeValue(String attributeName) {
    final String result = getAttributeValue(attributeName, null);

    if (result == null) {
      throw new IllegalArgumentException("Element '" + getLocalName() + " is missing required attribute '" + attributeName + "'!");
    }

    return result;
  }

  public String getAttributeValue(String attributeName, String defaultValue) {
    String result = null;

    if (hasAttributes()) {
      result = backref.asTag().attributes.get(attributeName);
    }

    return result == null ? defaultValue : result;
  }

  public boolean getAttributeBoolean(String attributeName) {
    final String result = getAttributeValue(attributeName, null);

    if (result == null) {
      throw new IllegalArgumentException("Element '" + getLocalName() + "' is missing required attribute '" + attributeName + "'!");
    }

    return "true".equalsIgnoreCase(result);
  }

  public boolean getAttributeBoolean(String attributeName, boolean defaultValue) {
    final String result = getAttributeValue(attributeName, null);
    return result == null ? defaultValue : "true".equalsIgnoreCase(result);
  }

  public int getAttributeInt(String attributeName) {
    final String result = getAttributeValue(attributeName, null);

    if (result == null) {
      throw new IllegalArgumentException("Element '" + getLocalName() + "' is missing required attribute '" + attributeName + "'!");
    }

    return Integer.parseInt(result);
  }

  public int getAttributeInt(String attributeName, int defaultValue) {
    final String result = getAttributeValue(attributeName, null);
    return result == null ? defaultValue : Integer.parseInt(result);
  }

  public long getAttributeLong(String attributeName) {
    final String result = getAttributeValue(attributeName, null);

    if (result == null) {
      throw new IllegalArgumentException("Element '" + getLocalName() + "' is missing required attribute '" + attributeName + "'!");
    }

    return Long.parseLong(result);
  }

  public long getAttributeLong(String attributeName, long defaultValue) {
    final String result = getAttributeValue(attributeName, null);
    return result == null ? defaultValue : Long.parseLong(result);
  }

  public boolean isAncestor(DomNode descendant, boolean selfIsAncestor) {
    return this.asTree().isAncestor(descendant.asTree(), selfIsAncestor);
  }

  public int getDepth() {
    return this.asTree().depth();
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
      markAsModified();
    }
  }

  public Attr removeAttributeNode(Attr oldAttr) {
    Attr result = null;

    if (hasAttributes()) {
      final DomNamedNodeMap attributes = getDomAttributes();
      result = (Attr)attributes.removeNamedItem(oldAttr.getName());
      markAsModified();
    }

    return result;
  }

  public void removeAttributeNS(String namespaceURI, String localName) {
    if (hasAttributes()) {
      final DomNamedNodeMap attributes = getDomAttributes();
      attributes.removeNamedItemNS(namespaceURI, localName);
      markAsModified();
    }
  }

  public void setAttribute(String name, String value) {
    final DomAttribute attr = getOwnerDomDocument().createDomAttribute(this, name, value);
    setAttributeNode(attr);
    markAsModified();
  }

  public Attr setAttributeNode(Attr newAttr) {
    //NOTE: newAttr's values are used and a new Attr instance is created!
    final DomNamedNodeMap attributes = getDomAttributes();
    markAsModified();
    return (Attr)attributes.setNamedItem(newAttr);
  }

  public Attr setAttributeNodeNS(Attr newAttr) {
    //NOTE: newAttr's values are used and a new Attr instance is created!
    final DomNamedNodeMap attributes = getDomAttributes();
    markAsModified();
    return (Attr)attributes.setNamedItemNS(newAttr);
  }

  public void setAttributeNS(String namespaceURI, String qualifiedName, String value) {
    final DomAttribute attr = (DomAttribute)(getOwnerDocument().createAttributeNS(namespaceURI, qualifiedName));
    attr.setValue(value);
    setAttributeNodeNS(attr);
    markAsModified();
  }

  public void setIdAttribute(String name, boolean isId) {
    markAsModified();
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public void setIdAttributeNode(Attr idAttr, boolean isId) {
    markAsModified();
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) {
    markAsModified();
    throw new UnsupportedOperationException("Implement when needed.");
  }
}
