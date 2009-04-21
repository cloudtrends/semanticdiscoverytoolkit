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

import org.sd.cluster.config.ClusterException;
import org.sd.cluster.config.Console;
import org.sd.cluster.io.Response;

import java.io.IOException;
import java.util.LinkedList;

/**
 * A JobPipeline is a controller that encapsulates defining and executing a
 * sequence of jobs across the cluster.
 * <p>
 * @author Spence Koehler
 */
public class JobPipeline {

  private Console console;
  private LinkedList<AbstractJob> jobs;

  public JobPipeline(Console console) {
    this.console = console;
    this.jobs = new LinkedList<AbstractJob>();
  }

  public void addJob(AbstractJob job) {
    this.jobs.add(job);
  }

  public Response[] run() throws IOException, ClusterException {
    Response[] result = null;
    if (jobs.size() > 0) {
//      fixupJobs();
      result = console.sendJob(jobs.get(0), 5000);
    }
    return result;
  }

//   // connect each "nextJob"; set stageNum & numStages.
//   private final void fixupJobs() {
//     final int numJobs = jobs.size();
    
//     int index = 0;
//     AbstractJob prevJob = null;
//     for (AbstractJob job : jobs) {
//       if (prevJob != null) prevJob.setNextJob(job);

//       job.setNumStages(numJobs);
//       job.setStageNum(index);

//       prevJob = job;
//       ++index;
//     }
//   }
}
