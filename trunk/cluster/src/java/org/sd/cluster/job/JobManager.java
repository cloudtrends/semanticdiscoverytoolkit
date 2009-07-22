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


import org.sd.cluster.config.BooleanResponse;
import org.sd.cluster.config.ClusterContext;
import org.sd.cluster.config.ClusterDefinition;
import org.sd.cluster.config.ClusterException;
import org.sd.cluster.config.ClusterRunner;
import org.sd.cluster.config.Config;
import org.sd.cluster.config.Console;
import org.sd.cluster.config.StringResponse;
import org.sd.cluster.io.Context;
import org.sd.cluster.io.Response;
import org.sd.cluster.io.Shutdownable;
import org.sd.cluster.job.work.WorkResponse;
import org.sd.io.FileLock;
import org.sd.io.FileUtil;
import org.sd.io.Publishable;
import org.sd.util.PropertiesParser;
import org.sd.util.ReflectUtil;
import org.sd.util.Timer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Singleton to manage all jobs for a ClusterNode.
 * <p>
 * @author Spence Koehler
 */
public class JobManager {

  private Config config;
  private ClusterDefinition clusterDefinition;
  private String identifier;
  private ExecutorService jobThreadPool;
  private AtomicBoolean isAlive = new AtomicBoolean(true);
  private Map<String, Integer> desc2jobId;
  private Map<Integer, Job> id2job;
  private Map<String, Integer> idString2localId;
  private List<Job> oldJobs;  //todo: make accessor messages (get, reset)
  private List<Job> badJobs;  //todo: make accessor messages (get, reset)
  private Console _console = null;
  private final Object consoleMutex = new Object();
  private List<Shutdownable> shutdownables;

  private FileLock<Integer> localIdFileLock;
  private FileLock.LockOperation<Integer> incrementIdOperation;


  /**
   * Construct a job manager.
   * <p>
   * Note that there will be one job manager per cluster node.
   * The ClusterNode's NodeServer will pass this jobManager in a ClusterContext to Messages (Jobs) that it handles.
   */
  public JobManager(Config config, ClusterDefinition clusterDefinition, final String identifier) {
    this.config = config;
    this.clusterDefinition = clusterDefinition;
    this.identifier = identifier;
    this.desc2jobId = new HashMap<String, Integer>();
    this.id2job = new HashMap<Integer, Job>();
    this.idString2localId = new HashMap<String, Integer>();
    this.oldJobs = new ArrayList<Job>();  //todo: put size limit and/or add reset method
    this.badJobs = new ArrayList<Job>();  //todo: put size limit and/or add reset method
    this.shutdownables = new ArrayList<Shutdownable>();

    final AtomicInteger jobThreadId = new AtomicInteger(0);
    this.jobThreadPool = Executors.newCachedThreadPool(
      new ThreadFactory() {
        public Thread newThread(Runnable r) {
          return new Thread(r, identifier + "-JobThread-" + jobThreadId.getAndIncrement());
        }
      });

    final String localIdFilePath = config.getJvmRootDir() + "data/job/next-local-id.txt";
    this.localIdFileLock = new FileLock<Integer>(localIdFilePath, 100);
    this.incrementIdOperation = new FileLock.LockOperation<Integer>() {
      public Integer operate(String filename) throws IOException {
        int curValue = 0;

        // slurp contents as integer
        if (new File(filename).exists()) {
          final String contents = FileUtil.readAsString(filename);
          curValue = Integer.parseInt(contents);
        }

        // rewrite file with incremented integer
        FileUtil.writeToFile(filename, Integer.toString(curValue + 1), false);

        return curValue;
      }
    };
  }

  public void registerShutdownable(Shutdownable shutdownable) {
    shutdownables.add(shutdownable);
  }

  public void shutdown(boolean now) {
    if (isAlive.get()) {
      isAlive.set(false);

System.out.println("JobManager-" + identifier + " shutting down! (now=" + now + ")");

      if (now) {
        jobThreadPool.shutdownNow();
      }
      else {
        jobThreadPool.shutdown();
      }

      for (Job job : id2job.values()) {
        job.shutdown(now);
      }
    }

    // call all registered shutdownables
    for (Shutdownable shutdownable : shutdownables) {
      shutdownable.shutdown(now);
    }

    synchronized (consoleMutex) {
      if (_console != null) {
        _console.shutdown();
        _console = null;
      }
    }
  }

