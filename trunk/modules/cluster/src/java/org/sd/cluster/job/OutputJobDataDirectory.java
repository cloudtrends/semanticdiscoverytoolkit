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
import org.sd.cluster.config.Console;

/**
 * A job data directory for accessing output from a job (to be transferred
 * as input to another job).
 * <p>
 * @author Spence Koehler
 */
public class OutputJobDataDirectory extends AbstractJobDataDirectory {
  
  public OutputJobDataDirectory(SteadyStateJob job) {
    this(job.getClusterContext(), job.getPartitionFunction(), job.getJobId(), job.getDataDir());
  }

  public OutputJobDataDirectory(ClusterContext clusterContext, PartitionFunction partitionFunction, String jobId, String dataDirName) {
    super(clusterContext, partitionFunction,
          JobUtil.getJobDataPath(clusterContext.getConfig(), jobId, dataDirName, JobUtil.Direction.OUTPUT));
  }

  public JobDataFile getJobDataFile(String workRequestString) {
    // sourceId = this node's id
    final String sourceId = getClusterContext().getConfig().getNodeName();

    // destId = destination based on group and partition function
    final String destId = computeDestId(workRequestString);

    return new JobDataFile(getAbsolutePath(), sourceId, destId, workRequestString);
  }

  public boolean forwardWork(JobDataFile workRequest, Job nextJob, Console console, int timeout) {

    final String absoluteSourcePath = workRequest.getAbsolutePath();
    final String absoluteDestPath = changeOutputToInput(absoluteSourcePath, workRequest.getDestId());

    return doForwardWork(workRequest, nextJob, console, timeout, absoluteSourcePath, absoluteDestPath);
  }

  private String changeOutputToInput(String absoluteSourcePath, String destId) {
    String result = absoluteSourcePath.replaceFirst("/output/", "/input/");
    return fixAbsolutePath(result, destId);
  }
}
