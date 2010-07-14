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
package org.sd.xml.record;


import org.sd.util.MathUtil;
import org.sd.util.tree.Tree;
import org.sd.xml.XPathApplicator;
import org.sd.xml.XmlFactory;
import org.sd.xml.XmlLite;
import org.sd.xml.XmlTreeHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Record abstraction holding attribute/value pairs, where each value can
 * be another record or a string.
 * <p>
 * @author Spence Koehler
 */
public class Record {
  
  //****************************************
  //
  // Utility and factory methods
  //

  /**
   * Determine whether the node has been marked as a record.
   */
  public static final boolean isRecord(Tree<XmlLite.Data> node) {
    return (node == null || node.getData() == null) ? false :
      node.getData().hasProperty("_record");
  }

  /**
   * Determine whether the node has been marked as a non-record.
   */
  public static final boolean isNonRecord(Tree<XmlLite.Data> node) {
    return (node == null || node.getData() == null) ? true :
      node.getData().getProperty("_noRecord") != null;
  }

  /**
   * Mark the given node as a non-record.
   */
  public static final void markNonRecord(Tree<XmlLite.Data> node) {
    if (node != null && node.getData() != null) {
      node.getData().setProperty("_noRecord", true);
    }
  }

  /**
   * Convenience function to build a record that pairs attributes[i] with
   * values[i].
   */
  public static final Record buildRecord(String name, String[] attributes, String[] values) {
    final Record result = new Record(name);
    for (int i = 0; i < attributes.length; ++i) {
      result.addValue(attributes[i], values[i]);
    }
    return result;
  }

  /**
   * Build a record with the given name from the given nodes.
   */
  public static final Record buildRecord(String name, List<Tree<XmlLite.Data>> nodes) {
    Record result = null;

    if (nodes.size() == 1) {
      // single child: if it is already a record, use it; otherwise, create a record.
      final Tree<XmlLite.Data> child = nodes.get(0);
      result = getRecord(child);
    }
    else {
      // multiple children: pull all children into a record container.
      result = new Record(name, nodes);
    }

    return result;
  }

  /**
   * Build a record that includes all of the given fields by finding the
   * deepest common ancestor to all of the field leaves.
   */
  public static final Record buildRecord(List<Field> fields) {
    // find lowestCommonParent of the trees
    Tree<XmlLite.Data> result = null;

    for (Record.Field field : fields) {
      final Tree<XmlLite.Data> curTree = field.leaf;
      result = (result == null) ? curTree : curTree.getDeepestCommonAncestor(result);
      if (result == null) break;
    }
    
    // walk up the resulting tree while it has no siblings
    if (result != null) {
      while (result.getNumSiblings() == 1) {
        result = result.getParent();
      }
    }

    return result == null ? null : Record.getRecord(result);
  }

  /**
   * Get (or build) the record for just the given node.
   * <p>
   * If the node does not have a record and is not marked as a non-record,
   * then create a record backed by the node.
   */
  public static final Record getRecord(Tree<XmlLite.Data> node) {
    Record result = null;

    if (!isNonRecord(node)) {
      final XmlLite.Data data = node.getData();
      final Object theRecord = data.getProperty("_record");
      if (theRecord != null) {
        result = (Record)theRecord;
      }
      else {
        final XmlLite.Text text = data.asText();
        if (text != null) {
          // this record is actually just a value.
          result = new Value(text);
        }
        else {
          // create a new record backed by this node.
          result = new Record(node);
        }
      }
    }

    return result;
  }

  /**
   * Build a record from the html File, marking deep record nodes using
   * a default html view.
   */
  public static final Record buildHtmlRecord(File htmlFile) throws IOException {
    return buildHtmlRecord(htmlFile, new Record.View(true, DEFAULT_HTML_VIEW_STRINGS));
  }

  /**
   * Build a record from the html File, marking deep record nodes using
   * the given view.
   */
  public static final Record buildHtmlRecord(File htmlFile, View view) throws IOException {
    final Tree<XmlLite.Data> htmlTree = XmlFactory.readXmlTree(htmlFile, true, true, false);
    return buildXmlRecord(htmlTree, view);
  }

