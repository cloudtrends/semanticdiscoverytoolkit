/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.atn.interp;


import java.util.ArrayList;
import java.util.List;
import org.sd.atn.Parse;
import org.sd.atn.ParseInterpretation;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;
import org.sd.xml.XmlLite;
import org.w3c.dom.NodeList;

/**
 * Template for specifying record-level interpretations.
 * <p>
 * @author Spence Koehler
 */
public class RecordTemplate {

  protected static NodeMatcher buildNodeMatcher(DomElement matchElement, InnerResources resources) {
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


  private DomElement recordNode;
  private InnerResources resources;

  private String name;
  private String id;
  private boolean top;

  private NodeMatcher matcher;
  private List<FieldTemplate> fieldTemplates;
  private FieldTemplate nameOverride;

  RecordTemplate(DomElement recordNode, InnerResources resources) {
    this.recordNode = recordNode;
    this.resources = resources;

    this.name = recordNode.getAttributeValue("name");
    this.id = recordNode.getAttributeValue("type", this.name);

    final DomElement matchElement = (DomElement)recordNode.selectSingleNode("match");
    if (matchElement != null) {
      this.top = true;
      this.matcher = buildNodeMatcher(matchElement, resources);
    }

    this.nameOverride = null;
    this.fieldTemplates = new ArrayList<FieldTemplate>();
    loadFields(recordNode);
  }

  final void loadFields(DomElement recordNode) {

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

  /**
   * Get the config node used to construct this instance.
   */
  public DomElement getRecordNode() {
    return recordNode;
  }

  public InnerResources getInnerResources() {
    return resources;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public boolean isTop() {
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

  ParseInterpretation interpret(Parse parse, DataProperties overrides, InterpretationController controller) {
    ParseInterpretation result = null;
      
    final Tree<String> parseTree = parse.getParseTree();
    final Tree<XmlLite.Data> interpTree = interpret(parse, parseTree, null, name, overrides, controller);

    if (interpTree != null) {
      // add 'rule' attribute to interp (top)
      interpTree.getData().asTag().attributes.put("rule", parse.getRuleId());

      result = new ParseInterpretation(interpTree);
    }

    return result;
  }

  Tree<XmlLite.Data> interpret(Parse parse, Tree<String> parseNode,
                               Tree<XmlLite.Data> parentNode, String fieldName,
                               DataProperties overrides,
                               InterpretationController controller) {

    fieldName = getNameOverride(parse, parseNode, fieldName);
    Tree<XmlLite.Data> result = XmlLite.createTagNode(fieldName);

    controller.interpRecordNodeHook(result, parse, parseNode, parentNode, fieldName, this, true, overrides);
      
    for (FieldTemplate fieldTemplate : fieldTemplates) {
      // select node(s) and extract value(s)...
      final List<Tree<String>> selectedNodes = fieldTemplate.select(parse, parseNode);

      if (selectedNodes != null && selectedNodes.size() > 0) {
        final StringBuilder builder = fieldTemplate.collapse() ? new StringBuilder() : null;

        for (Tree<String> selectedNode : selectedNodes) {
          final List<Tree<XmlLite.Data>> values = fieldTemplate.extract(parse, selectedNode, overrides, controller);

          if (values != null && values.size() > 0) {
            if (builder != null) {  // collapse
              for (Tree<XmlLite.Data> value : values) {
                if (builder.length() > 0) builder.append(' ');
                builder.append(value.getData().asDomNode().getTextContent());
              }
            }
            else {
              for (Tree<XmlLite.Data> value : values) {
                value = controller.interpFieldNodeHook(value, parse, selectedNode, result, fieldTemplate, overrides);
                if (value != null) result.addChild(value);
              }
            }
          }
        }

        if (builder != null) {  // collapse
          Tree<XmlLite.Data> collapsedNode = XmlLite.createTagNode(fieldTemplate.getName());
          collapsedNode.addChild(XmlLite.createTextNode(builder.toString()));
          collapsedNode = controller.interpFieldNodeHook(collapsedNode, parse, parseNode, result, fieldTemplate, overrides);
          if (collapsedNode != null) result.addChild(collapsedNode);
        }
      }
    }

    if (result.numChildren() > 0) {
      if (!id.equals(fieldName)) {
        result.getData().asTag().attributes.put("type", id);
      }
      if (result != null) {
        result = controller.interpRecordNodeHook(result, parse, parseNode, parentNode, fieldName, this, false, overrides);
      }
      if (result != null && parentNode != null) parentNode.addChild(result);
    }
    else result = null;

    return result;
  }
}
