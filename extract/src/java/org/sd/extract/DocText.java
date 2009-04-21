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
package org.sd.extract;


import org.sd.nlp.BreakStrategy;
import org.sd.nlp.GeneralBreakStrategy;
import org.sd.nlp.StringWrapper;
import org.sd.util.tree.Tree;
import org.sd.xml.PathKeyBuilder;
import org.sd.xml.TagStack;
import org.sd.xml.XmlData;
import org.sd.xml.XmlFactory;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Container for text from a document as served through a text container.
 * <p>
 * @author Spence Koehler
 */
public class DocText {

  private final TextContainer textContainer;

  private XmlData xmlData;
  private Map<String, Extraction> type2extraction;

  //lazily loaded cache variables
  private String _string = null;
  private String[] _strings = null;
  private StringWrapper _defaultStringWrapper = null;
  private Map<BreakStrategy, StringWrapper> _bs2sw = null;
  private String _pathKey = null;
  private Map<String, String> properties = null;

  /**
   * Utility method to turn an xmlString into a docText.
   */
  public static DocText makeDocText(String xmlString) throws IOException {
    return new DocText(null, new XmlData(1, XmlFactory.buildXmlTree(xmlString, false, false), null, null));
  }

  /**
   * Construct an instance, usually from a text container using an xml node ripper.
   */
  public DocText(TextContainer textContainer, XmlData xmlData) {
    this.textContainer = textContainer;
    this.xmlData = (xmlData == null) ? new XmlData(0, null, null, null) : xmlData;
    this.type2extraction = null;
  }

  public DocText(TextContainer textContainer, String string, boolean isXml) {
    this.textContainer = textContainer;
    this.xmlData = null;

    if (isXml) {
      try {
        this.xmlData = new XmlData(1, XmlFactory.buildXmlTree(string, false, false), null, null);
      }
      catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    }
    else {
      this._string = string;
    }
  }

  /**
   * Get this instance's text container.
   */
  public TextContainer getTextContainer() {
    return textContainer;
  }

  public XmlData getXmlData() {
    if (xmlData == null && _string != null) {
      try {
        xmlData = new XmlData(1, XmlFactory.buildXmlTree(_string, false, false), null, null);
      }
      catch (IOException e) {
        throw new IllegalArgumentException(e);
      }
    }
    return xmlData;
  }

  /**
   * Get this instance's tag stack.
   */
  public TagStack getTagStack() {
    return xmlData.tagStack;
  }

  /**
   * Get this instance's xml node.
   */
  public Tree<XmlLite.Data> getXmlNode() {
    return xmlData.xmlNode;
  }

  /**
   * Get this instance's path index in its document.
   */
  public int getPathIndex() {
    return xmlData.index;
  }

  /**
   * Get this instance's xml node's text string.
   * <p>
   * @return the non-null but possibly empty text string.
   */
  public String getString() {
    if (_string == null) {
      _string = (xmlData.xmlNode != null) ? XmlTreeHelper.getAllText(xmlData.xmlNode) : "";
    }
    return _string;
  }

  /**
   * Get this instance's xml node's text strings in document order.
   */
  public String[] getStrings() {
    if (_strings == null) {
      _strings = XmlTreeHelper.getEachText(xmlData.xmlNode);
    }
    return _strings;
  }

  /**
   * Get the default stringWrapper for this text.
   * <p>
   * Note that the default break strategy is a org.sd.nlp.GeneralBreakStrategy.
   */
  public StringWrapper getStringWrapper() {
    if (_defaultStringWrapper == null) {
      _defaultStringWrapper = getStringWrapper(GeneralBreakStrategy.getInstance());
    }
    return _defaultStringWrapper;
  }

  /**
   * Get the stringWrapper for this text using the given breakStrategy.
   * <p>
   * If the breakStrategy is null, then the default GeneralBreakStrategy will
   * be used.
   */
  public StringWrapper getStringWrapper(BreakStrategy breakStrategy) {
    if (breakStrategy == null) breakStrategy = GeneralBreakStrategy.getInstance();
    if (_bs2sw == null) _bs2sw = new HashMap<BreakStrategy, StringWrapper>();

    StringWrapper result = _bs2sw.get(breakStrategy);
    if (result == null) {
      result = new StringWrapper(getString(), breakStrategy);
      _bs2sw.put(breakStrategy, result);
    }

    return result;
  }

