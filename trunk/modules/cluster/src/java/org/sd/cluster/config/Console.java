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


import org.sd.cluster.io.Message;
import org.sd.cluster.io.NodeClient;
import org.sd.cluster.io.Response;
import org.sd.cluster.io.SafeDepositBox;
import org.sd.cluster.config.ClusterDefinition;
import org.sd.cluster.job.Job;
import org.sd.cluster.job.AcquireJobIdMessage;
import org.sd.cluster.job.GetGlobalJobIdMessage;
import org.sd.cluster.job.GetLocalJobIdMessage;
import org.sd.cluster.job.GlobalJobId;
import org.sd.cluster.job.JobCommandMessage;
import org.sd.cluster.job.JobManager;
import org.sd.cluster.job.LocalJobId;
import org.sd.util.ExecUtil;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Console for interacting with a cluster.
 * <p>
 * @author Spence Koehler
 */
public class Console {

  // set PING_FIRST to true if found to be useful; for now it seems redundant, so we're set to false.
  private static final boolean PING_FIRST = false;
  private static final Message PING = PING_FIRST ? new Ping() : null;

  /**
   * Build a console from the currently deployed cluster environment.
   *
   * @param consoleIdentifier  an arbitrary string to identify the console.
   */
  public static final Console buildConsole(String consoleIdentifier) {
    Console result = null;

    final String user = ExecUtil.getUser();
    final String defName = ConfigUtil.getActiveClusterName();
    final String[] machines = ConfigUtil.getActiveClusterMachines();

    if (defName != null && machines != null && machines.length > 0) {
      try {
        final ClusterDefinition clusterDef = new ClusterDefinition(user, defName, "vorta", machines);
        result = new Console(clusterDef, consoleIdentifier);
      }
      catch (IOException e) {
        e.printStackTrace(System.err);
      }
    }

    return result;
  }

  private ClusterDefinition clusterDef;
  private NodeClient consoleClient;
  private ClusterContext clusterContext;
  private String defaultGroup;
  private AtomicBoolean timedOut = new AtomicBoolean(false);

  private final Object SEND_MESSAGE_MUTEX = new Object();

  public Console(ClusterDefinition clusterDef, String identifier) {
    this.clusterDef = clusterDef;
    this.consoleClient = makeConsoleClient(identifier);

    final String userName = clusterDef.getUser();
    final int highestJvmNum = getHighestJvmNum(userName);
    final Config config = new Config(highestJvmNum, userName);
    this.clusterContext = new ConsoleClusterContext(config, clusterDef);
  }

  private final int getHighestJvmNum(String userName) {
    int result = 0;

    final int[] portOverride = ConfigUtil.getPortOverride();
    if (portOverride != null && portOverride.length > 1) {
      result = portOverride[1] - portOverride[0];
    }
    else {
      final UsersCsv usersCsv = UsersCsv.getInstance();
      result = usersCsv.getHighPort(userName) - usersCsv.getLowPort(userName);
    }

    return result;
  }

  public ClusterContext getClusterContext() {
    return clusterContext;
  }

  public void setDefaultGroup(String defaultGroup) {
    this.defaultGroup = defaultGroup;
  }

  private final NodeClient makeConsoleClient(String identifier) {
    NodeClient result = null;
    try {
      result = new NodeClient("console-client(" + clusterDef.getUser() + "@" +
                              clusterDef.getDefinitionName() + ", " + identifier + ")",
                              InetAddress.getLocalHost(), clusterDef.getNumNodes());
      result.start();
    }
    catch (UnknownHostException e) {
      throw new IllegalStateException("can't get localhost!", e);
    }
    return result;
  }

  public ClusterDefinition getClusterDefinition() {
    return clusterDef;
  }

  /**
   * Shutdown this console (not the cluster).
   */
  public void shutdown() {
    consoleClient.shutdown(false);
  }

