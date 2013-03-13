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


import org.w3c.dom.Text;

/**
 * Wrapper for a dom text node as provided through XmlLite.Text.
 * <p>
 * @author Spence Koehler
 */
public class DomText extends DomNode implements Text {
  
  private String _hyperTrimmedText;

  DomText(XmlLite.Text textData) {
    super(textData, "#text", "#text", textData.text);
  }

  /**
   * Safely, efficiently downcast this DomNode to a DomText if it is one.
   */
  public DomText asDomText() {
    return this;
  }

  public String getTextContent() {
    return nodeValue;
  }

  public String getHyperTrimmedText() {
    if (_hyperTrimmedText == null) {
      _hyperTrimmedText = hyperTrim(nodeValue);
    }
    return _hyperTrimmedText;
  }

  public boolean isAncestor(DomNode descendant, boolean selfIsAncestor) {
    return this.asTree().isAncestor(descendant.asTree(), selfIsAncestor);
  }

  public int getDepth() {
    return this.asTree().depth();
  }

  public short getNodeType() {
    return TEXT_NODE;
  }

  public String getWholeText() {
    throw new UnsupportedOperationException("Implement when needed.");    
  }

  public boolean isElementContentWhitespace() {
    return "".equals(getHyperTrimmedText());
  }

  public Text replaceWholeText(String content) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public Text splitText(int offset) {
    throw new UnsupportedOperationException("Implement when needed.");
  }

  public void appendData(String arg) {
    setNodeValue(nodeValue + arg);
    backref.asText().text = nodeValue;
  }

  public void deleteData(int offset, int count) {
    setNodeValue(nodeValue.substring(0, offset) + nodeValue.substring(offset + count));
    backref.asText().text = nodeValue;
  }

  public int getLength() {
    return nodeValue.length();
  }

  public void insertData(int offset, String arg) {
    setNodeValue(nodeValue.substring(0, offset) + arg + nodeValue.substring(offset));
    backref.asText().text = nodeValue;
  }

  public void replaceData(int offset, int count, String arg) {
    setNodeValue(nodeValue.substring(0, offset) + arg + nodeValue.substring(offset + count));
    backref.asText().text = nodeValue;
  }

  public String getData() {
    return nodeValue;
  }

  public void setData(String data) {
    setNodeValue(data);
    backref.asText().text = nodeValue;
  }

  public String substringData(int offset, int count) {
    return nodeValue.substring(offset, offset + count);
  }

  private static final String hyperTrim(String p) {
    final StringBuilder result = new StringBuilder();

    boolean sawWhite = false;

    for (int charIndex = 0; charIndex < p.length(); ++charIndex) {
      final char c = p.charAt(charIndex);

      final boolean isWhite = Character.isWhitespace(c);

      if (isWhite) {
        sawWhite = true;
      }
      else {
        if (sawWhite && result.length() > 0) result.append(' ');
        result.append(c);
        sawWhite = false;
      }
    }

    return result.toString();
  }
}
