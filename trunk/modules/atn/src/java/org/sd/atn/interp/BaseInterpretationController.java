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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sd.atn.Parse;
import org.sd.atn.ParseInterpretation;
import org.sd.atn.ParseInterpretationUtil;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.XmlLite;

/**
 * A base interpretation controller.
 * <p>
 * @author Spence Koehler
 */
public abstract class BaseInterpretationController <M> implements InterpretationController {
  
  protected boolean trace;
  protected boolean disabled;

  protected List<M> models;

  protected BaseInterpretationController(boolean trace, boolean disabled) {
    this.trace = trace;
    this.disabled = disabled;

    this.models = new ArrayList<M>();
  }

  /**
   * Get the final model(s) built.
   */
  public abstract List<M> getFinalModels();

  /**
   * Get the final interpretation object (to be set on the parseInterpretation instance.)
   */
  public abstract Serializable getInterpObject();

  /**
   * Get the final interpretation string (to be set on the parseInterpretation instance as its toStringOverride.)
   */
  public abstract String getInterpString();

  /**
   * Handle a record command.
   */
  protected abstract void handleRecordCommand(Tree<XmlLite.Data> recordNode, String cmd,
                                              String fieldName, boolean start, DataProperties overrides,
                                              RecordTemplate recordTemplate, Parse parse,
                                              Tree<String> parseNode, Tree<XmlLite.Data> parentNode);

  /**
   * Handle a field command.
   */
  protected abstract void handleFieldCommand(Tree<XmlLite.Data> fieldNode, String cmd,
                                             String fieldName, String fieldText,
                                             Tree<String> selectedNode, DataProperties overrides,
                                             FieldTemplate fieldTemplate, Parse parse,
                                             Tree<XmlLite.Data> parentNode);


  /**
   * @return true to execute recordTemplate.interpret(parse); otherwise, false.
   */
  public boolean foundMatchingTemplateHook(RecordTemplate recordTemplate, Parse parse, DataProperties overrides) {
    return true;
  }

  /**
   * Hook on a final interpretation.
   */
  public ParseInterpretation interpretationHook(ParseInterpretation interp, Parse parse, DataProperties overrides) {

    final Serializable interpObject = getInterpObject();
    if (interpObject != null) {
      interp.setInterpretation(getInterpObject());
    }

    final String interpString = getInterpString();
    if (interpString != null) {
      interp.setToStringOverride(interpString);
    }

    if (trace) System.out.println("*** BaseTemplateParseInterpreter interpreted '" + parse.getParseTree() + "'");

    return interp;
  }

  /**
   * Hook on each record interpNode just after creation and before insertion
   * as a child into its tree.
   */
  public Tree<XmlLite.Data> interpRecordNodeHook(Tree<XmlLite.Data> recordNode, Parse parse,
                                                 Tree<String> parseNode, Tree<XmlLite.Data> parentNode,
                                                 String fieldName, RecordTemplate recordTemplate,
                                                 boolean start, DataProperties overrides) {

    final String cmd = recordTemplate.getRecordNode().getAttributeValue("cmd", "no-op");

    if (trace) trace("record", recordNode, cmd, fieldName, start);

    if (disabled) return recordNode;

    handleRecordCommand(recordNode, cmd, fieldName, start, overrides, recordTemplate,
                        parse, parseNode, parentNode);

    return recordNode;
  }
  
  /**
   * Hook on each field interpNode just after creation and before insertion
   * as a child into its tree.
   */
  public Tree<XmlLite.Data> interpFieldNodeHook(Tree<XmlLite.Data> fieldNode, Parse parse,
                                                Tree<String> selectedNode, Tree<XmlLite.Data> parentNode,
                                                FieldTemplate fieldTemplate, DataProperties overrides) {
      
    final String fieldName = fieldTemplate.getName();
    final String cmd = fieldTemplate.getFieldElement().getAttributeValue("cmd", "NOOP");

    if (trace) trace("field", fieldNode, cmd, fieldName, null);

    if (disabled) return fieldNode;

    // text (and attribute) extraction fields
    final String fieldText = getText(fieldNode);

    handleFieldCommand(fieldNode, cmd, fieldName, fieldText, selectedNode, overrides, fieldTemplate,
                       parse, parentNode);

    return fieldNode;
  }

  protected final void checkSync(String expectedCmd, String cmd, Integer commandId) {
    if (!expectedCmd.equals(cmd)) outOfSync(cmd, commandId);
  }

  protected final void outOfSync(String cmd, Integer commandId) {
    throw new IllegalStateException("Command '" + cmd + "' out of sync with id=" + commandId + "!");
  }


  protected final void trace(String type, Tree<XmlLite.Data> newNode, String cmd, String templateName, Boolean start) {
    final StringBuilder result = new StringBuilder();

    result.append(cmd).append(" ");

    if (start != null) {
      result.append("[start=").append(start).append("] ");
    }

    final DomNode domNode = newNode.getData().asDomNode();
    if (domNode.getNodeType() == DomNode.ELEMENT_NODE) {
      ((DomElement)domNode).asFlatString(result);
    }
    else {
      result.append(domNode.getTextContent());
    }

    System.out.println("*** BaseTemplateParseInterpreter " + type + " (" + templateName + ") " + result.toString());
  }


  protected final String getText(Tree<XmlLite.Data> xmlNode) {
    return xmlNode.getData().asDomNode().getTextContent();
  }

  protected final String getTag(Tree<XmlLite.Data> xmlNode) {
    final XmlLite.Tag tag = xmlNode.getData().asTag();
    return tag == null ? null : tag.name;
  }
}