  /**
   * Send the message to the specified node.
   *
   * @param message  The message to send.
   * @param node     The node of the form machineName-jvmNum to send the message to.
   * @param timeout  Timeout for waiting for a response.
   *
   * @return the response from the node or null.
   *
   * @throws ClusterException to identify problems.
   */
  public Response sendMessageToNode(Message message, String node, int timeout) throws ClusterException {
    final InetSocketAddress serverAddress = clusterDef.getServerAddress(node);

    if (serverAddress == null) {
      throw new NonexistentNodeException("unknown node '" + node + "' in cluster '" +
                                         clusterDef.getDefinitionName() + "'");
    }

    if (PING != null && !isAlive(serverAddress, timeout)) {
      throw new UnreachableNodeException("dead node '" + node + "' (address=" +
                                         serverAddress + ") in cluster '" +
                                         clusterDef.getDefinitionName() + "'");
    }

    final Response result = sendMessage(serverAddress, message, timeout);

    if (result == null) {
      throw new UnresponsiveNodeException("no response from node '" + node + "' (address=" +
                                          serverAddress + ") in cluster '" +
                                          clusterDef.getDefinitionName() +
                                          "' (timeout=" + timeout + ")");
    }

    return result;
  }

  private final InetSocketAddress[] getServerAddresses(String groupName) throws ClusterException {
    InetSocketAddress[] serverAddresses = null;
    try {
      serverAddresses = clusterDef.getServerAddresses(groupName);
    }
    catch (IllegalStateException e) {
      throw new NonexistentNodeException(e);
    }
    return serverAddresses;
  }

  /**
   * Send the message to the specified nodes.
   *
   * @param message    The message to send.
   * @param groupName  The nodes to send the message to identified by group name in the cluster definition.
   *                   Note that level ClusterDefinition.ALL_NODES_GROUP indicates all nodes. Null indicates
   *                   the current node, but null will be returned as this is not intended to send a message
   *                   to self.
   * @param timeout    Timeout for waiting for a response.
   *
   * @return the response from the nodes or null.
   *
   * @throws ClusterException to identify problems.
   */
  public Response[] sendMessageToNodes(Message message, String groupName, int timeout, boolean requireAllResponses) throws ClusterException {
    final InetSocketAddress[] serverAddresses = getServerAddresses(groupName);

    if (serverAddresses == null) {
      throw new NonexistentNodeException("unknown group " + groupName + " in cluster '" +
                                         clusterDef.getDefinitionName() + "'");
    }

    if (PING != null && !isAlive(serverAddresses, timeout)) {
      throw new UnreachableNodeException("dead node(s) in group " + groupName + " in cluster '" +
                                         clusterDef.getDefinitionName() + "'");
    }

    return sendMessageToNodes(message, serverAddresses, timeout, requireAllResponses);
  }

  /**
   * Send the message to the specified nodes.
   *
   * @param message    The message to send.
   * @param nodeNames  The nodes to send the message to identified by group name in the cluster definition.
   *                   Note that level ClusterDefinition.ALL_NODES_GROUP indicates all nodes. Null indicates
   *                   the current node, but null will be returned as this is not intended to send a message
   *                   to self.
   * @param timeout    Timeout for waiting for a response.
   *
   * @return the response from the nodes or null.
   *
   * @throws ClusterException to identify problems.
   */
  public Response[] sendMessageToNodes(Message message, String[] nodeNames, int timeout, boolean requireAllResponses) throws ClusterException {
    final List<InetSocketAddress> serverAddresses = new ArrayList<InetSocketAddress>();

    for (int i = 0; i < nodeNames.length; ++i) {
      final InetSocketAddress[] nodeServerAddresses = getServerAddresses(nodeNames[i]);

      if (nodeServerAddresses != null) {
        for (InetSocketAddress nodeServerAddress : nodeServerAddresses) {
          serverAddresses.add(nodeServerAddress);
        }
      }
      else {
        throw new NonexistentNodeException("unknown node '" + nodeNames[i] + "' in cluster '" +
                                           clusterDef.getDefinitionName() + "'");
      }
    }

    return sendMessageToNodes(message, serverAddresses.toArray(new InetSocketAddress[serverAddresses.size()]), timeout, requireAllResponses);
  }

  /**
   * Send the message to the specified nodes.
   *
   * @param message    The message to send.
   * @param groupName  The nodes to send the message to identified by group name in the cluster definition.
   *                   Note that level ClusterDefinition.ALL_NODES_GROUP indicates all nodes. Null indicates
   *                   the current node, but null will be returned as this is not intended to send a message
   *                   to self.
   * @param timeout    Timeout for waiting for a response.
   *
   * @return the response from the nodes or null.
   *
   * @throws ClusterException to identify problems.
   */
  private final Response[] sendMessageToNodes(Message message, InetSocketAddress[] serverAddresses, int timeout, boolean requireAllResponses) throws ClusterException {

    timedOut.set(false);

    final long starttime = System.currentTimeMillis();
    final Response[] result = sendMessage(serverAddresses, message, timeout);
    final long elapsed = System.currentTimeMillis() - starttime;

    if (result == null || (requireAllResponses && result.length < serverAddresses.length)) {
      final int badCount = serverAddresses.length - ((result == null) ? 0 : result.length);
      throw new UnresponsiveNodeException("no response from " + badCount + " of " + serverAddresses.length +
                                          " nodes in cluster '" + clusterDef.getDefinitionName() + "'");
    }

    if (result == null || result.length < serverAddresses.length) {
      System.err.println(new Date() + ": Console.sendMessageToNodes received only " + (result == null ? 0 : result.length) +
                         " of " + serverAddresses.length + " responses! (timeOut=" + timeout + ", elapsed=" + elapsed + ")");
      timedOut.set(true);
    }

    return result;
  }

