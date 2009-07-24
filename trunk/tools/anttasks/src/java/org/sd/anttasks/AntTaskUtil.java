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
package org.sd.anttasks;


import java.io.File;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Resource;

/**
 * Shared utilities for ant tasks.
 * <p>
 * @author Spence Koehler
 */
public class AntTaskUtil {
  
  /**
   * Get the property from the project, ensuring system properties are set
   * and falling back to environment variables if necessary.
   */
  public static final String getProperty(Project project, String property) {
    project.setSystemProperties();
      
    String result = project.getProperty(property);

    // fall back to the environment
    if (result == null) {
      result = System.getenv(property);
    }

    return result;
  }

  /**
   * Make the directory that would contain destFile if it doesn't
   * already exist.
   */
  public static final void makeContainingDir(File destFile) {
    final File parentFile = destFile.getParentFile();
    if (!parentFile.exists()) {
      parentFile.mkdirs();
    }
  }

  /**
   * Get all files from the file sets as files.
   */
  public static final File[] toFileArray(List<FileSet> filesets) {
    final List<File> result = new ArrayList<File>();

    for (FileSet fileset : filesets) {
      final File dir = fileset.getDir();
      for (Iterator iter = fileset.iterator(); iter.hasNext(); ) {
        final Resource resource = (Resource)iter.next();
        final File file = new File(dir, resource.getName());
        result.add(file);
      }
    }

    return result.toArray(new File[result.size()]);
  }

  /**
   * Get all file paths from the file sets as strings.
   */
  public static final String[] toStringArray(List<FileSet> filesets) {
    final File[] files = toFileArray(filesets);
    final String[] result = new String[files.length];
    for (int i = 0; i < files.length; ++i) {
      result[i] = files[i].getAbsolutePath();
    }
    return result;
  }
}
