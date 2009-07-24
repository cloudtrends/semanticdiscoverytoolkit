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
import org.sd.io.FilePathIterator;

/**
 * Utility class to kick off a steady state job.
 * <p>
 * @author Spence Koehler
 */
public class SteadyStateJobStarter {
  
  private int timeout;
  private SteadyStateJob firstJob;
  private Console console;
  private FilePathIterator initialInput;
  private ClusterizingJobDataDirectory dataDir;

  /**
   * Construct with the given console and job, but no initial input.
   */
  public SteadyStateJobStarter(Console console, SteadyStateJob firstJob) {
    this(console, firstJob, null);
  }

  /**
   * Construct with the given console, job, and initial input to send.
   */
  public SteadyStateJobStarter(Console console, SteadyStateJob firstJob, FilePathIterator initialInput) {
    this.timeout = 5000;  // todo: parameterize this?
    this.firstJob = firstJob;
    this.console = console;

    if (firstJob.getClusterContext() == null) {
      firstJob.setClusterContext(console.getClusterContext());
    }

    this.initialInput = initialInput;
    this.dataDir = new ClusterizingJobDataDirectory(firstJob);
  }

  /**
   * Start all job stages and send the initial input (if any).
   */
  public final boolean start() {
    boolean result = false;

    if (startStages()) {
      result = sendInitialInput();
    }

    return result;
  }

  /**
   * Send the given file to the first job.
   */
  public final boolean sendWork(String absoluteFilePath) {
    final JobDataFile jobDataFile = dataDir.getJobDataFile(absoluteFilePath);
    return dataDir.forwardWork(jobDataFile, firstJob, console, timeout);
  }

  /**
   * Start all of the job's stages across the cluster.
   */
  private final boolean startStages() {
    boolean result = true;

    int stageNum = 1;
    for (SteadyStateJob jobToStart = firstJob; jobToStart != null; jobToStart = jobToStart.getNextJob()) {

      // log: starting stage #N
      System.out.println("\nStarting stage #" + stageNum + ": " + jobToStart.getJobId() + " (" + jobToStart.getDescription() + ")");

      try {
        final Response[] responses = console.sendJob(jobToStart, timeout);

        // log: responses.
        console.showResponses(System.out, responses);
      }
      catch (ClusterException e) {
        // log problem
        System.err.println("Couldn't start job! (stage=" + stageNum + " id=" + jobToStart.getJobId() + ")");
        e.printStackTrace(System.err);
        result = false;
      }

      ++stageNum;
    }

    return result;
  }

  private final boolean sendInitialInput() {
    boolean result = true;

    if (initialInput == null) return true;  // nothing to send is ok.

    String workRequest = initialInput.getNextFilePath();

    while (workRequest != null) {
      if (!sendWork(workRequest)) {
        // log/report failure.
        System.out.println("!!! Couldn't send work requst '" + workRequest + "'!");

        result = false;
      }

      workRequest = initialInput.getNextFilePath();
    }

    return result;
  }
}
