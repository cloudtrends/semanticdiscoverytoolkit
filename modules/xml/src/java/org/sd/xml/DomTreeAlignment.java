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
package org.sd.xml;


import java.util.ArrayList;
import java.util.List;
import org.sd.util.tree.Tree;
import org.sd.util.tree.align.LeafWrapper;
import org.sd.util.tree.align.NodeComparer;
import org.sd.util.tree.align.TextExtractor;
import org.sd.util.tree.align.TreeAlignment;

/**
 * TreeAlignment extension for xml data.
 * <p>
 * @author Spence Koehler
 */
public class DomTreeAlignment extends TreeAlignment<XmlLite.Data> {
  
  public DomTreeAlignment(Tree<XmlLite.Data> tree1, Tree<XmlLite.Data> tree2,
                          NodeComparer<XmlLite.Data> nodeComparer, TextExtractor<XmlLite.Data> textExtractor) {
    super(tree1, tree2, nodeComparer, textExtractor);
  }

  /**
   *  Accessor for the original xml disjunction nodes from the first tree.
   */
  public List<DomNode> getXmlDisjunctionNodes1() {
    return getXmlDisjunctionNodes(getTree1(), getLeafDiffer().getLeafWrappers1());
  }

  /**
   *  Accessor for the original xml disjunction nodes from the second tree.
   */
  public List<DomNode> getXmlDisjunctionNodes2() {
    return getXmlDisjunctionNodes(getTree2(), getLeafDiffer().getLeafWrappers2());
  }

  private List<DomNode> getXmlDisjunctionNodes(Tree<XmlLite.Data> tree, List<LeafWrapper<XmlLite.Data>> leafWrappers) {
    List<DomNode> result = null;

    final DomDocument domDocument = getDomDocument(tree);
    if (domDocument != null) {
      final List<Tree<XmlLite.Data>> disjNodes = getLeafDiffer().getDisjunctionNodes(leafWrappers);
      if (disjNodes != null) {
        result = new ArrayList<DomNode>();

        for (Tree<XmlLite.Data> disjNode : disjNodes) {
          final DomNode matchingNode = disjNode.getData().asDomNode();
          result.add(matchingNode);
        }
      }
    }

    return result;
  }

  private DomDocument getDomDocument(Tree<XmlLite.Data> tree) {
    return tree.getData().asDomNode().getOwnerDomDocument();
  }
}
