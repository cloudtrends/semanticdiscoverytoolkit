/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.atn;


import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sd.token.CategorizedToken;
import org.sd.token.Feature;
import org.sd.util.tree.NodePath;
import org.sd.util.tree.Tree;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.XmlLite;
import org.w3c.dom.NodeList;

/**
 * A generic parse interpreter.
 *
 * @author Spence Koehler
 */
public class RecordParseInterpreter implements ParseInterpreter {
  
  private String[] classifications;
  private InnerResources resources;
  private List<RecordTemplate> topTemplates;


  public RecordParseInterpreter(DomNode domNode, ResourceManager resourceManager) {
    this.resources = new InnerResources(resourceManager);
    this.topTemplates = new ArrayList<RecordTemplate>();

    // fill id2recordTemplate and topTemplates
    loadRecords(domNode, resources);

    final Set<String> topRecordTypes = new TreeSet<String>();
    for (RecordTemplate topTemplate : topTemplates) topRecordTypes.add(topTemplate.getId());
    this.classifications = topRecordTypes.toArray(new String[topRecordTypes.size()]);
  }

  /**
   * Get classifications offered by this interpreter.
   * <p>
   * Note that classifications are applied to parse-based tokens.
   */
  public String[] getClassifications() {
    return classifications;
  }

  /**
   * Get the interpretations for the parse or null.
   */
  public List<ParseInterpretation> getInterpretations(Parse parse) {
    List<ParseInterpretation> result = null;

    for (RecordTemplate topTemplate : topTemplates) {
      if (topTemplate.matches(parse)) {
        ParseInterpretation interp = topTemplate.interpret(parse);
        if (interp != null) {
          interp = interpretationHook(interp, parse);
        }
        if (interp != null) {
          if (result == null) result = new ArrayList<ParseInterpretation>();
          result.add(interp);
        }
      }
    }

    return result;
  }

  /**
   * Hook on a final interpretation.
   */
  protected ParseInterpretation interpretationHook(ParseInterpretation interp, Parse parse) {
    return interp;
  }

  /**
   * Hook on each record interpNode just after creation and before insertion
   * as a child into its tree.
   */
  protected Tree<XmlLite.Data> interpRecordNodeHook(Tree<XmlLite.Data> recordNode, Parse parse,
                                                    Tree<String> parseNode, Tree<XmlLite.Data> parentNode,
                                                    String fieldName, DomElement recordElement) {
    return recordNode;
  }

  /**
   * Hook on each field interpNode just after creation and before insertion
   * as a child into its tree.
   * <p>
   * Note that each non-root record node will come back through as a field
   * but not all fields come through as a record.
   */
  protected Tree<XmlLite.Data> interpFieldNodeHook(Tree<XmlLite.Data> fieldNode, Parse parse,
                                                   Tree<String> selectedNode, Tree<XmlLite.Data> parentNode,
                                                   String fieldName, DomElement fieldElement) {
    return fieldNode;
  }

  // fill id2recordTemplate and topTemplates
  private final void loadRecords(DomNode domNode, InnerResources resources) {
    final NodeList recordNodes = domNode.selectNodes("record");
    if (recordNodes != null) {
      final int numRecordNodes = recordNodes.getLength();
      for (int recordNodeNum = 0; recordNodeNum < numRecordNodes; ++recordNodeNum) {
        final DomElement recordNode = (DomElement)recordNodes.item(recordNodeNum);

        final RecordTemplate recordTemplate = new RecordTemplate(recordNode, resources, this);
        resources.id2recordTemplate.put(recordTemplate.getId(), recordTemplate);
        if (recordTemplate.isTop()) topTemplates.add(recordTemplate);
      }
    }
  }


  private static final class InnerResources {

    public final ResourceManager resourceManager;
    public final Map<String, RecordTemplate> id2recordTemplate;

    InnerResources(ResourceManager resourceManager) {
      this.resourceManager = resourceManager;
      this.id2recordTemplate = new HashMap<String, RecordTemplate>();
    }
  }


  private static final class RecordTemplate {

    private DomElement recordNode;
    private InnerResources resources;
    private RecordParseInterpreter controller;

    private String name;
    private String id;
    private boolean top;

    private NodeMatcher matcher;
    private List<FieldTemplate> fieldTemplates;
    private FieldTemplate nameOverride;

