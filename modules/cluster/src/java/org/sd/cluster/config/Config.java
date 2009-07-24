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
package org.sd.cluster.config;


import org.sd.util.ExecUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Configuration for the cluster.
 * <p>
 * This is largely initialized from the runtime environment. There will be one instance per jvm.
 *
 * @author Spence Koehler
 */
public class Config {
  
  /**
   * Helper to make a test config.
   * <p>
   * This is intened for use in testing (including mains).
   */
  public static final Config makeTestConfig(int jvmNum) {
    return new Config(jvmNum, ExecUtil.getUser());
  }


  public static final Object configMutex = new Object();

  private String machineName;     // name of machine running this jvm
  private String user;            // current user running this jvm
  private int jvmNum;             // id for this jvm 0...
  private int serverPort;         // server port for this jvm
  private String jvmRootDir;      // filesystem path to jvm-specific cluster root dir ("/home/$USER/cluster/jvm-N/")
  private String name;            // name or signature for config

  /**
   * Package protected because, other than for testing, only a ClusterNode should ever construct a config.
   * The ClusterNode's NodeServer will pass this config in a ClusterContext to Messages that it handles.
   * The Message will pass it to Clusterable objects.
   */
  Config(int jvmNumHint, String user) {

// 2007-10-24 (sbk): Turning off 'running.csv'  ... it has been more trouble than help in the end!
//    final int computedJvmNum = RunningCsv.getInstance().getCurrentJvmId();
//
//    if (jvmNumHint != computedJvmNum) {
//      System.err.println("***WARNING: jvmNumHint(" + jvmNumHint + ") != computedJvmNum(" + computedJvmNum + "). Using computed.");
//      new RuntimeException("I'm here").printStackTrace(System.err);
//    }
//    this.jvmNum = computedJvmNum;

    this.jvmNum = jvmNumHint;

    final int pid = ExecUtil.getProcessId();
    System.out.println(new Date() + ": Config initialization : jvmNum=" + jvmNum + " pid=" + pid);

    init(ExecUtil.getMachineName(),
         user == null ? ExecUtil.getUser() : user,
         jvmNum,
         ConfigUtil.getServerPort(jvmNum, user),
         ConfigUtil.getClusterRootDir() + "jvm-" + Integer.toString(jvmNum) + "/");
  }

  /**
   * For JUnit testing only!
   */
  Config(String machineName, String user, int jvmNum, int serverPort, String jvmRootDir) {
    init(machineName, user, jvmNum, serverPort, jvmRootDir);
  }

  private final void init(String machineName, String user, int jvmNum, int serverPort, String jvmRootDir) {
    this.machineName = machineName;
    this.user = user;
    this.jvmNum = jvmNum;
    this.serverPort = serverPort;
    this.jvmRootDir = jvmRootDir;

    final int pid = ExecUtil.getProcessId();
    this.name = user + "@" + machineName + "-" + Integer.toString(jvmNum) + ":" + serverPort + ":" + pid;
  }


  public String getMachineName() {
    return machineName;
  }

  public int getJvmNum() {
    return jvmNum;
  }

  public String getNodeName() {
    return machineName + "-" + Integer.toString(jvmNum);
  }

  public String getUser() {
    return user;
  }

  public int getServerPort() {
    return serverPort;
  }

  public String getJvmRootDir() {
    return jvmRootDir;
  }

  public String getName() {
    return name;
  }

  public String getJobDataPath(String jobIdString, String dataDirName) {
    final StringBuilder result = new StringBuilder();

    result.
      append(jvmRootDir).append("data/job/").
      append(jobIdString).append('/').
      append(dataDirName).append('/');

    return result.toString();
  }

  public String getOutputDataRoot(String jobIdString) {
    final StringBuilder result = new StringBuilder();

    result.
      append(getOutputDataRootParent()).
      append(jobIdString).append('/');

    return result.toString();
  }