  /**
   * Get this text's path key.
   * <p>
   * The path key is of the form t1.t2....tN, where t1 is the document's root
   * tag name, tN is the xml node's tag name if the xml node contains a tag
   * or the tag path's deepest tag if the xml node is a text node. The remaining
   * t's are the tags leading from t1 to tN.
   */
  public String getPathKey() {
    if (_pathKey == null) {
      if (xmlData.tagStack == null) {
        _pathKey = "";
      }
      else {
        final PathKeyBuilder result = new PathKeyBuilder();
        result.add(xmlData.tagStack.getPathKey());
        final XmlLite.Tag tag = xmlData.xmlNode.getData().asTag();
        if (tag != null) {
          result.add(tag.name);
        }
        _pathKey = result.getPathKey();
      }
    }
    return _pathKey;
  }

  /**
   * Get this instance's path key with the path to the node in its tree appended.
   */
  public String getPathKey(Tree<XmlLite.Data> node) {
    final PathKeyBuilder result = new PathKeyBuilder();
    result.add(getPathKey());

    final LinkedList<XmlLite.Tag> tags = new LinkedList<XmlLite.Tag>();

    for (Tree<XmlLite.Data> walkUp = node; walkUp != null; walkUp = walkUp.getParent()) {
      final XmlLite.Tag tag = walkUp.getData().asTag();
      if (tag != null) {
        tags.addFirst(tag);
      }
    }
    result.addAll(tags);

    return result.getPathKey();
  }

  /**
   * Get the deepest common data (tag) between this and the other doc texts.
   *
   * @return the common data (tag) or null if the other is from another document.
   */
  public XmlLite.Data getCommonData(DocText other) {
    XmlLite.Data result = null;

    // check the root of the trees for common data.
    if (xmlData.xmlNode.getData() == other.xmlData.xmlNode.getData()) {
      result = xmlData.xmlNode.getData();
    }
    else {
      final TagStack otherTagStack = other.getTagStack();
      result = xmlData.tagStack.getDeepestCommonTag(otherTagStack);
    }

    return result;
  }

  /**
   * Determine whether this doc text instance has the given data instance
   * in its tag stack or xml node.
   */
  public boolean hasData(XmlLite.Data xmlData) {
    boolean result = false;

    final XmlLite.Tag xmlTag = xmlData.asTag();
    if (xmlTag != null) {
      result = this.xmlData.tagStack.hasTag(xmlTag) >= 0;

      if (!result) {
        // check in the tree
        for (Iterator<Tree<XmlLite.Data>> iter = this.xmlData.xmlNode.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
          final Tree<XmlLite.Data> curNode = iter.next();
          if (curNode.getData() == xmlData) {
            result = true;
            break;
          }
        }
      }
    }

    return result;
  }

  /**
   * Clear this instance's lazily loaded caches.
   */
  public void compact() {
    this._string = null;
    this._strings = null;
    this._defaultStringWrapper = null;
    this._bs2sw = null;
    this._pathKey = null;
  }

  /**
   * Get the extraction for the given type set on this instance.
   *
   * @return the extraction of the given type or null.
   */
  public Extraction getExtraction(String extractionType) {
    Extraction result = null;
    if (type2extraction != null) {
      result = type2extraction.get(extractionType);
    }
    return result;
  }

  /**
   * Determine whether this docText has any extractions.
   *
   * @return true if this docText has any extractions; otherwise, false.
   */
  public boolean hasExtraction() {
    return type2extraction != null && type2extraction.size() > 0;
  }

  /**
   * Get the types of extractions set on this instance or null if none are set.
   *
   * @return the extraction types or null.
   */
  public Set<String> getExtractionTypes() {
    return type2extraction != null ? type2extraction.keySet() : null;
  }

  /**
   * Set a property in this text container.
   */
  public void setProperty(String propertyName, String propertyValue) {
    if (properties == null) properties = new HashMap<String, String>();
    properties.put(propertyName, propertyValue);
  }

