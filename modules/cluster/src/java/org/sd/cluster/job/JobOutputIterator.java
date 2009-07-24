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
package org.sd.cluster.job;


import org.sd.io.FileUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Utility class for iterating over a node's cluster job output files.
 * <p>
 * Job output lands in directories under the form:
 * <p>
 * /home/USER/cluster/jvm-N/data/output/JOB-ID/DATADIR/NODE-N.out
 * <p>
 * This output can be combined such that DATADIR contains multiple .out files.
 * <p>
 * This iterater returns file handles to NODE-N.out files under DATADIR
 * patterns for constant USER, jvm-N, and JOB-ID.
 * 
 * @author Spence Koehler
 */
public class JobOutputIterator implements Iterator<File> {
  private static final Pattern NODE_DIR_PATTERN = Pattern.compile("^.*-\\d+\\.out$");
  private static final int MARKER_BUFFER = 10;

  private JobOutputMarker marker;
  private boolean sort;
  private File[] dataDirs;
  private int nextDataDirIndex;
  private String nodeName;
  private File curDataDir;
  private File[] outFiles;
  private int nextOutFileIndex;

  /**
   * Construct with the path to the job-id directory and the pattern for the
   * datadirs within. Iterate over all '*.out' directories within (unsorted).
   *
   * @param jobIdPath       path to the /home/USER/cluster/jvm-N/data/output/JOB-ID directory.
   *                        (i.e. config.getOutputDataRoot(jobIdString="WorkJob"))
   * @param dataDirPattern  pattern to identify targeted datadir names in the job-id directory.
   *                        (i.e. dataDirName="FeedExtractor" + "_\\d+_" + servletId="posts" + "Index")
   */
  public JobOutputIterator(File jobIdPath, String dataDirPattern) {
    this(jobIdPath, dataDirPattern, null, false, null);
  }

  /**
   * Construct with the path to the job-id directory and the pattern for the
   * datadirs within. Iterate over all '*.out' directories within.
   *
   * @param jobIdPath       path to the /home/USER/cluster/jvm-N/data/output/JOB-ID directory.
   *                        (i.e. config.getOutputDataRoot(jobIdString="WorkJob"))
   * @param dataDirPattern  pattern to identify targeted datadir names in the job-id directory.
   *                        (i.e. dataDirName="FeedExtractor" + "_\\d+_" + servletId="posts" + "Index")
   * @param sort            true if the files should be returned in sorted order.
   */
  public JobOutputIterator(File jobIdPath, String dataDirPattern, boolean sort) {
    this(jobIdPath, dataDirPattern, null, sort, null);
  }