  public String getOutputDataRootParent() {
    return jvmRootDir + "data/output/";
  }

  public String getOutputDataPath(String jobIdString, String dataDirName) {
    return getOutputDataPath(jobIdString, dataDirName, getNodeName());
  }

  public String getOutputDataPath(String jobIdString, String dataDirName, String baseName) {
    final StringBuilder result = new StringBuilder();

    result.
      append(getOutputDataRoot(jobIdString)).
      append(dataDirName).append('/').
      append(baseName).
      append(".out");

    return result.toString();
  }

  public String getOutputDataPath(String jobIdString, String dataDirParent, String dataDirName, String baseName) {
    final StringBuilder result = new StringBuilder();

    result.
      append(getOutputDataRoot(jobIdString)).
      append(dataDirParent).append('/').
      append(dataDirName).append('/').
      append(baseName).
      append(".out");

    return result.toString();
  }


  /**
   * Get the next available index dir as the last element of the list (and all existing dirs
   * as prior elements).
   */
  public List<File> nextAvailableIndexDir(String jobIdString, String dataDirName, String indexId, String indexType) {
    return nextAvailableIndexDir(jobIdString, dataDirName, indexId, false, indexType);
  }

  /**
   * Get the next available index dir as the last element of the list (and all existing dirs
   * as prior elements).
   */
  public List<File> nextAvailableIndexDir(String jobIdString, String dataDirName, String indexId, boolean doAsian, String indexType) {
    List<File> result = new ArrayList<File>();

    int index = 0;
    while (true) {
      String dirValue = buildIndexName(jobIdString, dataDirName, index, indexId, doAsian, indexType);
      File dir = new File(dirValue);
      result.add(dir);
      if (!dir.exists()) break;
      index++;
    }

    return result;
  }

  public String buildIndexName(String jobId, String dataDirName, int index, String indexId, String indexType) {
    return getOutputDataPath(jobId, dataDirName + "_" + index + "_" + indexId + indexType/*"Index"*/);
  }

  public String buildIndexName(String jobId, String dataDirName, int index, String indexId, boolean doAsian, String indexType) {
    String result = buildIndexName(jobId, dataDirName, index, indexId, indexType);

    if (doAsian) {
      result = result + "-asian";
    }

    return result;
  }

  /**
   * Get the next available index dir as the last element of the list (and all existing dirs
   * as prior elements).
   */
  public List<File> nextAvailableIndexDir(String jobIdString, String dataDirParent, String dataDirName, String indexId, String indexType) {
    return nextAvailableIndexDir(jobIdString, dataDirParent, dataDirName, indexId, false, indexType);
  }

  /**
   * Get the next available index dir as the last element of the list (and all existing dirs
   * as prior elements).
   */
  public List<File> nextAvailableIndexDir(String jobIdString, String dataDirParent, String dataDirName, String indexId, boolean doAsian, String indexType) {
    List<File> result = new ArrayList<File>();

    int index = 0;
    while (true) {
      String dirValue = buildIndexNameWithParent(jobIdString, dataDirParent, dataDirName, index, indexId, doAsian, indexType);
      File dir = new File(dirValue);
      result.add(dir);
      if (!dir.exists()) break;
      index++;
    }

    return result;
  }

  public String buildIndexNameWithParent(String jobId, String dataDirParent, String dataDirName, int index, String indexId, String indexType) {
    return getOutputDataPath(jobId, dataDirParent, dataDirName + "_" + index + "_" + indexId + indexType/*"Index"*/, getNodeName());
  }

  public String buildIndexNameWithParent(String jobId, String dataDirParent, String dataDirName, int index, String indexId, boolean doAsian, String indexType) {
    String result = buildIndexNameWithParent(jobId, dataDirParent, dataDirName, index, indexId, indexType);

    if (doAsian) {
      result = result + "-asian";
    }

    return result;
  }
}
