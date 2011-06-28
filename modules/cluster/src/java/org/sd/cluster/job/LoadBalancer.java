/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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


import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.sd.cluster.config.ClusterDefinition;
import org.sd.cluster.config.ClusterException;
import org.sd.cluster.config.ClusterNode;
import org.sd.cluster.config.Console;
import org.sd.cluster.io.Message;
import org.sd.cluster.io.Response;

/**
 * Utility class for load-balanced sending of messages to a group of nodes.
 * <p>
 * @author Spence Koehler
 */
public class LoadBalancer {
  
  private static final boolean DEBUG = false;


  private Console console; // console borrowed from JobManager
  private boolean verbose;

  private int numGroupNodes;
  private List<String> groupNodes;
  private ConcurrentLinkedQueue<NodeInfo> nodeInfos;  // group nodes to contact

  private int singleInitTimeout;
  private int singleNormalTimeout;

  private int cycleLimit;
  private int countdownStart;

  //
  // Timeouts:
  // - singleInitTimeout: timeout for single "first try" interaction
  // - singleNormalTimeout: timeout for interacting with an "up" (or "unknown") node
  // - totalFailTimeout: timeout for total failure
  // - where
  //   - singleNormalTimeout < singleInitTimeout << totalFailTimeout
  //   - totalFailTimeout = singleInitTimeout * (numGroupNodes + 1)
  //
  // (ideas:
  //    on a separate thread, send last message to all nodes that have been "down" for some time period
  // )
  //
  // Algorithm:
  // - Start timing, set loopCountdown to numGroupNodes * cycleLimit
  // - Looping until request is handled or fullTimeout is reached or loopCountdown hits 0,
  //   - Choose "next" node from the head of the queue, immediately placing back onto the tail of the queue
  //     - decrement loopCountdown
  //   - If the node is "initializing", then skip (loop)
  //     - increment loopCountdown (this doesn't count as a visit)
  //   - Else If the node is "unkown" or "up",
  //       - then synchronously send/receive message/response using singleInitTimeout (unknown) or singleNormalTimeout (up)
  //         - first mark "unknown" as "initializing"
  //     - If successful, return response; else, mark node as "down" and loop.
  //       - mark node as "up" if unknown/initializing before returning successful response
  //   - Else (node is "down")
  //     - If singleInitTimeout > remaining time
  //       - If numRemainingNodes > 1, ignore and loop
  //       - else, synchronously send/receive message/response using remainingTimeTimeout
  //         - before send, set status to initializing
  //         - after send, update status appropriately
  //     - else, synchronously send/receive message/response using singleInitTimeout
  //       - before send, set status to initializing
  //       - after send, set status to initializing
  //       - If successful, mark node as "up" and return response; else, loop
  //

  public LoadBalancer(ClusterNode clusterNode, String group, int singleInitTimeout, int singleNormalTimeout, int cycleLimit) {
    this.console = clusterNode.getJobManager().getConsole();
    this.verbose = false;
    final ClusterDefinition clusterDef = clusterNode.getClusterDefinition();
    this.groupNodes = clusterDef.getGroupNodeNames(group, true);
    this.numGroupNodes = groupNodes.size();
    this.nodeInfos = new ConcurrentLinkedQueue<NodeInfo>();
    for (String groupNode : groupNodes) {
      nodeInfos.add(new NodeInfo(groupNode));
    }

    System.out.println(new Date() + ": LoadBalancer init(" + group +
                       ", initTime=" + singleInitTimeout +
                       ", normTime=" + singleNormalTimeout +
                       ", cycles=" + cycleLimit +
                       ") : numGroupNodes=" + numGroupNodes);

    this.singleInitTimeout = singleInitTimeout;
    this.singleNormalTimeout = singleNormalTimeout;
    this.cycleLimit = cycleLimit;
    this.countdownStart = cycleLimit * numGroupNodes;
  }

  public boolean setVerbose(boolean verbose) {
    final boolean result = this.verbose;
    this.verbose = verbose;
    return result;
  }

  public boolean getVerbose() {
    return this.verbose;
  }

  public List<String> getGroupNodes() {
    return groupNodes;
  }

  public ConcurrentLinkedQueue<NodeInfo> getNodeInfos() {
    return nodeInfos;
  }