  /**
   * Construct with the path to the job-id directory and the pattern for the
   * datadirs within.
   *
   * @param jobIdPath       path to the /home/USER/cluster/jvm-N/data/output/JOB-ID directory.
   *                        (i.e. config.getOutputDataRoot(jobIdString="WorkJob"))
   * @param dataDirPattern  pattern to identify targeted datadir names in the job-id directory.
   *                        (i.e. dataDirName="FeedExtractor" + "_\\d+_" + servletId="posts" + "Index")
   * @param nodeName        name of cluster node (i.e. "gowron-0" or "gowron-0.out") or null if
   *                        it is to match the pattern '*-\\d+\\.out'.
   * @param sort            true if the files should be returned in sorted order.
   * @param jobMarkerId     string id for job marker, job marker is null if jobMarkerId is null or empty.
   */
  public JobOutputIterator(File jobIdPath, String dataDirPattern, String nodeName, boolean sort, String jobMarkerId) {
    this.sort = sort;
    final Pattern p = Pattern.compile(dataDirPattern);
    this.dataDirs = jobIdPath.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          final Matcher m = p.matcher(name);
          return m.matches();
        }
      });
    if (this.dataDirs == null) {
      this.dataDirs = new File[0];
    }
    else if (sort) {
      Arrays.sort(dataDirs);
    }

    System.out.println(new Date() + ": NOTE: JobOutputIterator found " + dataDirs.length + " dataDirs under '" +
                       jobIdPath + "' using pattern=" + dataDirPattern + "  ...nodeName=" + nodeName);

    this.nextDataDirIndex = 0;
    this.nodeName = nodeName;
    this.curDataDir = null;

    if (nodeName != null && !nodeName.endsWith(".out")) {
      this.nodeName = nodeName + ".out";
    }

    // if appropriate, find all "node-\\d+.out" dirs in the base index container
    setOutFiles();

    if(jobMarkerId != null && !"".equals(jobMarkerId)) { 
      try {
        this.marker = new JobOutputMarker(jobIdPath, jobMarkerId, MARKER_BUFFER);
        rollToMarker();
      }
      catch(IOException ioe){
        System.err.println("Unable to create marker at path=" + jobIdPath.getAbsolutePath() + "!: " + ioe.getMessage());
        this.marker = null;
      }
    }
    else {
      this.marker = null;
    }
  }

  // set the iterator such that the next next() will return the 
  // record after the last position of the maker
  private void rollToMarker(){
    if(this.marker == null) return;

    if(!this.sort){
      System.err.println("Can't roll to marker on an unsorted JobOutputIterator!");
      return;
    }

    String lastOutFilePath = this.marker != null ? this.marker.getLastOutFilePath() : null;

    if(lastOutFilePath == null){
      return;
    }
    else {
      if(setToOutputFile(new File(lastOutFilePath))){
        System.out.println("Iterator has rolled forward to out file: " + lastOutFilePath);
        increment();
      }
      else {
        System.err.println("Iterator failed to find out file: " + lastOutFilePath);
      }
    }
  }

  public boolean hasNext() {
    return nextDataDirIndex < dataDirs.length;
  }

  public File next() {
    this.curDataDir = getCurrent();
    increment();  // increment pointers such that getCurrent will return the next file.

    if(this.marker != null) {
      if(curDataDir != null) {
        try {
          this.marker.writeNext(curDataDir.getAbsolutePath());
        }
        catch (IOException ioe){
          System.err.println("Unable to write line '" + curDataDir.getAbsolutePath() + "' to marker!: " + ioe.getMessage());
        }
      }
    }
    return curDataDir;
  }

  /**
   * Remove the last file returned by next.
   */
  public void remove() {
    if (curDataDir != null && curDataDir.exists()) {
      FileUtil.deleteDir(curDataDir);
    }
  }

  /**
   * Get the file last returned by next.
   */
  public File getCurFile() {
    return curDataDir;
  }

  /**
   * Get the total number of dataDirs (out file containers) being iterated over.
   * <p>
   * Note that a datadir may hold out files for multiple nodes, but here we are
   * only counting the datadirs.
   */
  public int getNumDataDirs() {
    return dataDirs != null ? dataDirs.length : 0;
  }

  /**
   * Get the number of dataDirs fully visited.
   * <p>
   * Note that a datadir may hold out files for multiple nodes, but here we are
   * only counting the datadirs.
   */
  public int getNumDataDirsVisited() {
    return nextDataDirIndex;
  }

  /**
   * Get the current outFileIndex.
   */
  public int getOutFileIndex() {
    return outFiles == null ? 0 : nextOutFileIndex + 1;
  }

  /**
   * Get the total number of outFileIndexes.
   */
  public int getNumOutFiles() {
    return outFiles == null ? 0 : outFiles.length;
  }

  /**
   * Set position such that 'next' will return the given output file.
   *
   * @return true if successful; otherwise, false.
   */
  public final boolean setToOutputFile(File outFile) {
    // jump to the appropriate dataDir
    final File dataDir = outFile.getParentFile();
    nextDataDirIndex = 0;
    for (; nextDataDirIndex < dataDirs.length; ++nextDataDirIndex) {
      if (dataDir.equals(dataDirs[nextDataDirIndex])) break;
    }
    if (nextDataDirIndex < dataDirs.length) {
      this.curDataDir = dataDir;
      setOutFiles();

      // iterate forward to the indicated outFile
      File currentFile = getCurrent();
      while (hasNext() && !outFile.equals(getCurrent())) {
        increment();
      }
    }

    return outFile.equals(getCurrent());
  }

  /**
   * Get the file currently waiting to be served as the next.
   */
  private final File getCurrent() {
    File result = null;

    if (outFiles != null) {
      // get the current node index dir within the container
      if (nextOutFileIndex < outFiles.length) {
        result = outFiles[nextOutFileIndex];
      }
    }
    else {
      // get the current container
      if (nextDataDirIndex < dataDirs.length) {
        final File base = dataDirs[nextDataDirIndex];
        result = new File(base, nodeName);
      }
    }

    return result;
  }

  private final void setOutFiles() {
    if (nodeName == null) {
      if (nextDataDirIndex < dataDirs.length) {
        final File base = dataDirs[nextDataDirIndex];
        this.outFiles = base.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
              final Matcher m = NODE_DIR_PATTERN.matcher(name);
              return m.matches();
            }
          });
        if (this.outFiles == null || this.outFiles.length == 0) {
          // The base container is empty of node dirs matching pattern.
          // increment until we find one.
          increment();
        }
        else {
          if (sort) Arrays.sort(this.outFiles);
          this.nextOutFileIndex = 0;
        }
      }
      else {
        this.outFiles = new File[0];
        this.nextOutFileIndex = 0;
      }
    }
  }

  /**
   * Increment to the next index.
   */
  private final void increment() {
    boolean incIndex = true;

    if (outFiles != null) {
      if (nextOutFileIndex < outFiles.length - 1) {
        ++nextOutFileIndex;
        incIndex = false;
      }
    }

    if (incIndex) {
      // increment over index containers
      if (nextDataDirIndex < dataDirs.length) {
        ++nextDataDirIndex;
        setOutFiles();
      }
    }
  }

  protected class JobOutputMarker{
    private static final String markerSuffix = "marker";

    private int count;
    private int bufferSize;
    private String lastOutFilePath;

    private final String markerId;
    private File markerFile;
    private BufferedWriter markerBuffer;

    public JobOutputMarker(File baseDir, String markerId, int bufferSize) throws IOException{
      this.markerId = markerId;
      this.markerFile = new File(baseDir, markerId + "." + markerSuffix);

      this.bufferSize = (bufferSize < 1 ? 1: bufferSize);
      this.lastOutFilePath = null;
      init();
    }

    public void init() throws IOException{
      System.out.println("Initializing marker file '" + markerFile.getAbsolutePath() + "' with markerId=" + markerId + ",bufferSize=" + bufferSize);
      if(markerFile.exists()){
        ArrayList<String> lines = FileUtil.readLines(markerFile.getAbsolutePath());
        count = lines.size();
        if(lines.size() != 0){
          lastOutFilePath = lines.get(lines.size() - 1);
        }

        System.out.println("Marker file found from prior iteration: " + count + "/" + bufferSize + " lines, lastOutFilePath=" + lastOutFilePath);
        markerBuffer = FileUtil.getWriter(markerFile, true/*append*/);
      } 
      else {
        count = 0;
        markerBuffer = FileUtil.getWriter(markerFile, true/*append*/);
      }
    }

    public String getLastOutFilePath(){
      return lastOutFilePath;
    }

    public boolean writeNext(String line) throws IOException{
      synchronized (markerFile) {
        if(count >= bufferSize){
          roll();
        }

        markerBuffer.write(line + "\n");
        markerBuffer.flush();
        count++;
      }

      return true;
    }

    private void roll() throws IOException{
      // copy old file to backup
      markerBuffer.flush();
      boolean success = FileUtil.copyFile(markerFile, new File(markerFile.getAbsolutePath() + ".bup"));
      if(!success){
        throw new IOException("Unable to create backup file due to low disk space!");
      }

      // clean up old file
      try{
        markerBuffer.close();
      }
      finally {
        markerFile.delete();
      }

      // start new file
      count = 0;
      markerBuffer = FileUtil.getWriter(markerFile, true/*append*/);
    }

    public void close(){
      try{
        markerBuffer.close();
      }
      catch(IOException ioe){
        System.err.println("Unable to close BufferedWriter for marker file!: " + ioe.getMessage());
      }
      finally {
        markerBuffer = null;
      }
    }
  }
}
