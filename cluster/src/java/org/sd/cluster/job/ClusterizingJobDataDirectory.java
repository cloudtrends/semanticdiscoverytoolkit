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
import org.sd.io.FileUtil;

/**
 * A job data directory for accessing file data by absolute path to be
 * transferred as input to another job.
 * <p>
 * @author Spence Koehler
 */
public class ClusterizingJobDataDirectory extends AbstractJobDataDirectory {
  
  public ClusterizingJobDataDirectory(SteadyStateJob job) {
    this(job.getClusterContext(), job.getPartitionFunction(), job.getJobId(), job.getDataDir());
  }

  public ClusterizingJobDataDirectory(ClusterContext clusterContext, PartitionFunction partitionFunction, String jobId, String dataDirName) {
    super(clusterContext, partitionFunction,
          JobUtil.getJobDataPath(clusterContext.getConfig(), jobId, dataDirName, JobUtil.Direction.INPUT));
  }

  /**
   * Get a job data file given an absolute file path.
   */
  public JobDataFile getJobDataFile(String absoluteFilePath) {
    final String dataDir = FileUtil.getBasePath(absoluteFilePath);
    final String name = FileUtil.getFileName(absoluteFilePath);

    // sourceId = this node's id
    final String sourceId = getClusterContext().getConfig().getNodeName();

    // destId = destination based on group and partition function
    final String destId = computeDestId(name);

    return new JobDataFile(dataDir, sourceId, destId, name);
  }

  public boolean forwardWork(JobDataFile workRequest, Job nextJob, Console console, int timeout) {

    final String absolutePath = fixAbsolutePath(getAbsolutePath(), workRequest.getDestId());
    final JobDataFile clusterFile = new JobDataFile(absolutePath, workRequest.getSourceId(), workRequest.getDestId(), workRequest.getName());

    final String absoluteSourcePath = workRequest.getDataDir() + workRequest.getName();
    final String absoluteDestPath = clusterFile.getAbsolutePath();

    return doForwardWork(workRequest, nextJob, console, timeout, absoluteSourcePath, absoluteDestPath);
  }
}
