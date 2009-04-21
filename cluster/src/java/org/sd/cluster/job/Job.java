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


import org.sd.cluster.config.Clusterable;
import org.sd.cluster.io.Message;
import org.sd.cluster.io.Response;
import org.sd.io.Publishable;

import java.util.Map;

/**
 * A job is a type of message handled by a cluster node.
 * <p>
 * @author Spence Koehler
 */
public interface Job extends Message, Clusterable {

  public String getJobId();  // identifies job on server, created by client

  public String getDescription();


  public Response operate(Publishable request);

  public void setStatus(JobStatus jobStatus);

  public JobStatus getStatus();  // identifies state: running, paused, finished, ...

  public void setGlobalJobId(GlobalJobId globalJobId);

  public GlobalJobId getGlobalJobId();

  public void setProperties(Map<String, String> properties); // for Probe data

  public void initialize(boolean start);  // initialize the job and (optionally) start.

  public void start();  // start processing job's work.

  public void stop();  // end processing job in this jvm; could still suspend? can't resume.

  public void pause();  // pause a job in this jvm to be resumed.

  public void resume();  // resume paused job.

  public boolean flush(Publishable payload);  // flush job

  public void suspend();  // suspend to disk for restoration through JobManager in another jvm

  public String getGroupName();  // identifies participating nodes: null ==> single node

  public void shutdown(boolean now);  // called when cluster is being shutdown

  public String dumpDetails();  // dump details of this job to the log

//  public int totalOps();  // total number of units of work
 
//  public int numCompletedOps();  // number of completed units of work

}
