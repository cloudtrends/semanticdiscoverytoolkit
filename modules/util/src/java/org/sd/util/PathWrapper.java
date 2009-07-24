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
package org.sd.util;


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility class to combine paths, dereferencing 'parent' and 'root' markers.
 * <p>
 * @author Spence Koehler
 */
public class PathWrapper {

  private LinkedList<String> basePathPieces;
  private String prefix;

  /**
   * Construct with a base path.
   * <p>
   * NOTE that multiple consecutive "/" symbols in the path are treated as a
   * single path delimiter instead of as redirection of the path to root.
   */
  public PathWrapper(String path) {
    this.basePathPieces = new LinkedList<String>();
    this.prefix = null;

    if (path != null && !"".equals(path)) {
      if ("/".equals(path)) {
        basePathPieces.add("");
      }
      else {
        final int css = path.indexOf("://");
        if (css >= 0) {
          prefix = path.substring(0, css + 3);
          path = path.substring(css + 3);
        }

        final String[] pathPieces = path.split("/+");
        fill(basePathPieces, pathPieces);
      }
    }
  }

  private static final void fill(LinkedList<String> basePathPieces, String[] pathPieces) {
    if (pathPieces.length == 0) return;

    for (String pathPiece : pathPieces) {
      if (".".equals(pathPiece)) {
        // do nothing
      }
      else if ("..".equals(pathPiece)) {
        if (basePathPieces.size() > 0) {
          basePathPieces.removeLast();
        }
      }
      else {
        if ("".equals(pathPiece)) {
          basePathPieces.clear();
        }

        basePathPieces.add(pathPiece);
      }
    }
  }

  /**
   * Get this path combined with the given relative path, leaving this path
   * unaltered.
   * <p>
   * NOTE that multiple consecutive "/" symbols in the path are treated as a
   * single path delimiter instead of as redirection of the path to root.
   */
  public String getCombined(String relativePath) {
    return getCombined(relativePath.split("/+"));
  }

  /**
   * Get this path combined with the given relative path pieces, leaving this
   * path unaltered.
   */
  public String getCombined(String[] relativePathPieces) {
    final LinkedList<String> result = new LinkedList<String>(basePathPieces);
    fill(result, relativePathPieces);
    return concat(result);
  }

  /**
   * Concatenate the pieces into a path.
   */
  private final String concat(List<String> pieces) {
    final StringBuilder result = new StringBuilder();

    if (prefix != null) {
      result.append(prefix);
    }

    for (Iterator<String> iter = pieces.iterator(); iter.hasNext(); ) {
      final String piece = iter.next();
      result.append(piece);
      if (iter.hasNext()) result.append('/');
    }

    return result.toString();
  }

  /**
   * Get the fixed base path.
   */
  public String getPath() {
    return concat(basePathPieces);
  }
}
