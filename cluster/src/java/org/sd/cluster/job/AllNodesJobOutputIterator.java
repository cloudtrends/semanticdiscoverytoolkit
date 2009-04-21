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


import org.sd.cluster.config.ConfigUtil;
import org.sd.io.FileUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Utility class for iterating over all nodes' cluster job output files
 * on a machine.
 * <p>
 * See JobOutputIterator for details of iterating job output for a single
 * node.
 *
 * @author Spence Koehler
 */
public class AllNodesJobOutputIterator implements Iterator<File> {

  private String jobId;
  private String dataDirPattern;
  private String nodeName;
  private boolean sort;
  private String jobMarkerId;
  private File[] jvmPaths;
  private int jvmIndex;
  private JobOutputIterator curIter;
  private File curNext;
  private File nextNext;

  /**
   * Construct with the job-id and the pattern for the datadirs within.
   * Iterate over all '*.out' directories within (unsorted).
   *
   * @param jobId           JOB-ID for constructing paths to the /home/USER/cluster/jvm-N/data/output/JOB-ID directories.
   * @param dataDirPattern  pattern to identify targeted datadir names in the job-id directory.
   *                        (i.e. dataDirName="FeedExtractor" + "_\\d+_" + servletId="posts" + "Index")
   */
  public AllNodesJobOutputIterator(String jobId, String dataDirPattern) {
    this(jobId, dataDirPattern, null, false, null);
  }

  /**
   * Construct with the job-id and the pattern for the datadirs within.
   * Iterate over all '*.out' directories within.
   *
   * @param jobId           JOB-ID for constructing paths to the /home/USER/cluster/jvm-N/data/output/JOB-ID directories.
   * @param dataDirPattern  pattern to identify targeted datadir names in the job-id directory.
   *                        (i.e. dataDirName="FeedExtractor" + "_\\d+_" + servletId="posts" + "Index")
   * @param sort            true if the files should be returned in sorted order.
   */
  public AllNodesJobOutputIterator(String jobId, String dataDirPattern, boolean sort) {
    this(jobId, dataDirPattern, null, sort, null);
  }

  /**
   * Construct with the job-id and the pattern for the datadirs within.
   *
   * @param jobId           JOB-ID for constructing paths to the /home/USER/cluster/jvm-N/data/output/JOB-ID directories.
   * @param dataDirPattern  pattern to identify targeted datadir names in the job-id directory.
   *                        (i.e. dataDirName="FeedExtractor" + "_\\d+_" + servletId="posts" + "Index")
   * @param nodeName        name of cluster node (i.e. "gowron-0" or "gowron-0.out") or null if
   *                        it is to match the pattern '*-\\d+\\.out'.
   * @param sort            true if the files should be returned in sorted order.
   * @param jobMarkerId     string id for job marker, job marker is null if jobMarkerId is null or empty.
   */
  public AllNodesJobOutputIterator(String jobId, String dataDirPattern, String nodeName, boolean sort, String jobMarkerId) {
    this.jobId = jobId;
    this.dataDirPattern = dataDirPattern;
    this.nodeName = nodeName;
    this.sort = sort;
    this.jobMarkerId = jobMarkerId;
    

    final Pattern p = Pattern.compile("^jvm-\\d+$");
    final File clusterDir = new File(ConfigUtil.getClusterRootDir());
    this.jvmPaths = clusterDir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          final Matcher m = p.matcher(name);
          return m.matches();
        }
      });
    if (this.jvmPaths == null) {
      this.jvmPaths = new File[0];
    }
    else if (sort) {
      Arrays.sort(jvmPaths);
    }
    this.jvmIndex = 0;

    setNodeIterator();

    this.curNext = null;
    this.nextNext = getNext();
  }

  public boolean hasNext() {
    return nextNext != null;
  }

  public File next() {
    this.curNext = nextNext;
    this.nextNext = getNext();
    return curNext;
  }

  /**
   * Get the file last returned by next.
   */
  public File getCurFile() {
    return curNext;
  }

  /**
   * Get the current jvmIndex.
   */
  public int getJvmIndex() {
    return jvmIndex + 1;
  }

  /**
   * Get the total number of jvms.
   */
  public int getNumJvms() {
    return jvmPaths == null ? 0 : jvmPaths.length;
  }

  /**
   * Get the current jobOutputIterator
   */
  public JobOutputIterator getJobOutputIterator() {
    return curIter;
  }


  private final File getNext() {
    File result = null;

    while (curIter != null) {
      if (curIter.hasNext()) {
        result = curIter.next();
        break;
      }
      else if (jvmIndex + 1 < jvmPaths.length) {
        ++jvmIndex;
        setNodeIterator();
      }
      else {
        curIter = null;
      }
    }

    return result;
  }

  public void remove() {
    if (curNext != null && curNext.exists()) {
      FileUtil.deleteDir(curNext);
    }
  }

  /**
   * Set the iterator such that the next  "next" will return the given file
   * and continue iterating forward.
   *
   * @return true if successfuly set; otherwise, false.
   */
  public final boolean setToOutputFile(File outFile) {
    boolean result = false;

    if (jvmPaths != null && jvmPaths.length > 0) {
      final String clusterDir = ConfigUtil.getClusterRootDir();
      final Pattern p = Pattern.compile("^" + clusterDir + "(jvm-\\d+)/.*$");
      final Matcher m = p.matcher(outFile.getAbsolutePath());
      if (m.matches()) {
        final String targetJvmName = m.group(1);
        for (int i = 0; i < jvmPaths.length; ++i) {
          final File jvmPath = jvmPaths[i];
          final String jvmName = jvmPath.getName();
          if (targetJvmName.equals(jvmName)) {
            this.jvmIndex = i;
            setNodeIterator();
            if (curIter != null && curIter.hasNext()) {
              result = curIter.setToOutputFile(outFile);
              if (result) {
                nextNext = getNext();
              }
            }
            break;
          }
        }
      }
    }

    return result;
  }

  private final void setNodeIterator() {
    if (jvmIndex < jvmPaths.length) {
      final String jobIdDir = jvmPaths[jvmIndex] + "/data/output/" + jobId;
      final File jobIdPath = new File(jobIdDir);

      this.curIter = new JobOutputIterator(jobIdPath, dataDirPattern, nodeName, sort, jobMarkerId);
    }
    else {
      System.out.println("setNodeIterator to null!");
      this.curIter = null;
    }
  }
}
