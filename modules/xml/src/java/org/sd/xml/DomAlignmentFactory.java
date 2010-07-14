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
import java.io.InputStream;
import org.sd.util.tree.Tree;
import org.sd.util.tree.align.NodeComparer;
import org.sd.util.tree.align.TextExtractor;
import org.sd.util.tree.align.TreeAlignment;

/**
 * Factory class for obtaining DomTreeAlignment instances.
 * <p>
 * @author Spence Koehler
 */
public class DomAlignmentFactory {
  
  public static final DomNodeComparer DOM_NODE_COMPARER = new DomNodeComparer();
  public static final DomTextExtractor DOM_TEXT_EXTRACTOR = new DomTextExtractor();

  public static TreeAlignment<XmlLite.Data> getXmlAlignment(File sourceXml1, File sourceXml2, boolean isHtml) throws IOException {
    final Tree<XmlLite.Data> tree1 = loadXmlTree(sourceXml1, isHtml);
    final Tree<XmlLite.Data> tree2 = loadXmlTree(sourceXml2, isHtml);

    return getXmlAlignment(tree1, tree2);
  }

  /**
   *  Create a TreeAlignment instance from the xml (html) to be read in
   *  through the given streams.
   */
  public static TreeAlignment<XmlLite.Data> getXmlAlignment(InputStream sourceXml1, InputStream sourceXml2, boolean isHtml) throws IOException {
    final Tree<XmlLite.Data> tree1 = loadXmlTree(sourceXml1, isHtml);
    final Tree<XmlLite.Data> tree2 = loadXmlTree(sourceXml2, isHtml);

    return getXmlAlignment(tree1, tree2);
  }

  /**
   *  Create a TreeAlignment instance from the given (xml/html) trees.
   */
  public static DomTreeAlignment getXmlAlignment(Tree<XmlLite.Data> tree1, Tree<XmlLite.Data> tree2) {
    return new DomTreeAlignment(tree1, tree2, DOM_NODE_COMPARER, DOM_TEXT_EXTRACTOR);
  }

  /**
   *  Load xml from the stream into a Tree&lt;String&gt; instance.
   */
  public static Tree<XmlLite.Data> loadXmlTree(File sourceXml, boolean isHtml) throws IOException {
    final DomDocument domDocument = XmlFactory.loadDocument(sourceXml, isHtml);
    final DomElement domElement = domDocument.getDocumentDomElement();
    return domElement.asTree();
  }

  /**
   *  Load xml from the stream into a Tree&lt;String&gt; instance.
   */
  public static Tree<XmlLite.Data> loadXmlTree(InputStream sourceXml, boolean isHtml) throws IOException {
    return XmlFactory.readXmlTree(sourceXml, Encoding.UTF8, true, isHtml, null, false);
  }


  /**
	 *  Simple implementation of a NodeComparer applicable to Xml data.
   */
	public static class DomNodeComparer implements NodeComparer<XmlLite.Data> {

		public DomNodeComparer() { }

    /**
		 *  Determine whether the two nodes match each other.
     */
		public boolean matches(Tree<XmlLite.Data> node1, Tree<XmlLite.Data> node2) {
			return node1.getData().equals(node2.getData());
		}
	}


  /**
	 *  Simple implementation of a TextExtractor applicable to Xml data.
   */
	public static class DomTextExtractor implements TextExtractor<XmlLite.Data> {

		private DomTextExtractor() { }

    /**
		 *  Extract text contained in the given node, possibly returning empty or
		 *  null.
     */
		public String extractText(Tree<XmlLite.Data> node) {
      String result = null;

      final XmlLite.Text textData = node.getData().asText();

      if (textData != null) {
        result = textData.text;
      }

			return result == null ? "" : result;
		}
	}

}
