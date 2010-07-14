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
import java.util.LinkedList;
import java.util.List;
import org.w3c.dom.NodeList;

/**
 * Non-thread-safe helper class for walking through a DOM's text.
 * <p>
 * @author Spence Koehler
 */
public class DomTextWalker {
  
  private DomNode topNode;
  public DomNode getTopNode() {
    return topNode;
  }

  // The ending position of the last text node encountered.
  private int curEndPos;
  public int getCurEndPos() {
    return curEndPos;
  }

  // The starting position of the last text node encountered.
  private int curStartPos;
  public int getCurStartPos() {
    return curStartPos;
  }

  // The last text node encountered while walking.
  private DomText curTextNode;
  public DomText getCurTextNode() {
    return curTextNode;
  }

  // The last text node's text encountered while walking.
  private String curText;
  public String getCurText() {
    return curText;
  }


  private LinkedList<DomNode> queue;
  private DomNode curNode;
  private String _trimmedNodeText;

  public DomTextWalker(DomNode topNode) {
    this.topNode = topNode;
    this.queue = new LinkedList<DomNode>();
    this._trimmedNodeText = null;

    reset();
  }

  public void reset() {
    this.curEndPos = 0;
    this.curStartPos = 0;
    this.curTextNode = null;
    this.curText = null;

    this.queue.clear();
    queue.addLast(topNode);
    this.curNode = null;
  }


  /**
   *  Collect all trimmed text under TopNode, concatenating text across nodes
   *  with a single space.
   */
  public String getTrimmedNodeText() {
    if (_trimmedNodeText == null) {
      final StringBuilder result = new StringBuilder();

      reset();
      while (increment(null)) {
        if (result.length() > 0) result.append(' ');
        result.append(curText);
      }

      _trimmedNodeText = result.toString();
    }

    return _trimmedNodeText;
  }


  /**
   *  Get the position of the deep  node's trimmed text as an offset within
   *  the TopNode's text.
   */
  public int getTextPos(DomNode deepNode) {
    int result = -1;

    reset();

    while (increment(deepNode)) {
      if (curNode == deepNode) {
        if (curTextNode == deepNode) {
          result = curStartPos;
        }
        else {
          // haven't reached the next text node yet
          result = curEndPos;

          // account for space if necessary
          if (curEndPos > 0) ++result;
        }

        break;
      }
    }

    return result;
  }

  /**
   *  Get the text nodes that contain the consecutive text from startPos to
   *  endPos if possible.
   *
   * @param startPos The starting position of text under the TopNode (inclusive)
   * @param endPos The ending position of text under TopNode(exclusive)
   * @param newStartPos The starting position of text withing result[0].
   * @param newEndPos The ending position of text within result[N-1].
   *
   * @returns The list of consecutive non-empy text nodes encompassing the text or null.
   */
  public List<DomText> getTextNodes(int startPos, int endPos, int[] newStartPos, int[] newEndPos) {
    List<DomText> result = null;

    if (setPosition(startPos, null, false)) {
      newStartPos[0] = startPos - curStartPos;
      result = new ArrayList<DomText>();
      result.add(curTextNode);

      if (setPosition(endPos, result, true)) {
        newEndPos[0] = endPos - curStartPos;
      }
      else result = null;
    }

    return result;
  }


  private boolean setPosition(int position, List<DomText> collector, boolean isEndPos) {
    boolean result = false;

    if (curTextNode != null && position >= curStartPos && position < curEndPos) {
      if (collector != null) collector.add(curTextNode);
      return true;
    }

    if (isEndPos && curEndPos == position) return true;

    if (curEndPos > position) reset();

    while (increment(null)) {
      result = (position >= curStartPos && position < curEndPos);

      // if collector is non-null, collect all text nodes encountered up to and
      // including that whose text includes 'position'.
      if (collector != null) collector.add(curTextNode);

      if (!result && position == curEndPos) result = true;

      if (result) break;
    }

    return result;
  }

  /**
   *  Increment to the next text node, stopping if stopAtNode is encountered first.
   */
  private boolean increment(DomNode stopAtNode) {
    boolean result = false;

    while (queue.size() > 0) {
      curNode = queue.removeFirst();

      if (curNode.getNodeType() == DomNode.TEXT_NODE) {
        curTextNode = (DomText)curNode;

        curText = curTextNode.getHyperTrimmedText();

        if (curText.length() > 0) {
          // account for a space between non-empty text nodes
          if (curEndPos > 0) ++curEndPos;
          final int nextEndPos = curEndPos + curText.length();

          curStartPos = curEndPos;
          curEndPos = nextEndPos;

          result = true;
          break;
        }
      }
      else if (curNode.hasChildNodes()) {
        final NodeList childNodes = curNode.getChildNodes();
        for (int childIndex = childNodes.getLength() - 1; childIndex >= 0; --childIndex) {
          final DomNode childNode = (DomNode)childNodes.item(childIndex);
          if (childNode != null) {
            queue.addFirst(childNode);
          }
        }
      }

      if (stopAtNode != null && stopAtNode == curNode) {
        result = true;
        break;
      }
    }

    return result;
  }
}
