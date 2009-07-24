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
package org.sd.cluster.config;


import org.sd.cluster.io.NodeClient;
import org.sd.cluster.io.NodeServer;
import org.sd.cluster.io.SafeDepositBox;
import org.sd.cluster.job.JobManager;
import org.sd.cluster.util.LogManager;
import org.sd.util.ExecUtil;
import org.sd.util.PropertiesParser;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Class to encapsulate a cluster node.
 * <p>
 * @author Spence Koehler
 */
public class ClusterNode implements ClusterContext {

  private int jvmNumHint;
  private ClusterDefinition clusterDef;
  private Config config;
  private JobManager jobManager;
  private SafeDepositBox safeDepositBox;
  private NodeServer listener;
  private NodeClient client;
  private MBeanServer mbs;


  // this node's logs for monitoring purposes.
  private LogManager.LogInfo errorLog;
  private LogManager.LogInfo outputLog;

  private ClusterLogVisitor logVisitor;
  // Note that instead of going through a central logging system, messages
  // are sent to stdout and stderr. These handles are obtained so that the
  // logs can be monitored and appropriate alerts given. It is recommended
  // that we eventually migrate to a central logging system and only use
  // these handles until then.


  private final AtomicBoolean stayAlive = new AtomicBoolean(true);

  /**
   * Only to be created by this class's main. One per jvm (except for testing).
   */
  private ClusterNode(ClusterDefinition clusterDef, int jvmNumHint) throws UnknownHostException, IOException {
    this.jvmNumHint = jvmNumHint;
    this.clusterDef = clusterDef;

    if (!this.clusterDef.isValid()) {
      throw new IllegalStateException("Invalid cluster definition '" + clusterDef.getDefinitionName() + "'!");
    }

    this.config = new Config(jvmNumHint, null);

    final int numNodes = clusterDef.getNumNodes();

    // start enough threads to listen from and send connections to either all
    // of the nodes or 50 (arbitrary, but reflective of limits we'd like to put
    // on a single jvm's total thread count) at a time; whichever is more.
//    final int numThreads = Math.max(50, numNodes);
    final int numThreads = Math.min(50, numNodes);

    //todo: tune the thread parameters.
    init(config, numThreads, numThreads, numThreads, null);
  }

  private static final ClusterDefinition createClusterDefinition(String clusterName, String gateway, String[] machines) throws IOException {
    ClusterDefinition result = null;

    if (gateway == null && machines == null) {
      result = new ClusterDefinition(ExecUtil.getUser(), clusterName);
    }
    else {
      result = new ClusterDefinition(ExecUtil.getUser(), clusterName, gateway, machines);
    }

    return result;
  }

  private final void init(Config config, int numberOfParents, int numMessageHandlerThreads, int numberOfChildren, String identifier) throws UnknownHostException, IOException {
    final InetAddress localhost = InetAddress.getLocalHost();

    this.jobManager = new JobManager(config, clusterDef, identifier);

    this.listener = new NodeServer(this,
                                   new InetSocketAddress(localhost,
                                                         config.getServerPort()),
                                   numberOfParents,
                                   numMessageHandlerThreads);
    final String clientName = (identifier == null) ? config.getName() : config.getName() + "-" + identifier;
    this.client = new NodeClient(clientName, localhost, numberOfChildren);
    this.mbs = null;
  }

  private final void initLogHandles(int jvmNumHint) {
    if (jvmNumHint >= 99) return;

    this.errorLog = null;
    this.outputLog = null;
    this.logVisitor = null;

    final LogManager logManager = new LogManager();
    final LogManager.LogInfo[] logs = logManager.getLatestLogs();
    for (LogManager.LogInfo log : logs) {

      if (log.nodeNum == jvmNumHint) {
        if (errorLog == null && log.isErrLog) {
          this.errorLog = log;
          System.out.println(new Date() + ": Found errLog : " + log.logFile);
        }
        if (outputLog == null && !log.isErrLog) {
          this.outputLog = log;
          System.out.println(new Date() + ": Found outLog : " + log.logFile);
        }
      }
      if (errorLog != null && outputLog != null) break;
    }

    if (errorLog == null) {
      System.err.println(new Date() + ": ERROR : ClusterNode Can't find error log!");
    }
    if (outputLog == null) {
      System.err.println(new Date() + ": ERROR : ClusterNode Can't find output log!");
    }

    this.logVisitor = new ClusterLogVisitor(config, clusterDef, jobManager == null ? null : jobManager.getConsole());
  }