  /**
   * Build a record from the xml tree, marking deep record nodes using
   * a default html view.
   */
  public static final Record buildXmlRecord(Tree<XmlLite.Data> xmlTree) {
    return buildXmlRecord(xmlTree, new Record.View(true, DEFAULT_HTML_VIEW_STRINGS));
  }

  /**
   * Build a record from the xml tree, marking deep record nodes using
   * the given view.
   */
  public static final Record buildXmlRecord(Tree<XmlLite.Data> xmlTree, View view) {
    return buildXmlRecord(xmlTree, view, 0, new int[]{0, 0});
  }

  /**
   * Worker method to do the building, keeping track of sibling and path numbers.
   */
  private static final Record buildXmlRecord(Tree<XmlLite.Data> xmlTree, View view, int sibNum, int[] pathNum) {
    Record result = null;

    final XmlLite.Data xmlData = xmlTree.getData();
    if (xmlData.asText() != null) {
      xmlData.setProperty("_pathId", pathNum[1]++);  // set path id whether empty or not
      final String text = xmlData.asText().text;
      if (!"".equals(text)) {
        final Value value = new Value(xmlData.asText());
        value.setPathNum(pathNum[0]++);  // non-empty path number
        result = value;
      }
    }
    else if (xmlData.asTag() != null && xmlTree.hasChildren()) {
      int sibid = 0;
      for (Tree<XmlLite.Data> child : xmlTree.getChildren()) {
        final Record record = buildXmlRecord(child, view, sibid++, pathNum);
        if (record != null) {
          if (result == null) {
            result = new Record(xmlTree);
            result.setSibId(sibNum);
          }
        }
      }
    }

    if (result == null) {
      Record.markNonRecord(xmlTree);
    }
    else {
      result.setView(view);
      if (view != null) view.getGroupId(result);  // force setting of groupId
    }

    return result;
  }

  public static final String[] TAG_NAME_VIEW_STRINGS = new String[]{""};
  public static final String[] DEFAULT_HTML_VIEW_STRINGS = new String[]{"class", "id", ""};

  public static final View TAG_NAME_VIEW = new View(true, TAG_NAME_VIEW_STRINGS);


  //****************************************
  //
  // Class implementation
  //

  // view: fallback strategy for view of a record's name. null is for the tag.name;
  //       non-null is for a tag attribute. If value is null, fallback to next view.
  private View view;
  private String name;  // this record's name and final view fallback

  private XPathApplicator _xpathApplicator;
  private Tree<XmlLite.Data> _xmlTree;

  /**
   * Create a new (empty) record.
   */
  public Record() {
    this("");
  }

  /**
   * Create a new (empty) record.
   * <p>
   * Note that currently a record as a value may only be used once.
   */
  public Record(String name) {
    this._xmlTree = null;
    this.name = name;
  }

  /**
   * Build a record from the xml tree, marking deep record nodes using
   * a default html view.
   */
  public Record(File xmlFile) throws IOException {
    this(XmlFactory.readXmlTree(xmlFile, true, false, false));
  }

  /**
   * Create a new record backed by the xml tree.
   * <p>
   * NOTE: The xmlTree's data must be an XmlLite.Tag or an IllegalArgumentException
   *       will be thrown. This new instance will be associated with the tree's tag
   *       as its record.
   */
  public Record(Tree<XmlLite.Data> xmlTree) {
    this._xmlTree = xmlTree;

    final XmlLite.Data data = xmlTree.getData();
    if (data != null) {
      data.setProperty("_record", this);
      if (data.asTag() != null) {
        this.name = data.asTag().name;
      }
    }
    else {
      throw new IllegalArgumentException("xmlTree must have data!");
    }

    if (this.name == null) this.name = "";
  }

  /**
   * Create a new record with the given xmlTrees as its attributes.
   */
  public Record(List<Tree<XmlLite.Data>> xmlTrees) {
    this("", xmlTrees);
  }

  /**
   * Create a new record with the given xmlTrees as its attributes.
   */
  public Record(String name, List<Tree<XmlLite.Data>> xmlTrees) {
    this.name = name == null ? "" : name;
    boolean commonCase = xmlTrees.get(0).getData().asTag().commonCase;
    this._xmlTree = new Tree<XmlLite.Data>(new XmlLite.Tag(name, commonCase));
    this._xmlTree.getData().setContainer(this._xmlTree);
    for (Tree<XmlLite.Data> xmlTree : xmlTrees) {
      _xmlTree.addChild(xmlTree);
    }
  }