  /**
   * Retrieve a property from this container.
   */
  public String getProperty(String propertyName) {
    return (properties == null) ? null : properties.get(propertyName);
  }

  /**
   * Clear this container's properties.
   */
  public void clearProperties() {
    properties = null;
  }

  /**
   * Get this container's properties.
   */
  public Map<String, String> getProperties() {
    return properties;
  }

  public TextContainer asTextContainer(boolean keepDocTexts) {
    TextContainer result = null;

    if (textContainer != null) {
      result = textContainer.convertToTextContainer(this, keepDocTexts);
    }

    return result;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    String xmlString = null;
    try {
      xmlString = XmlLite.asXml(xmlData.xmlNode, false).replaceAll("\\s+", " ").trim();
    }
    catch (IOException e) {
      xmlString = e.toString();
    }

    result.
      append(xmlData.index).append('|').
      append(xmlData.tagStack).append('|').
      append(xmlString).append('|').
      append(XmlTreeHelper.getAllText(xmlData.xmlNode)).append('|').
      append(getExtractionTypes());

    return result.toString();
  }

  void setExtraction(Extraction extraction) {
    if (extraction != null) {
      if (type2extraction == null) {
        type2extraction = new HashMap<String, Extraction>();
        xmlData.xmlNode.getData().setProperty(ExtractionProperties.EXTRACTIONS_PROPERTY, type2extraction);
      }
      type2extraction.put(extraction.getExtractionType(), extraction);
    }
  }


  /**
   * Utility method for merging a list of docTexts from the same underlying
   * document into an xml tree.
   */
  public static final Tree<XmlLite.Data> buildXmlTree(List<DocText> docTexts) {
    if (docTexts == null || docTexts.size() == 0) return null;

    final int num = docTexts.size();
    int index = 0;
    DocText docText1 = docTexts.get(index++);
    TagStack tagStack1 = docText1.getTagStack();
    Tree<XmlLite.Data> xmlNode1 = docText1.getXmlNode();

    final Tree<XmlLite.Data> result = XmlTreeHelper.buildXmlTree(tagStack1, xmlNode1, 0);

    DocText docText2 = null;
    TagStack tagStack2 = null;
    Tree<XmlLite.Data> xmlNode2 = null;

    // Strategy: XmlTreeHelper.buildXmlTree from first tag stack
    // graft in remainder of tree from next tag stack at the deepest common tag from the most recently grafted tag stack

    while (index < num) {
      docText2 = docTexts.get(index++);
      tagStack2 = docText2.getTagStack();
      xmlNode2 = docText2.getXmlNode();

      final XmlLite.Tag commonTag = tagStack1.getDeepestCommonTag(tagStack2);
      if (commonTag == null) {  // unexpected
        throw new IllegalStateException("No common tag! index1=" +
                                        (index - 2) + "/" + num + ",docText1=" + docText1 +
                                        "; index2=" + (index - 1) + ",docText2=" + docText2);
      }

      final Tree<XmlLite.Data> commonNode = XmlTreeHelper.findNode(result, commonTag);
      final int commonDepth = tagStack2.hasTag(commonTag);
      final Tree<XmlLite.Data> graftNode = XmlTreeHelper.buildXmlTree(tagStack2, xmlNode2, commonDepth + 1);

      if (graftNode == null) {  // unexpected
        throw new IllegalStateException("No graft node! commonTag=" + commonTag + ", index1=" +
                                        (index - 2) + "/" + num + ",docText1=" + docText1 +
                                        "; index2=" + (index - 1) + ",docText2=" + docText2);
      }

      if (!isEmpty(graftNode)) {
        commonNode.addChild(graftNode);
      }

      // shift
      docText1 = docText2;
      tagStack1 = tagStack2;
      xmlNode1 = xmlNode2;
    }

    return result;
  }

  private static final boolean isEmpty(Tree<XmlLite.Data> node) {
    final XmlLite.Text text = node.getData().asText();
    return (text != null && (text.text == null || "".equals(text.text)));
  }
}
