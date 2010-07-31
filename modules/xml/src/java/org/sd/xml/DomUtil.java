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


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.sd.io.DataHelper;
import org.sd.util.tree.Tree;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utilities for interacting with a dom.
 * <p>
 * @author Spence Koehler
 */
public class DomUtil {
  
  /**
   *  Get the first child that is an DomElement under the given domElement
   *  or null.
   */
  public static DomElement getFirstChild(DomElement domElement) {
    DomElement result = null;

    if (domElement != null && domElement.hasChildNodes()) {
      final NodeList childNodes = domElement.getChildNodes();
      for (int i = 0; i < childNodes.getLength(); ++i) {
        final Node childNode = childNodes.item(i);
        if (childNode.getNodeType() == DomNode.ELEMENT_NODE) {
          result = (DomElement)childNode;
          break;
        }
      }
    }

    return result;
  }


  /**
   *  Returns the integer value of the node's text or the default value or throws
   *  and expection if the existing text in a found element cannot be parsed as an
   *  integer.
   */
  public static int getSelectedNodeInt(DomElement domElement, String xPath, int defaultValue) {
    final DomNode selectedNode = (DomNode)domElement.selectSingleNode(xPath);
    return getNodeInt(selectedNode, defaultValue);
  }


  /**
   *  Returns the integer value of the node's text or the default value or throws
   *  and expection if the existing text cannot be parsed as an integer.
   */
  public static int getNodeInt(DomNode domNode, int defaultValue) {
    int result = defaultValue;

    if (domNode != null) {
      final String text = domNode.getTextContent();
      if (text != null && !"".equals(text)) {
        result = Integer.parseInt(text);
      }
    }

    return result;
  }


  /**
   *  Utility to get the trimmed node text under a dom node.
   * 
   *  This trims extra whitespace around node text and inserts a single
   *  space character between node texts.
   * 
   *  NOTE: Currently text is not trimmed within a text node's String.
   */
  public static String getTrimmedNodeText(DomNode domNode) {
    final DomTextWalker textWalker = new DomTextWalker(domNode);
    return textWalker.getTrimmedNodeText();
  }

  /**
   *  Get the position of the deep node's trimmed text as an offset within
   *  the ancestor's node text.
   */
  public static int getTextPos(DomNode ancestorNode, DomNode deepNode) {
    final DomTextWalker textWalker = new DomTextWalker(ancestorNode);
    return textWalker.getTextPos(deepNode);
  }


  /**
   *  Given a dom node over text, find the deepest dom node that fully
   *  encompasses the portion of text from startPos to endPos.
   */
  public static DomNode getDeepestNode(DomNode topNode, int startPos, int endPos) {
    int[] newStartPos = new int[]{0};
    int[] newEndPos = new int[]{0};
    return getDeepestNode(topNode, startPos, endPos, newStartPos, newEndPos);
  }

  /**
   *  Given a dom node over text, find the text nodes that contain the
   *  consecutive text from startPos to endPos.
   *
   *  @param topNode The top dom node over the text
   *  @param startPos The starting position of text under the topNode (inclusive)
   *  @param endPos The ending position of text under the topNode (exclusive)
   *  @param newStartPos The starting position of text within result[0].
   *  @param newEndPos The ending position of text within result[N-1].
   *
   *  @returns The list of consecutive non-empty text nodes encompassing the text.
   */
  public static List<DomText> getTextNodes(DomNode topNode, int startPos, int endPos, int[] newStartPos, int[] newEndPos) {
    final DomTextWalker textWalker = new DomTextWalker(topNode);
    return textWalker.getTextNodes(startPos, endPos, newStartPos, newEndPos);
  }


