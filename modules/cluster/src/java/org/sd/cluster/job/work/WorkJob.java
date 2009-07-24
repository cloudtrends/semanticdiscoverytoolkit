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


import org.sd.cio.MessageHelper;
import org.sd.cluster.config.ClusterContext;
import org.sd.cluster.config.ClusterException;
import org.sd.cluster.config.ClusterRunner;
import org.sd.cluster.config.Console;
import org.sd.cluster.io.Context;
import org.sd.cluster.io.Response;
import org.sd.cluster.job.AbstractJob;
import org.sd.cluster.job.JobStatus;
import org.sd.cluster.job.LocalJobId;
import org.sd.io.Publishable;
import org.sd.util.MathUtil;
import org.sd.util.PropertiesParser;
import org.sd.util.ReflectUtil;
import org.sd.util.Timer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A job for serving and/or requesting and doing work.
 * <p>
 * @author Spence Koehler
 */
public class WorkJob extends AbstractJob {
  
  private Properties properties;

  private transient Worker worker;
  private transient QueueDesignator queueDesignator;
  private transient WorkClient workClient;
  private transient WorkServer workServer;
  private transient WorkPool workPool;
  private transient AtomicBoolean pause = new AtomicBoolean(false);
  private transient JobStatus clientStatus;
  private transient JobStatus serverStatus;
  private transient Date startDate;
  private transient String workJobId;

  public WorkJob() {
    super();
  }

  /**
   * Construct with properties.
   * <p>
   * At this level, the properties are:
   * <ul>
   * <li>worker -- classpath for a Worker implementation with a properties constructor.</li>
   * <li>workClient -- classpath for a WorkClient implementation with a properties constructor.</li>
   * <li>workServer -- classpath for a WorkServer implementation with a properties constructor.</li>
   * <li>maxPoolQueueSize -- [optional, default=1] max queue size in work pool.</li>
   * <li>maxWorkPoolErrors -- [optional, default=100] max number of errors allowed in work pool before shutdown.</li>
   * <li>maxSecondsPerUnit -- [optional, default=600] timelimit for working on a unit before killing.</li>
   * <li>workJobId -- [optional, default=WorkJob] identifier for the work job</li>
   * </ul>
   * Must have a (workServer) and/or a (workClient and worker).
   */
  public WorkJob(Properties properties) {
    super(properties);

    this.properties = properties;
    this.workJobId = properties.getProperty("workJobId");

    init();

    if (workServer == null && workClient == null && worker == null) {
      throw new IllegalStateException(new Date() + " : ERROR '" + getDescription() +
                                      "' needs 'workServer' and/or 'workClient' and/or 'worker'");
    }
  }

  private final void init() {
    initWorker();
    initQueueDesignator();
    initWorkClient();
    initWorkServer();
  }

  private final void initWorker() {
    final String workerClass = properties.getProperty("worker");

    if (workerClass != null) {
      this.worker = (Worker)ReflectUtil.buildInstance(workerClass, properties);
    }
    else {
      System.out.println(new Date() + " : NOTE '" + getDescription() + "' has no 'worker'!");
      this.worker = null;
    }
  }

  private final void initQueueDesignator() {
    final String queueDesignatorClass = properties.getProperty("queueDesignator");

    if (queueDesignatorClass != null) {
      this.queueDesignator = (QueueDesignator)ReflectUtil.buildInstance(queueDesignatorClass, properties);
    }
    else {
      System.out.println(new Date() + " : NOTE '" + getDescription() + "' has no 'queueDesignator'!");
      this.queueDesignator = null;
    }
  }

  private final void initWorkClient() {
    final String workClientClass = properties.getProperty("workClient");

    if (workClientClass != null) {
      this.workClient = (WorkClient)ReflectUtil.buildInstance(workClientClass, properties);
      workClient.setWorkJob(this);
    }
    else {
      System.out.println(new Date() + " : NOTE '" + getDescription() + "' has no 'workClient'!");
      this.workClient = null;
    }
  }

  private final void initWorkServer() {
    final String workServerClass = properties.getProperty("workServer");

    if (workServerClass != null) {
      this.workServer = (WorkServer)ReflectUtil.buildInstance(workServerClass, properties);
    }
    else {
      System.out.println(new Date() + " : NOTE '" + getDescription() + "' has no 'workServer'!");
      this.workServer = null;
    }
  }

  private final void initWorkPool(ClusterContext clusterContext, String jobIdString, String dataDirName) {
    if (worker != null && workClient != null) {
      final int maxPoolQueueSize = Integer.parseInt(properties.getProperty("maxPoolQueueSize", "1"));
      final int maxWorkPoolErrors = Integer.parseInt(properties.getProperty("maxWorkPoolErrors", "100"));
      final int maxSecondsPerUnit = Integer.parseInt(properties.getProperty("maxSecondsPerUnit", "600"));  // default=10 minutes

      this.workPool = new WorkPool(clusterContext, jobIdString, dataDirName,
                                   workClient, worker, workServer, queueDesignator,
                                   getJobId(), getNumThreads(),
                                   maxPoolQueueSize, maxWorkPoolErrors, maxSecondsPerUnit);
    }
    else {
      System.out.println(new Date() + " : NOTE '" + getDescription() + "' has no WorkPool (due to absence of 'worker' and 'workClient')!");
      this.workPool = null;
    }
  }

