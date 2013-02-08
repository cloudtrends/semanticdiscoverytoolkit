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


import org.sd.cluster.job.Job;
import org.sd.cluster.job.JobManager;
import org.sd.io.FileUtil;
import org.sd.util.ExecUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A class to create a "cluster" in a single JVM for testing purposes.
 * <p>
 * Interaction:
 * <code>
 *   final VirtualCluster fooCluster = new VirtualCluster("foo", "5m7n.1-2-4");
 *   try {
 *     fooCluster.start();
 *     final Console console = fooCluster.getConsole();
 *     //...send/receive messages/jobs through the console
 *   }
 *   finally {
 *     fooCluster.shutdown();
 *   }
 * </code>
 *
 * @author Spence Koehler
 */
public class VirtualCluster {
  
  private final String clusterName;
  private final String userName;
  private final SubstituteClusterDefinition clusterDefinition;
  private final String identifier;
  private final List<String> nodeNames;
  private final int numNodes;
  private final Map<Integer, Integer> num2port;
  private final Map<Integer, String> num2dir;
  private final Map<Integer, ClusterNode> num2node;
  private final ExecutorService threadPool;
  private final transient AtomicInteger nodeThreadId = new AtomicInteger(0);

  private Console console;

  /**
   * Construct a new virtual cluster.
   *
   * @param clusterName  a name to identify this cluster's threads and data space.
   * @param defName      the name of a cluster definition to use.
   */
  public VirtualCluster(final String clusterName, final String defName, final String identifier) throws UnknownHostException, IOException {
    this.clusterName = clusterName;
    this.userName = "junit";
    this.clusterDefinition = new SubstituteClusterDefinition(userName, defName);
    this.identifier = identifier;
    this.nodeNames = clusterDefinition.getNodeNames();
    this.numNodes = nodeNames.size();
    this.console = null;  // don't create console until we start the cluster
    this.num2port = new HashMap<Integer, Integer>();
    this.num2dir = new HashMap<Integer, String>();
    this.num2node = new HashMap<Integer, ClusterNode>();
    initializeMaps(nodeNames, clusterDefinition, identifier);

    this.threadPool =
      Executors.newFixedThreadPool(
        numNodes,
        new ThreadFactory() {
          public Thread newThread(Runnable r) {
            return new Thread(r, clusterName + "-VirtualCluster-" + nodeThreadId.getAndIncrement());
          }
        });
  }

  public ClusterDefinition getClusterDefinition() {
    return clusterDefinition;
  }

  /**
   * Start (listening for messages in) the virtual cluster.
   */
  public void start() {
    this.console = new Console(clusterDefinition, "VirtualCluster-" + identifier);

    // start each cluster node on a thread.
    for (int i = 0; i < numNodes; ++i) {
      final ClusterNode node = num2node.get(i);

      final int nodeNum = i;
      threadPool.execute(new Runnable() {
          public void run() {
            node.listenForMessages();
          }
        });
    }
  }

  /**
   * Shutdown this virtual cluster.
   */
  public void shutdown() {
    // shutdown cluster nodes and threads.
    for (ClusterNode clusterNode : num2node.values()) {
      clusterNode.shutdown(false);
    }
    threadPool.shutdown();
    if (console != null) console.shutdown();

    // leave jvmRootDirs undeleted until we run again for manual review
  }

  /**
   * Get this virtual cluster's console for sending messages.
   */
  public Console getConsole() {
    return console;
  }

  public Collection<ClusterNode> getNodes() {
    return num2node.values();
  }

  public ClusterNode getNode(int num) {
    return num2node.get(num);
  }

  public List<Job> getRunningJobs() {
    final List<Job> result = new ArrayList<Job>();

    for (ClusterNode node : num2node.values()) {
      final JobManager jobManager = node.getJobManager();
      final Collection<Integer> jobIds = jobManager.getActiveJobIds(null);
      for (Integer jobId : jobIds) {
        final Job job = jobManager.getActiveJob(jobId);
        if (job != null) result.add(job);
      }
    }

    return result;
  }

  private final String buildJvmRootDir(int nodeNum) {
    final StringBuilder result = new StringBuilder();
    result.append("/tmp/test/").
      append(ExecUtil.getUser()).
      append("/cluster/").
      append(clusterName).
      append("jvm-").append(nodeNum).append('/');

    return result.toString();
  }

  private final void deleteJvmRootDir(String path) {
    final File dir = new File(path);
    FileUtil.deleteDir(dir);
  }

  private final void initializeMaps(List<String> nodeNames, SubstituteClusterDefinition clusterDefinition, String identifier) throws UnknownHostException, IOException {
    final Map<String, Integer> name2count = new HashMap<String, Integer>();
    int index = 0;
    for (String nodeName : nodeNames) {
      final int port = PortServer.getInstance().getNextTestPort();
      num2port.put(index, port);

      final String jvmRootDir = buildJvmRootDir(index);
      num2dir.put(index, jvmRootDir);

      // clean up old jvmRootDir if necessary
      if (new File(jvmRootDir).exists()) deleteJvmRootDir(jvmRootDir);

      final Config config = new Config("localhost", "junit", index, port, jvmRootDir);
      final ClusterNode clusterNode = new ClusterNode(clusterDefinition, config, 5, identifier);
      num2node.put(index, clusterNode);

      // map cluster-def name to config name for group name substitutions in cluster def
      Integer count = name2count.get(nodeName);
      if (count == null) count = 0;
      name2count.put(nodeName, count + 1);
      final String defName = nodeName + "-" + count;
      clusterDefinition.mapName(defName, "localhost-" + index);

      ++index;
    }
  }

  private final class SubstituteClusterDefinition extends ClusterDefinition {
    private final Map<String, String> def2conf;
    private final Map<String, String> conf2def;

    SubstituteClusterDefinition(String user, String defName) throws IOException {
      super(user, defName);
      this.def2conf = new HashMap<String, String>();
      this.conf2def = new HashMap<String, String>();
    }

    InetSocketAddress getInetSocketAddress(String hostname, int jvmNum) {
      final String configName = def2conf.get(hostname + "-" + jvmNum);
      final String[] pieces = configName.split("-");
      final int newJvmNum = Integer.parseInt(pieces[1]);
      return new InetSocketAddress("localhost", num2port.get(newJvmNum));
    }

    public void mapName(String defName, String confName) {
      def2conf.put(defName, confName);
      conf2def.put(confName, defName);
    }

    public List<String> getGroupNodeNames(String groupName, boolean fix) {
      final List<String> nodeNames = super.getGroupNodeNames(groupName, fix);
      final List<String> result = new ArrayList<String>();

      for (String nodeName : nodeNames) {
        result.add(def2conf.get(nodeName));
      }

      return result;
    }

    public InetSocketAddress getServerAddress(String nodeWithNum) {
      final String def = conf2def.get(nodeWithNum);
      return super.getServerAddress(def == null ? nodeWithNum : def);
    }

    public String getJvmBasePath(int jvmNum) {
      return buildJvmRootDir(jvmNum);
    }
  }
}
