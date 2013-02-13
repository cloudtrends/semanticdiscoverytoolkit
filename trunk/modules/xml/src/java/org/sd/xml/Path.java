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
}
