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
 * Utility class to build a path key.
 * <p>
 * @author Spence Koehler
 */
public class PathKeyBuilder {

  private StringBuilder pathKey;

  public PathKeyBuilder() {
    this.pathKey = new StringBuilder();
  }

  public String getPathKey() {
    return pathKey.toString();
  }

  /**
   * Add all elements to end from left to right.
   */
  public void addAll(List<XmlLite.Tag> tags) {
    for (XmlLite.Tag tag : tags) {
      add(tag);
    }
  }

  /**
   * Add element to end.
   */
  public void add(XmlLite.Tag tag) {
    if (tag != null) {
      if (pathKey.length() > 0) pathKey.append('.');
      pathKey.append(tag.name);

      if ("td".equals(tag.name) || "table".equals(tag.name)) {
        // special case 'td' and 'table' tags.  todo: make this specific to html only?
        pathKey.append(tag.getChildNum());
      }
    }
  }

  /**
   * Add an already constructed pathKey to this pathKey.
   */
  public void add(String otherPathKey) {
    if (otherPathKey != null) {
      if (pathKey.length() > 0) pathKey.append('.');
      pathKey.append(otherPathKey);
    }
  }

  public String toString() {
    return pathKey.toString();
  }
}
