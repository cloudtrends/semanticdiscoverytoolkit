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
package org.sd.util.thread;


import org.sd.io.FileUtil;
import org.sd.util.ExecUtil;

import java.io.File;

/**
 * Enumeration of bucket types.
 * <p>
 * @author Spence Koehler
 */
public enum Bucket {

  RECEIVING_INPUT("01_input_receiving"),
  READY_INPUT("02_input_ready"),
  ACTIVE_INPUT("03_input_active"),
  PROCESSED_INPUT("04_input_processed"),
  FAILED_INPUT("05_input_failed"),

  PROCESSING_OUTPUT("06_output_processing"),
  FINISHED_OUTPUT("07_output_finished"),
  FAILED_OUTPUT("08_output_failed"),
  SENT_OUTPUT("09_output_sent");

  private String name;

  Bucket(String name) {
    this.name = name;
  }

  /**
   * Get this bucket's name.
   */
  public String getName() {
    return name;
  }

  /**
   * Cast the given source file (in any bucket) as a file in this bucket.
   * No movement occurs, this just manipulates names.
   * <p>
   * NOTE: A side effect is for this bucket to be created if it doesn't yet
   *       exist AND the source file exists.
   */
  public File asFile(File source) {
    final File bucketDir = asDirectory(source.getParentFile());
    final String filename = source.getName();

    final String activeWorkUnit = FileUtil.getFilename(bucketDir, filename);
    final File result = new File(activeWorkUnit);

    return result;
  }

  /**
   * Cast the given (arbitrary) bucket directory to this bucket's directory.
   * <p>
   * NOTE: A side effect is for this bucket's directory to be created if
   *       it doesn't yet exist AND the given directory exists.
   */
  public File asDirectory(File dir) {
    final String parent = dir.getParent();
    final String bucketDir = FileUtil.getFilename(parent, name);
    final File result = new File(bucketDir);

    if (dir.exists()) {
      result.mkdirs();
    }

    return result;
  }

  /**
   * Claim the given source file (in any bucket) into this bucket.
   * <p>
   * NOTE: The source file will be moved to this bucket if it exists.
   */
  public File claim(File source) {
    final File claimedFile = asFile(source);
    if (source.exists()) {
      source.renameTo(claimedFile);
    }
    return claimedFile;
  }

  /**
   * Delete this bucket's version of the file (ok if in another bucket) if it
   * exists.
   */
  public void delete(File file) {
    final File bFile = asFile(file);
    if (bFile.exists()) {
      // delete through shell since it could be a file or a directory.
      ExecUtil.executeProcess("rm -rf " + bFile.getAbsolutePath());
    }
  }
}
