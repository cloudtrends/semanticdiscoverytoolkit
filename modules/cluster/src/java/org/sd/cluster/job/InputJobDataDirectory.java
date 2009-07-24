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
import org.sd.cluster.config.ClusterDefinition;
import org.sd.cluster.config.Config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A job data directory for accessing input to the job.
 * <p>
 * @author Spence Koehler
 */
public class InputJobDataDirectory implements JobDataDirectory {
  
  private static final String NAME_PATTERN_STRING = "^([a-zA-Z]+-\\d+)\\.([a-zA-Z]+-\\d+)\\.(.*)$";
  private static final Pattern NAME_PATTERN = Pattern.compile(NAME_PATTERN_STRING);

  private String absolutePath;

  public InputJobDataDirectory(SteadyStateJob job) {
    this(job.getClusterContext(), job.getJobId(), job.getDataDir());
  }

  public InputJobDataDirectory(ClusterContext clusterContext, String jobId, String dataDirName) {
    final Config config = clusterContext.getConfig();
    final ClusterDefinition clusterDef = clusterContext.getClusterDefinition();

    this.absolutePath = JobUtil.getJobDataPath(config, jobId, dataDirName, JobUtil.Direction.INPUT);
  }

  public JobDataFile getJobDataFile(String workRequestString) {
    // assume workRequestString is of the form srcNode-jvmNum.destNode-jvmNum.name
    final Matcher m = NAME_PATTERN.matcher(workRequestString);
    if (!m.matches()) {
      throw new IllegalArgumentException("work request string '" + workRequestString + "' must be of the form: '" +
                                         "srcNode-jvmNum.destNode-jvmNum.name' when direction=INPUT !");
    }

    final String sourceId = m.group(1);
    final String destId = m.group(2);
    final String name = m.group(3);

    return new JobDataFile(absolutePath, sourceId, destId, name);
  }
}
