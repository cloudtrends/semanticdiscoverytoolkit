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


import org.sd.util.tree.Tree;

/**
 * Container for an xml node and its tag stack.
 * <p>
 * @author Spence Koehler
 */
public class XmlData {

  public final int index;
  public final Tree<XmlLite.Data> xmlNode;
  public final TagStack tagStack;

  private XmlLite.Tag pBlockTag;
  private XmlLite.Tag nBlockTag;
  private Integer headingStrength;

  public XmlData(int index, Tree<XmlLite.Data> xmlNode, TagStack tagStack, XmlLite.Tag pBlockTag) {
    this.index = index;
    this.xmlNode = xmlNode;
    this.tagStack = tagStack;
    this.pBlockTag = pBlockTag;
    this.nBlockTag = null;
    this.headingStrength = null;
  }

  public void setHeadingStrength(Integer headingStrength) {
    this.headingStrength = headingStrength;
  }

  public Integer getHeadingStrength() {
    return headingStrength;
  }

  public void setNextBlockTag(XmlData nextXmlData) {
    this.nBlockTag = tagStack.getDeepestCommonTag(nextXmlData.tagStack);
  }

  public boolean isHeading() {
    boolean result = false;

    if (headingStrength != null) {
      result = headingStrength > 3;
    }

    return result;
  }

  public boolean isTitle() {
    boolean result = false;

    if (headingStrength != null) {
      result = headingStrength == 7;
    }
    else {
      result = tagStack.hasTag("title") >= 0;
    }

    return result;
  }

  public XmlLite.Tag getBlockTag() {
    final boolean next = !isHeading();
    return getBlockTag(next);
  }

  public XmlLite.Tag getBlockTag(boolean next) {
    return (next && nBlockTag != null) ? nBlockTag : pBlockTag;
  }

  /**
   * Determine whether this xmlData is in the block started by blockStarter.
   * <p>
   * Note that this xmlData is in the block iff this.tagStack.hasTag(blockStarter.blockTag)
   *
   * @return true if this instance is in the block started by blockStarter; otherwise, false.
   */
  public boolean isInBlock(XmlData blockStarter) {
    final XmlLite.Tag blockTag = blockStarter.getBlockTag();
    return isInBlock(blockTag);
  }

  /**
   * Determine whether this xmlData is in the block of blockTag.
   * <p>
   * Note that this xmlData is in the block iff this.tagStack.hasTag(blockTag)
   *
   * @return true if this instance is in the block of blockTag; otherwise, false.
   */
  public boolean isInBlock(XmlLite.Tag blockTag) {
    return this.tagStack.hasTag(blockTag) >= 0;
  }
}