  public final synchronized int registerJob(Job job, String myNodeName, boolean beginImmediately) {
    if (!isAlive.get()) return -1;

    final GlobalJobId globalJobId = job.getGlobalJobId();
    final LocalJobId localJobId = globalJobId.getLocalJobId(myNodeName);
    Integer lid = null;

    if (localJobId == null) {
      // place the job on a "bad" list for reporting.
      badJobs.add(job);

      // log problem
      System.out.println(myNodeName + ".JobManager: can't find my (" + myNodeName + ") local job ID! (job=" + job + ")");
    }
    else {
      boolean jobExists = false;

      // check for existing job by description and replace
      final Integer existingJobId = desc2jobId.get(job.getDescription());
      if (existingJobId != null) {
        final Job existingJob = id2job.get(existingJobId);
        System.out.println("WARNING: Recived duplicate job registration '" + job.getDescription() + "'!");

        if (existingJob.getStatus() == JobStatus.FINISHED) {
          System.out.println("\tDropping old 'finished' job and retaining new.");
          id2job.remove(existingJobId);
          idString2localId.remove(existingJob.getJobId());
          desc2jobId.remove(existingJob.getDescription());
        }
        else {
          System.out.println("WARNING: Received duplicate job registration '" + job.getDescription() + "'! Retaining existing, but reassigning its local ID.");

          //retain previous jobId so every node doesn't have to constantly update
          //don't remove old: 'id2job.remove(existingJobId);'
          job = existingJob;
          jobExists = true;
        }
      }

      lid = localJobId.getId();
      if (!jobExists) jobThreadPool.execute(new NewJobHandler(job, lid, beginImmediately));
      id2job.put(lid, job);
      idString2localId.put(job.getJobId(), lid);
      desc2jobId.put(job.getDescription(), lid);

      System.out.println(myNodeName +
                         ".JobManager: registered job (localId=" + lid +
                         ", globalId=" + globalJobId.getGlobalJobId() + ") '" +
                         job + "'");
    }

    return lid;
  }

  public final Integer getLocalId(String jobDescription) {
    return desc2jobId.get(jobDescription);
  }

  public final String getAllJobStrings() {
    final StringBuilder result = new StringBuilder();

    for (Map.Entry<Integer, Job> jobEntry : id2job.entrySet()) {
      result.
        append("lid=").append(jobEntry.getKey()).
        append(": ").append(jobEntry.getValue().getStatus()).
        append("\t:\t").append(jobEntry.getValue().toString()).append('\n');
    }

    // log it to stdout for a permanent record.  todo: parameterize this?
    System.out.println(new Date() + ": " + result.toString());

    return result.toString();
  }

  public final int getNextLocalJobId() {
    return localIdFileLock.operateWhileLocked(incrementIdOperation);
  }

  public final GlobalJobId getGlobalJobId(int localJobId) {
    GlobalJobId result = null;

    final Job job = id2job.get(localJobId);
    if (job != null) {
      result = job.getGlobalJobId();
    }

    return result;
  }

  public final Collection<Integer> getActiveJobIds(String jobIdString) {
//    synchronized (idString2localId) {
      return idString2localId.values();
//    }
  }

  public final Job getActiveJob(int localJobId) {
    return id2job.get(localJobId);
  }

  private final Job getJob(LocalJobId localJobId) {
    Job result = null;

    if (localJobId.getJobDescription() != null) {
      final Integer locId = getLocalId(localJobId.getJobDescription());
      if (locId != null) {
        localJobId.setId(locId);
      }
    }

    return id2job.get(localJobId.getId());
  }