  public boolean timedOut() {
    return timedOut.get();
  }

  public void showResponses(PrintStream out, Response[] responses) {
    showResponses(out, responses, defaultGroup);
  }

  public void showResponses(PrintStream out, Response[] responses, String groupName) {
    if (out != null) {
      final Set<String> expectedResponders = getNodeNames(groupName);
      final int numResponders = expectedResponders != null ? expectedResponders.size() : -1;

      out.println("Received " + responses.length + " (of " + numResponders + ") responses:");

      final List<String> responseStrings = new ArrayList<String>();
      for (Response response : responses) {
        responseStrings.add(response.toString());
        if (expectedResponders != null && response instanceof SignedResponse) {
          expectedResponders.remove(((SignedResponse)response).getNodeName().toLowerCase());
        }
      }
      Collections.sort(responseStrings);

      for (String responseString : responseStrings) out.println(responseString);

      if (expectedResponders != null && expectedResponders.size() > 0 && responses.length != numResponders) {
        out.println("\nNot responding: " + expectedResponders + "\n");
      }
    }
  }

  public Set<String> getNodeNames(String groupName) {
    return groupName == null ? null : new TreeSet<String>(clusterDef.getGroupNodeNames(groupName, true));
  }

  /**
   * Explode the node or group names to identify only nodes.
   */
  public String[] explode(String[] nodesOrGroups) {
    final Set<String> result = new HashSet<String>();

    for (String nodeOrGroup : nodesOrGroups) {
      result.addAll(clusterDef.getGroupNodeNames(nodeOrGroup, true));
    }

    return result.toArray(new String[result.size()]);
  }

  public Response[] sendJob(Job job, int timeout) throws ClusterException {
    final String groupName = job.getGroupName();
    job.setGlobalJobId(acquireGlobalJobId(groupName, timeout));
    return sendMessageToNodes(job, groupName, timeout, true);
  }

  public GlobalJobId acquireGlobalJobId(String groupName, int timeout) throws ClusterException {
    GlobalJobId result = null;

    final InetSocketAddress[] serverAddresses = getServerAddresses(groupName);

    if (serverAddresses != null) {
      final Message message = new AcquireJobIdMessage();
      final Response[] responses = sendMessage(serverAddresses, message, timeout);

      if (responses != null && responses.length > 0) {
        result = new GlobalJobId();
        for (Response response : responses) {
          final IntegerResponse integerResponse = (IntegerResponse)response;
          result.add(integerResponse.getNodeName(), integerResponse.getValue());
        }
      }
    }

    return result;
  }

  /**
   * Send the given job command only to the given node.
   */
  public Response sendJobCommandToNode(JobCommandMessage message, int timeout) throws ClusterException {
    final LocalJobId localJobId = message.getLocalJobId();
    final InetSocketAddress serverAddress = clusterDef.getServerAddress(localJobId.getNodeName());
    final Response result = sendMessage(serverAddress, message, timeout);
//System.err.println("Sending jobCommandToNode: " + message + " serverAddress=" + serverAddress + " response=" + result);
    return result;
  }

  /**
   * Send the given job command to all nodes working on the job.
   * <p>
   * Note that the job is identified by its local id on one node in order to
   * get the global id and send the command to all job nodes.
   */
  public Response[] sendJobCommand(JobCommandMessage message, int timeout) throws ClusterException {
    Response[] result = null;

    final LocalJobId localJobId = message.getLocalJobId();

    if (localJobId.getJobDescription() == null) {
      final GlobalJobId globalJobId = getGlobalJobId(localJobId.getNodeName(), localJobId.getId(), timeout);
      result = sendJobCommand(message, globalJobId, timeout);
    }
    else {
      result = sendMessageToNodes(message, localJobId.getNodeName(), timeout, false);
    }

    return result;
  }

