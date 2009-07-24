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
package org.sd.cio;


import org.sd.io.FileUtil;
import org.sd.util.NameGenerator;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

/**
 * Utility to use a NameGenerator to create successive file (or dir) names.
 * <p>
 * @author Spence Koehler
 */
public class FilenameGenerator {
  
  public enum CreateFlag { FILE, DIR, NONE };

  public final File dir;
  public final NameGenerator nameGenerator;
  public final int maxCount;

  public final FilenameFilter filenameFilter;

  private File lastFile;
  private int fileCount;

  /**
   * Construct with the given params.
   *
   * @param dir  The directory in which the filenames are being generated.
   * @param nameGenerator  The name generator to use.
   * @param maxCount  The maximum number of filenames to generate in the dir.
   */
  public FilenameGenerator(final File dir, final NameGenerator nameGenerator, int maxCount) {
                             
    this.dir = dir;
    this.nameGenerator = nameGenerator;
    this.maxCount = maxCount;

    this.filenameFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return nameGenerator.isValidName(name);
        }
      };

    this.lastFile = null;
    this.fileCount = 0;
  }

  /**
   * Get all existing files matching the name generator (sorted).
   */
  public final File[] listFiles() {
    final File[] files = dir.listFiles(filenameFilter);
    if (files != null && files.length > 0) {
      fileCount = files.length;
      Arrays.sort(files);  // assume alphabetical ordering holds true
    }
    return files;
  }

  /**
   * Get the last file matching the name generator.
   */
  public final File getLastFile() {
    File result = null;

    final File[] files = listFiles();
    if (files != null && files.length > 0) {
      result = files[files.length - 1];
    }

    return result;
  }

  /**
   * Generate the next available local name.
   *
   * @param createFlag  if null or NONE, do nothing;
   *                    if FILE, then create an empty file to reserve the name;
   *                    if DIR, then create an empty dir to reserve the name.
   *
   * @return the local name part such that new File(dir, result) references the new file;
   *         or null if no more files will fit in the directory.
   */
  public final String getNextLocalName(CreateFlag createFlag) {

    if (maxCount > 0 && fileCount >= maxCount) return null;  // already reached limit.

    String result = null;

    final File lastFile = recallLastFile();
    if (lastFile == null && fileCount > 0) return null;  // can't generate another

    final String lastName = lastFile == null ? null : lastFile.getName();

    result = nameGenerator.getNextName(lastName);

    if (result != null) {
      if (createFlag != null) {
        final File nextFile = new File(dir, result);

        switch (createFlag) {
        case FILE :
          try {
            FileUtil.touch(nextFile);  // reserve the name.
          }
          catch (IOException ignore) {}
          break;
        case DIR :
          nextFile.mkdirs();
          break;
        }

        this.lastFile = nextFile;  // this is now the last existing file.
        ++fileCount;  // account for another file now on disk.
      }
    }

    return result;
  }

  /**
   * Read from disk to find the last existing file.
   */
  private final File getLastDiskFile() {
    File result = null;

    final File[] files = listFiles();
    if (files != null && files.length > 0) {
      if (maxCount > 0 && files.length >= maxCount) {
        result = null;  // can't generate another.
      }
      else {
        result = files[files.length - 1];
      }
    }

    return result;
  }

  /**
   * Find te last disk file either by incrementing until finding where the
   * pattern ceases to exist on disk or by checking the disk directly when
   * necessary.
   */
  private final File recallLastFile() {

    if (this.lastFile == null) {
      // read from disk to find the last file
      this.lastFile = getLastDiskFile();
    }
    else {
      // increment from last to find last existing
      File potentialLastFile = getNextFile(this.lastFile);
      while (potentialLastFile != null && potentialLastFile.exists()) {

        // more files have appeared on disk. need to account for them.
        ++fileCount;
        if (maxCount > 0 && fileCount >= maxCount) {
          potentialLastFile = null;  // can't generate another
          break;
        }

        this.lastFile = potentialLastFile;
        potentialLastFile = getNextFile(this.lastFile);
      }
      if (potentialLastFile == null) {
        this.lastFile = null;  // can't generate another.
      }
    }

    return this.lastFile;
  }

  private final File getNextFile(File refFile) {
    final String refName = refFile.getName();
    final String nextName = nameGenerator.getNextName(refName);
    return (nextName == null) ? null : new File(dir, nextName);
  }
}