  /**
   * For JUnit testing only.
   */
  ClusterNode(ClusterDefinition clusterDefinition, Config config, int threadCount, String identifier) throws UnknownHostException, IOException {
    this.clusterDef = clusterDefinition;
    this.config = config;
    this.jvmNumHint = 99;
    init(config, threadCount, threadCount, threadCount, identifier);
  }

  public String getName() {
    return config.getName();
  }

  public Config getConfig() {
    return config;
  }

  public ClusterDefinition getClusterDefinition() {
    return clusterDef;
  }

  public JobManager getJobManager() {
    return jobManager;
  }

  public SafeDepositBox getSafeDepositBox() {
    // lazily create and register the safe deposit box.
    if (safeDepositBox == null) {
//todo: use config params from environment instead of defaults.
      this.safeDepositBox = new SafeDepositBox();
      jobManager.registerShutdownable(safeDepositBox);

      // Register MXBean
      if (mbs != null) {
        try {
          final ObjectName sdbName = new ObjectName("org.sd.cluster.io:type=SafeDepositBox");
          mbs.registerMBean(safeDepositBox, sdbName);
        }
        catch (Exception e) {
          System.err.println("*** WARNING: Unable to register SafeDepositBoxMXBean!");
          e.printStackTrace(System.err);
        }
      }
    }

    return safeDepositBox;
  }

  public NodeClient getNodeClient() {
    return client;
  }

  public void setMBeanServer(MBeanServer mbs) {
    this.mbs = mbs;
  }

  public void start() {
    this.listener.start();
    this.client.start();


    // initialize handles to logs (needs JobManager to have been initialized)
    try {
      initLogHandles(jvmNumHint);
    }
    catch (Error e) {
      System.err.println(new Date() + ": WARNING: Unable to initLogHandles! " + e.toString());
      e.printStackTrace(System.err);
    }
  }

  public void listenForMessages() {
    start();

    while (stayAlive.get()) {
      try {
        while (listener.isUp() && stayAlive.get()) {
          Thread.sleep(100);

          // visit the logs (on an appropriate schedule)
          visitLogs();
        }
      }
      catch (InterruptedException e) {
        System.out.println(new Date() + ": WARNING: ClusterNode.listenForMessages() interrupted!");
        e.printStackTrace(System.out);
      }
    }

    shutdown(false);
  }

  private final void visitLogs() {
    if (logVisitor != null) {
      boolean actionableEvent = false;

      if (errorLog != null) {
        //todo: determine whether it is time to visit yet or not
        try {
          actionableEvent |= errorLog.visit(logVisitor);
        }
        catch (IOException e) {
          logVisitor.disable();
          System.err.println(new Date() + ": ERROR : ClusterNode unable to visit error log!");
          e.printStackTrace(System.err);
        }
      }
      if (outputLog != null) {
        //todo: determine whether it is time to visit yet or not
        try {
          actionableEvent |= outputLog.visit(logVisitor);
        }
        catch (IOException e) {
          logVisitor.disable();
          System.err.println(new Date() + ": ERROR : ClusterNode unable to visit output log!");
          e.printStackTrace(System.err);
        }
      }

      if (actionableEvent) {
        logVisitor.sendReport();
      }
    }
  }

  public boolean shutdown(boolean now) {
    if (stayAlive.compareAndSet(true, false)) {
      client.shutdown(now);
      listener.shutdown(now);
      jobManager.shutdown(now);
    }
    return true;
  }

