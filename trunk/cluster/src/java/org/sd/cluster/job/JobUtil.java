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


import org.sd.cluster.config.Config;

/**
 * Common job utilities.
 * <p>
 * @author Spence Koehler
 */
public final class JobUtil {
  
  /**
   * Enumeration for data direction from a node's running job's perspective.
   */
  public enum Direction {INPUT, OUTPUT};

  /**
   * Get the job's absolute data directory path.
   * <p>
   * The data path is of the form ~/cluster/jvm-N/data/job/jobId/[input|output]/dataDirName/
   *
   * @param config       The current config.
   * @param jobId        The job's ID.
   * @param dataDirName  A label for the current run's data.
   * @param direction    INPUT for input to the job, or OUTPUT for output from the job.
   *
   * @return the job data path.
   */
  public static final String getJobDataPath(Config config, String jobId, String dataDirName, Direction direction) {
    final StringBuilder result = new StringBuilder();

    result.append(config.getJvmRootDir()).append("data/job/").
      append(jobId).append('/').
      append(direction == Direction.INPUT ? "input" : "output").append('/').
      append(dataDirName).append('/');

    return result.toString();
  }

  /**
   * <id>.<serverNodeName>-<serverNodeJvmNum>
   */
  public static final LocalJobId parseWorkServerId(String workServer) {
    if (workServer == null) return null;

    return new LocalJobId(workServer);

//     final String[] pieces = workServer.toLowerCase().split("\\.");
//     final String workServerName = pieces[1];
//     final Integer workServerJobId = new Integer(pieces[0]);
//     return new LocalJobId(workServerName, workServerJobId);
  }

}