  /**
   * Get the global job id for the local job on the given node.
   */
  protected GlobalJobId getGlobalJobId(String oneNodeName, int localJobId, int timeout) throws ClusterException {
    GlobalJobId result = null;

    // ask the node for the global job id of the job
    final Message message = new GetGlobalJobIdMessage(localJobId);
    final PublishableResponse response = (PublishableResponse)sendMessageToNode(message, oneNodeName, timeout);
    if (response != null) {
      result = (GlobalJobId)response.getValue();
    }

    return result;
  }

  public LocalJobId getLocalJobId(String oneNodeName, Job job, int timeout) throws ClusterException {
    return getLocalJobId(oneNodeName, job.getDescription(), timeout);
  }

  public LocalJobId getLocalJobId(String oneNodeName, String jobDescription, int timeout) throws ClusterException {
    LocalJobId result = null;

    final Message message = new GetLocalJobIdMessage(jobDescription);
    final PublishableResponse response = (PublishableResponse)sendMessageToNode(message, oneNodeName, timeout);
    if (response != null) {
      result = (LocalJobId)response.getValue();
    }

    return result;
  }

  /**
   * Send the job command to all nodes working on the job as defined by the
   * global job id.
   */
  public Response[] sendJobCommand(JobCommandMessage message, GlobalJobId globalJobId, int timeout) throws ClusterException {
    if (globalJobId == null) return null;

    final Collection<LocalJobId> localJobIds = globalJobId.getLocalJobIds();
    final InetSocketAddress[] serverAddresses = new InetSocketAddress[localJobIds.size()];
    final Message[] messages = new Message[localJobIds.size()];
    int index = 0;
    for (LocalJobId localJobId : localJobIds) {
      serverAddresses[index] = clusterDef.getServerAddress(localJobId.getNodeName());
      messages[index] = new JobCommandMessage(message.getJobCommand(), localJobId);
      ++index;
    }

    final Message[] responses = consoleClient.sendMessages(serverAddresses, messages, 10, 100, timeout);
    return convertMessagesToResponses(responses);
  }

  private final boolean isAlive(InetSocketAddress serverAddress, int timeout) {
    final Response response = sendMessage(serverAddress, PING, timeout);
    return response != null;
  }

  private final boolean isAlive(InetSocketAddress[] serverAddresses, int timeout) {
    final Response[] responses = sendMessage(serverAddresses, PING, timeout);
    return responses != null && responses.length == serverAddresses.length;
  }

  private final Response sendMessage(InetSocketAddress serverAddress, Message message, int timeout) {
    return (Response)consoleClient.sendMessage(serverAddress, message, 10, 100, timeout);
  }

  private final Response[] sendMessage(InetSocketAddress[] serverAddresses, Message message, int timeout) {
    Message[] responses = null;

    synchronized (SEND_MESSAGE_MUTEX) {
      responses = consoleClient.sendMessage(serverAddresses, message, 10, 100, timeout);
    }

    return convertMessagesToResponses(responses);
  }

  private final Response[] convertMessagesToResponses(Message[] responses) {
    if (responses == null) return null;
    final List<Response> result = new ArrayList<Response>();

    for (Message response : responses) {
      if (response != null) result.add((Response)response);
    }

    return result.toArray(new Response[result.size()]);
  }

  private static final class ConsoleClusterContext implements ClusterContext {

    private Config config;
    private ClusterDefinition clusterDefinition;

    ConsoleClusterContext(Config config, ClusterDefinition clusterDefinition) {
      this.config = config;
      this.clusterDefinition = clusterDefinition;
    }

    public String getName() {
      return "ConsoleClusterContext";
    }

    public Config getConfig() {
      return config;
    }

    public ClusterDefinition getClusterDefinition() {
      return clusterDefinition;
    }

    public JobManager getJobManager() {
//todo: add one if ever needed. = new JobManager(config, clusterDef, "consoleJobManager")
      return null;
    }

    /** The console cluster context has no safe deposit box. */
    public SafeDepositBox getSafeDepositBox() {
//todo: add one if ever needed.
      return null;
    }
  }

// enter an interactive command loop

// ping cluster nodes to see who is up (wait until all are up?)
// send GetJobsMessage.getInstance() message to all ClusterNodes
// send pause/resume/stop commands to jobs
// send new jobs to cluster (reflectively)
// start, stop, purge cluster? ...maybe not from in here.

}