  /**
   *  Given a dom node over text, find the deepest dom node that fully
   *  encompasses the portion of text from startPos to endPos.
   * 
   *  Return in the reference parameters the new starting and ending position
   *  of the text with respect to the returned node.
   */
  public static DomNode getDeepestNode(DomNode topNode, int startPos, int endPos, int[] newStartPos, int[] newEndPos) {
    DomNode result = topNode;

    if (topNode.getNodeType() == DomNode.TEXT_NODE) return result;

    final DomTextWalker textWalker = new DomTextWalker(topNode);
    final List<DomText> textNodes = textWalker.getTextNodes(startPos, endPos, newStartPos, newEndPos);

    if (textNodes != null) {
      final DomNode startNode = textNodes.get(0);
      final DomNode endNode = textNodes.get(textNodes.size() - 1);

      if (startNode != null && endNode != null) {
        if (startNode != endNode) {
          result = getDeepestCommonAncestor(startNode, endNode);

          if (result != startNode) {
            if (result == topNode) {
              // back where we started from, no need to waste cycles computing
              newStartPos[0] = startPos;
              newEndPos[0] = endPos;
            }
            else {
              // shift newStartPos and newEndPos relative to result
              newStartPos[0] += DomUtil.getTextPos(result, startNode);
              newEndPos[0] = newStartPos[0] + (endPos - startPos);
            }
          }
        }
        else {
          result = startNode;
        }
      }
    }

    return result;
  }


  /**
   *  Determine whether the given DomNode is a non-empty text node.
   */
  public static boolean isNonEmptyTextNode(Node domNode) {
    boolean result = false;

    if (domNode.getNodeType() == Node.TEXT_NODE) {
      final String nodeText = domNode.getNodeValue();
      result = nodeText != null && !"".equals(nodeText.trim());
    }

    return result;
  }


  /**
   *  Create an dom element with the given name, optionally adding as a child of refNode.
   */
  public static DomElement createElement(DomNode refNode, String name, boolean addAsChild) {
    final DomElement newElement = (DomElement)refNode.getOwnerDocument().createElement(name);

    if (addAsChild) {
      refNode.appendChild(newElement);
    }

    return newElement;
  }

  /**
   *  Add an attribute to the given dom node.
   */
  public static DomAttribute addAttribute(DomElement domElement, String attrName, String attrValue) {
    domElement.setAttribute(attrName, attrValue);
    return (DomAttribute)domElement.getAttributeNode(attrName);
  }


  /**
   *  Prune the given DomNode from its document.
   */
  public static void pruneNode(DomNode domNode) {
    final Node parentNode = domNode.getParentNode();

    if (parentNode != null) {
      parentNode.removeChild(domNode);
    }
  }


  /**
   *  Get the depth of the given node in its DOM tree, where the root
   *  (DocumentElement) is at depth 0.
   */
  public static int getDepth(DomNode domNode) {
    int result = -1;

    final short nodeType = domNode.getNodeType();
    if (nodeType == DomNode.ELEMENT_NODE) {
      result = domNode.getDepth() + 1;
    }
    else if (nodeType == DomNode.DOCUMENT_NODE) {
      result = 0;
    }

    return result;
  }

  /**
   *  Get the DomNode instances from the root to the given node (inclusive).
   */
  public static LinkedList<DomNode> getRootPath(DomNode domNode) {
    final LinkedList<DomNode> result = new LinkedList<DomNode>();

    while (domNode != null) {
      result.addFirst(domNode);
      domNode = (DomNode)domNode.getParentNode();
    }

    return result;
  }


  /**
   *  Build a path String for the domNode, optionally including sibling
   *  index information and text content.
   */
  public static String buildPathString(DomNode domNode, boolean indexFlag, boolean textFlag) {
    final LinkedList<DomNode> rootPath = DomUtil.getRootPath(domNode);
    return buildPathString(rootPath, indexFlag, textFlag);
  }

  /**
   *  Build a path String for the rootPath, optionally including sibling
   *  index information and text content.
   */
  public static String buildPathString(LinkedList<DomNode> rootPath, boolean indexFlag, boolean textFlag) {
    final StringBuilder result = new StringBuilder();

    for (DomNode curDomNode : rootPath) {
      final short nodeType = curDomNode.getNodeType();
      if (nodeType == DomNode.TEXT_NODE) {
        if (textFlag) {
          result.append("='").append(curDomNode.getNodeValue()).append('\'');
        }
      }
      else {
        if (result.length() > 0) result.append('/');
        result.append(curDomNode.getNodeName());

        if (indexFlag) {
          final int sibNum = DomUtil.getRelativeSiblingNumber(curDomNode);
          result.append('[').append(sibNum).append(']');
        }
      }
    }

    return result.toString();
  }