    RecordTemplate(DomElement recordNode, InnerResources resources, RecordParseInterpreter controller) {
      this.recordNode = recordNode;
      this.resources = resources;
      this.controller = controller;

      this.name = recordNode.getAttributeValue("name");
      this.id = recordNode.getAttributeValue("type", this.name);

      final DomElement matchElement = (DomElement)recordNode.selectSingleNode("match");
      if (matchElement != null) {
        this.top = true;
        this.matcher = buildNodeMatcher(matchElement, resources);
      }

      this.nameOverride = null;
      this.fieldTemplates = new ArrayList<FieldTemplate>();
      final NodeList fieldList = recordNode.selectNodes("field");
      if (fieldList != null) {
        final int numFields = fieldList.getLength();
        for (int fieldNum = 0; fieldNum < numFields; ++fieldNum) {
          final DomElement fieldElement = (DomElement)fieldList.item(fieldNum);
          final FieldTemplate fieldTemplate = new FieldTemplate(fieldElement, resources);

          if (fieldTemplate.isNameOverride()) {
            this.nameOverride = fieldTemplate;
          }
          else {
            fieldTemplates.add(fieldTemplate);
          }
        }
      }
    }

    String getId() {
      return id;
    }

    boolean isTop() {
      return top;
    }

    String getNameOverride(Parse parse, Tree<String> parseNode, String fieldName) {
      String result = fieldName;

      if (nameOverride != null) {
        final List<Tree<String>> selected = nameOverride.select(parse, parseNode);
        if (selected != null && selected.size() > 0) {
          final Tree<String> selectedParseNode = selected.get(0);
          final String string = nameOverride.extractString(parse, selectedParseNode);
          if (string != null) result = string;
        }
      }

      return result;
    }

    boolean matches(Parse parse) {
      boolean result = true;

      if (matcher != null) {
        result = matcher.matches(parse);
      }

      return result;
    }

    ParseInterpretation interpret(Parse parse) {
      ParseInterpretation result = null;
      
      final Tree<String> parseTree = parse.getParseTree();
      final Tree<XmlLite.Data> interpTree = interpret(parse, parseTree, null, name);

      if (interpTree != null) {
        // add 'rule' attribute to interp (top)
        interpTree.getData().asTag().attributes.put("rule", parse.getRuleId());

        result = new ParseInterpretation(interpTree);
      }

      return result;
    }

    Tree<XmlLite.Data> interpret(Parse parse, Tree<String> parseNode,
                                 Tree<XmlLite.Data> parentNode, String fieldName) {

      fieldName = getNameOverride(parse, parseNode, fieldName);
      Tree<XmlLite.Data> result = XmlLite.createTagNode(fieldName);

      
      for (FieldTemplate fieldTemplate : fieldTemplates) {
        // select node(s) and extract value(s)...
        final List<Tree<String>> selectedNodes = fieldTemplate.select(parse, parseNode);

        if (selectedNodes != null && selectedNodes.size() > 0) {
          final StringBuilder builder = fieldTemplate.collapse() ? new StringBuilder() : null;

          for (Tree<String> selectedNode : selectedNodes) {
            final List<Tree<XmlLite.Data>> values = fieldTemplate.extract(parse, selectedNode);

            if (values != null && values.size() > 0) {
              if (builder != null) {  // collapse
                for (Tree<XmlLite.Data> value : values) {
                  if (builder.length() > 0) builder.append(' ');
                  builder.append(value.getData().asDomNode().getTextContent());
                }
              }
              else {
                for (Tree<XmlLite.Data> value : values) {
                  value = controller.interpFieldNodeHook(value, parse, selectedNode, result, fieldTemplate.getName(), fieldTemplate.getFieldElement());
                  if (value != null) result.addChild(value);
                }
              }
            }
          }

          if (builder != null) {  // collapse
            Tree<XmlLite.Data> collapsedNode = XmlLite.createTagNode(fieldTemplate.getName());
            collapsedNode.addChild(XmlLite.createTextNode(builder.toString()));
            collapsedNode = controller.interpFieldNodeHook(collapsedNode, parse, parseNode, result, fieldTemplate.getName(), fieldTemplate.getFieldElement());
            if (collapsedNode != null) result.addChild(collapsedNode);
          }
        }
      }

      if (result.numChildren() > 0) {
        if (!id.equals(fieldName)) {
          result.getData().asTag().attributes.put("type", id);
        }
        if (result != null) {
          result = controller.interpRecordNodeHook(result, parse, parseNode, parentNode, fieldName, recordNode);
        }
        if (result != null && parentNode != null) parentNode.addChild(result);
      }
      else result = null;

      return result;
    }
  }