  /**
   * Safely downcast this record to a value if it is one.
   * <p>
   * Note that as a record, this will never be a value and null will always
   * be returned.
   */
  public Value asValue() {
    return null;
  }

  /**
   * Get the name (type) of this record.
   * <p>
   * Note that if this record is a value, the name will be empty.
   */
  public String getName() {
    return name;
  }

  /**
   * Set this record's name, leaving the tag name unchanged.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Set the view strategy for this record's view name.
   * <p>
   * Each view is either an attribute on this record's tag whose
   * value will become the view or empty to indicate the tag name
   * or null to indicate the record name. The record name will
   * always be the final fallback in the strategy and need not
   * be explicitly specified in the view.
   */
  public void setView(View view) {
    this.view = view;
  }

  /**
   * Get this record's view name.
   * <p>
   * Note that this is computed using the view by getting the value
   * on this record's tag for each view attribute until a non-null
   * value is found. An empty view identifies the tag name (or text
   * text) and a null view identifies the record name, which will
   * always be the last "fallback" for a view name whether explicitly
   * indicated or not.
   */
  public String getViewName() {
    final Tree<XmlLite.Data> xmlTree = asTree();

    // no xmlTree or no view strategy, view name can only be the record name
    if (xmlTree == null || view == null) return name;

    return view.getViewName(xmlTree);
  }

  /**
   * Change the tag name for this record, leaving the name unchanged.
   */
  public void setTagName(String name) {
    if (_xmlTree != null) {
      final XmlLite.Tag curTag = _xmlTree.getData().asTag();
      if (curTag != null) {
        _xmlTree.setData(new XmlLite.Tag(curTag));
      }
    }
  }

  /**
   * Set this record's sibling id.
   */
  public void setSibId(int sibId) {
    setProperty("_sibid", sibId);
  }

  /**
   * Get this record's sibling id.
   */
  public int getSibId() {
    final Object result = getProperty("_sibid");
    return (result == null) ? -1 : (Integer)result;
  }

  /**
   * Set this record's group id according to the view.
   */
  public void setGroupId(int groupId) {
    setProperty("_groupId", groupId);
  }

  /**
   * Get this record's group id.
   */
  public int getGroupId() {
    final Object result = getProperty("_groupId");
    return (result == null) ? -1 : (Integer)result;
  }

  /**
   * Set this record's key according to the view.
   */
  public void setKey(String key) {
    setProperty("_key", key);
  }

  /**
   * Get this record's key.
   */
  public String getKey() {
    final Object result = getProperty("_key");
    return (result == null) ? null : (String)result;
  }

  /**
   * Set the property on this record's data.
   */
  public void setProperty(String property, Object value) {
    final Tree<XmlLite.Data> xmlTree = asTree();
    xmlTree.getData().setProperty(property, value);
  }

  /**
   * Get the property on this record's data.
   */
  public Object getProperty(String property) {
    final Tree<XmlLite.Data> xmlTree = asTree();
    return xmlTree.getData().getProperty(property);
  }

  /**
   * Add the given (terminal) data to this record associated with the given
   * (simple) attribute.
   * <p>
   * Note that any attribute can have multiple values associated with it.
   */
  public void addValue(String attribute, String data) {
    addValue(new Value(attribute, data));
  }

  /**
   * Add the given record to this record associated with its name as
   * the relational (simple) attribute.
   * <p>
   * Note that any attribute can have multiple values associated with it.
   */
  public void addValue(Record record) {
    final Tree<XmlLite.Data> xmlTree = asTree();
    xmlTree.addChild(record.asTree());
  }

  /**
   * Add the given value to this record associated with its name as
   * the relational (simple) attribute.
   * <p>
   * Note that any attribute can have multiple values associated with it.
   */
  public void addValue(Value value) {
    final Tree<XmlLite.Data> xmlTree = asTree();
    final boolean commonCase = xmlTree.getData().asTag().commonCase;
    final Tree<XmlLite.Data> child = new Tree<XmlLite.Data>(new XmlLite.Tag(value.getName(), commonCase));
    child.getData().setContainer(child);
    xmlTree.addChild(child);
    child.addChild(value.asTree());
  }

