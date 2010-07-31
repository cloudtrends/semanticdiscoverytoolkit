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


import java.util.LinkedList;
import org.sd.util.InputContext;

/**
 * A dom-based input context.
 * <p>
 * @author Spence Koehler
 */
public class DomContext implements InputContext {
  
  private DomNode domNode;
  public DomNode getDomNode() {
    return domNode;
  }

  private int iterationId;
  public int getIterationId() {
    return iterationId;
  }
  public int getId() {
    return iterationId;
  }

  private String nodeText;
  public String getText() {
    return nodeText;
  }

  private LinkedList<DomNode> _rootPath;
  /**
   *  The nodes from the document root to this instance's dom node.
   */
  public LinkedList<DomNode> getRootPath() {
    if (_rootPath == null && domNode != null) {
      _rootPath = DomUtil.getRootPath(domNode);
    }
    return _rootPath;
  }

  private String _simplePathString;
  /**
   *  A simple path string for this instance's dom node.
   */
  public String getSimplePathString() {
    if (_simplePathString == null) {
      _simplePathString = DomUtil.buildPathString(getRootPath(), false, false);
    }
    return _simplePathString;
  }

  private String _indexedPathString;
  /**
   *  An indexed path string for this instance's dom node (without terminal text).
   */
  public String getIndexedPathString() {
    if (_indexedPathString == null) {
      _indexedPathString = DomUtil.buildPathString(getRootPath(), true, false);
    }
    return _indexedPathString;
  }

  public DomContext(DomNode domNode, int iterationId) {
    this.domNode = domNode;
    this.iterationId = iterationId;
    this.nodeText = DomUtil.getTrimmedNodeText(domNode);
    this._rootPath = null;
    this._simplePathString = null;
  }

  /**
   * Find the DomNode corresponding to the indexedPathString.
   */
  public DomNode findDomNode(String indexedPathString) {
    final DomElement rootElement = domNode.getOwnerDomDocument().getDocumentDomElement();
    return DomUtil.findDomNode(rootElement, indexedPathString);
  }

  /**
   *  Get the startPosition of the other context's text within
   *  this context or return false if the other context is not
   *  contained within this context.
   */
  public boolean getPosition(InputContext otherContext, int[] startPosition) {
    boolean result = false;

    if (otherContext instanceof DomContext) {
      final DomContext other = (DomContext)otherContext;

      if (other.domNode.getOwnerDocument() == this.domNode.getOwnerDocument() && this.domNode.isAncestor(other.domNode, true)) {
        result = true;
        startPosition[0] = DomUtil.getTextPos(this.domNode, other.domNode);
      }
    }

    return result;
  }

  /**
   * Get this context's ultimate container or itself if it is not contained
   * by anything.
   */
  public InputContext getContextRoot() {
    return domNode.getOwnerDomDocument().getDomContext();
  }

  public String toString() {
    return DomUtil.buildPathString(domNode, true, true);
  }
}
