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


import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.sd.cluster.config.BooleanResponse;
import org.sd.cluster.config.ClusterException;
import org.sd.cluster.config.Console;
import org.sd.cluster.config.VirtualCluster;
import org.sd.cluster.io.Response;
import org.sd.util.Waiter;
import org.sd.util.thread.UnitCounter;

/**
 * JUnit Tests for the CountingJob class.
 * <p>
 * @author Spence Koehler
 */
public class TestCountingJob extends TestCase {

  public TestCountingJob(String name) {
    super(name);
  }
  

  public void testAll() throws UnknownHostException, ClusterException, IOException {

    final VirtualCluster cluster = new VirtualCluster("TestCountingJob", "1m1n.1.def", "TestCountingJob");
    JobCommandMessage jobCommandMessage = null;
    Response[] responses = null;
    UnitCounter uc = null;
    long curCount = 0;

    try {
      cluster.start();
      final Console console = cluster.getConsole();

      // initialize job
      final Properties properties = new Properties();
      properties.setProperty("jobId", "CountingJob");
      properties.setProperty("groupName", "all");
      properties.setProperty("dataDirName", "counts");
      properties.setProperty("verbose", "false");
      properties.setProperty("countTo", "100");
      properties.setProperty("countWait", "5");
      properties.setProperty("checkInterval", "1");
      
      // create/send job
      final Job job = new CountingJob(properties);
      responses = console.sendJob(job, 5000);
      assertEquals(1, responses.length);

      // wait for job to arrive
      assertTrue(Waiter.waitFor(new Waiter() {
          public boolean event() {
            final List<Job> runningJobs = cluster.getRunningJobs();
            return (runningJobs.size() == 1);
          }
        }, 1000, 1));
      final CountingJob countingJob = (CountingJob)cluster.getRunningJobs().get(0);

      // wait for job to start running
      assertTrue(Waiter.waitFor(new StatusWaiter(countingJob, JobStatus.RUNNING), 1000, 1));

      uc = countingJob.getUnitCounter();

      // wait for counter to change
      assertTrue(Waiter.waitFor(new ChangeWaiter(uc), 1000, 1));

      // pause the instance through the JobManager
      jobCommandMessage = new JobCommandMessage(JobCommand.PAUSE, countingJob.getLocalJobId());
      responses = console.sendJobCommand(jobCommandMessage, 5000);
      assertEquals(1, responses.length);

      // wait for the instance to pause
      assertTrue(Waiter.waitFor(new StatusWaiter(countingJob, JobStatus.PAUSED), 1000, 1));

      // make sure the count doesn't change.
      assertFalse(Waiter.waitFor(new ChangeWaiter(uc), 20, 1));
      assertEquals(JobStatus.PAUSED, countingJob.getStatus());

      // resume the instance
      jobCommandMessage = new JobCommandMessage(JobCommand.RESUME, countingJob.getLocalJobId());
      responses = console.sendJobCommand(jobCommandMessage, 5000);
      assertEquals(1, responses.length);

      // make sure the count does change.
      assertTrue(Waiter.waitFor(new StatusWaiter(countingJob, JobStatus.RUNNING), 1000, 1));
      assertTrue(Waiter.waitFor(new ChangeWaiter(uc), 20, 1));
      curCount = uc.doneSoFar();

      // bounce the resumed instance
      jobCommandMessage = new JobCommandMessage(JobCommand.BOUNCE, countingJob.getLocalJobId());
      responses = console.sendJobCommand(jobCommandMessage, 5000);
      assertEquals(1, responses.length);
      assertTrue(((BooleanResponse)responses[0]).getValue());

      // make sure the count changes
      assertTrue(Waiter.waitFor(new StatusWaiter(countingJob, JobStatus.RUNNING), 1000, 1));
      uc = countingJob.getUnitCounter();        // need to get fresh instance
      assertTrue(curCount > uc.doneSoFar());    // should have gone backwards since restarted

      // let advance, then pause to bounce while paused
      assertTrue(Waiter.waitFor(new ChangeWaiter(uc), 20, 1));
      jobCommandMessage = new JobCommandMessage(JobCommand.PAUSE, countingJob.getLocalJobId());
      responses = console.sendJobCommand(jobCommandMessage, 5000);
      assertEquals(1, responses.length);

      // make sure the count doesn't change.
      assertTrue(Waiter.waitFor(new StatusWaiter(countingJob, JobStatus.PAUSED), 1000, 1));
      assertFalse(Waiter.waitFor(new ChangeWaiter(uc), 20, 1));
      assertEquals(JobStatus.PAUSED, countingJob.getStatus());
      curCount = uc.doneSoFar();

      // bounce the paused instance
      jobCommandMessage = new JobCommandMessage(JobCommand.BOUNCE, countingJob.getLocalJobId());
      responses = console.sendJobCommand(jobCommandMessage, 5000);
      assertEquals(1, responses.length);
      assertTrue(((BooleanResponse)responses[0]).getValue());

      // make sure the count changes
      assertTrue(Waiter.waitFor(new StatusWaiter(countingJob, JobStatus.RUNNING), 1000, 1));
      uc = countingJob.getUnitCounter();        // need to get fresh instance
      assertTrue(curCount > uc.doneSoFar());    // should have gone backwards since restarted
      assertTrue(Waiter.waitFor(new ChangeWaiter(uc), 20, 1));  // and should be moving forward
    }
    finally {
      cluster.shutdown();
    }
  }

  private static final class ChangeWaiter extends Waiter {

    private long start;
    private UnitCounter uc;

    ChangeWaiter(UnitCounter uc) {
      this.start = uc.doneSoFar();
      this.uc = uc;
    }

    public boolean event() {
      return (start < uc.doneSoFar());   // true when counter has increased from start
    }
  }

  private static final class StatusWaiter extends Waiter {

    private CountingJob countingJob;
    private JobStatus status;

    StatusWaiter(CountingJob countingJob, JobStatus status) {
      this.countingJob = countingJob;
      this.status = status;
    }

    public boolean event() {
      return (status == countingJob.getStatus());  // true when status matches
    }
  }


  public static Test suite() {
    TestSuite suite = new TestSuite(TestCountingJob.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
