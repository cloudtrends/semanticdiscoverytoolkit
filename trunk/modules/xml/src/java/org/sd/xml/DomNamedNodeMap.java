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


import java.util.Map;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Implementation of the dom NamedNodeMap interface.
 * <p>
 * @author Spence Koehler
 */
public class DomNamedNodeMap implements NamedNodeMap {
  
  private DomElement containingNode;

  private DomNamespaceMap<DomAttribute> attr2node;
  private DomNode[] _attributeNodes;

  DomNamedNodeMap(DomElement containingNode) {
    this.containingNode = containingNode;
    this.attr2node = null;
    this._attributeNodes = null;
  }

  public boolean containsKey(String name) {
    return lookupValue(null, name, name) != null;
  }

  public boolean containsKey(String namespaceURI, String localName) {
    String namespacePrefix = containingNode.getNamespacePrefix(namespaceURI);
    String name = buildName(namespaceURI, localName);
    return (lookupValue(namespacePrefix, localName, name) != null);
  }

  public XmlLite.Tag getTag() {
    return containingNode.getBackReference().asTag();
  }

  public Map<String, String> getAttributes() {
    return getTag().attributes;
  }

  public int getLength() {
    return getAttributes().size();
  }

  public Node getNamedItem(String name) {
    DomAttribute result = attr2node == null ? null : attr2node.get(name);

    if (result == null) {
      String value = lookupValue(null, name, name);

      if (value != null) {
        result = new DomAttribute(containingNode, name, value);

        if (attr2node == null) attr2node = new DomNamespaceMap<DomAttribute>();
        attr2node.put(name, result);
      }
    }

    return result;
  }

  public Node getNamedItemNS(String namespaceURI, String localName) {
    String namespacePrefix = containingNode.getNamespacePrefix(namespaceURI);
    DomAttribute result = attr2node == null ? null : attr2node.get(namespacePrefix, localName);

    if (result == null) {
      String name = buildName(namespaceURI, localName);
      String value = lookupValue(namespacePrefix, localName, name);

      if (value != null) {
        result = new DomAttribute(containingNode, localName, value);

        String defaultPrefix = containingNode.getDefaultNamespacePrefix();
        if (attr2node == null) attr2node = new DomNamespaceMap<DomAttribute>(defaultPrefix);
        else attr2node.setDefaultPrefix(defaultPrefix);

        attr2node.put(namespacePrefix, localName, result);
      }
    }

    return result;
  }

  public Node item(int index) {
    DomNode result = null;

    if (index >= 0 && index < getLength()) {
      DomNode[] attributeNodes = getAttributeNodes(false);
      if (index < attributeNodes.length) {
        result = attributeNodes[index];
      }
    }

    return result;
  }

  public Node removeNamedItem(String name) {
    Node result = getNamedItem(name);

    // remove 'name' from underlying tree
    if (result != null) {
      removeValue(null, name, name);
      attr2node.remove(name);

      _attributeNodes = null;
    }

    return result;
  }

  public Node removeNamedItemNS(String namespaceURI, String localName) {
    Node result = getNamedItemNS(namespaceURI, localName);

    // remove 'name' from underlying tree
    if (result != null) {
      String name = buildName(namespaceURI, localName);
      String namespacePrefix = containingNode.getNamespacePrefix(namespaceURI);
      removeValue(namespacePrefix, localName, name);

      attr2node.remove(namespacePrefix, localName);

      _attributeNodes = null;
    }

    return result;
  }

  public Node setNamedItem(Node arg) {
    Node result = arg;

    final String name = arg.getNodeName();
    final String value = arg.getNodeValue();

    if (setAttribute(name, value)) {
      if (attr2node != null) {
        final Node oldItem = attr2node.remove(name);
      }
    }

    if (arg instanceof DomAttribute) {
      if (attr2node == null) attr2node = new DomNamespaceMap<DomAttribute>();
      attr2node.put(name, (DomAttribute)arg);
    }
    else {
      result = getNamedItem(name);
    }

    if (result != arg) {
      _attributeNodes = null;
    }

    return arg;
  }

  public Node setNamedItemNS(Node arg) {
    Node result = arg;

    final String nsUri = arg.getNamespaceURI();
    final String prefix = arg.lookupPrefix(nsUri);
    final String name = arg.getNodeName();
    final String value = arg.getNodeValue();

    if (setAttributeNS(nsUri, prefix, name, value)) {
      if (attr2node != null) {
        final Node oldItem = attr2node.remove(name);
      }
    }

    if (arg instanceof DomAttribute) {
      if (attr2node == null) attr2node = new DomNamespaceMap<DomAttribute>();
      attr2node.put(name, (DomAttribute)arg);
    }
    else {
      result = getNamedItemNS(nsUri, name);
    }

    if (result != arg) {
      _attributeNodes = null;
    }

    return arg;
  }

