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

/**
 * Implementation of the TagStack interface for generic xml.
 * <p>
 * @author Spence Koehler
 */
public class XmlTagStack extends MutableTagStack {

  protected final LinkedList<XmlLite.Tag> tags;

  private List<XmlLite.Tag> _tags;

  public XmlTagStack() {
    this(false);
  }
  protected XmlTagStack(boolean useTagEquivalents) {
    super(useTagEquivalents);
    this.tags = new LinkedList<XmlLite.Tag>();
    this._tags = null;
  }

  /**
   * Get a copy of the current stack from the top (root) to the bottom
   * (farthest from root).
   * <p>
   * NOTE: The result is a copy of the current stack and will not be effected
   *       by subsequent stack operations.
   */
  public List<XmlLite.Tag> getTags() {
    if (_tags == null) {
      _tags = new ArrayList<XmlLite.Tag>(tags);
    }
    return _tags;
  }

  /**
   * Push the given tag onto this stack.
   */
  public void pushTag(XmlLite.Tag tag) {
    if (tags.size() > 0) {
      final XmlLite.Tag lastTag = tags.getLast();
      tag.setChildNum(lastTag.incNumChildren());
    }

    tags.addLast(tag);

    clearPathKey();
    _tags = null;
  }

  /**
   * Pop the tag with the given name from this stack.
   */
  public XmlLite.Tag popTag(String tagName) {
    XmlLite.Tag result = null;

    final String lTagName = tagName.toLowerCase();
    while (tags.size() > 0) {
      final XmlLite.Tag tag = tags.removeLast();
      if (tag.commonCase) {
        if (tag.name.equals(lTagName)) {
          result = tag;
          break;
        }
      }
      else {
        if (tag.name.equals(tagName)) {
          result = tag;
          break;
        }
      }
    }

    clearPathKey();
    _tags = null;
    return result;
  }

  /**
   * Pop the last tag pushed onto the stack.
   *
   * @return the popped tag or null if the stack is empty.
   */
  public XmlLite.Tag popTag() {
    XmlLite.Tag result = null;
    if (tags.size() > 0) {
      result = tags.removeLast();
    }

    clearPathKey();
    _tags = null;
    return result;
  }

  /**
   * Reset this tag stack for reuse.
   */
  public void reset() {
    tags.clear();
    clearPathKey();
    _tags = null;
  }

  /**
   * Get a handle on the instance's tags list ordered from the (top) root
   * to the bottom.
   */
  protected List<XmlLite.Tag> getTagsList() {
    return tags;
  }
}