  private static final NodeMatcher buildNodeMatcher(DomElement matchElement, InnerResources resources) {
    NodeMatcher result = null;
    final String attribute = matchElement.getAttributeValue("attribute", "any");

    if ("id".equals(attribute)) {
      // match against rule id
      result = new RuleIdMatcher(matchElement, resources);
    }
    else if ("path".equals(attribute)) {
      // match against node path
      result = new NodePathMatcher(matchElement, resources);
    }
    else {
      result = new AnyNodeMatcher(matchElement, resources);
    }

    return result;
  }

  private static interface NodeMatcher {
    public boolean matches(Parse parse);
  }

  private static final class RuleIdMatcher implements NodeMatcher {

    private Pattern pattern;

    RuleIdMatcher(DomElement matchElement, InnerResources resources) {
      //todo: decode attributes, for now, only doing regex 'matches'
      this.pattern = Pattern.compile(matchElement.getTextContent());
    }

    public boolean matches(Parse parse) {
      boolean result = false;
      final String ruleId = parse.getRuleId();
      if (ruleId != null && !"".equals(ruleId)) {
        final Matcher m = pattern.matcher(ruleId);
        result = m.matches();
      }
      return result;
    }
  }

  private static final class NodePathMatcher implements NodeMatcher {

    private NodePath<String> nodePath;

    NodePathMatcher(DomElement matchElement, InnerResources resources) {
      //todo: decode attributes, for now, only doing regex 'matches'
      this.nodePath = new NodePath<String>(matchElement.getTextContent());
    }

    public boolean matches(Parse parse) {
      boolean result = false;

      final Tree<String> parseTree = parse.getParseTree();
      return nodePath.apply(parseTree) != null;
    }
  }

  private static final class AnyNodeMatcher implements NodeMatcher {
    AnyNodeMatcher(DomElement matchElement, InnerResources resources) {
    }

    public boolean matches(Parse parse) {
      return true;
    }
  }


  private static final class FieldTemplate {

    private DomElement fieldElement;

    private String name;
    private boolean repeats;
    private boolean nameOverride;
    private boolean collapse;
    private NodeSelector selector;
    private NodeExtractor extractor;

    private NodeSelector nameSelector;
    private NodeExtractor nameExtractor;

    FieldTemplate(DomElement fieldElement, InnerResources resources) {
      this.fieldElement = fieldElement;
      this.name = fieldElement.getAttributeValue("name");
      this.repeats = fieldElement.getAttributeBoolean("repeats", false);
      this.nameOverride = "nameOverride".equals(fieldElement.getAttributeValue("type", null));
      this.collapse = fieldElement.getAttributeBoolean("collapse", false);

      // select
      final DomElement selectNode = (DomElement)fieldElement.selectSingleNode("select");
      this.selector = buildNodeSelector(selectNode, resources);

      // extract
      final DomElement extractNode = (DomElement)fieldElement.selectSingleNode("extract");
      this.extractor = buildNodeExtractor(this, extractNode, resources);
    }

    public DomElement getFieldElement() {
      return fieldElement;
    }

    boolean isNameOverride() {
      return nameOverride;
    }

    String getName() {
      return name;
    }

    boolean repeats() {
      return repeats;
    }

    boolean collapse() {
      return collapse;
    }

    List<Tree<String>> select(Parse parse, Tree<String> parseNode) {
      return selector.select(parse, parseNode);
    }

    List<Tree<XmlLite.Data>> extract(Parse parse, Tree<String> parseNode) {
      return extractor.extract(parse, parseNode);
    }

    String extractString(Parse parse, Tree<String> parseNode) {
      return extractor.extractString(parse, parseNode);
    }
  }


  public static final NodeSelector buildNodeSelector(DomElement matchElement, InnerResources resources) {
    //todo: decode attributes... for now, only NodePathSelector is being used...
    return new NodePathSelector(matchElement, resources);
  }