  public String getDescription() {
    return getName() + "-" + getJobId();
  }

  protected final String getName() {
    if (workJobId == null) {
      final String[] namePieces = this.getClass().getName().split("\\.");
      this.workJobId = namePieces[namePieces.length - 1];
    }
    return workJobId;
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);

    MessageHelper.writeProperties(dataOutput, properties);
  }

  /**
   * Read this message's contents from the dataInput stream that was written by
   * this.write(dataOutput).
   * <p>
   * NOTE: this requires all implementing classes to have a default constructor
   *       with no args.
   *
   * @param dataInput  the data output to write to.
   */
  public void read(DataInput dataInput) throws IOException {
    super.read(dataInput);

    this.properties = MessageHelper.readProperties(dataInput);
    this.workJobId = properties.getProperty("workJobId");
  }

  public boolean isServingWork() {
    return getStatus() == JobStatus.RUNNING && this.serverStatus == JobStatus.RUNNING;
  }

  public boolean isAcceptingWork() {
    return getStatus() == JobStatus.RUNNING && this.clientStatus == JobStatus.RUNNING;
  }

  public Response operate(Publishable request) {
    if (!isServingWork()) return WorkResponse.getDownResponse(getClusterContext());

    Response result = null;

    if (request instanceof WorkRequest) {
      // requesting work from work server
      if (workServer != null) {
        //todo: do we need multiple work server threads?
        result = workServer.processRequest((WorkRequest)request);
      }

      if (result == null) {
        // log request failure.
        System.err.println(new Date() + ": WARNING '" + getDescription() + "' unable to process request '" + request.toString() + "'.");
      }
    }
    else {
      // command to worker or work client
    }

    return result;
  }
  
  /**
   * Called by the handle method only if 'canHandle' returns true before
   * registering this job with the job manager. If this hook returns false,
   * this job will NOT be registered with the job manager and the postHandleHook
   * will not be called.
   */
  protected final boolean preHandleHook(Context context) {
    if (!super.preHandleHook(context)) return false;

    // initialize worker, workClient, and workServer.
    init();

    // build work pool
    initWorkPool(getClusterContext(), getName(), getJobId());

    // initialize starting time.
    this.startDate = new Date();

    // perform workserver initializations
    boolean result = (workServer != null) ? workServer.initialize(getClusterContext(), getName(), getJobId()) : true;

    // perform workclient initializations
    if (result && workClient != null) result = workClient.initialize();

    // perform worker initializations
    if (result && worker != null) result = worker.initialize(getClusterContext(), getName(), getJobId());

    return result;
  }  

  public void start() {  // start processing job's work.
    this.serverStatus = JobStatus.RUNNING;
    this.clientStatus = JobStatus.RUNNING;
    setStatus(JobStatus.RUNNING);

    if (workPool != null) {
//      int numRetries = 100;

      final Timer workTimer = new Timer(600000);  // report every 10 minutes
      while (getStatus() == JobStatus.RUNNING && clientStatus == JobStatus.RUNNING && !pause.get()) {
        if (!workPool.addWork(pause)) {
          if (workServer != null) {
            final Long sleepTime = workServer.shouldKeepRunning();

            if (sleepTime == null) {
              break;
            }
            else {
              try {
                if (workTimer.reachedTimerMillis()) {
                  System.out.println(new Date() + " : " + getDescription() +
                                     " : workServer reported 'shouldKeepRunning' (" +
                                     sleepTime + ") after workPool failed to addWork.");
                }
                Thread.sleep(sleepTime);
              }
              catch (InterruptedException e) {
                break;  // time to exit this loop.
              }
            }
          }
          else {
            break;
          }
        }
        else {
          workTimer.setTime();  // reset this timer whenever we successfully added work.
        }
      }

      // wait for work to finish
      if (getStatus() == JobStatus.RUNNING && clientStatus == JobStatus.RUNNING) {
        System.out.println(new Date() + " : " + getDescription() +
                           " : done adding work to workPool. marking job as finished. waiting for workPool to finish. (" +
                           workPool.getPool().getNumQueued() + " queued.)");
        clientStatus = JobStatus.FINISHED;

        workPool.waitUntilFinished(5000L);  //todo: parameterize wait interval?

        System.out.println(new Date() + " : " + getDescription() +
                           " : workPool is finished!");
      }

      // notify workserver not to expect/wait for more work when its done.
      if (workServer != null) {
        workServer.setNoMoreWork(true);

        System.out.println(new Date() + " : " + getDescription() + " : waiting for workServer to finish.");

        // wait for workserver to finish
        final Timer logTimer = new Timer(600000);  // report every 10 minutes
        while (getStatus() == JobStatus.RUNNING && serverStatus == JobStatus.RUNNING) {
          final KeyedWork nextWork = workServer.peek(null);
          if (nextWork == null) {
            System.out.println(new Date() + " : " + getDescription() + " : workServer is finished!");
            break;
          }
          else {
            if (logTimer.reachedTimerMillis()) {
              System.out.println(new Date() + " : " + getDescription() + " : workServer not finished yet! (next=" + nextWork + ")");
            }
            try {
              Thread.sleep(500);
            }
            catch (InterruptedException e) {
              break;
            }
          }
        }
        serverStatus = JobStatus.FINISHED;
      }

      // mark (super) status as done (run's doneHandlingHook)
      setStatus(JobStatus.FINISHED);
    }
  }

  public void stop() {  // end processing job in this jvm; could still suspend? can't resume.
    setStatus(JobStatus.INTERRUPTED);
  }

  public void pause() {  // pause a job in this jvm to be resumed.
    pause.set(true);
    setStatus(JobStatus.PAUSED);
  }

  public void resume() {  // resume paused job.
    pause.set(false);
    start();
  }

  public boolean flush(Publishable payload) {  // flush the worker.
    boolean result = true;

    if (worker != null) {
      result = worker.flush(payload);
    }

    return result;
  }

  public void suspend() {  // suspend to disk for restoration through JobManager in another jvm
    setStatus(JobStatus.PAUSED);
  }

  public synchronized void shutdown(boolean now) {  // called when cluster is being shutdown
    if (workServer != null) {
      System.out.println(new Date() + ": WorkJob '" + workJobId + "' closing workServer.");
      workServer.close();
    }
    if (workClient != null) {
      System.out.println(new Date() + ": WorkJob '" + workJobId + "' closing workClient.");
      workClient.close();
    }
    if (worker != null) {
      System.out.println(new Date() + ": WorkJob '" + workJobId + "' closing worker.");
      worker.close();
    }
    if (workPool != null) {
      System.out.println(new Date() + ": WorkJob '" + workJobId + "' closing workPool (now=" + now + ").");
      workPool.shutdown(now);
    }
    

    this.clientStatus = JobStatus.FINISHED;
    this.serverStatus = JobStatus.FINISHED;
    setStatus(JobStatus.FINISHED);
  }
  
  public String toString() {
    final StringBuilder result = new StringBuilder();
    result.append(getDescription());

    if (startDate != null) {
      result.append("  started:").append(dateFormat.format(startDate));

      final long upTime = new Date().getTime() - startDate.getTime();
      result.append("  upTime:").append(MathUtil.timeString(upTime, true));
      
      if (workPool != null) {
        result.append("  ").append(workPool.toString());
      }
      if (workServer != null) {
        result.
          append("\n  serverStatus=").append(serverStatus).
          append("  numServedUnits:").append(workServer.getNumServedUnits()).
          append(" ").append(workServer.toString());
      }
    }

    return result.toString();
  }

  /**
   * Run a work job defined by the properties.
   */
  public static final void run(String[] args) throws IOException, ClusterException {
    final PropertiesParser pp = new PropertiesParser(args, true);
    final Console console = new ClusterRunner(true/*useActiveCluster*/, pp.getProperties()).getConsole();
    run(pp.getProperties(), console);
    console.shutdown();
  }

  /**
   * Run a work job defined by the properties.
   */
  public static final void run(Properties properties, Console console) throws ClusterException {
    if (console == null) return;

    final WorkJob workJob = new WorkJob(properties);

    //
    // start the work job on a node(s)
    //

    final String groupName = properties.getProperty("groupName");

    console.showResponses(System.out, console.sendJob(workJob, 5000));

    // ensure job(s) started by waiting for local job id to register
    final List<String> serverNodeNames = console.getClusterDefinition().getGroupNodeNames(groupName, true);
    if (serverNodeNames == null) {  // just one node. get the local job id.
      final String localJobId = getLocalJobId(console, groupName, workJob);
    }
    else {  // multiple nodes. need to get multiple local job ids.
      for (String serverNodeName : serverNodeNames) {
        final String localJobId = getLocalJobId(console, serverNodeName, workJob);
      }
    }
  }

  private static final String getLocalJobId(Console console, String serverNodeName, WorkJob workJob) throws ClusterException {
    String result = null;
    LocalJobId localJobId = null;

    for (int retryCount = 500; retryCount > 0; --retryCount) {
      localJobId = console.getLocalJobId(serverNodeName, workJob, 5000);

      if (localJobId.getId() < 0) {
        System.err.println(serverNodeName + ": Couldn't get localJobId. Waiting (" + (retryCount - 1) + " more retries.)");

        try {
          Thread.sleep(500);
        }
        catch (InterruptedException e) {
          //do nothing.
        }
      }
      else {
        break;
      }
    }

    if (localJobId != null) {
      result = localJobId.toString();
    }
    System.out.println("*** " + workJob.getDescription() + " localJobId = " + localJobId);
    return result;
  }

  public static final void main(String[] args) throws IOException, ClusterException {
    run(args);
  }
}
