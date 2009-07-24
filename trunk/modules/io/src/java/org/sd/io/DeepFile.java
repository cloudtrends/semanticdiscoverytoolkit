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
package org.sd.io;


import java.io.File;

/**
 * Utility class to keep track of a file in a deep directory tree,
 * distinguishing relevant from irrelevant parts of its path.
 * <p>
 * @author Spence Koehler
 */
public class DeepFile {

  private File file;
  private String prefix;
  private String _deepName;

  public DeepFile(File file, String prefix) {
    this.file = file;
    this.prefix = prefix;
    this._deepName = null;
  }

  /**
   * Get the file handle to this file.
   */
  public File getFile() {
    return file;
  }

  /**
   * Get the "irrelevant" prefix to this deep file's actual path.
   */
  public String getPrefix() {
    return prefix;
  }

  /**
   * Get the "relevant" path segment of this deep file, down to and including
   * its name.
   * <p>
   * Note that the "relevant" path segment will never start with a slash.
   */
  public String getDeepName() {
    if (_deepName == null) {
      _deepName = buildDeepName();
    }

    return _deepName;
  }

  private final String buildDeepName() {
    final String fullPath = file.getAbsolutePath();
    String result = fullPath;  // default deep name if problems.

    if (prefix != null && prefix.length() > 0) {
      final int ppos = fullPath.indexOf(prefix);
      if (ppos >= 0) {
        result = fullPath.substring(ppos + prefix.length());
      }
    }

    // fall back to "empty" prefix.
    if (result == null) {
      result = fullPath;
    }

    if (result.startsWith("/")) {
      result = result.substring(1);
    }

    return result;
  }
}