  private static interface NodeSelector {
    public List<Tree<String>> select(Parse parse, Tree<String> parseTreeNode);
  }

  private static final class NodePathSelector implements NodeSelector {

    private NodePath<String> nodePath;
    private InnerResources resources;

    NodePathSelector(DomElement selectElement, InnerResources resources ) {
      //todo: decode attributes, none for now

      this.resources = resources;
      this.nodePath = new NodePath<String>(selectElement.getTextContent());
    }

    public List<Tree<String>> select(Parse parse, Tree<String> parseTreeNode) {
      return nodePath.apply(parseTreeNode);
    }
  }


  private static final NodeExtractor buildNodeExtractor(FieldTemplate fieldTemplate, DomElement extractElement, InnerResources resources) {
    NodeExtractor result = null;

    final String type = extractElement.getAttributeValue("type");
    final String data = extractElement.getTextContent();

    // <extract type='record'>relative</extract>
    // <extract type='attribute'>eventClass</extract>
    // <extract type='interp'>date</extract>
    // <extract type='text' delims='true' />

    if ("record".equals(type)) {
      result = new RecordNodeExtractor(fieldTemplate, resources, data);
    }
    else if ("attribute".equals(type)) {
      result = new AttributeNodeExtractor(fieldTemplate, resources, data);
    }
    else if ("interp".equals(type)) {
      result = new InterpNodeExtractor(fieldTemplate, resources, extractElement, data);
    }
    else if ("text".equals(type)) {
      result = new TextNodeExtractor(fieldTemplate, resources, extractElement);
    }

    return result;
  }

  private static interface NodeExtractor {
    public List<Tree<XmlLite.Data>> extract(Parse parse, Tree<String> parseNode);
    public String extractString(Parse parse, Tree<String> parseNode);
  }

  private static abstract class AbstractNodeExtractor implements NodeExtractor {
    protected FieldTemplate fieldTemplate;
    protected InnerResources resources;

    AbstractNodeExtractor(FieldTemplate fieldTemplate, InnerResources resources) {
      this.fieldTemplate = fieldTemplate;
      this.resources = resources;
    }

    protected List<Tree<XmlLite.Data>> cleanup(Tree<XmlLite.Data> result, Parse parse, Tree<String> parseNode, boolean insertFieldNode) {
      List<Tree<XmlLite.Data>> retval = null;

      if (result != null) {
        retval = new ArrayList<Tree<XmlLite.Data>>();

        if (insertFieldNode) {
          final Tree<XmlLite.Data> fieldNode = XmlLite.createTagNode(fieldTemplate.getName());
          fieldNode.addChild(result);
          result = fieldNode;
        }

        retval.add(result);
      }

      return retval;
    }

    // if !repeats, insert a node designating ambiguity if necessary
    protected List<Tree<XmlLite.Data>> cleanup(List<Tree<XmlLite.Data>> result, Parse parse, Tree<String> parseNode, boolean insertFieldNode) {
      List<Tree<XmlLite.Data>> retval = result;

      if (retval != null) {

        final boolean isAmbiguous = !fieldTemplate.repeats() && retval != null && retval.size() > 1;

        if (insertFieldNode || isAmbiguous) {
          final Tree<XmlLite.Data> fieldNode = XmlLite.createTagNode(fieldTemplate.getName());
          if (isAmbiguous) fieldNode.getAttributes().put("ambiguous", "true");
          for (Tree<XmlLite.Data> child : result) fieldNode.addChild(child);

          retval = new ArrayList<Tree<XmlLite.Data>>();
          retval.add(fieldNode);
        }
      }

      return retval;
    }
  }

  private static final class RecordNodeExtractor extends AbstractNodeExtractor {

    private String recordId;

    RecordNodeExtractor(FieldTemplate fieldTemplate, InnerResources resources, String recordId) {
      super(fieldTemplate, resources);
      this.recordId = recordId;
    }

    public List<Tree<XmlLite.Data>> extract(Parse parse, Tree<String> parseNode) {
      List<Tree<XmlLite.Data>> result = null;

      final RecordTemplate recordTemplate = resources.id2recordTemplate.get(recordId);
      if (recordTemplate != null) {
        final Tree<XmlLite.Data> interpNode = recordTemplate.interpret(parse, parseNode, null, fieldTemplate.getName());
        result = super.cleanup(interpNode, parse, parseNode, false);
      }

      return result;
    }

