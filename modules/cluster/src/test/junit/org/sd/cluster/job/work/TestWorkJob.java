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
package org.sd.cluster.job.work;


import org.sd.cluster.config.ClusterContext;
import org.sd.cluster.config.ClusterException;
import org.sd.cluster.config.Console;
import org.sd.cluster.config.StringResponse;
import org.sd.cluster.config.VirtualCluster;
import org.sd.cluster.io.Response;
import org.sd.cluster.job.JobCommand;
import org.sd.cluster.job.JobCommandMessage;
import org.sd.cluster.job.JobStatus;
import org.sd.cluster.job.LocalJobId;
import org.sd.io.FileUtil;
import org.sd.io.Publishable;
import org.sd.io.PublishableString;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit Tests for the WorkJob class.
 * <p>
 * @author Spence Koehler
 */
public class TestWorkJob extends TestCase {

  private final String strings = "Don, Dave, Ryan, Abe, Ken, Spence";

  public TestWorkJob(String name) {
    super(name);
  }
  
  public void testNull() {
  }

  public void test1() throws IOException, ClusterException {
    final VirtualCluster cluster = new VirtualCluster("TestWorkJob", "test-5m7n.1-2-4.def", "TestWorkJob");
    try {
      cluster.start();
      final Console console = cluster.getConsole();

      //
      // "A[workServer] -- B[workClient worker workServer] -- C[workClient worker workServer] -- D[workClient worker]
      //
      // A: queue a batch of strings
      // B: reverse the strings
      // C: reverse the (reversed) strings
      // D: create a batch of strings
      //

      // create/send WorkJob "A"
      final Properties wjaProperties = new Properties();
      wjaProperties.setProperty("jobId", "WorkJob-A");
      wjaProperties.setProperty("groupName", "node1-0");
      wjaProperties.setProperty("workServer", BatchWorkServer.class.getName());
      wjaProperties.setProperty("strings", strings);
      wjaProperties.setProperty("serverQueueDir", "/tmp/TestWorkJob/WorkJob-A-Queue");
//      final String idOfServerA = WorkJob.run(wjaProperties, console);  // run the job
      WorkJob.run(wjaProperties, console);  // run the job
      final String idOfServerA = "WorkJob-WorkJob-A.node1-0";

      // create WorkJob "B"
      final Properties wjbProperties = new Properties();
      wjbProperties.setProperty("numThreads", "5");
      wjbProperties.setProperty("jobId", "WorkJob-B");
      wjbProperties.setProperty("groupName", "node2-0");
      wjbProperties.setProperty("workClient", ServerWorkClient.class.getName());
      wjbProperties.setProperty("workServerId", idOfServerA);
      wjbProperties.setProperty("worker", StringReverserWorker.class.getName());
      wjbProperties.setProperty("workServer", QueueWorkServer.class.getName());
      wjbProperties.setProperty("serverQueueDir", "/tmp/TestWorkJob/WorkJob-B-Queue");
//      final String idOfServerB = WorkJob.run(wjbProperties, console);  // run the job
      WorkJob.run(wjbProperties, console);  // run the job
      final String idOfServerB = "WorkJob-WorkJob-B.node2-0";

      // create WorkJob "C"
      final Properties wjcProperties = new Properties();
      wjcProperties.setProperty("numThreads", "5");
      wjcProperties.setProperty("jobId", "WorkJob-C");
      wjcProperties.setProperty("groupName", "node3-0");
      wjcProperties.setProperty("workClient", ServerWorkClient.class.getName());
      wjcProperties.setProperty("workServerId", idOfServerB);
      wjcProperties.setProperty("worker", StringReverserWorker.class.getName());
      wjcProperties.setProperty("workServer", QueueWorkServer.class.getName());
      wjcProperties.setProperty("serverQueueDir", "/tmp/TestWorkJob/WorkJob-C-Queue");
//      final String idOfServerC = WorkJob.run(wjcProperties, console);  // run the job
      WorkJob.run(wjcProperties, console);  // run the job
      final String idOfServerC = "WorkJob-WorkJob-C.node3-0";

      // create WorkJob "D"
      final Properties wjdProperties = new Properties();
      wjdProperties.setProperty("numThreads", "5");
      wjdProperties.setProperty("jobId", "WorkJob-D");
      wjdProperties.setProperty("groupName", "node4-0");
      wjdProperties.setProperty("workClient", ServerWorkClient.class.getName());
      wjdProperties.setProperty("workServerId", idOfServerC);
      wjdProperties.setProperty("worker", QueueDrainWorker.class.getName());
      wjdProperties.setProperty("queueDrainerOutput", "/tmp/TestWorkJob/queueDrainerOutput.txt");
//      final String idOfServerD = WorkJob.run(wjdProperties, console);  // run the job
      WorkJob.run(wjdProperties, console);  // run the job
      final String idOfServerD = "WorkJob-WorkJob-D.node4-0";

      // wait until WorkJob "D" is done
//      final LocalJobId serverDJobId = new LocalJobId("node4-0", Integer.parseInt(idOfServerD.split("\\.")[0]));
      final LocalJobId serverDJobId = new LocalJobId(idOfServerD);
      final JobCommandMessage checkStatusMessage = new JobCommandMessage(JobCommand.STATUS, serverDJobId);
      while (true) {
        try {
          final Response response = console.sendJobCommandToNode(checkStatusMessage, 500);
          if (response != null) {
            final String statusString = ((StringResponse)response).getValue();
            final JobStatus jobStatus = Enum.valueOf(JobStatus.class, statusString);

            System.out.println("!!!! got jobStatus=" + jobStatus + " !!!!");

            if (jobStatus != JobStatus.RUNNING) break;  // finished!
          }
          else break;

          try {
            Thread.sleep(500);
          }
          catch (InterruptedException e) {
            break;
          }
        }
        catch (ClusterException e) {
          e.printStackTrace(System.err);
          break;
        }
      }
    }
    finally {
      System.out.println("TestJobManager: shutting down cluster.");
      cluster.shutdown();
    }

    // make sure we drained all of the output
    final Set<String> drained = new HashSet<String>();
    FileUtil.readStrings(drained, new File("/tmp/TestWorkJob/queueDrainerOutput.txt"), null, null, null, false, false);
    final String[] sstrings = strings.split("\\s*,\\s*");
    assertEquals(sstrings.length, drained.size());
    for (String string : sstrings) {
      assertTrue(string, drained.contains(string));
    }

    // clean up after self.
    FileUtil.deleteDir(new File("/tmp/TestWorkJob"));
  }

