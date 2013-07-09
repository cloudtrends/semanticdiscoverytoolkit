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


import org.apache.commons.lang.StringEscapeUtils;
import org.sd.util.StringSplitter;
import org.sd.util.tree.Tree;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Read in xml as a tree, discarding all comments and normalizing all text.
 * <p>
 *
 * @author Spence Koehler
 */
public class XmlLite {
  
  // note: if changing, do clean compile to take effect in referring classes!
  // note: optional end tags will NOT be preserved through default html "ripping"
  public static final String[] DEFAULT_OPTIONAL_END_TAGS = new String[] {/*"p", */"br", "dir", "hr", "img", "meta", "input", "option", /*"dd", "dt", "li", */"link", /*"a", "embed", "param"*/};
  public static final Set<String> OPTIONAL_END_TAGS = new HashSet<String>();
  static {
    for (String doet : DEFAULT_OPTIONAL_END_TAGS) {
      OPTIONAL_END_TAGS.add(doet);
    }
  }

  // tag    terminated by open or end
  // ---    -------------------------
  // p      p
  // li     li, ul, ol, menu
  // dt     dt, dd, dl
  // dd     dt, dd, dl
  public static final String[][] SPECIAL_RULE_END_TAGS = new String[][] {
    // when see start or end of tag[0], end first open of tag[1+]
    {"p", "p"},
    {"li", "li"},
    {"ul", "li"},
    {"ol", "li"},
    {"menu", "li"},
    {"dt", "dd", "dt"},
    {"dd", "dt", "dd"},
    {"dl", "dd", "dt"},
  };
  public static final Map<String, String[]> SPECIAL_RULE_END_TAG_MAP = new HashMap<String, String[]>();
  static {
    for (String[] sret : SPECIAL_RULE_END_TAGS) {
      SPECIAL_RULE_END_TAG_MAP.put(sret[0], sret);
    }
  }

  private XmlTagParser xmlTagParser;
  private Set<String> ignoreTags;
  private boolean commonCase;

  public XmlLite(XmlTagParser xmlTagParser, Set<String> ignoreTags) {
    this(xmlTagParser, ignoreTags, true);
  }

  public XmlLite(XmlTagParser xmlTagParser, Set<String> ignoreTags, boolean commonCase) {
    this.xmlTagParser = xmlTagParser;
    this.ignoreTags = ignoreTags;
    this.commonCase = commonCase;
  }

  public Tree<Data> parse(XmlInputStream inputStream) throws IOException {
    return parse(inputStream, null);
  }

  public Tree<Data> parse(XmlInputStream inputStream, AtomicBoolean die) throws IOException {
    return readText(inputStream, new StringBuilder(), false, die, false, null);
  }

  public Tree<Data> parse(String xmlString) throws IOException {
    return parse(xmlString, null);
  }

  public Tree<Data> parse(String xmlString, AtomicBoolean die) throws IOException {
    Tree<Data> result = null;
    InputStream inputStream = null;
    XmlInputStream xmlInputStream = null;

    try {
      inputStream = new ByteArrayInputStream(xmlString.getBytes());
      xmlInputStream = new XmlInputStream(inputStream, Encoding.UTF8);
      result = parse(xmlInputStream, die);
    }
    finally {
      if (xmlInputStream != null) xmlInputStream.close();
      if (inputStream != null) inputStream.close();
    }

    return result;
  }

  /**
   * Read the next (top) xml node, without reading its children.
   * <p>
   * This is intended as a mechanism to use for incremental parsing of an
   * xml stream that is too large to fit in memory all at once. Once the top
   * has been read, each child can be read, one at a time, using getNextChild.
   * <p>
   * NOTE: comments will be ignored regardless of this instance's setting.
   */
  public Tree<Data> getTop(XmlInputStream inputStream) throws IOException {
    Tree<Data> root = new Tree<Data>(new Tag("root bogus=\"true\"", commonCase));
    root.getData().setContainer(root);
    Tree<Data> curNode = root;

    final boolean[] keepGoing = new boolean[]{true};
    final StringBuilder data = new StringBuilder();

    //NOTE: this only makes sense when comments are ignored!
    while (keepGoing[0]) {
      if (keepGoing[0]) {
        curNode = doReadText(inputStream, curNode, data, keepGoing);
        if (keepGoing[0]) {
          curNode = readTag(inputStream, curNode, data, keepGoing, true, null);

          // kick out when read first node
          if (root != curNode) {
            if (curNode != null) {
              final Tag tag = curNode.getData().asTag();
              if (tag != null && tag.name.length() > 0) {
                keepGoing[0] = false;
              }
            }
          }
        }
      }
    }

    return curNode;
  }

