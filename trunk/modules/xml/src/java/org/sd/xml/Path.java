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
package org.sd.xml;


import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.sd.util.tree.Tree;

/**
 * Container for a (usually terminal) xml tree node and its TagStack.
 * <p>
 * @author Spence Koehler
 */
public class Path {
  
  private Tree<XmlLite.Data> node;
  private TagStack tagStack;
  private String _text;

  public Path(Tree<XmlLite.Data> node, TagStack tagStack) {
    this.node = node;
    this.tagStack = tagStack;
    this._text = null;
  }

  /**
   * Determine whether this path holds non-empty text.
   */
  public boolean hasText() {
    final String text = getText();
    return text != null && !"".equals(text);
  }

  /**
   * Get this path's text (possibly null or empty).
   */
  public String getText() {
    if (_text == null && hasNode()) {
      _text = computeText();
    }
    return _text;
  }

  private final String computeText() {
    final StringBuilder result = new StringBuilder();

    final List<Tree<XmlLite.Data>> leaves = PathHelper.gatherLeaves(node, null);
    for (Tree<XmlLite.Data> leaf : leaves) {
      final XmlLite.Text textData = leaf.getData().asText();
      if (textData != null && !"".equals(textData.text)) {
        if (result.length() > 0) result.append(' ');
        result.append(textData.text);
      }
    }

    return result.toString();
  }

  public boolean hasNode() {
    return node != null;
  }

  public Tree<XmlLite.Data> getNode() {
    return node;
  }

  public boolean hasTagStack() {
    return tagStack != null;
  }

  public TagStack getTagStack() {
    return tagStack;
  }

  // determine if the other path is inline with this one
  public boolean isInline(Path other) 
  {
    // find first divergent tag
    // if first divergent tag == this path.depth()
    //   and the tag at that index is not block tags
    //   then this is inline
    // else not inline

    boolean result = false;
    if(!hasTagStack())
      return result;

    TagStack otherStack = other.getTagStack();
    int idx = tagStack.findFirstDivergentTag(otherStack);
    if(idx == tagStack.depth())
    {
      XmlLite.Tag tag = otherStack.getTag(idx);
      if(tag != null)
      {
        result = 
          !HtmlHelper.EXTENDED_BLOCK_TAGS.contains(tag.name) &&
          !"p".equals(tag.name);
      }
    }
    return result;
  }

  // get the first index of any of the tags in this path's tag stack
  public int indexOfTag(Set<String> tags)
  {
    int result = -1;
    if (hasTagStack()) 
    {
      TagStack tstack = getTagStack();
      result = tstack.hasTag(tags);
    }
    return result;
  }

  // get the last index of any of the tags in this path's tag stack
  public int lastIndexOfTag(Set<String> tags)
  {
    int result = -1;
    if (hasTagStack()) 
    {
      TagStack tstack = getTagStack();
      result = tstack.findDeepestTag(tags);
    }
    return result;
  }

  public String toString() 
  {
    StringBuilder result = new StringBuilder();
    for (XmlLite.Tag tag : tagStack.getTags()) {
      if (result.length() > 0) result.append('.');
      result.append(tag.name);
    }
    return result.toString();
  }

  public String toString(int endIdx) 
  {
    StringBuilder result = new StringBuilder();
    final List<XmlLite.Tag> tags = tagStack.getTags();
    for (int i = 0; i <= endIdx; ++i) {
      final XmlLite.Tag tag = tags.get(i);
      if (result.length() > 0) result.append('.');
      result.append(tag.name);
    }
    return result.toString();
  }

}
