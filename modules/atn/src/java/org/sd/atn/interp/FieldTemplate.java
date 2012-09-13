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


import java.util.List;
import org.sd.atn.Parse;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;
import org.sd.xml.XmlLite;

/**
 * Template for specifying field-level interpretations.
 * <p>
 * @author Spence Koehler
 */
public class FieldTemplate {
  
  public static final NodeSelector buildNodeSelector(DomElement matchElement, InnerResources resources) {
    //todo: decode attributes... for now, only NodePathSelector is being used...
    return new NodePathSelector(matchElement, resources);
  }


  protected static NodeExtractor buildNodeExtractor(FieldTemplate fieldTemplate, DomElement extractElement, InnerResources resources) {
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


  private DomElement fieldElement;

  private String name;
  private boolean repeats;
  private boolean nameOverride;
  private boolean collapse;
  private NodeSelector selector;
  private NodeExtractor extractor;
  private String ifOption;
  private String unlessOption;
  private boolean verbose;

  private NodeSelector nameSelector;
  private NodeExtractor nameExtractor;

  FieldTemplate(DomElement fieldElement, InnerResources resources) {
    this.fieldElement = fieldElement;
    this.name = fieldElement.getAttributeValue("name");
    this.repeats = fieldElement.getAttributeBoolean("repeats", false);
    this.nameOverride = "nameOverride".equals(fieldElement.getAttributeValue("type", null));
    this.collapse = fieldElement.getAttributeBoolean("collapse", false);
    this.ifOption = fieldElement.getAttributeValue("if", null);
    this.unlessOption = fieldElement.getAttributeValue("unless", null);
    this.verbose = fieldElement.getAttributeBoolean("verbose", false);

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

  public boolean isNameOverride() {
    return nameOverride;
  }

  public String getName() {
    return name;
  }

  public boolean repeats() {
    return repeats;
  }

  public boolean collapse() {
    return collapse;
  }

  List<Tree<String>> select(Parse parse, Tree<String> parseNode) {
    final List<Tree<String>> result = selector.select(parse, parseNode);

    if (verbose) {
      System.out.println("FieldTemplate (" + name + ") select " + ((result == null) ? "FAIL" : "SUCCESS"));
    }

    return result;
  }

  List<Tree<XmlLite.Data>> extract(Parse parse, Tree<String> parseNode, DataProperties overrides, InterpretationController controller) {
    List<Tree<XmlLite.Data>> result = null;
    boolean doExtract = true;

    if (ifOption != null) {
      final boolean hasIfOption = overrides == null ? false : overrides.getBoolean(ifOption, false);
      if (!hasIfOption) doExtract = false;
    }

    if (unlessOption != null) {
      final boolean hasUnlessOption = overrides == null ? false : overrides.getBoolean(unlessOption, false);
      if (hasUnlessOption) doExtract = false;
    }

    if (doExtract) {
      result = extractor.extract(parse, parseNode, overrides, controller);

      if (verbose) {
        System.out.println("FieldTemplate (" + name + ") extract " + ((result == null) ? "FAIL" : "SUCCESS"));

        if (result == null) {
          System.out.println("\t" + parseNode + " [attributes:" + parseNode.getAttributes() + "]");
        }
      }
    }

    return result;
  }

  String extractString(Parse parse, Tree<String> parseNode) {
    return extractor.extractString(parse, parseNode);
  }
}