  /**
   * Read the next xml node.
   * <p>
   * This is intended as a mechanism to use for incremental parsing of an
   * xml stream that is too large to fit in memory all at once. Typically,
   * this is called after a call to getTop.
   *
   * NOTE: comments will be ignored regardless of this instance's setting.
   */
  public Tree<Data> getNextChild(XmlInputStream inputStream) throws IOException {
    return getNextChild(inputStream, null);
  }

  /**
   * Read the next xml node.
   * <p>
   * This is intended as a mechanism to use for incremental parsing of an
   * xml stream that is too large to fit in memory all at once. Typically,
   * this is called after a call to getTop.
   * <p>
   * Usually altTopNode will be null; but if it isn't, it is used as the
   * root of the child being read.
   *
   * NOTE: comments will be ignored regardless of this instance's setting.
   */
  public Tree<Data> getNextChild(XmlInputStream inputStream, Tree<Data> altTopNode) throws IOException {
    final Tree<Data> result = readText(inputStream, new StringBuilder(), true, null, true, altTopNode);
    return result;
  }

  /**
   * Read the full xml node after having read its open tag.
   */
  public void readFullNode(XmlInputStream inputStream, Tree<Data> tagNode) throws IOException {
    if (tagNode != null) {
      final XmlLite.Tag tag = tagNode.getData().asTag();
      if (!tag.isSelfTerminating()) {
        readText(inputStream, new StringBuilder(), false, null, false, tagNode);
      }
    }
  }

  public static void writeXml(Tree<Data> tree, BufferedWriter writer) throws IOException {
    if (tree != null) {
      doWriteXmlNodes(tree, writer, true);
    }
  }

