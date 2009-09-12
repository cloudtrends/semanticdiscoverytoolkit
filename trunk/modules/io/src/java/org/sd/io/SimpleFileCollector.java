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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class to collect files under a common parent up to a limit.
 * <p>
 * @author Spence Koehler
 */
public class SimpleFileCollector implements MultiPartRecordFactory<File, List<File>> {
  
  private Integer maxFiles;  // optional limit on files. unlimited if null.

  /**
   * Default constructor with no limit on the number of files to collect in a
   * group.
   */
  public SimpleFileCollector() {
    this(null);
  }

  /**
   * Construct such that only up to maxFiles files are collected in a group.
   */
  public SimpleFileCollector(Integer maxFiles) {
    this.maxFiles = maxFiles;
  }

  public MultiPartRecord<File,List<File>> newMultiPartRecord() {
    return new MultiPartRecord<File, List<File>>() {
      private List<File> files = new ArrayList<File>();
      private File parent = null;
      private final AtomicBoolean hitLimit = new AtomicBoolean(false);
      public boolean addPiece(File file) {
        boolean result = false;

        if (parent == null || file.getParentFile().equals(parent)) {
          if (parent == null) parent = file.getParentFile();

          if (maxFiles != null && files.size() >= maxFiles) {
            // this file belongs in the same group, but won't be added because
            // the group size limit (maxFiles) has been reached.
            result = false;
            hitLimit.set(true);
          }
          else {
            files.add(file);
            result = true;
            hitLimit.set(false);
          }
        }
//            else System.out.println("reached end of group (parent=" + parent + ", file=" + file + ")");

        return result;
      }
      public List<File> getRecord() {return files;}
      public boolean isComplete() {return !hitLimit.get();}
    };
  }
}
