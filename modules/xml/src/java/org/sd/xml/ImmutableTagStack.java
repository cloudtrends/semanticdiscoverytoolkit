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

import java.util.List;

/**
 * An implementation of the TagStack interface for immutable stacks.
 * <p>
 * @author Spence Koehler
 */
public class ImmutableTagStack extends BaseTagStack {
  
  final List<XmlLite.Tag> tags;

  public ImmutableTagStack(TagStack tagStack) {
    super();
    this.tags = tagStack.getTags();
  }

  public ImmutableTagStack(List<XmlLite.Tag> tags, List<XmlLite.Tag> savedTags) {
    this(tags, savedTags, false);
  }
  public ImmutableTagStack(List<XmlLite.Tag> tags, List<XmlLite.Tag> savedTags, 
                           boolean useTagEquivalents) {
    super(savedTags, useTagEquivalents);
    this.tags = tags;
  }

  /**
   * Get a copy of the current stack from the top (root) to the bottom
   * (farthest from root).
   * <p>
   * NOTE: The result is a copy of the current stack and will not be effected
   *       by subsequent stack operations.
   */
  public List<XmlLite.Tag> getTags() {
    return tags;
  }

  /**
   * Get a handle on the instance's tags list ordered from the (top) root
   * to the bottom.
   */
  protected List<XmlLite.Tag> getTagsList() {
    return tags;
  }
}