  /**
   * Convenience method to get this record's (first) value for the given
   * (simple or complex) tag name attribute.
   * <p>
   * Note that this gets the first child of the child named "attribute",
   * which is a "grandchild". To just get the child with the given attribute
   * as its name, use getRecord(attribute).
   */
  public Record getValue(String attribute) {
    if (_xmlTree == null) return null;

    Record result = null;

    // try the attribute as simple
    final Tree<XmlLite.Data> xmlTree = asTree();
    final XPathApplicator xpathApplicator = getXPathApplicator();

    if (attribute.indexOf('@') >= 0) {
      final String text = xpathApplicator.getFirstText(name + "." + attribute, xmlTree);
      if (text != null) {
        result = new Value(text);
      }
    }
    else {
      final Tree<XmlLite.Data> matchingNode = xpathApplicator.getFirstNode(name + "." + attribute, xmlTree);

      if (matchingNode != null && matchingNode.hasChildren()) {
        final Tree<XmlLite.Data> child = matchingNode.getChildren().get(0);
        result = getRecord(child);
      }
    }

    return result;
  }

  /**
   * Get this record's values for the given (simple or complex) tag name
   * attribute.
   * <p>
   * Note that this gets the children of the child named "attribute",
   * which are "grandchildren". To just get the children with the given
   * attribute as their name, use getRecords(attribute).
   */
  public List<Record> getValues(String attribute) {
    if (_xmlTree == null) return null;

    List<Record> result = null;

    // try the attribute as simple
    final Tree<XmlLite.Data> xmlTree = asTree();
    final XPathApplicator xpathApplicator = getXPathApplicator();

    if (attribute.indexOf('@') >= 0) {
      final List<String> texts = xpathApplicator.getText(name + "." + attribute, xmlTree, false, false);
      if (texts != null && texts.size() > 0) {
        result = new ArrayList<Record>();
        for (String text : texts) {
          result.add(new Value(text));
        }
      }
    }
    else {
      final List<Tree<XmlLite.Data>> matchingNodes = xpathApplicator.getNodes(name + "." + attribute, xmlTree);

      if (matchingNodes != null && matchingNodes.size() > 0) {
        result = new ArrayList<Record>();
        for (Tree<XmlLite.Data> matchingNode : matchingNodes) {
          if (matchingNode != null && matchingNode.hasChildren()) {
            for (Tree<XmlLite.Data> child : matchingNode.getChildren()) {
              final Record value = getRecord(child);
              if (value != null) {
                result.add(value);
              }
            }
          }
        }
      }
    }

    return result;
  }

  /**
   * Convenience method to get this record's (first) record with the given
   * (simple or complex) attribute name.
   * <p>
   * Note that this gets the first child named "attribute" as a record.
   */
  public Record getRecord(String attribute) {
    if (_xmlTree == null) return null;

    Record result = null;

    // try the attribute as simple
    final Tree<XmlLite.Data> xmlTree = asTree();
    final XPathApplicator xpathApplicator = getXPathApplicator();
    final Tree<XmlLite.Data> matchingNode = xpathApplicator.getFirstNode(name + "." + attribute, xmlTree);

    if (matchingNode != null) {
      result = getRecord(matchingNode);
    }

    return result;
  }

  /**
   * Get this record's child records with the given (simple or complex) tag
   * (attribute) name.
   * <p>
   * Note that this gets the children named "attribute" as records.
   */
  public List<Record> getRecords(String attribute) {
    if (_xmlTree == null) return null;

    List<Record> result = null;

    // try the attribute as simple
    final Tree<XmlLite.Data> xmlTree = asTree();
    final XPathApplicator xpathApplicator = getXPathApplicator();
    final List<Tree<XmlLite.Data>> matchingNodes = xpathApplicator.getNodes(name + "." + attribute, xmlTree);

    if (matchingNodes != null && matchingNodes.size() > 0) {
      result = new ArrayList<Record>();
      for (Tree<XmlLite.Data> matchingNode : matchingNodes) {
        if (matchingNode != null) {
          result.add(getRecord(matchingNode));
        }
      }
    }

    return result;
  }

