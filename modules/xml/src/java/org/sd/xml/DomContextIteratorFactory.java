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
import java.util.LinkedList;
import java.util.List;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Factory class for creating DomContextIterator instances using text or text block nodes.
 * <p>
 * @author Spence Koehler
 */
public class DomContextIteratorFactory {
  
  /**
   * Build a DomContextIterator over the single DomNode.
   */
  public static final DomContextIterator getDomContextIterator(DomNode domNode) {
    final DomContextIterator result = new DomContextIterator();
    result.add(domNode);
    return result;
  }

  /**
   * Build a DomContextIterator over the specific DomNode instances.
   */
  public static final DomContextIterator getDomContextIterator(List<DomNode> domNodes) {
    final DomContextIterator result = new DomContextIterator();

    if (domNodes != null) {
      for (DomNode domNode : domNodes) {
        result.add(domNode);
      }
    }

    return result;
  }

  /**
   * Build a DomContextIterator over text or block nodes in the xmlFile.
   */
  public static final DomContextIterator getDomContextIterator(File xmlFile, boolean isHtml, boolean textBlocks) throws IOException {
    DomContextIterator result = null;

    if (textBlocks) {
      // DomTextBlockIterationStrategy
      result = getDomContextIterator(xmlFile, isHtml, DomTextBlockIterationStrategy.INSTANCE);
    }
    else {
      // DomTextIterationStrategy
      result = getDomContextIterator(xmlFile, isHtml, DomTextIterationStrategy.INSTANCE);
    }

    return result;
  }

  /**
   * Build a DomContextIterator over the strategy-selected nodes in the xmlFile.
   */
  public static final DomContextIterator getDomContextIterator(File xmlFile, boolean isHtml, DomIterationStrategy strategy) throws IOException {
    final DomContextIterator result = new DomContextIterator();
    
    final DomDocument domDocument = XmlFactory.loadDocument(xmlFile, isHtml);
    loadDomNodes(domDocument.getDocumentDomElement(), strategy, result);

    return result;
  }

  /**
   * Build a DomContextIterator over the strategy-selected, diff-negated nodes in the xmlFile.
   */
  public static final DomContextIterator getDomContextIterator(File xmlFile, File diffFile, boolean isHtml, DomIterationStrategy strategy) throws IOException {
    DomContextIterator result = null;

    if (diffFile == null) {
      result = getDomContextIterator(xmlFile, isHtml, strategy);
    }
    else {
      final DomTreeAlignment treeAlignment = (DomTreeAlignment)DomAlignmentFactory.getXmlAlignment(xmlFile, diffFile, isHtml);
      result = getDomContextIterator(treeAlignment.getXmlDisjunctionNodes1(), strategy);
    }

    return result;
  }

  /**
   * Build a DomContextIterator over the strategy-selected, diff-negated nodes under the rootNode.
   */
  public static final DomContextIterator getDomContextIterator(DomNode rootNode, DomNode diffNode, DomIterationStrategy strategy) {
    DomContextIterator result = null;

    if (diffNode == null) {
      result = new DomContextIterator();
      loadDomNodes(rootNode, strategy, result);
    }
    else {
      final DomTreeAlignment treeAlignment = DomAlignmentFactory.getXmlAlignment(rootNode.asTree(), diffNode.asTree());
      result = getDomContextIterator(treeAlignment.getXmlDisjunctionNodes1(), strategy);
    }

    return result;
  }

  /**
   * Build a DomContextIterator over the strategy-selected nodes under each domNode.
   */
  public static final DomContextIterator getDomContextIterator(List<DomNode> domNodes, DomIterationStrategy strategy) {
    final DomContextIterator result = new DomContextIterator();
    
    for (DomNode domNode : domNodes) {
      loadDomNodes(domNode, strategy, result);
    }

    return result;
  }

  /**
   * Add strategy-selected nodes under the rootNode to the DomContextIterator result.
   */
  public static final void loadDomNodes(DomNode rootNode, DomIterationStrategy strategy, DomContextIterator result) {
    if (rootNode == null) return;

    final LinkedList<DomNode> queue = new LinkedList<DomNode>();
    queue.addLast(rootNode);
    
    while (queue.size() > 0) {
      final DomNode curNode = queue.removeFirst();

      if (strategy.isIterableNode(curNode)) {
        result.add(curNode);
      }
      else if (curNode.hasChildNodes()) {
        final NodeList childNodes = curNode.getChildNodes();
        int childNum = 0;
        for (int i = 0; i < childNodes.getLength(); ++i) {
          final Node childNode = childNodes.item(i);
          if (childNode == null) continue;
          final short childNodeType = childNode.getNodeType();
          if (childNodeType == Node.ELEMENT_NODE || childNodeType == Node.TEXT_NODE) {
            final DomNode childDomNode = (DomNode)childNode;
            if (strategy.shouldQueueNode(childDomNode)) {
              queue.add(childNum++, childDomNode);
            }
          }
        }
      }
    }
  }
}