  public static final class BatchWorkServer extends QueueWorkServer {
    private String strings;
    private final AtomicLong counter = new AtomicLong(0);

    public BatchWorkServer(Properties properties) {
      super(properties);

      this.strings = properties.getProperty("strings");
    }

    protected boolean doInitialize(ClusterContext clusterContext, String jobIdString, String dataDirName) {
      boolean result = super.doInitialize(clusterContext, jobIdString, dataDirName);

      if (result) {
        for (String string : strings.split("\\s*,\\s*")) {
          addWork(new WorkRequest(counter.getAndIncrement(), new PublishableString(string)));
        }
      }

      setNoMoreWork(true);  // notify that we won't be adding any more work.

      return result;
    }
  }

  public static final class StringReverserWorker implements Worker {

    private final AtomicLong counter = new AtomicLong(0);

    public StringReverserWorker(Properties properties) {
    }

    public boolean initialize(ClusterContext clusterContext, String jobIdString, String dataDirName) {
      return true;
    }

    public boolean performWork(KeyedWork keyedWork, AtomicBoolean die, AtomicBoolean pause, QueueDesignator queueDesignator, WorkQueue destination) {
      final PublishableString publishableString = (PublishableString)keyedWork.getWork();
      final String string = publishableString.getString();
      final String reversed = reverse(string);

      destination.addWork(new WorkRequest(counter.getAndIncrement(), new PublishableString(reversed)));

      return true;
    }

    private String reverse(String string) {
      final StringBuilder result = new StringBuilder();

      for (int i = string.length() - 1; i >= 0; --i) {
        result.append(string.charAt(i));
      }

      return result.toString();
    }

    public boolean flush(Publishable payload) {
      return true;  // nothing to do... is there?
    }

    public void close() {}

    public String getStatusString() {return null;}
  }

  public static final class QueueDrainWorker implements Worker {

    private String outputFilename;
    private BufferedWriter outputWriter;

    public QueueDrainWorker(Properties properties) {
      this.outputFilename = properties.getProperty("queueDrainerOutput");
    }

    public boolean initialize(ClusterContext clusterContext, String jobIdString, String dataDirName) {
      try {
        this.outputWriter = FileUtil.getWriter(outputFilename);
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
      return true;
    }

    public boolean performWork(KeyedWork keyedWork, AtomicBoolean die, AtomicBoolean pause, QueueDesignator queueDesignator, WorkQueue destination) {

      final PublishableString publishableString = (PublishableString)keyedWork.getWork();
      final String string = publishableString.getString();

      synchronized (outputWriter) {
        try {
          outputWriter.write(string);
          outputWriter.newLine();
          outputWriter.flush();
        }
        catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }

      return true;
    }

    public boolean flush(Publishable payload) {
      return true;  // nothing to do... is there?
    }

    public void close() {
      if (outputWriter != null) {
        try {
          outputWriter.close();
        }
        catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }
    }

    public String getStatusString() {return null;}
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(TestWorkJob.class);
    return suite;
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
