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
package org.sd.cluster.io;


/**
 * Node utilities.
 * <p>
 * @author Spence Koehler
 */
public final class NodeUtil {

  public static final String DELIM = "-";

  /**
   * Build a name for a node based on the given data.
   *
   * @param prefix         The prefix for the node (i.e. "NodeClient" or "NodeServer").
   * @param name           A string to distinguish one node in the same jvm from another. ok if null.
   * @param addressString  The address of the node for identification purposes.
   * @param id             A unique id for the node, usually its instance number in this jvm.
   *
   * @return the constructed node name.
   */
  public static final String buildNodeName(String prefix, String name, String addressString, int id) {
    final StringBuilder result = new StringBuilder();

    String delim = "";
    if (prefix != null) {
      result.append(prefix);
      delim = DELIM;
    }

    if (addressString != null) {
      result.append(delim);
      result.append(addressString);
      delim = DELIM;
    }

    result.append(id);
    delim = DELIM;

    if (name != null) {
      result.append(delim);
      result.append(name);
      delim = DELIM;
    }

    return result.toString();
  }

}
