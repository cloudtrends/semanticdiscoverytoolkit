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
import org.sd.atn.ParseInterpretationUtil;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.DomElement;
import org.sd.xml.XPath;
import org.sd.xml.XmlLite;

/**
 * NodeExtractor for Interp's.
 * <p>
 * @author Spence Koehler
 */
public class InterpNodeExtractor extends AbstractNodeExtractor {
  
  private String classification;
  private String subType;
  private XPath xpath;
  private String select;

  InterpNodeExtractor(FieldTemplate fieldTemplate, InnerResources resources, DomElement extractElement, String classification) {
    super(fieldTemplate, resources);
    this.classification = classification;

    // subTypes 'tree' (default), 'toString' (future: attribute)
    this.subType = extractElement.getAttributeValue("subType", "tree");
    this.xpath = null;
    this.select = null;

    if ("tree".equals(subType)) {
      final String xpathText = extractElement.getAttributeValue("xpath", null);
      if (xpathText != null) {
        this.xpath = new XPath(xpathText);
        this.select = extractElement.getAttributeValue("select", "first");
      }
    }
  }

  public List<Tree<XmlLite.Data>> extract(Parse parse, Tree<String> parseNode, DataProperties overrides, InterpretationController controller) {
    List<Tree<XmlLite.Data>> result = null;

    final List<ParseInterpretation> interps = ParseInterpretationUtil.getInterpretations(parseNode, classification);
    if (interps != null) {

//todo: apply a disambiguation function to the interps here (e.g. fix 2-digit years) using full context of parse

      result = new ArrayList<Tree<XmlLite.Data>>();
      for (ParseInterpretation interp : interps) {
        final List<Tree<XmlLite.Data>> content = getContent(interp);
        if (content != null) {
          result.addAll(content);
        }
      }
    }

    return cleanup(result, parse, parseNode, false);
  }

  public String extractString(Parse parse, Tree<String> parseNode) {
    return null;
  }

  private final List<Tree<XmlLite.Data>> getContent(ParseInterpretation interp) {
    List<Tree<XmlLite.Data>> result = null;
    Tree<XmlLite.Data> singleResult = null;

    if ("tree".equals(subType)) {
      singleResult = interp.getInterpTree();

      if (singleResult != null && xpath != null) {
        final List<Tree<XmlLite.Data>> matches = xpath.getNodes(singleResult);
        if (matches != null && matches.size() > 0) {
          if ("first".equals(select)) {
            singleResult = matches.get(0);
          }
          else if ("last".equals(select)) {
            singleResult = matches.get(matches.size() - 1);
          }
          else {
            result = matches;
          }
        }
        else {
          singleResult = null;
        }
      }
    }
    else {
      final String inputText = interp.getInputText();
      final String toString = interp.toString();

      if ("toString".equals(subType)) {
        singleResult = XmlLite.createTagNode(interp.getClassification());
        singleResult.addChild(XmlLite.createTextNode(toString));

        if (inputText != null && !"".equals(inputText)) {
          singleResult.getData().asTag().setAttribute("altText", inputText);
        }
      }
      else if ("inputText".equals(subType)) {
        singleResult = XmlLite.createTagNode(interp.getClassification());
        singleResult.addChild(XmlLite.createTextNode(inputText));

        if (!"".equals(toString)) {
          singleResult.getData().asTag().setAttribute("altText", toString);
        }
      }
    }

    if (result == null && singleResult != null) {
      result = new ArrayList<Tree<XmlLite.Data>>();
      result.add(singleResult);
    }

    return result;
  }
}
