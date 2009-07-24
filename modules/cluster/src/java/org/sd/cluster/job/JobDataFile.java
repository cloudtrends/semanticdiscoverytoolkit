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


/**
 * Encapsulation of a job data file in the cluster.
 * <p>
 * @author Spence Koehler
 */
public class JobDataFile {
  
  public static final String buildWorkRequestString(String sourceId, String destId, String filename) {
    final StringBuilder result = new StringBuilder();

    result.
      append(sourceId).append('.').
      append(destId).append('.').
      append(filename);

    return result.toString();
  }

  private String dataDir;
  private String sourceId;
  private String destId;
  private String name;
  private String workRequestString;
  private String absolutePath;

  /**
   * Construct a job data file.
   *
   * @param dataDir   The absolute path to the data directory.
   * @param sourceId  The id of the source node.
   * @param destId    The id of the destination node.
   * @param name      The base filename.
   */
  public JobDataFile(String dataDir, String sourceId, String destId, String name) {
    this.dataDir = dataDir;
    this.sourceId = sourceId;
    this.destId = destId;
    this.name = name;
    this.workRequestString = buildWorkRequestString(sourceId, destId, name);
    this.absolutePath = dataDir + workRequestString;
  }

  /**
   * Get the absolute path to this file's data directory.
   */
  public String getDataDir() {
    return dataDir;
  }

  /**
   * Get the source Id of the form nodeName-jvmNum.
   */
  public String getSourceId() {
    return sourceId;
  }

  /**
   * Get the destination Id of the form nodeName-jvmNum.
   */
  public String getDestId() {
    return destId;
  }

  /**
   * Get the base name of this file. The base name does not have the
   * source or destination id's or any path information.
   */
  public String getName() {
    return name;
  }

  /**
   * Get this job data file suitable for submission as a work request string.
   */
  public String getWorkRequestString() {
    return workRequestString;
  }

  /**
   * Get the absolute path to this file.
   */
  public String getAbsolutePath() {
    return absolutePath;
  }
}