  /**
   * Get this record's simple (immediate, local) attributes (tag names).
   */
  public List<String> getSimpleAttributes() {
    if (_xmlTree == null) return null;

    final Tree<XmlLite.Data> xmlTree = asTree();
    if (!xmlTree.hasChildren()) return null;

    final List<Tree<XmlLite.Data>> children = xmlTree.getChildren();
    final List<String> result = new ArrayList<String>();

    for (Tree<XmlLite.Data> child : children) {
      final XmlLite.Tag tag = child.getData().asTag();
      if (tag != null) {
        result.add(tag.name);
      }
    }

    return result;
  }

  /**
   * Get this record's instantiated (simple and complex) tag name attributes
   * with their values (where these attribute/value combinations are also
   * referred to as fields.)
   * <p>
   * Note that this yields attributes suitable for the getValue methods.
   */
  public List<Field> getFields() {
    return getFields(TAG_NAME_VIEW);
  }

  /**
   * Get this record's instantiated (simple and complex) attributes
   * with their values (where these attribute/value combinations are also
   * referred to as fields) using this instance's current view.
   * <p>
   * Note that this does not necessarily yield attributes suitable for the
   * getValue methods.
   */
  public List<Field> getViewFields() {
    return getFields(view);
  }

  /**
   * Get the fields using the given view.
   */
  public final List<Field> getFields(View view) {
    if (_xmlTree == null) return null;

    final Tree<XmlLite.Data> xmlTree = asTree();
    if (!xmlTree.hasChildren()) return null;

    final List<Field> result = new ArrayList<Field>();
    final List<Tree<XmlLite.Data>> leaves = xmlTree.gatherLeaves();
    for (Tree<XmlLite.Data> leaf : leaves) {
      final XmlLite.Text text = leaf.getData().asText();
      if (text != null) {
        result.add(new Field(xmlTree, leaf, view));
      }
    }

    return result;
  }

  /**
   * Get the attribute off the record (if it exists).
   */
  public String getAttribute(String attribute) {
    String result = null;

    final Tree<XmlLite.Data> xmlTree = asTree();
    final XmlLite.Tag tag = xmlTree.getData().asTag();
    if (tag != null) {
      result = tag.getAttribute(attribute);
    }

    return result;
  }

  /**
   * Get the minimum path index of fields covered by this record.
   */
  public final int minPathIndex() {
    final Tree<XmlLite.Data> xmlTree = asTree();
    Object result = null;

    if (!xmlTree.hasChildren()) {
      result = getPathId(xmlTree);
    }
    else {
      final List<Tree<XmlLite.Data>> leaves = xmlTree.gatherLeaves();

      for (Tree<XmlLite.Data> leaf : leaves) {
        result = getPathId(leaf);
        if (result != null) break;
      }
    }

    return (result == null) ? -1 : (Integer)result;
  }

  /**
   * Get the maximum path index of fields covered by this record.
   */
  public final int maxPathIndex() {
    final Tree<XmlLite.Data> xmlTree = asTree();
    Object result = null;

    if (!xmlTree.hasChildren()) {
      result = getPathId(xmlTree);
    }
    else {
      final List<Tree<XmlLite.Data>> leaves = xmlTree.gatherLeaves();
      for (int i = leaves.size() - 1; i >= 0; --i) {
        final Tree<XmlLite.Data> leaf  = leaves.get(i);
        result = getPathId(leaf);
        if (result != null) break;
      }
    }

    return (result == null) ? -1 : (Integer)result;
  }

  private final Object getPathId(Tree<XmlLite.Data> textNode) {
    Object result = null;
    final XmlLite.Text text = textNode.getData().asText();
    if (text != null) {
      result = text.getProperty("_pathId");
    }
    return result;
  }

  /**
   * Convenience method to get this record's (first) value string for the
   * given (simple or complex) tag name attribute.
   * 
   * @return the value string or null if the attribute's value is not
   *         instantiated, not a record, or not present.
   */
  public String getValueString(String attribute) {
    String result = null;

    final Record record = getValue(attribute);
    if (record != null) {
      final Value value = record.asValue();
      if (value != null) {
        result = value.getData();
      }
    }

    return result;
  }

