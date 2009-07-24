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


import org.sd.cluster.config.ClusterContext;
import org.sd.cluster.config.ClusterException;
import org.sd.cluster.config.Console;
import org.sd.cluster.config.DataPusher;
import org.sd.cluster.io.Response;
import org.sd.io.PublishableString;

/**
 * Abstract implementation of the job data directory interface.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractJobDataDirectory implements JobDataDirectory {
  
  private ClusterContext clusterContext;
  private PartitionFunction partitionFunction;
  private String absolutePath;

  protected AbstractJobDataDirectory(ClusterContext clusterContext, PartitionFunction partitionFunction, String absolutePath) {
    this.clusterContext = clusterContext;
    this.partitionFunction = partitionFunction;
    this.absolutePath = absolutePath;
  }

  public ClusterContext getClusterContext() {
    return clusterContext;
  }

  public PartitionFunction getPartitionFunction() {
    return partitionFunction;
  }

  public String getAbsolutePath() {
    return absolutePath;
  }

  protected final boolean doForwardWork(JobDataFile workRequest, Job nextJob, Console console, int timeout,
                                        String absoluteSourcePath, String absoluteDestPath) {
    boolean result = true;

    if (nextJob != null) {

      final String destId = workRequest.getDestId();
      final String destMachine = getDestMachine(destId);

      DataPusher.sendFileToNode(absoluteSourcePath, destMachine, absoluteDestPath);

      final GlobalJobId globalJobId = nextJob.getGlobalJobId();
      final LocalJobId localJobId = globalJobId.getLocalJobId(destId);
      final String workString = workRequest.getWorkRequestString();
      final JobCommandMessage jobCommand = new JobCommandMessage(JobCommand.OPERATE, localJobId, new PublishableString(workString));
      try {
        final Response response = console.sendJobCommandToNode(jobCommand, timeout);
//todo: log notification and response.
      }
      catch (ClusterException e) {
        e.printStackTrace(System.err);
        result = false;
      }
    }

    return result;
  }

  protected final String computeDestId(String workRequestString) {
    return partitionFunction.computeDestinationId(workRequestString.hashCode());
  }

  protected final String getDestMachine(String destId) {
    String result = null;

    final int dashPos = destId.indexOf('-');
    if (dashPos >= 0) {
      result = destId.substring(0, dashPos);
    }

    return (result == null) ? destId : result;
  }

  /**
   * Fix the absolute path to have the destinaion's jvm-num, not this node's jvm-num.
   */
  protected final String fixAbsolutePath(String absolutePath, String destId) {
    final String jvmNum = getJvmNum(destId);
    return absolutePath.replaceFirst("/jvm-\\d+/", "/jvm-" + jvmNum + "/");
  }

  /**
   * Return "N" from "nodeName-N".
   */
  protected final String getJvmNum(String destId) {
    String jvmNum = null;

    final int dashPos = destId.indexOf('-');
    if (dashPos >= 0) {
      jvmNum = destId.substring(dashPos + 1);
    }

    return jvmNum == null ? "0" : jvmNum;
  }


}
