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
package org.sd.atn.interp;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.sd.atn.Parse;
import org.sd.atn.ParseInterpretation;
import org.sd.atn.ParseInterpreter;
import org.sd.atn.ResourceManager;
import org.sd.util.Usage;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;
import org.sd.xml.DomNode;
import org.sd.xml.XmlLite;
import org.w3c.dom.NodeList;

/**
 * Abstract base class for a (field and record) template-based parse interpreter.
 * <p>
 * @author Spence Koehler
 */
@Usage(notes =
       "An abstract org.sd.org.atn.ParseInterpreter base implementation\n" +
       "that allows for parse node targeting using org.sd.util.tree.NodePaths\n" +
       "(similar to XPaths, but for parse trees) and manipulating the contents\n" +
       "of selected nodes for generating an org.sd.atn.ParseInterpretation,\n" +
       "where generated interpretations are viewed as records (non-terminal\n" +
       "XML elements) containing fields (nested or terminal elements)."
  )
public abstract class TemplateParseInterpreter implements ParseInterpreter {
  
  /**
   * Factory for building an interpretation controller instance for thread safety
   * during concurrent interpretations.
   */
  protected abstract InterpretationController buildInterpretationController();

  private String[] classifications;
  private InnerResources resources;
  private List<RecordTemplate> topTemplates;
  private boolean trace;

  private static final Object interpLock = new Object();

  public TemplateParseInterpreter(DomNode domNode, ResourceManager resourceManager) {
    this.resources = new InnerResources(resourceManager);
    this.topTemplates = new ArrayList<RecordTemplate>();

    load(domNode, resourceManager);
  }

  private final void load(DomNode domNode, ResourceManager resourceManager) {

    this.trace = domNode.getAttributeBoolean("trace", false);
    if (!this.trace && resourceManager.getOptions().getBoolean("TemplateParseInterpreter.trace", false)) {
      this.trace = true;
    }

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
  public List<ParseInterpretation> getInterpretations(Parse parse, DataProperties overrides) {
    List<ParseInterpretation> result = null;

    final InterpretationController interpController = buildInterpretationController();

    for (RecordTemplate topTemplate : topTemplates) {
      if (topTemplate.matches(parse)) {
        final boolean doInterpret = interpController.foundMatchingTemplateHook(topTemplate, parse, overrides);
        ParseInterpretation interp = doInterpret ? topTemplate.interpret(parse, overrides, interpController) : null;
        if (interp != null) {
          interp = interpController.interpretationHook(interp, parse, overrides);
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
   * Supplement this interpreter according to the given domElement.
   */
  public void supplement(DomElement domElement) {
    load(domElement, resources.resourceManager);
  }

  protected InnerResources getInnerResources() {
    return resources;
  }

  protected List<RecordTemplate> getTopTemplates() {
    return topTemplates;
  }

  protected boolean getTrace() {
    return trace;
  }

  protected void setTrace(boolean trace) {
    this.trace = trace;
  }

  // fill id2recordTemplate and topTemplates
  private final void loadRecords(DomNode domNode, InnerResources resources) {
    final NodeList recordNodes = domNode.selectNodes("record");
    if (recordNodes != null) {
      final int numRecordNodes = recordNodes.getLength();
      for (int recordNodeNum = 0; recordNodeNum < numRecordNodes; ++recordNodeNum) {
        final DomElement recordNode = (DomElement)recordNodes.item(recordNodeNum);

        boolean override = recordNode.getAttributeBoolean("override", true);

        if (!override) {
          // augment existing record template
          final RecordTemplate recordTemplate = findRecordTemplate(recordNode);
          if (recordTemplate != null) {
            // augment
            recordTemplate.loadFields(recordNode);
          }
          else {
            // no existing template found, so treat as new/override
            override = true;
          }
        }

        if (override) {
          // override (or add new) record template
          final RecordTemplate recordTemplate = new RecordTemplate(recordNode, resources);
          final RecordTemplate displaced = resources.add(recordTemplate);
          if (displaced != null && displaced.isTop()) topTemplates.remove(displaced);
          if (recordTemplate.isTop()) topTemplates.add(recordTemplate);
        }
      }
    }
  }

  private final RecordTemplate findRecordTemplate(DomElement recordNode) {
    final String name = recordNode.getAttributeValue("name");
    final String id = recordNode.getAttributeValue("type", name);
    return resources.get(id);
  }

  protected static void trace(String type, Tree<XmlLite.Data> newNode, String templateName, Boolean start) {
    final StringBuilder result = new StringBuilder();

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

    System.out.println("*** TemplateParseInterpreter " + type + " (" + templateName + ") " + result.toString());
  }
}
