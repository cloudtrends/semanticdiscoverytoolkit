/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
 * Class to represent a folder based dropbox where the contents of folders are the work items
 * Subfolders indicate input, output, process and error queues
 * <p>
 * @author Abe Sanderson
 */
public class DropBox
{
  private static final String INDIR_NAME = "input";
  private static final String OUTDIR_NAME = "output";
  private static final String PROCDIR_NAME = "process";
  private static final String ERRDIR_NAME = "error";

  private File dir;
  private File inDir;
  private File outDir;
  private File procDir;
  private File errDir;
  
  public File getDir() { return dir; }

  public DropBox(File location)
  {
    this.dir = location;
    this.inDir = new File(location, INDIR_NAME);
    this.outDir = new File(location, OUTDIR_NAME);
    this.procDir = new File(location, PROCDIR_NAME);
    this.errDir = new File(location, ERRDIR_NAME);
    this.inDir.mkdirs();
    this.outDir.mkdirs();
    this.procDir.mkdirs();
    this.errDir.mkdirs();
  }

  public File[] getInputFiles() { return inDir.listFiles(); }
  public File[] getOutputFiles() { return outDir.listFiles(); }
  public File[] getProcessFiles() { return procDir.listFiles(); }
  public File[] getErrorFiles() { return errDir.listFiles(); }

  private void moveToDir(File file, File dir, String id)
  {
    String filePath = file.getAbsolutePath();
    File newFile = new File(dir, file.getName());
    if(!file.renameTo(newFile))
      throw new IllegalArgumentException("unable to move to "+id+" directory: " + filePath);
    System.out.println("move to "+id+" file path: "+newFile.getAbsolutePath()+", "+filePath);
  }
  private void moveToSubdir(File file, File subdir, File dir, String id)
  {
    if(subdir == null || !subdir.isDirectory())
      throw new IllegalArgumentException("sub-directory is not a valid folder: " + subdir.getAbsolutePath());

    String filePath = file.getAbsolutePath();
    File newDir = new File(dir, subdir.getName());
    if(!newDir.exists() && !newDir.mkdirs())
      throw new IllegalArgumentException("unable to create "+id+" sub-directory: " + newDir.getAbsolutePath());

    File newFile = new File(newDir, file.getName());
    if(!file.renameTo(newFile))
      throw new IllegalArgumentException("unable to move to "+id+" directory: " + filePath);
    
    System.out.println("move to "+id+" file path: "+newFile.getAbsolutePath()+", "+filePath);
  }
  private void mergeToDir(File dir, File subdir, String id)
  {
    String dirPath = dir.getAbsolutePath();
    File newDir = new File(dir, subdir.getName());
    try {
      FileUtil.copyDir(subdir, newDir);
    }
    catch(Exception ex) {
      throw new IllegalArgumentException("unable to merge with "+id+" directory: " + dirPath, ex);
    }
    System.out.println("move to "+id+" dir path: "+newDir.getAbsolutePath()+", "+dirPath);
  }

  public void moveToInput(File file) { 
    if(file.isDirectory())
      mergeToDir(file, inDir, "input"); 
    else
      moveToDir(file, inDir, "input"); 
  }
  public void moveToOutput(File file) { 
    if(file.isDirectory())
      mergeToDir(file, outDir, "output"); 
    else
      moveToDir(file, outDir, "output"); 
  }
  public void moveToProcess(File file) { 
    if(file.isDirectory())
      mergeToDir(file, procDir, "process"); 
    else
      moveToDir(file, procDir, "process"); 
  }
  public void moveToError(File file) { 
    if(file.isDirectory())
      mergeToDir(file, errDir, "error"); 
    else
      moveToDir(file, errDir, "error"); 
  }

  public void moveToInput(File file, File subdir) { moveToSubdir(file, subdir, inDir, "input"); }
  public void moveToOutput(File file, File subdir) { moveToSubdir(file, subdir, outDir, "output"); }
  public void moveToProcess(File file, File subdir) { moveToSubdir(file, subdir, procDir, "process"); }
  public void moveToError(File file, File subdir) { moveToSubdir(file, subdir, errDir, "error"); }

  public String toString() {
    return dir.getAbsolutePath();
  }
}
