/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sd.util.tree.NodePath;
import org.sd.util.tree.Tree;
import org.sd.xml.XPathApplicator;
import org.sd.xml.XmlLite;

/**
 * A helper object for (efficiently) building generic parses/parse results.
 * <p>
 * @author Spence Koehler
 */
public class GenericParseHelper {
  
  private Map<String, NodePath<String>> nodePaths;  //performance cache
  private XPathApplicator xpathApplicator;          //performance cache

  public GenericParseHelper() {
    this.nodePaths = new HashMap<String, NodePath<String>>();
    this.xpathApplicator = new XPathApplicator();
  }

  public GenericParseResults buildGenericParseResults(ParseOutputCollector parseOutput) {
    return new GenericParseResults(parseOutput, this);
  }

  public GenericParse buildGenericParse(ParseInterpretation interp) {
    GenericParse result = null;

    if (interp != null && interp.getParse() != null) {
      result = new GenericParse(interp, this);
    }

    return result;
  }

  /**
   * Package protected for access from GenericParse.
   */
  final NodePath<String> getNodePath(String id) {
    if (nodePaths == null) nodePaths = new HashMap<String, NodePath<String>>();
    NodePath<String> result = nodePaths.get(id);
    if (result == null) {
      result = new NodePath<String>("**." + id);
      nodePaths.put(id, result);
    }
    return result;
  }

  final List<Tree<XmlLite.Data>> getNodes(String xpath, Tree<XmlLite.Data> interpTree) {
    return xpathApplicator.getNodes(xpath, interpTree);
  }
}
