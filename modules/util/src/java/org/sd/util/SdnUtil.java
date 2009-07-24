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


import org.sd.io.FileUtil;

/**
 * SDN Utilities.
 * <p>
 * @author Spence Koehler
 */
public class SdnUtil {
  
  private static String _sdnRoot = null;

  /**
   * Get the absolute path to the sdn root directory.
   */
  public static final String getSdnRootDir() {
    if (_sdnRoot == null) {
      _sdnRoot = System.getProperty("SDN_ROOT");
      if (_sdnRoot == null) {
        _sdnRoot = System.getenv("SDN_ROOT");
      }
      if (_sdnRoot == null) {
        _sdnRoot = ExecUtil.getUserHome() + "/cluster";
      }
    }
    return _sdnRoot;
  }

  /**
   * Get the absolute path to the sdn resource directory.
   */
  public static final String getSdnResourcePath() {
    String result = null;

    final String sdnRoot = getSdnRootDir();
    if (sdnRoot != null) {
      result = FileUtil.getFilename(sdnRoot, "resources");
    }

    return result;
  }

  /**
   * Given a relative sdn resource path (without the "resources/" component), get
   * the absolute path to the resource.
   */
  public static final String getSdnResourcePath(String relativeResource) {
    String result = null;

    final String resourcePath = getSdnResourcePath();
    if (resourcePath != null) {
      result = FileUtil.getFilename(resourcePath, relativeResource);
    }

    return result;
  }

  public static void main(String[] args) {
    System.out.println("     sdnRootDir=" + getSdnRootDir());
    System.out.println("sdnResourcePath=" + getSdnResourcePath());
    System.out.println();

    for (int i = 0; i < args.length; ++i) {
      final String resource = args[i];
      final String absolutePath = getSdnResourcePath(resource);
      System.out.println("sdnResourcePath(" + resource + ")=" + getSdnResourcePath(resource) + " (exists=" + new java.io.File(absolutePath).exists() + ")");
    }
  }
}