  public static DomNode findDomNode(DomElement rootElement, String indexedPathString) {
    DomNode result = rootElement;

    final String[] pathPieces = indexedPathString.split("/");
    final int[] pathIndex = new int[]{0};
    final String rootTag = parseIndexedPathPiece(pathPieces[0], pathIndex);

    if (!rootElement.getNodeName().equals(rootTag) || DomUtil.getRelativeSiblingNumber(rootElement) != pathIndex[0]) {
      // rootElement doesn't align with first indexedPathString
      return null;
    }

    for (int pathPieceIndex = 1; pathPieceIndex < pathPieces.length; ++pathPieceIndex) {
      final String pathPiece = pathPieces[pathPieceIndex];
      final String pieceTag = parseIndexedPathPiece(pathPiece, pathIndex);

      result = findChild(result, pieceTag, pathIndex[0]);

      if (result == null) break;
    }

    return result;
  }

  public static String parseIndexedPathPiece(String pathPiece, int[] index) {
    String result = pathPiece;
    index[0] = 0;

    final int lbPos = pathPiece.indexOf('[');
    if (lbPos >= 0) {
      result = pathPiece.substring(0, lbPos);

      final int rbPos = pathPiece.lastIndexOf(']');
      if (rbPos >= 0) {
        final String indexString = pathPiece.substring(lbPos + 1, rbPos).trim();
        if (!"".equals(indexString)) {
          index[0] = Integer.parseInt(indexString);
        }
      }
    }

    return result;
  }

  public static DomNode findChild(DomNode parent, String nodeName, int relativeSibNum) {
    DomNode result = null;

    if (parent.hasChildNodes()) {
      int sibNum = -1;
      final NodeList childNodes = parent.getChildNodes();
      for (int childNum = 0; childNum < childNodes.getLength(); ++childNum) {
        final Node childNode = childNodes.item(childNum);
        if (childNode == null || childNode.getNodeType() != DomNode.ELEMENT_NODE) continue;
        if (nodeName.equals(childNode.getNodeName())) {
          ++sibNum;
          if (sibNum == relativeSibNum) {
            result = (DomNode)childNode;
            break;
          }
        }
      }
    }

    return result;
  }

  /**
   *  Get the deepest common ancestor of the two nodes (inclusive of either
   *  node.)
   */
  public static DomNode getDeepestCommonAncestor(DomNode node1, DomNode node2) {
    if (node1 == null || node2 == null) return null;
    if (node1 == node2) return node1;

    final LinkedList<DomNode> rootPath1 = DomUtil.getRootPath(node1);
    final LinkedList<DomNode> rootPath2 = DomUtil.getRootPath(node2);

    int dcaIndex = getDeepestCommonAncestorIndex(rootPath1, rootPath2);

    return (dcaIndex >= 0) ? rootPath1.get(dcaIndex) : null;
  }

  /**
   *  Get the index of the deepest common ancestor accross the given rootPaths.
   */
  public static int getDeepestCommonAncestorIndex(LinkedList<DomNode> rootPath1, LinkedList<DomNode> rootPath2) {
    if (rootPath1 == null || rootPath2 == null) return -1;

    int result = -1;

    final Iterator<DomNode> pathEnumerator1 = rootPath1.iterator();
    final Iterator<DomNode> pathEnumerator2 = rootPath2.iterator();

    int curIndex = 0;
    while (pathEnumerator1.hasNext() && pathEnumerator2.hasNext()) {
      final DomNode curNode1 = pathEnumerator1.next();
      final DomNode curNode2 = pathEnumerator2.next();

      if (curNode1 != curNode2) {
        // paths diverge here
        break;
      }
      else {
        result = curIndex++;
      }
    }

    return result;
  }


  /**
   *  Find the child node of the parent that is an ancestor of descendant.
   */
  public static DomNode findChildNode(DomNode parent, DomNode descendant) {
    final LinkedList<DomNode> rootPath = getRootPath(descendant);
    return findChildNode(rootPath, parent, descendant);
  }