  public boolean setAttribute(String name, String value) {
    final boolean changed = setValue(null, name, value);
    if (changed) {
      _attributeNodes = null;
    }
    return changed;
  }

  public boolean setAttributeNS(String nsUri, String prefix, String name, String value) {
    final boolean changed = setValue(prefix, name, value);
    if (changed) {
      _attributeNodes = null;
    }
    return changed;
  }


  private DomNode[] getAttributeNodes(boolean usingNamespaces) {
    if (_attributeNodes == null || _attributeNodes.length != getLength()) {
      _attributeNodes = new DomNode[getLength()];

      int attrIdx = 0;
      for (Map.Entry<String, String> attrEntry : getAttributes().entrySet()) {
        String attrKey = attrEntry.getKey();
        String attrValue = attrEntry.getValue();

        DomAttribute domAttribute = (attr2node == null) ? null : attr2node.get(attrKey);

        if (domAttribute == null) {
          domAttribute = new DomAttribute(containingNode, attrKey, attrValue);

          String defaultPrefix = usingNamespaces ? containingNode.getDefaultNamespacePrefix() : null;
          if (attr2node == null) attr2node = new DomNamespaceMap<DomAttribute>(defaultPrefix);
          else if (defaultPrefix != null) attr2node.setDefaultPrefix(defaultPrefix);
          attr2node.put(attrKey, domAttribute);
        }

        _attributeNodes[attrIdx++] = domAttribute;
      }
    }
    return _attributeNodes;
  }

  private String buildName(String namespaceURI, String localName) {
    String result = localName;

    if (namespaceURI != null) {
      String namespacePrefix = containingNode.getNamespacePrefix(namespaceURI);
      if (namespacePrefix != null) {
        result = namespacePrefix + ":" + localName;
      }
    }

    return result;
  }

  private String lookupValue(String namespacePrefix, String localName, String fullName) {
    String value = null;

    if (getLength() > 0) {
      String defaultPrefix = containingNode.getDefaultNamespacePrefix();

      if (namespacePrefix == null) {
        value = retrieveValue(localName);

        if (value == null && localName.indexOf(':') < 0 && defaultPrefix != null) {
          value = retrieveValue(defaultPrefix + ":" + localName);
        }
      }
      else {
        value = retrieveValue(fullName);

        if (value == null) {
          if (namespacePrefix.equals(defaultPrefix)) {
            value = retrieveValue(localName);
          }
        }
      }
    }

    return value;
  }

  // return true if replacing value for existing name; false if new name
  private boolean setValue(String namespacePrefix, String localName, String value) {
    boolean result = false;

    if (namespacePrefix == null) {
      result = setAttributeValue(localName, value);
    }
    else {
      result = setAttributeValue(namespacePrefix + ":" + localName, value);
    }

    return result;
  }

  /**
   * Retrieve the attribute value from the underlying attribute map.
   *
   * @return null if the attribute doesn't exist, empty string if the
   *         attribute's mapped value is null or empty, or the existing
   *         value.
   */
  private String retrieveValue(String lookupName) {
    String result = null;

    if (hasAttribute(lookupName)) {
      result = getAttributes().get(lookupName);
    }

    return result;
  }

  private boolean hasAttribute(String lookupName) {
    return getAttributes().containsKey(lookupName);
  }

  private boolean setAttributeValue(String lookupName, String value) {
    final Map<String, String> attrMap = getAttributes();
    final boolean result = attrMap.containsKey(lookupName) && !value.equals(attrMap.get(lookupName));

    attrMap.put(lookupName, value);

    if (result) {
      _attributeNodes = null;
    }

    return result;
  }

  private void removeValue(String namespacePrefix, String localName, String fullName) {
    if (getLength() > 0) {
      String defaultPrefix = containingNode.getDefaultNamespacePrefix();

      if (namespacePrefix == null) {
        if (hasAttribute(localName)) {
          getAttributes().remove(localName);
        }
        else if (localName.indexOf(':') < 0 && defaultPrefix != null) {
          getAttributes().remove(defaultPrefix + ":" + localName);
        }
      }
      else {
        if (hasAttribute(fullName)) {
          getAttributes().remove(fullName);
        }
        else if (namespacePrefix.equals(defaultPrefix)) {
          getAttributes().remove(localName);
        }
      }
    }
  }
}
