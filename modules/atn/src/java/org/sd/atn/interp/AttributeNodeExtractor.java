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
import org.sd.atn.ParseInterpretationUtil;
import org.sd.token.Feature;
import org.sd.util.tree.Tree;
import org.sd.xml.DataProperties;
import org.sd.xml.XmlLite;

/**
 * NodeExtractor for Attribute's.
 * <p>
 * @author Spence Koehler
 */
public class AttributeNodeExtractor extends AbstractNodeExtractor {
  
  private String attribute;

  AttributeNodeExtractor(FieldTemplate fieldTemplate, InnerResources resources, String attribute) {
    super(fieldTemplate, resources);
    this.attribute = attribute;
  }

  public List<Tree<XmlLite.Data>> extract(Parse parse, Tree<String> parseNode, DataProperties overrides, InterpretationController controller) {
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

    // include attributes on the parse node
    if (parseNode.hasAttributes()) {
      final Object value = parseNode.getAttributes().get(attribute);
      if (value != null) {
        if (result == null) result = new ArrayList<Tree<XmlLite.Data>>();
        final Tree<XmlLite.Data> featureNode = XmlLite.createTextNode(value.toString());
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