  /**
   * Convenience method to get this record's (non-empty) value strings for the
   * given (simple or complex) tag name attribute.
   * 
   * @return the value strings or null if the attribute's values are not
   *         instantiated, not records, or not present.
   */
  public Set<String> getValueStrings(String attribute) {
    Set<String> result = null;

    final List<Record> records = getValues(attribute);
    if (records != null) {
      for (Record record : records) {
        final Value value = record.asValue();
        if (value != null) {
          if (result == null) result = new HashSet<String>();
          final String data = value.getData();
          if (data != null && !"".equals(value)) {
            result.add(data);
          }
        }
      }
    }

    return result;
  }

  /**
   * Get all of this record's text.
   * <p>
   * All fields' text will be concatenated with a space.
   */
  public String getText() {
    final Tree<XmlLite.Data> xmlTree = asTree();
    return XmlTreeHelper.getAllText(xmlTree);
  }

  /**
   * Get all of this record's text.
   * <p>
   * All fields' text will be concatenated with a space.
   * <p>
   * If the text is null, then use the "property" attribute on the record
   * to retrieve the property to use to get the text from properties. If
   * the property is also missing, fallback to defaultText.
   */
  public String getText(Properties properties, String defaultText) {
    final Tree<XmlLite.Data> xmlTree = asTree();
    String text = XmlTreeHelper.getAllText(xmlTree);
    if ((text == null || "".equals(text)) && properties != null) {
      final XmlLite.Tag tag = xmlTree.getData().asTag();
      if (tag != null) {
        final String property = tag.getAttribute("property");
        if (property != null) {
          text = properties.getProperty(property);
        }
      }
    }
    return text == null ? defaultText : text;
  }

  /**
   * Determine whether this record already has a(n xml)Tree.
   * <p>
   * Note that asTree will build one if it doesn't exist yet for this
   * record. This method is a way to check whether the tree has been
   * built yet without forcing it to build.
   */
  public boolean hasTree() {
    return _xmlTree != null;
  }

  /**
   * Get this record as a tree.
   */
  public Tree<XmlLite.Data> asTree() {
    if (_xmlTree == null) {
      final XmlLite.Tag tag = new XmlLite.Tag(name, false); //todo: parameterize commonCase?
      tag.setProperty("_record", this);
      _xmlTree = new Tree<XmlLite.Data>(tag);
      tag.setContainer(_xmlTree);
    }
    return _xmlTree;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append(name);

    final List<Field> fields = getFields(view);
    if (fields != null) {
      for (Field field : fields) {
        result.
          append("\n ").
          append(MathUtil.integerString(field.getPathId(), Integer.toString(fields.size()).length())).
          append(": ").
          append(field.toString());
      }
    }

    return result.toString();
  }

  /**
   * Get this record's XPathApplicator.
   */
  public XPathApplicator getXPathApplicator() {
    if (_xpathApplicator == null) {
      _xpathApplicator = new XPathApplicator();
    }
    return _xpathApplicator;
  }

  /**
   * Build the xpath from the given anchor down to and including the given
   * (descendent) node according to the view.
   */
  private static final String buildAttribute(Tree<XmlLite.Data> anchor, Tree<XmlLite.Data> node, View view) {
    final StringBuilder result = new StringBuilder();

    while (node != null && node != anchor) {
      final String viewName = buildViewName(node, view);
      if (viewName != null) {
        if (result.length() > 0) result.insert(0, '.');
        result.insert(0, viewName);
      }
      node = node.getParent();
    }

    return result.toString();
  }

  private static final String buildViewName(Tree<XmlLite.Data> node, View view) {
    return (view == null) ? "" : view.getViewName(node);
  }

  private static final Object getProperty(Tree<XmlLite.Data> node, String property) {
    Object result = null;

    if (node != null) {
      final XmlLite.Data data = node.getData();
      result = data.getProperty(property);
    }

    return result;
  }


  /**
   * Definition of a view strategy over a record.
   */
  public static final class View {

    public final boolean showSibNum;
    public final String[] view;

    private final AtomicInteger nextGroupId = new AtomicInteger(0);
    private Map<String, Integer> key2id;

    public View(boolean showSibNum, String[] view) {
      this.showSibNum = showSibNum;
      this.view = view;
      this.key2id = null;
    }

