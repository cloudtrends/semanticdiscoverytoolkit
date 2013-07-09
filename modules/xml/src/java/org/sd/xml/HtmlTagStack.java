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


/**
 * Implementation of the TagStack interface for html.
 * <p>
 * @author Spence Koehler
 */
public class HtmlTagStack extends XmlTagStack {
  
  public HtmlTagStack() {
    this(false);
  }
  public HtmlTagStack(boolean useTagEquivalents) {
    super(useTagEquivalents);
  }

  /**
   * Push the given tag onto this stack.
   */
  public void pushTag(XmlLite.Tag tag) {
    // if optional end tag, don't push
    if (!XmlLite.OPTIONAL_END_TAGS.contains(tag.name) || tag.isSelfTerminating()) {
      // if special rule end tag,
      if (XmlLite.SPECIAL_RULE_END_TAG_MAP.containsKey(tag.name)) {
        // pop special tag
        final String[] toClose = XmlLite.SPECIAL_RULE_END_TAG_MAP.get(tag.name);
        for (int i = 1; i < toClose.length; ++i) {
          final String tagToClose = toClose[i];
          if (findTag(tagToClose)) {
            super.popTag(tagToClose);
            break;
          }
        }
      }

      // push the tag
      super.pushTag(tag);
    }
  }

  /**
   * Pop the tag with the given name from this stack.
   */
  public XmlLite.Tag popTag(String tagName) {
    XmlLite.Tag result = null;

    // if don't find match, ignore and leave everything
    if (findTag(tagName)) {
      result = super.popTag(tagName);
    }

    return result;
  }

  protected final boolean findTag(String tagName) {
    final String lTagName = tagName.toLowerCase();

    for (XmlLite.Tag tag : tags) {
      if (tag.commonCase) {
        if (tag.name.equals(lTagName)) {
          return true;
        }
      }
      else {
        if (tag.name.equals(tagName)) {
          return true;
        }
      }
    }
    return false;
  }
}
