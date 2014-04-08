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

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to work with numbered files.
 * <p>
 * @author Spence Koehler
 */
public abstract class BaseNumberedFile {
  
  public final File dir;  // directory containing numbered files
  public final String namePrefix;  // name of files before number
  public final String nameSuffix;  // name of files after number

  protected Pattern namePattern;  // pattern for matching numbered files
  private boolean useFileLocks;

  protected BaseNumberedFile(File dir, String namePrefix, String nameSuffix,
                             boolean useFileLocks) 
  {
    this.dir = dir;
    this.namePrefix = namePrefix;
    this.nameSuffix = nameSuffix;

    this.namePattern = Pattern.compile(namePrefix + "(\\d+)" + nameSuffix);
    this.useFileLocks = useFileLocks;
  }

  private final FileLock.LockOperation<File[]> s_lockOp =
    new FileLock.LockOperation<File[]>()
  {
    public File[] operate(String filename) 
      throws IOException 
    {
      return findFiles();
    }
  };

  /**
   * Get all existing numbered files and, as the last element of the list, the
   * next logically sequenced numbered file.
   * <p>
   * Allow numbers to be skipped or missing in the sequence, always returning
   * one more than the highest number in the "next" file's name.
   *
   * @return existing and next files or null if there is an error such as the
   *         containing directory not existing or an I/O exception while
   *         listing.
   */
  public List<File> getExistingAndNext() {
    List<File> result = null;
    
    // if file locks are used, and there is still a lock on the dir
    // return as if no files were found, 
    // but do increment the counter so that the next file obtained
    // uses the correct number
    boolean locked = false;
    File[] files = null;
    if(useFileLocks)
    {
      String dirPath = dir.getAbsolutePath()+File.separatorChar+"foo";
      FileLock<File[]> fileLock = new FileLock<File[]>(dirPath, 100);
      files = fileLock.operateWhileLocked(s_lockOp);

      if(files == null)
      {
        locked = true;
        files = new File[] {};
      }
    }
    else
      files = findFiles();

    if (files != null) {
      final TreeMap<Integer, File> sortedFiles = new TreeMap<Integer, File>();
      int nextNumber = -1;

      result = new ArrayList<File>();
      for (File file : files) {
        final int curNumber = getNumber(file);

        if(!locked)
          sortedFiles.put(curNumber, file);

        if (curNumber > nextNumber) {  // keep track of maximum
          nextNumber = curNumber;
        }
      }

      for (File file : sortedFiles.values()) {
        result.add(file);
      }

      ++nextNumber;
      result.add(newFile(nextNumber));
    }

    return result;
  }

  protected abstract File[] findFiles();
  protected abstract File newFile(int number);

  /**
   * Get the number part of the numbered file.
   *
   * @return the number between the namePrefix and nameSuffix, or -1 if the
   *         pattern doesn't match.
   */
  public int getNumber(File numberedFile) {
    int result = -1;

    final String name = numberedFile.getName();
    final Matcher m = namePattern.matcher(name);
    if (m.matches()) {
      final String numberString = m.group(1);
      result = Integer.parseInt(numberString);
    }

    return result;
  }
}
