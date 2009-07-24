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
package org.sd.util.tree;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to cache and apply node paths.
 * <p>
 * @author Spence Koehler
 */
public class NodePathApplicator <T> {
  
  private Map<String, NodePath<T>> pattern2nodePath;

  public NodePathApplicator() {
    this.pattern2nodePath = new HashMap<String, NodePath<T>>();
  }

  public NodePath<T> getNodePath(String pattern) {
    NodePath<T> result = pattern2nodePath.get(pattern);
    if (result == null) {
      result = new NodePath<T>(pattern);
      pattern2nodePath.put(pattern, result);
    }
    return result;
  }

  public List<Tree<T>> apply(String pattern, Tree<T> node) {
    final NodePath<T> nodePath = getNodePath(pattern);
    return nodePath.apply(node);
  }
}