  /**
   * Handle the command for the specified local job.
   * <p>
   * If the local job id is null, send the command to all jobs.
   *
   * @param context     the current context.
   * @param jobCommand  the job command to handle.
   * @param localJobId  the target job for receiving the command. (all jobs if null).
   * @param payload     the payload accompanying the command (usually null).
   * @param remoteJobId the (optional) remote job sending the command.
   *
   * @return the response; usually a BooleanResponse that is true if successful; false if failed.
   */
  public final Response handleJobCommand(Context context, JobCommand jobCommand, LocalJobId localJobId, Publishable payload,
                                         LocalJobId remoteJobId) {
    Response result = null;
    boolean booleanResult = false;

    final Iterator<Job> jobIter = (localJobId == null) ? id2job.values().iterator() : null;
    Job job = (localJobId != null) ? getJob(localJobId) : jobIter.hasNext() ? jobIter.next() : null;

    if (job == null) {
      System.err.println(new Date() + ": NOTE: JobManager.handleJobCommand got 'null' job! (" + jobCommand +
                         ") localJobId=" + localJobId +
                         " id2job=" + id2job +
                         " payload=" + payload +
                         " remoteJobId=" + remoteJobId);

      return null;
    }

    int loopCount = 0;

    while (job != null) {
      booleanResult = true;

      switch (jobCommand) {

        case OPERATE :
          booleanResult = false;
          final JobStatus status = job.getStatus();
          if (status != JobStatus.RUNNING) {
            result = WorkResponse.getDoneResponse((ClusterContext)context);

//             System.out.println(new Date() + ": ***WARNING: Rejecting work request '" + payload + "' because job '" +
//                                job.getDescription() + "' is not running! (jobStatus=" + status + ")");
          }
          else {
            //System.err.println("calling operate loopCount=" + loopCount + " jobCommand=" + jobCommand + " localJobId=" + localJobId + " payload=" + payload + " remoteJobId=" + remoteJobId);

            result = job.operate(payload);  // note: was new StringResponse(context, stringResult);
          }
          break;

        case PAUSE :
          job.pause();
          break;
        case RESUME :
          // invoke in a new jobHandler thread
          jobThreadPool.execute(new ResumeJobHandler(job, localJobId.getId()));
          break;
        case FLUSH :
          booleanResult = job.flush(payload);
          break;
        case BOUNCE :  // stop, re-initialize, and restart the job
//           synchronized (id2job) {
//             id2job.remove(localJobId);
//           }
          // end the existing jobHandler thread
          job.setStatus(JobStatus.INTERRUPTED);

          // wait (limited) for job to finish
          final Timer timer = new Timer(5000, new Date()); //todo: parameterize wait time
          while (job.getStatus() == JobStatus.INTERRUPTED && !timer.reachedTimerMillis()) {}

          if (job.getStatus() != JobStatus.INTERRUPTED) {
            // and start a new one
            jobThreadPool.execute(new NewJobHandler(job, localJobId.getId(), true));
            booleanResult = true;
          }
          else booleanResult = false;

          break;
        case INTERRUPT :
          job.setStatus(JobStatus.INTERRUPTED);
          break;

        case STATUS :
          result = new StringResponse(context, job.getStatus().name());
          break;
        case PROBE :
          final JobProbeResponse jpr = (result == null) ? new JobProbeResponse(context) : (JobProbeResponse)result;
          jpr.add(new JobProbeData(job));
          if (result == null) result = jpr;
          break;
        case DETAIL :
          final String stringResult = job.dumpDetails();
          result = new StringResponse(context, stringResult);  // send back the details.
          System.out.println(stringResult);                    // and log it locally to boot.
          break;

        case SPLIT :
          //pause local job, split work and send portion to remote job, bounce local job, respond appropriately.
          job.pause();
//todo: I'm here...
          break;

//todo: implement handling other commands...
//        case PURGE : interrupt; wait; delete; break;
      }

      if (jobIter != null && jobIter.hasNext()) {
        job = jobIter.next();
      }
      else {
        job = null;
      }

      ++loopCount;
    }

    if (!booleanResult && result == null) {
      // log problem
      System.err.println(new Date() + ": ERROR: can't handle jobCommand (" + jobCommand +
                         ") localJobId=" + localJobId +
                         " id2job=" + id2job +
                         " payload=" + payload +
                         " remoteJobId=" + remoteJobId);
    }

    /**
     * Create a boolean response if a response has not be created.
     */
    if (result == null) {
      result = new BooleanResponse(context, booleanResult);
    }

    return result;
  }

  public Console getConsole() {
    synchronized (consoleMutex) {
      if (_console == null) {
        this._console = new Console(clusterDefinition, "JobManager-Console");
      }
    }
    return _console;
  }

  private abstract class JobHandler implements Runnable {

    public final Job job;
    public final int localJobId;

    public JobHandler(Job job, int localJobId) {
      this.job = job;
      this.localJobId = localJobId;
    }

    protected abstract void handle();

    public void run() {
      handle();

      JobStatus jobStatus = job.getStatus();
      while (jobStatus != JobStatus.FINISHED && jobStatus != JobStatus.INTERRUPTED && jobStatus != JobStatus.PAUSED) {
        try {
          Thread.sleep(100);
        }
        catch (InterruptedException e) {
          //ignore
        }
        jobStatus = job.getStatus();
      }

      if (jobStatus == JobStatus.FINISHED) {
        // take job off of the active list...
//         synchronized (id2job) {
//           id2job.remove(localJobId);
//         }

        // ...and put it on the 'old' list for reporting.
//         synchronized (oldJobs) {
//           oldJobs.add(job);
//         }
      }
    }
  }

  private final class NewJobHandler extends JobHandler {
    private boolean start;

    public NewJobHandler(Job job, int localJobId, boolean start) {
      super(job, localJobId);
      this.start = start;
    }

    protected void handle() {
      job.initialize(start);
    }
  }

  private final class ResumeJobHandler extends JobHandler {

    public ResumeJobHandler(Job job, int localJobId) {
      super(job, localJobId);
    }

    protected void handle() {
      job.resume();
    }
  }




  /**
   * Send a job to a running cluster.
   */
  public static final void main(String[] args) throws IOException, ClusterException {
    //
    // properties: jobId, groupName, dataDirName, [numThreads], [user], [defName], [machines],
    //
    // properties
    //
    // job -- (required) classpath for job to run (using properties constructor)
    // sendTimeout -- (optional, default=5000) timeout for sending job.
    //

    Job job = null;
    Console console = null;

    final PropertiesParser pp = new PropertiesParser(args, true);
    final Properties properties = pp.getProperties();

    final String jobClass = properties.getProperty("job");
    if (jobClass == null) {
      throw new IllegalArgumentException("Must define 'job' class to run!");
    }
    final int sendTimeout = Integer.parseInt(properties.getProperty("sendTimeout", "5000"));

    // use properties
    console = new ClusterRunner(true/*useActiveCluster*/, properties).getConsole();
    job = (Job)ReflectUtil.buildInstance(jobClass, properties);


    if (console != null) {
      console.showResponses(System.out, console.sendJob(job, sendTimeout));
      console.shutdown();
    }
  }
}