  // arg1: jvmNum hint
  // arg2: (optional) name of cluster def in resources/cluster/<clusterName>.def
  public static void main(String[] args) throws UnknownHostException, IOException {
    // properties:
    //   reportInterval [unused] -- (optional) freq in millis to report on server stats
    //   clusterHome -- (optional) override for location of $HOME/cluster
    //   portRange -- (optional, values >= 10100) override for "lowPort:highPort"
    //   single -- (optional, default="false", values={"true", "false"}) If "true"
    //             then a single-node cluster definition is generated on the fly)
    //   heapSize -- (optional) recommended size (in M) for java heap.

    // ensure out/err logs are initialized
    System.out.print("");
    System.err.print("");

    final PropertiesParser pp = new PropertiesParser(args);
    final Properties properties = pp.getProperties();
    args = pp.getArgs();

    //
    // get and use properties
    //

    // clusterHome
    final String clusterHome = properties.getProperty("clusterHome");
    if (clusterHome != null) {
      ConfigUtil.setClusterRootDir(clusterHome);
      System.out.println("ClusterNode: clusterHome=" + clusterHome);
    }

    // portRangeString
    final String portRangeString = properties.getProperty("portRange");
    if (portRangeString != null) {
      ConfigUtil.setPortOverride(portRangeString);
    }
    else {
      // check for active override
      final int[] activePortOverride = Admin.getActivePortOverride();
      if (activePortOverride != null) {
        ConfigUtil.setPortOverride(activePortOverride[0], activePortOverride[1]);
        System.out.println("ClusterNode: Applying (active) port override: " + activePortOverride[0] + ":" + activePortOverride[1]);
      }
    }

    // single
    final boolean single = "true".equalsIgnoreCase(properties.getProperty("single", "false"));

    // heapSize
    final String heapSize = properties.getProperty("heapSize");

    // create cluster definition
    ClusterDefinition clusterDef = null;

    if (!single) {
      final String clusterName = (args.length == 1) ? Admin.getActiveClusterName() : args[1];
      final String gateway = Admin.getActiveGateway();
      final String[] machines = Admin.getActiveMachines();
      clusterDef = createClusterDefinition(clusterName, gateway, machines);
    }
    else {
      final ConfigGenerator configGenerator = new ConfigGenerator(new String[] {
          ".", "server:node1",
        });
      clusterDef = configGenerator.buildClusterDefinition("localhost", ExecUtil.getUser(), new String[]{ExecUtil.getMachineName()});

      Admin.configure(ConfigUtil.getClusterPath("conf/"), null, clusterDef);
    }

    // create cluster node instance
    final int jvmNum = Integer.parseInt(args[0]);
    final ClusterNode clusterNode = new ClusterNode(clusterDef, jvmNum);

    final Config config = clusterNode.getConfig();

    // start the cluster node
    try {
      System.out.println("Starting " + config.getName() + "...");
      System.out.println("..." + config.getName() + " is listening on port " + config.getServerPort());

      // Register MXBeans if possible
      MBeanServer mbs = null;
      try {
        mbs = ManagementFactory.getPlatformMBeanServer();
      }
      catch (Exception e) {
        System.err.println("*** WARNING: Unable to getPlatformMBeanServer!");
        e.printStackTrace(System.err);
        mbs = null;
      }
      if (mbs != null) {
        final ObjectName nodeServerName = new ObjectName("org.sd.cluster.io:type=NodeServer");
        mbs.registerMBean(clusterNode.listener, nodeServerName);
        clusterNode.setMBeanServer(mbs);
      }

      clusterNode.listenForMessages();
    }
    catch (Throwable t) {
      System.out.println(new Date() + ": ClusterNode.main(" + jvmNum + ") dying with throwable:");
      t.printStackTrace(System.out);
    }
    finally {
      clusterNode.shutdown(false);
      System.out.println("shutting down " + config.getName() + " at " + new Date());
    }
  }
}
