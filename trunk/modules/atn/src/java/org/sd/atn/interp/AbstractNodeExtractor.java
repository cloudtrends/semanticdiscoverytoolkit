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
import org.sd.util.tree.Tree;
import org.sd.xml.XmlLite;

/**
 * Abstract implementation of the NodeExtractor interface.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractNodeExtractor implements NodeExtractor {
  
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