  /**
   *  Find the child node of the parent that is an ancestor of descendant
   *  using the given rootPath.
   */
  public static DomNode findChildNode(LinkedList<DomNode> rootPath, DomNode parent, DomNode descendant) {
    if (parent == descendant) return null;

    DomNode result = null;

    final int pIdx = rootPath.indexOf(parent);
    if (pIdx != -1 && pIdx + 1 < rootPath.size()) {
      result = rootPath.get(pIdx + 1);
    }

    return result;
  }

  /**
   *  Get the absolute sibling number of the given node, including counting
   *  text nodes.
   */
  public static int getAbsoluteSiblingNumber(DomNode domNode) {
    int result = 0;

    Node curNode = domNode;

    while (curNode.getPreviousSibling() != null) {
      curNode = curNode.getPreviousSibling();
      if (curNode.getNodeType() == Node.ELEMENT_NODE) ++result;
    }

    return result;
  }


  /**
   *  Get the relative sibling number of the given node, which is defined as
   *  the occurrence among (not-necessarily consecutive) siblings of the
   *  domNode's name.
   */
  public static int getRelativeSiblingNumber(DomNode domNode) {
    int result = 0;

    final String nodeName = domNode.getNodeName();
    Node curNode = domNode;

    while (curNode.getPreviousSibling() != null) {
      curNode = curNode.getPreviousSibling();

      if (curNode.getNodeType() == Node.ELEMENT_NODE) {
        if (nodeName.equals(curNode.getNodeName())) {
          ++result;
        }
      }
    }

    return result;
  }

  /**
   * Get the subtree's xml that includes the path from the root to the node
   * and the node's entire subTree.
   *
   * @return null if domNode is null; otherwise, the subTree string.
   */
  public static String getSubtreeXml(DomNode domNode) {
    if (domNode == null) return null;

    Tree<XmlLite.Data> nodeTree = domNode.asTree();
    String nodeXml = null;

    try {
      nodeXml = XmlLite.asXml(nodeTree, false);
    }
    catch (IOException e) {
      System.err.println("Warning: couldn't convert tree to xml text!");
      e.printStackTrace(System.err);
      nodeXml = null;
    }

    if (nodeXml == null) return null;

    final StringBuilder result = new StringBuilder();

    result.append(nodeXml);
    for (nodeTree = nodeTree.getParent(); nodeTree != null; nodeTree = nodeTree.getParent()) {
      // for each parent
      final XmlLite.Data data = nodeTree.getData();
      // insert open tag in front
      result.insert(0, data.toString());
      // append close tag at end
      final XmlLite.Tag tag = data.asTag();
      result.append("</").append(tag.name).append('>');
    }

    return result.toString();
  }

  /**
   * Write the (possibly null) domNode to the output.
   * <p>
   * Write the node and its full subtree as well as its path back to its
   * root.
   */
  public static void writeDomNode(DataOutput dataOutput, DomNode domNode) throws IOException {
    if (domNode == null) {
      dataOutput.writeBoolean(false);
      return;
    }

    final Tree<XmlLite.Data> nodeTree = domNode.asTree();
    dataOutput.writeInt(nodeTree.depth());

    final String xmlString = getSubtreeXml(domNode);
    DataHelper.writeString(dataOutput, xmlString);
  }

  /**
   * Read the (possibly null) domNode from the input.
   * <p>
   * The node will be returned with its subtree along with its path back to the
   * root preserved.
   */
  public static DomNode readDomNode(DataInput dataInput) throws IOException {

    final boolean hasData = dataInput.readBoolean();
    if (!hasData) return null;

    final int nodeDepth = dataInput.readInt();

    final String xmlString = DataHelper.readString(dataInput);
    Tree<XmlLite.Data> xmlTree = XmlFactory.buildXmlTree(xmlString, true, false);

    for (int depth = 0; depth < nodeDepth; ++depth) {
      if (xmlTree.numChildren() != 1) {
        xmlTree = null;
        break;
      }
      xmlTree = xmlTree.getChildren().get(0);
    }

    return xmlTree == null ? null : xmlTree.getData().asDomNode();
  }
}