    public String getViewName(Record record) {
      return getViewName(record.asTree(), record, showSibNum);
    }

    public String getViewName(Tree<XmlLite.Data> node) {
      return getViewName(node, null, showSibNum);
    }

    public String buildKey(Record record) {
      return buildKey(record.asTree());
    }

    public String buildKey(Tree<XmlLite.Data> node) {
      final XmlLite.Data data = node.getData();
      String key = null;
      Object keyObj = data.getProperty("_key");

      if (keyObj == null) {
        final StringBuilder builder = new StringBuilder();

        final List<Tree<XmlLite.Data>> pathNodes = node.getPath();
        for (Tree<XmlLite.Data> pathNode : pathNodes) {
          if (builder.length() > 0) builder.append('.');
          builder.append(getViewName(pathNode, null, false));
        }
        key = builder.toString();
        data.setProperty("_key", key);
      }
      else {
        key = (String)keyObj;
      }

      return key;
    }

    public int getGroupId(Record record) {
      return getGroupId(record.asTree());
    }

    public int getGroupId(Tree<XmlLite.Data> node) {
      final XmlLite.Data data = node.getData();
      Integer id = null;
      Object idObj = data.getProperty("_groupId");

      if (idObj == null) {
        final String key = buildKey(node);
        if (key2id == null) key2id = new HashMap<String, Integer>();
        id = key2id.get(key);
        if (id == null) {
          id = nextGroupId.getAndIncrement();
          key2id.put(key, id);
        }
        data.setProperty("_groupId", id);
      }
      else {
        id = (Integer)idObj;
      }

      return id;
    }

    private final String getViewName(Tree<XmlLite.Data> node, Record theRecord, boolean showSibNum) {
      final StringBuilder result = new StringBuilder();

      final String viewName = doGetViewName(node, theRecord);
      if (viewName != null) {
        result.append(viewName);

        if (showSibNum) {
          final Object sibId = node.getData().getProperty("_sibid");
          if (sibId != null) {
            result.append('[').append(sibId).append(']');
          }
        }
      }

      return result.toString();
    }

    private final String doGetViewName(Tree<XmlLite.Data> node, Record theRecord) {
      String result = null;

      if (node != null && node.getData() != null) {
        final XmlLite.Data data = node.getData();
        final XmlLite.Tag tag = data.asTag();
        final XmlLite.Text text = data.asText();

        if (view != null) {
          for (String attr : view) {
            if (attr == null) {
              break;  // fall back to record name
            }
            else if ("".equals(attr)) {
              result = (tag != null) ? tag.name : (text != null) ? text.text : null;
            }
            else if (tag != null) {
              result = tag.getAttribute(attr);
            }
            if (result != null) break;  // found the view name
          }
        }
        // else fall back to record name

        // fall back to record name if it exists
        if (result == null && tag != null) {
          final Record record = (theRecord == null) ? (Record)tag.getProperty("_record") : theRecord;
          if (record != null) {
            result = record.getName();
          }
          else {
            result = tag.name;  // this would be the record name if it had a record.
          }
        }
      }

      return result;
    }
  }

  /**
   * Container for a terminal attribute/value pair.
   */
  public static final class Field {
    public final Tree<XmlLite.Data> root;
    public final Tree<XmlLite.Data> leaf;
    public final View view;

    public final String attribute;
    public final String value;

    public Field(Tree<XmlLite.Data> root, Tree<XmlLite.Data> leaf, View view) {
      final XmlLite.Text text = leaf.getData().asText();
      if (text == null) {
        throw new IllegalArgumentException("leaf must be a text node!");
      }

      this.root = root;
      this.leaf = leaf;
      this.view = view;

      this.value = text.text;
      this.attribute = Record.buildAttribute(root, leaf.getParent(), view);
    }

    public String toString() {
      return attribute + ": " + value;
    }

    /**
     * Get the (non-empty) path number.
     */
    public int getPathNum() {
      final Object result = leaf.getData().getProperty("_pathNum");
      return (result == null) ? -1 : (Integer)result;
    }

    /**
     * Get the path id.
     */
    public int getPathId() {
      final Object result = leaf.getData().getProperty("_pathId");
      return (result == null) ? -1 : (Integer)result;
    }
  }
}