    public String extractString(Parse parse, Tree<String> parseNode) {
      return null;
    }
  }

  private static final class AttributeNodeExtractor extends AbstractNodeExtractor {

    private String attribute;

    AttributeNodeExtractor(FieldTemplate fieldTemplate, InnerResources resources, String attribute) {
      super(fieldTemplate, resources);
      this.attribute = attribute;
    }

    public List<Tree<XmlLite.Data>> extract(Parse parse, Tree<String> parseNode) {
      List<Tree<XmlLite.Data>> result = null;

      //todo: parameterize featureClass (currently null) if/when needed
      final List<Feature> features = ParseInterpretationUtil.getAllTokenFeatures(parseNode, attribute, null);
      if (features != null) {
        result = new ArrayList<Tree<XmlLite.Data>>();
        for (Feature feature : features) {
          final Tree<XmlLite.Data> featureNode = XmlLite.createTextNode(feature.getValue().toString());
          result.add(featureNode);
        }
      }

      return cleanup(result, parse, parseNode, true);
    }

    public String extractString(Parse parse, Tree<String> parseNode) {
      String result = null;

      //todo: parameterize featureClass (currently null) if/when needed
      final Feature feature = ParseInterpretationUtil.getFirstTokenFeature(parseNode, attribute, null);
      if (feature != null) {
        result = feature.getValue().toString();
      }

      return result;
    }
  }

  private static final class InterpNodeExtractor extends AbstractNodeExtractor {

    private String classification;
    private String subType;

    InterpNodeExtractor(FieldTemplate fieldTemplate, InnerResources resources, DomElement extractElement, String classification) {
      super(fieldTemplate, resources);
      this.classification = classification;

      // subTypes 'tree' (default), 'toString' (future: attribute)
      this.subType = extractElement.getAttributeValue("subType", "tree");
    }

    public List<Tree<XmlLite.Data>> extract(Parse parse, Tree<String> parseNode) {
      List<Tree<XmlLite.Data>> result = null;

      final List<ParseInterpretation> interps = ParseInterpretationUtil.getInterpretations(parseNode, classification);
      if (interps != null) {

//todo: apply a disambiguation function to the interps here (e.g. fix 2-digit years) using full context of parse

        result = new ArrayList<Tree<XmlLite.Data>>();
        for (ParseInterpretation interp : interps) {
          final Tree<XmlLite.Data> content = getContent(interp);
          if (content != null) {
            result.add(content);
          }
        }
      }

      return cleanup(result, parse, parseNode, false);
    }

    public String extractString(Parse parse, Tree<String> parseNode) {
      return null;
    }

    private final Tree<XmlLite.Data> getContent(ParseInterpretation interp) {
      Tree<XmlLite.Data> result = null;

      if ("tree".equals(subType)) {
        result = interp.getInterpTree();
      }
      else if ("toString".equals(subType)) {
        result = XmlLite.createTagNode(interp.getClassification());
        result.addChild(XmlLite.createTextNode(interp.toString()));
      }

      return result;
    }
  }

  private static final class TextNodeExtractor extends AbstractNodeExtractor {

    private boolean delims;

    TextNodeExtractor(FieldTemplate fieldTemplate, InnerResources resources, DomElement extractElement) {
      super(fieldTemplate, resources);

      this.delims = extractElement.getAttributeBoolean("delims", false);
    }

    public List<Tree<XmlLite.Data>> extract(Parse parse, Tree<String> parseNode) {
      return super.cleanup(XmlLite.createTextNode(getText(parseNode)), parse, parseNode, true);
    }

    public String extractString(Parse parse, Tree<String> parseNode) {
      return getText(parseNode);
    }

    private String getText(Tree<String> parseNode) {
      String result = null;

      final CategorizedToken cToken = ParseInterpretationUtil.getCategorizedToken(parseNode);
      if (cToken != null) {
        result = delims ? cToken.token.getTextWithDelims() : cToken.token.getText();
      }

      if (result == null) {
        result = parseNode.getLeafText();
      }

      return result;
    }
  }
}