  public Response sendMessageToNode(Message message) {
    return sendMessageToNode(message, singleInitTimeout, singleNormalTimeout);
  }

  public Response sendMessageToNode(Message message, int singleInitTimeout, int singleNormalTimeout) {
    Response result = null;
    NodeInfo nextNode = null;
    final int totalFailTimeout = singleInitTimeout * (numGroupNodes + 1);

    // Start the clock
    final long starttime = System.currentTimeMillis();
    int loopCountdown = countdownStart;

    while (result == null && (System.currentTimeMillis() - starttime) < totalFailTimeout && loopCountdown > 0) {
      nextNode = null; // reset

      // Grab the next node, waiting until one is available
      for (; nextNode == null && (System.currentTimeMillis() - starttime) < totalFailTimeout; nextNode = nodeInfos.poll());

      // Immediately place node back on queue
      if (nextNode != null) {
        nodeInfos.add(nextNode);

        if (DEBUG && verbose) {
          System.out.print(new Date() + ": LoadBalancer rolled nodes:");
          for (NodeInfo ni : nodeInfos) {
            System.out.print(" " + ni);
          }
          System.out.println();
        }
      }
      else break;  // failed to acquire node in allotted time

      --loopCountdown;

      final NodeStatus nodeStatus = nextNode.getStatus();
      final int remainingTime = totalFailTimeout - (int)(System.currentTimeMillis() - starttime);
      int timeout = (nodeStatus == NodeStatus.UP) ? singleNormalTimeout : singleInitTimeout;
      timeout = Math.min(timeout, remainingTime);

      switch (nodeStatus) {
        case INITIALIZING :
          ++loopCountdown;  // don't count this as a visit
          break;            // skip this node

        case UNKNOWN :  // drop through to UP
        case UP :
          result = doSendMessage(nextNode, message, nodeStatus, timeout);
          break;

        case DOWN :
          if (loopCountdown < numGroupNodes) {
            result = doSendMessage(nextNode, message, nodeStatus, timeout);
          }
          // else, ignore and loop
          break;
      }
    }

    return result;
  }

  private final Response doSendMessage(NodeInfo nodeInfo, Message message, NodeStatus expectedStatus, int timeout) {
    Response result = null;

    final NodeStatus newStatus = expectedStatus == NodeStatus.UP ? expectedStatus : NodeStatus.INITIALIZING;
    if (nodeInfo.changeStatus(expectedStatus, newStatus)) {

      if (verbose) {
        System.out.println(new Date() + ": LoadBalancer sending message to '" +
                           nodeInfo.getNodeName() + "' (" + expectedStatus + ")");
      }

      // send/receive message, construct responseInfo
      try {
        result = console.sendMessageToNode(message, nodeInfo.getNodeName(), timeout);
      }
      catch (ClusterException e) {
        System.err.println(new Date() + ": LoadBalancer failed sending message to node '" + nodeInfo.getNodeName() + "'");
        e.printStackTrace(System.err);
      }
      
      // if successful,... mark node as "up" since we succeeded
      if (result != null) {
        nodeInfo.changeStatus(newStatus, NodeStatus.UP);
      }
      else {
        // node interaction failed, need to reset
        nodeInfo.setStatus(NodeStatus.DOWN);
      }
    }

    return result;
  }


  private enum NodeStatus { UP, DOWN, INITIALIZING, UNKNOWN };

  public static final class NodeInfo {
    private String nodeName;
    private AtomicReference<NodeStatus> status;

    public NodeInfo(String nodeName) {
      this.nodeName = nodeName;
      this.status = new AtomicReference<NodeStatus>(NodeStatus.UNKNOWN);
    }

    public String getNodeName() {
      return nodeName;
    }

    public boolean changeStatus(NodeStatus fromStatus, NodeStatus toStatus) {
      return status.compareAndSet(fromStatus, toStatus);
    }

    public NodeStatus getStatus() {
      return status.get();
    }

    public NodeStatus setStatus(NodeStatus newStatus) {
      return status.getAndSet(newStatus);
    }

    public String toString() {
      final StringBuilder result = new StringBuilder();
      result.append(nodeName).append('(').append(status.get()).append(')');
      return result.toString();
    }
  }

}