  public static String asXml(Tree<Data> tree, boolean includeXmlHeader) throws IOException {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bytes));

    if (tree != null) {
      doWriteXmlNodes(tree, writer, includeXmlHeader);
    }

    writer.close();
    bytes.close();
    return bytes.toString("UTF-8");
  }

  public static Tree<XmlLite.Data> createTagNode(String tag) {
    final Tree<XmlLite.Data> result = new Tree<XmlLite.Data>(new XmlLite.Tag(tag, false));
    result.getData().setContainer(result);
    return result;
  }

  public static Tree<XmlLite.Data> createTextNode(String text) {
    final Tree<XmlLite.Data> result = new Tree<XmlLite.Data>(new XmlLite.Text(text));
    result.getData().setContainer(result);
    return result;
  }

  public static Set<String> buildTagSet(String[] tags) {
    final Set<String> result = new HashSet<String>();
    for (String tag : tags) result.add(tag);
    return result;
  }

  public static Tree<XmlLite.Data> findTag(Tree<XmlLite.Data> node, Set<String> tagNames) {
    while (node != null) {
      final XmlLite.Data data = node.getData();
      final XmlLite.Tag tag = data.asTag();
      if (tag != null && tagNames.contains(tag.name)) {
        return node;
      }
      node = node.getParent();
    }
    return null;
  }

  public static Tree<XmlLite.Data> findTag(Tree<XmlLite.Data> node, String tagName) {
    while (node != null) {
      final XmlLite.Data data = node.getData();
      final XmlLite.Tag tag = data.asTag();
      if (tag != null) {
        if (tag.commonCase) tagName = tagName.toLowerCase();
        if (tag.name.equals(tagName)) {
          return node;
        }
      }
      node = node.getParent();
    }
    return null;
  }

  public static String simplePathKey(Tree<Data> node) {
    final StringBuilder result = new StringBuilder();
    Tree<Data> curNode = node;
    while (curNode != null) {
      final Data data = curNode.getData();
      if (data.asText() != null) {
        result.append("text=").append(data.asText().text);
      }
      else if (data.asTag() != null) {
        String tagName = data.asTag().name;
        if (result.length() > 0) {
          result.insert(0, tagName + ".");
        }
        else {
          result.append(tagName);
        }
      }
      curNode = curNode.getParent();
    }
    return result.toString();
  }

  private static final void doWriteXmlNodes(Tree<Data> tree, BufferedWriter writer, boolean includeXmlHeader) throws IOException {
    boolean wroteXmlHeader = includeXmlHeader ? false : true;
    final List<Tree<Data>> topNodes = splitTopNodes(tree);

    for (Tree<Data> topNode : topNodes) {
      final boolean needXmlHeader = wroteXmlHeader ? false : !isXmlHeader(topNode);
      if (needXmlHeader) {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.newLine();
      }
      doWriteXml(topNode, writer);
      writer.newLine();
      wroteXmlHeader = true;
    }
  }

  private static final void doWriteXml(Tree<Data> tree, BufferedWriter writer) throws IOException {
    final Data data = tree.getData();
    writer.write(data.toString());
    final List<Tree<Data>> children = tree.getChildren();
    if (children != null) {
      for (Tree<Data> child : children) {
        doWriteXml(child, writer);
      }
    }
    final Tag tag = data.asTag();
    if (tag != null && !tag.isSelfTerminating()) {
      writer.write("</" + tag.name + ">");
    }
  }

  private final static List<Tree<Data>> splitTopNodes(Tree<Data> tree) {
    List<Tree<Data>> result = null;
    final Tag tag = tree.getData().asTag();
    if (tag != null && "true".equals(tag.attributes.get("invented"))) {
      result = tree.getChildren();
    }
    else {
      result = new ArrayList<Tree<Data>>();
      result.add(tree);
    }
    return result;
  }

  private final static boolean isXmlHeader(Tree<Data> node) {
    final Comment comment = node.getData().asComment();
    return comment != null && comment.text.startsWith("?xml ");
  }

  /**
   * Unescape, hypertrim, etc.
   */
  public static String fixText(String text) {
    // convert entities (unescape) first so that i.e. &nbsp; can be trimmed.
    text = EntityConverter.unescape(text);

    // trim all extra whitespace
    text = StringSplitter.hypertrim(text);

    return text;
  }

  private static final String escape(String string) {
    return XmlUtil.escape(string);
    //return StringEscapeUtils.escapeXml(string);
//    return EntityConverter.escape(string);
  }

  private final Tree<Data> readText(XmlInputStream inputStream, StringBuilder data, boolean incremental, AtomicBoolean die, boolean forceIgnoreComments, Tree<XmlLite.Data> altTopNode) throws IOException {
    Tree<Data> topNode = altTopNode;
    if (topNode == null) {
      topNode = new Tree<Data>(new Tag("root bogus=\"true\"", commonCase));
      topNode.getData().setContainer(topNode);
    }
    doReading(inputStream, topNode, data, incremental, die, forceIgnoreComments, altTopNode);

    if (altTopNode == null) {
      final List<Tree<Data>> children = topNode.getChildren();
      if (children == null || children.size() == 0) {
        topNode = null;
      }
      else {
        Tree<Data> onlyTagChild = null;
        for (Tree<Data> child : children) {
          if (child.getData().asTag() != null) {
            if (onlyTagChild == null) {
              onlyTagChild = child;
            }
            else {
              onlyTagChild = null;
              break;
            }
          }
        }
        if (onlyTagChild != null) {
          topNode = onlyTagChild;
          topNode.prune();
        }
      }
    }

    return topNode;
  }

  private final void doReading(XmlInputStream inputStream, Tree<Data> curNode, StringBuilder data, boolean incremental, AtomicBoolean die, boolean forceIgnoreComments, Tree<Data> stopNode) throws IOException {
    final Tree<Data> origNode = curNode;

    boolean[] keepGoing = new boolean[]{true};
    while (keepGoing[0] && (die == null || !die.get())) {
      curNode = doReadText(inputStream, curNode, data, keepGoing);
      if (keepGoing[0]) {
        curNode = readTag(inputStream, curNode, data, keepGoing, forceIgnoreComments, stopNode);

        // kick out now when incremental
        if (incremental && curNode == origNode) {
          final Tag tag = curNode.getData().asTag();
          if (tag != null && tag.name.length() > 0) {
            keepGoing[0] = false;
          }
        }
      }
    }
  }

  private final Tree<Data> doReadText(XmlInputStream inputStream, Tree<Data> curNode, StringBuilder data, boolean[] keepGoing) throws IOException {
    keepGoing[0] = (inputStream.readToChar('<', data, -1) >= 0);
    final String text = getBuiltText(data, curNode, true);

    if (text.length() > 0) {
      if (curNode != null) {
        addTextUnder(curNode, text);
      }
      else if (!keepGoing[0]) {
        final Text textData = new Text(text);
        curNode = new Tree<Data>(textData);
        textData.setContainer(curNode);
      }
    }

    return curNode;
  }

  private final Tree<Data> readTag(XmlInputStream inputStream, Tree<Data> curNode, StringBuilder data, boolean[] keepGoing, boolean forceIgnoreComments, Tree<Data> stopNode) throws IOException {
    final XmlTagParser.TagResult tagResult = xmlTagParser.readTag(inputStream, data, forceIgnoreComments, commonCase);

    if (tagResult != null) {

      // ignore (skip over) comment
      if (tagResult.hasComment()) {
        final Comment comment = tagResult.getComment();
        final Tree<Data> commentNode = new Tree<Data>(comment);
        if (curNode == null) {
          curNode = new Tree<Data>(new Tag("xml invented=\"true\"", commonCase));
          curNode.getData().setContainer(curNode);
        }
        curNode.addChild(commentNode);
      }

      // deal with end tag
      else if (tagResult.hasEndTag() && !tagResult.hasTag()) {
        final String endTag = tagResult.getEndTag();
        Tree<Data> tagNode = findTag(curNode, endTag);

        if (tagNode == null) 
        {
          // didn't find a tag, make a self-terminating version so
          // that we get a break between text nodes.
          final Tag tag = new Tag(endTag, commonCase);
          tag.setSelfTerminating();
          tagNode = new Tree<Data>(tag);
          tag.setContainer(tagNode);
          curNode.addChild(tagNode);
        }
        else { // (tagNode != null)
          curNode = closeTag(tagNode);

          if (stopNode != null && tagNode == stopNode) {
            keepGoing[0] = false;
            return curNode;
          }
        }
      }

      // deal with script
      else if (tagResult.hasScript()) {
        final Script script = tagResult.getScript();
        final Tree<Data> scriptNode = new Tree<Data>(script);
        if (curNode == null) {
          curNode = scriptNode;
        }
        else {
          curNode.addChild(scriptNode);
        }
      }

      // deal with style
      else if (tagResult.hasStyle()) {
        final Style style = tagResult.getStyle();
        final Tree<Data> styleNode = new Tree<Data>(style);
        if (curNode == null) {
          curNode = styleNode;
        }
        else {
          curNode.addChild(styleNode);
        }
      }

      // deal with start tag
      else if (tagResult.hasTag()) {
        final Tag tag = tagResult.getTag();
        final Tree<Data> tagNode = new Tree<Data>(tag);
        tag.setContainer(tagNode);

        final boolean isOptionalEndTag = isOptionalEndTag(tag.name, curNode);
        if (curNode == null) {
          curNode = tagNode;
        }
        else {
          curNode = getClosingNode(curNode, tagNode);
          curNode.addChild(tagNode);
          if (!tag.isSelfTerminating() && !isOptionalEndTag) {
            // push tag to be current
            curNode = tagNode;
          }
        }

        if (!tag.isSelfTerminating() && isOptionalEndTag) {
          tag.setSelfTerminating();
        }
      }

      // reached end of input
      if (tagResult.hitEndOfStream()) {
        keepGoing[0] = false;
        return curNode;
      }
    }

    keepGoing[0] = true;

    return curNode;
  }

  private final Tree<Data>
    getClosingNode(Tree<Data> curNode, Tree<Data> tagNode)
  {
    Tree<Data> result = curNode;
    final Tag tag = tagNode.getData().asTag();

    if (xmlTagParser.isSpecialRuleEndTag(tag.name)) {
      // need to end first open special rule tag
      final String[] toClose = xmlTagParser.getSpecialToCloseTags(tag.name);
      Tree<Data> specialTag = null;
      for (int i = 1; i < toClose.length && specialTag == null; ++i) {
        final String tagToClose = toClose[i];
        specialTag = findTag(curNode, tagToClose);
      }
      if (specialTag != null) {
        result = closeTag(specialTag);
      }
    }

    return result;
  }

  private final Tree<Data> closeTag(Tree<Data> tagNode) {
    if (tagNode.getChildren() == null) {
      final Tag tag = tagNode.getData().asTag();
      tag.setSelfTerminating();
    }
    return tagNode.getParent() != null ? tagNode.getParent() : tagNode;
  }

  private final void addTextUnder(Tree<Data> node, String text) {
    // if the last child is a text node, append the new text to it
    final List<Tree<Data>> children = node.getChildren();
    if (children != null) {
      final int numChildren = children.size();
      if (numChildren > 0) {
        final Tree<Data> lastChild = children.get(numChildren - 1);
        final Text textData = lastChild.getData().asText();
        if (textData != null) {
          final Text newTextData = new Text(textData.text + " " + text);
          lastChild.setData(newTextData);
          newTextData.setContainer(lastChild);
          return;
        }
      }
    }

    // otherwise, create a new text child
    Tree<Data> child = node.addChild(new Text(text));
    child.getData().setContainer(child);
  }

  private final boolean isOptionalEndTag(String tagName, Tree<Data> curNode) {
    boolean result = xmlTagParser.isOptionalEndTag(tagName);

    // special case to account for documents that have thousands of spurious
    // font tags found in some blogs. In general, font is not self-terminating
    // and we use the text under a font node to determine headings; however,
    // when a font is nested within other font nodes, we will regard them as
    // self terminating.
    if (!result && "font".equals(tagName)) {
      while (curNode != null) {
        final Tag tag = curNode.getData().asTag();
        if (tag != null && "font".equals(tag.name)) {
          result = true;
          break;
        }
        curNode = curNode.getParent();
      }      
    }

    return result;
  }

  private final String getBuiltText(StringBuilder data, Tree<XmlLite.Data> curNode, boolean fix) {
    if (data == null) return "";

    String result = null;

    if (hasIgnoreTag(curNode)) {
      result = "";
    }
    else {
      result = fix ? fixText(data.toString()) : StringSplitter.hypertrim(data.toString());
    }

    data.setLength(0);

    return result;
  }

  private final boolean hasIgnoreTag(Tree<XmlLite.Data> node) {
    boolean result = false;
    if (ignoreTags == null) return result;

    while (node != null) {
      final XmlLite.Tag tag = node.getData().asTag();
      if (tag != null && ignoreTags.contains(tag.name)) {
        result = true;
        break;
      }
      node = node.getParent();
    }

    return result;
  }

  /**
   * Interface for data in an xml tree node (like text or tag data).
   */
  public static interface Data {
    public Text asText();
    public Tag asTag();
    public Comment asComment();
    public Script asScript();
    public Style asStyle();

    public void setProperty(String name, Object value);
    public Object getProperty(String name);
    public void clearProperties();
    public void removeProperty(String name);
    public boolean hasProperty(String name);

    public DomNode asDomNode();

    public void setContainer(Tree<Data> container);
    public Tree<Data> getContainer();
  }

  public static abstract class AbstractData implements Data {
    protected Tree<Data> container;
    protected final Map<String, Object> properties = new HashMap<String, Object>();

    public Text asText() { return null; }
    public Tag asTag() { return null; }
    public Comment asComment() { return null; }
    public Script asScript() { return null; }
    public Style asStyle() { return null; }

    public void setProperty(String name, Object value) {properties.put(name, value);}
    public Object getProperty(String name) {return properties.get(name);}
    public void clearProperties() {properties.clear();}
    public void removeProperty(String name) {properties.remove(name);}
    public boolean hasProperty(String name) {return properties.containsKey(name);}

    public DomNode asDomNode() {return null;}

    public void setContainer(Tree<Data> container) {this.container = container;}
    public Tree<Data> getContainer() {return container;}
  }

  public static final class Text extends AbstractData {
    public String text;
    private DomText domText;

    public Text(String text) {
      this.text = text;
    }

    public final Text asText() {
      return this;
    }

    public DomNode asDomNode() {
      if (domText == null && container != null) {
        domText = new DomText(this);
      }
      return domText;
    }

    public String toString() {
      return "\n" + escape(text) + "\n";
    }

    public boolean equals(Object o) {
      boolean result = (this == o);

      if (!result && o instanceof Text) {
        final Text other = (Text)o;
        result = text.equals(other.text);
      }

      return result;
    }

    public int hashCode() {
      return text.hashCode();
    }
  }

  public static final class Tag extends AbstractData {
    public String name;  //TODO: refactor to add setter/getter for this attribute
    public final boolean commonCase;
    public final Map<String, String> attributes;
    private boolean selfTerminating;
    private int numChildren;
    private int childNum;
    private DomElement domElement;

    /**
     * Construct with the name and attributes string.
     */
    public Tag(String nameAndAttributesString, boolean commonCase) {
      this.commonCase = commonCase;
      this.attributes = new LinkedHashMap<String, String>();
      this.selfTerminating = false;
      this.numChildren = 0;
      this.childNum = 0;

      final int len = nameAndAttributesString.length();
      if (len > 0) {
        if ('/' == nameAndAttributesString.charAt(len - 1)) {
          this.selfTerminating = true;
          nameAndAttributesString = nameAndAttributesString.substring(0, len - 1);
        }
        this.name = parseAttributes(nameAndAttributesString, attributes);
      }
      else {
        this.name = "";
        this.selfTerminating = true;
      }
    }

    /**
     * Copy constructor.
     */
    public Tag(Tag other) {
      super.container = other.container;
      this.name = other.name;
      this.commonCase = other.commonCase;
      this.attributes = new LinkedHashMap<String, String>(other.attributes);
      this.selfTerminating = other.selfTerminating;
      this.numChildren = other.numChildren;
      this.childNum = other.childNum;
      properties.putAll(other.properties);
    }

    public String getAttribute(String name) {
      return attributes.get(fixText(commonCase ? name.toLowerCase() : name));
    }

    public void setAttribute(String name, String value) {
      attributes.put(fixText(commonCase ? name.toLowerCase() : name), EntityConverter.unescape(value));
    }

    /**
     * Keep the attribute, but map it to an empty string.
     */
    public void clearAttribute(String name) {
      setAttribute(name, "");
    }

    public void removeAttribute(String name) {
      attributes.remove(fixText(commonCase ? name.toLowerCase() : name));
    }

    public Set<Map.Entry<String, String>> getAttributeEntries() {
      return attributes.entrySet();
    }

    public boolean isSelfTerminating() {
      return selfTerminating;
    }

    public void setSelfTerminating() {
      this.selfTerminating = true;
    }

    public void setSelfTerminating(boolean selfTerminating) {
      this.selfTerminating = selfTerminating;
    }

    /**
     * Utility to help keep track of the number of children this tag has.
     * <p>
     * Invoked in tag instances built with an XmlTagStack.
     */
    public int incNumChildren() {
      return numChildren++;
    }

    /**
     * Utility to help keep track of the number of children this tag has.
     * <p>
     * Used by XmlTagStack to determine the current child index, for output of non-tag nodes
     */
    public int getNumChildren() { 
      return numChildren; 
    }

    /**
     * Utility to help keep track of the child position of this tag.
     * <p>
     * Invoked in tag instances built with an XmlTagStack.
     */
    public void setChildNum(int childNum) {
      this.childNum = childNum;
    }

    /**
     * Utility to get the child position of this tag in its original xml tree.
     * <p>
     * Available in tag instances built with an XmlTagStack.
     */
    public int getChildNum() {
      return childNum;
    }

    /**
     * Parse the name and attributes, returning the name and populating the map
     * with attributes (mapped to values).
     *
     * @return the name.
     */
    private final String parseAttributes(String nameAndAttributesString, Map<String, String> attributes) {
      final int nameBoundary = delimOrEnd(nameAndAttributesString, ' ', 0);
      final String name = fixText(nameAndAttributesString.substring(0, nameBoundary));
      if (nameBoundary < nameAndAttributesString.length()) {
        extractAttributes(nameAndAttributesString.substring(nameBoundary + 1), attributes);
      }
      return commonCase ? name.toLowerCase() : name;
    }

    private final void extractAttributes(String attributesString, Map<String, String> attributes) {
      final int eqPos = attributesString.indexOf('=');
      if (eqPos >= 0) {
        String att = attributesString.substring(0, eqPos);
        if (commonCase) att = att.toLowerCase();
        final int alen = attributesString.length();
        final int endAttPos = endAttributePos(attributesString, eqPos + 1);
        final int eap = (endAttPos < alen) ? endAttPos : alen;
        final String value = (eap <= eqPos + 1) ? "" : stripQuotes(attributesString.substring(eqPos + 1, eap));

        attributes.put(fixText(att), EntityConverter.unescape(value));

        if (endAttPos < attributesString.length()) {
          extractAttributes(attributesString.substring(endAttPos + 1), attributes);
        }
      }
    }

    private final int delimOrEnd(String string, int delim, int fromPos) {
      int result = -1;

      if (commonCase) {
        // find delim from fromPos that isn't immediately preceded by a backslash
        for (result = string.indexOf(delim, fromPos); result > 0 && string.charAt(result - 1) == '\\';
             result = string.indexOf(delim, fromPos)) {
          fromPos = result + 1;
        }
      }
      else {
        // don't use backslash quoting in normal xml (where commonCase is here used as/equivalent to htmlFlag)
        result = string.indexOf(delim, fromPos);
      }
      
      return (result < 0) ? string.length() : result; 
    }

    private final int endAttributePos(String attributesString, int fromPos) {
      if (fromPos >= attributesString.length()) return fromPos;  // empty attribute.
      final char firstChar = attributesString.charAt(fromPos);

      return
        (firstChar == ' ') ?

        // empty attribute ("att="). return fromPos.
        fromPos :

        (firstChar == '"') ?

        // search to end double quote
        delimOrEnd(attributesString, '"', fromPos + 1) + 1 :

        (firstChar == '\'') ?

        // search to end single quote
        delimOrEnd(attributesString, '\'', fromPos + 1) + 1 :

        // search to space or end of string
        delimOrEnd(attributesString, ' ', fromPos);
    }

    private final String stripQuotes(String string) {
      if (string == null) return string;
      final int len = string.length();
      if (len == 0) return string;
      final char firstChar = string.charAt(0);
      final int startPos = (firstChar == '"' || firstChar == '\'') ? 1 : 0;
      final char lastChar = string.charAt(len - 1);
      final int endPos = (lastChar == '"' || lastChar == '\'') ? len - 1 : len;
      return (startPos > 0 || endPos < len) ? ((endPos > startPos) ? string.substring(startPos, endPos) : "") : string;
    }

    public final Tag asTag() {
      return this;
    }

    public DomNode asDomNode() {
      if (domElement == null && container != null) {
        domElement = new DomElement(this);
      }
      return domElement;
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();

      result.append('<').append(name);
      for (Map.Entry<String, String> entry : attributes.entrySet()) {
        result.append(' ').
          append(entry.getKey()).
          append('=').
          append('"').
          append(escape(entry.getValue())).
          append('"');
      }
      if (selfTerminating) result.append('/');
      result.append('>');

      return result.toString();
    }

    public boolean equals(Object o) {
      boolean result = (this == o);

      if (!result && o instanceof Tag) {
        final Tag other = (Tag)o;
        result = name.equals(other.name) && attributes.equals(other.attributes);
      }

      return result;
    }

    public int hashCode() {
      return (17 + name.hashCode()) * 17 + attributes.hashCode();
    }
  }

  public static final class Comment extends AbstractData {
    public final String text;

    protected Comment(String text) {
      this.text = text;
    }

    public final Comment asComment() {
      return this;
    }

    public String toString() {
      return "<" + text + ">";  // leave comment text as-is
    }

    public boolean equals(Object o) {
      boolean result = (this == o);

      if (!result && o instanceof Text) {
        final Text other = (Text)o;
        result = text.equals(other.text);
      }

      return result;
    }

    public int hashCode() {
      return text.hashCode();
    }
  }

  public static final class Script extends AbstractData {
    public final String text;

    protected Script(String text) {
      this.text = text;
    }

    public final Script asScript() {
      return this;
    }

    public String toString() {
      return "<script>" + text + "</script>";  // leave script text as-is
    }

    public boolean equals(Object o) {
      boolean result = (this == o);

      if (!result && o instanceof Text) {
        final Text other = (Text)o;
        result = text.equals(other.text);
      }

      return result;
    }

    public int hashCode() {
      return text.hashCode();
    }
  }

  public static final class Style extends AbstractData {
    public final String text;

    protected Style(String text) {
      this.text = text;
    }

    public final Style asStyle() {
      return this;
    }

    public String toString() {
      return "<style>" + text + "</style>";  // leave style text as-is
    }

    public boolean equals(Object o) {
      boolean result = (this == o);

      if (!result && o instanceof Text) {
        final Text other = (Text)o;
        result = text.equals(other.text);
      }

      return result;
    }

    public int hashCode() {
      return text.hashCode();
    }
  }

  public static void main(String[] args) throws IOException {
    final Tree<XmlLite.Data> xmlTree = XmlFactory.readXmlTree(new File(args[0]), false, true, false);  // keep comments, htmlFlag, don't require xml
    final String xmlString = XmlLite.asXml(xmlTree, true);
    System.out.println(xmlString);
  }
}
