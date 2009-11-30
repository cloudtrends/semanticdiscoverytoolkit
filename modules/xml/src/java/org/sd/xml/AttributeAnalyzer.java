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


import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.sd.util.Histogram;
import org.sd.util.KVPair;
import org.sd.util.tree.TraversalIterator;
import org.sd.util.tree.Tree;

/**
 * Utility to analyze attributes on nodes.
 * <p>
 * @author Spence Koehler
 */
public class AttributeAnalyzer {

  private Tree<XmlLite.Data> xmlTree;
  private Histogram<KVPair<String, String>> nodeAttrHistogram;
  private Histogram<KVPair<String, String>> attrValueHistogram;

  public AttributeAnalyzer(Tree<XmlLite.Data> xmlTree) {
    this.xmlTree = xmlTree;
    createHistograms(xmlTree);
  }

  private final void createHistograms(Tree<XmlLite.Data> xmlTree) {
    this.nodeAttrHistogram = new Histogram<KVPair<String, String>>();
    this.attrValueHistogram = new Histogram<KVPair<String, String>>();

    for (Iterator<Tree<XmlLite.Data>> iter = xmlTree.iterator(Tree.Traversal.DEPTH_FIRST); iter.hasNext(); ) {
      final Tree<XmlLite.Data> xmlNode = iter.next();

      final XmlLite.Tag tag = xmlNode.getData().asTag();
      if (tag != null) {
        for (Map.Entry<String, String> entry : tag.attributes.entrySet()) {
          final String attrName = entry.getKey();
          final String attrValue = entry.getValue();

          nodeAttrHistogram.add(new KVPair<String, String>(tag.name, attrName));
          attrValueHistogram.add(new KVPair<String, String>(tag.name + "." + attrName, attrValue));
        }
      }
    }
  }


  public static void main(String[] args) throws IOException {
    final Tree<XmlLite.Data> xmlTree = XmlFactory.readXmlTree(new File(args[0]), true, true, false);
    final AttributeAnalyzer aa = new AttributeAnalyzer(xmlTree);

    System.out.println(aa.nodeAttrHistogram);
    System.out.println(aa.attrValueHistogram);
  }
}
