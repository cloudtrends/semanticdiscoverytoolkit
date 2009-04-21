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
package org.sd.cluster.io;


import org.sd.cluster.config.ClusterException;
import org.sd.cluster.config.Console;
import org.sd.util.StatsAccumulator;
import org.sd.util.thread.TimeLimitedThreadPool;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An agent to mitigate making SafeDepositBox deposits and withdrawls.
 * <p>
 * @author Spence Koehler
 */
public class SafeDepositAgent {
  
  private Console console;
  private SafeDepositMessage message;
  private String[] nodesToContact;
  private int groupSize;
  private Map<String, Integer> name2pos;
  private long responseTimeout;
  private long withdrawalTimeout;

  private TimeLimitedThreadPool<List<SafeDepositReceipt>> nodePool;
  private List<Callable<List<SafeDepositReceipt>>> nodeCallables;

  // return parameters.
  private List<SafeDepositBox.Withdrawal> withdrawals;  // withdrawal from each node contacted.
  private AtomicBoolean timedOut = new AtomicBoolean(false);

  private StatsAccumulator responseTimes;
  private StatsAccumulator queryTimes;
  private StatsAccumulator cumulativeResponseTimes;
  private StatsAccumulator cumulativeQueryTimes;
  private AtomicInteger responseCount;

  /**
   *
   * @param console              A console configured to communicate with a
   *                             running cluster.
   * @param message              The message that builds contents for deposit.
   * @param nodesToContact       The directly contacted node groups or nodes on
   *                             which deposits will be made.
   * @param groupSize            Number of nodes to contact at once. (3 is usually good.)
   * @param responseTimeout      Millis to wait for each response. This needs to be
   *                             long enough to allow the message to be sent and
   *                             all data streaming back to be received. Note that
   *                             each node to contact will be given this amount of
   *                             time serially.
   * @param withdrawalTimeout    Millis to wait for withdrawals before giving up.
   */
  public SafeDepositAgent(Console console, SafeDepositMessage message, String[] nodesToContact,
                          int groupSize, long responseTimeout, long withdrawalTimeout) {
    this.console = console;
    this.message = message;
    this.nodesToContact = console.explode(nodesToContact);
    this.groupSize = groupSize;
    this.name2pos = buildName2Pos(this.nodesToContact);
    this.responseTimeout = responseTimeout;
    this.withdrawalTimeout = withdrawalTimeout;

    this.withdrawals = new ArrayList<SafeDepositBox.Withdrawal>();

    this.nodePool =
      new TimeLimitedThreadPool<List<SafeDepositReceipt>>(
        console.getClusterDefinition().getDefinitionName(),
        groupSize);

    this.responseTimes = new StatsAccumulator("roundTripMillis");
    this.queryTimes = new StatsAccumulator("queryMillis");
    this.responseCount = new AtomicInteger(0);

    this.nodeCallables = buildNodeCallables(console, message, responseTimeout, this.nodesToContact,
                                            withdrawals, queryTimes, responseCount);

    this.cumulativeResponseTimes = new StatsAccumulator("cumulativeRoundTripMillis");
    this.cumulativeQueryTimes = new StatsAccumulator("cumulativeQueryMillis");
  }

  private final Map<String, Integer> buildName2Pos(String[] nodeNames) {
    final Map<String, Integer> result = new HashMap<String, Integer>();
    for (int i = 0; i < nodeNames.length; ++i) {
      result.put(nodeNames[i], i);
    }
    return result;
  }

  private final List<Callable<List<SafeDepositReceipt>>> buildNodeCallables(Console console, SafeDepositMessage message, long responseTimeout, String[] nodesToContact,
                                                                            List<SafeDepositBox.Withdrawal> withdrawals, StatsAccumulator queryTimes,
                                                                            AtomicInteger responseCount) {
    final List<Callable<List<SafeDepositReceipt>>> result = new ArrayList<Callable<List<SafeDepositReceipt>>>();

    for (String nodeToContact : nodesToContact) {
      result.add(new NodeCallable(console, message, responseTimeout, nodeToContact, withdrawals, queryTimes, responseCount));
    }

    return result;
  }

  /**
   * Shutdown resources associated with this agent.
   * <p>
   * This includes thread pools, etc., but not the console.
   */
  public void close() {
    nodePool.shutdown();
  }

  /**
   * Reset this instance for re-use on the same thread.
   */
  public void reset(SafeDepositMessage message) {
    //if we could re-use the message instance, then we would do:
    // this.message.resetClaimTickets();
    // but we couldn't use the instance to know whether it has changed.


    if (!this.message.equals(message)) {
      this.message = message;
      this.withdrawals.clear();
      this.responseCount.set(0);

      for (Callable<List<SafeDepositReceipt>> callable : nodeCallables) {
        final NodeCallable nodeCallable = (NodeCallable)callable;
        nodeCallable.setMessage(message);
      }
    }

    timedOut.set(false);
    responseTimes.clear();
    queryTimes.clear();
  }

  /**
   * Get this agent's message.
   */
  public SafeDepositMessage getMessage() {
    return message;
  }

  /**
   * Initiate messaging and collect the withdrawals.
   *
   * @return true if results were collected.
   */
  public boolean collectWithdrawals() {

    final long starttime = System.currentTimeMillis();
    final long expirationTime = starttime + withdrawalTimeout;
    int loopCount = 0;

    // aggregate results
    while (System.currentTimeMillis() < expirationTime && responseCount.get() < nodesToContact.length) {

      final TimeLimitedThreadPool.ExecutionInfo<List<SafeDepositReceipt>> executionInfo =
        nodePool.execute(nodeCallables, withdrawalTimeout);

      responseTimes.combineWith(executionInfo.getOperationTimes());

      ++loopCount;
    }

    cumulativeQueryTimes.combineWith(queryTimes);
    cumulativeResponseTimes.combineWith(responseTimes);

    System.out.println(responseTimes);
    System.out.println(cumulativeResponseTimes);
    System.out.println(queryTimes);
    System.out.println(cumulativeQueryTimes);
    System.out.println("responseRatio=" + getResponseRatio() + "  loopCount=" + loopCount + "  withdrawalCount=" + withdrawals.size());

    if (responseCount.get() < nodesToContact.length) {
System.out.print("\tmissing responses from:");
      for (Callable<List<SafeDepositReceipt>> callable : nodeCallables) {
        final NodeCallable nodeCallable = (NodeCallable)callable;
        if (!nodeCallable.retrieved()) System.out.print(" " + nodeCallable.getNodeName().toUpperCase());
      }
      System.out.println();
    }

    return withdrawals.size() > 0;
  }

  /**
   * Count the number of successful withdrawals.
   */
  public int numWithdrawals() {
    return withdrawals.size();
  }

  /**
   * Get the withdrawals.
   */
  public List<SafeDepositBox.Withdrawal> getWithdrawals() {
    return withdrawals;
  }

  /**
   * Get the number of nodes that responded.
   */
  public int numResponded() {
    return responseCount.get();
  }

  /**
   * Get the percentage of nodes that responded.
   */
  public double getResponseRatio() {
    return ((double)responseCount.get() / (double)nodesToContact.length);
  }

  /**
   * Get the number of individual nodes to contact through this agent.
   */
  public int getNumNodes() {
    return nodesToContact.length;
  }

  /**
   * Get whether the last withdrawal timed out at any time.
   */
  public boolean timedOut() {
    return responseTimes.getN() < nodesToContact.length;
  }


  private static final class NodeCallable implements Callable<List<SafeDepositReceipt>> {
    private Console console;
    private SafeDepositMessage message;
    private long responseTimeout;
    private String nodeName;
    private boolean retrieved;

    private List<SafeDepositBox.Withdrawal> withdrawals;
    private StatsAccumulator queryTimes;
    private AtomicInteger responseCount;

    NodeCallable(Console console, SafeDepositMessage message, long responseTimeout, String nodeName,
                 List<SafeDepositBox.Withdrawal> withdrawals, StatsAccumulator queryTimes,
                 AtomicInteger responseCount) {
      this.console = console;
      this.message = message;
      this.responseTimeout = responseTimeout;
      this.nodeName = nodeName;
      this.retrieved = false;

      this.withdrawals = withdrawals;
      this.queryTimes = queryTimes;
      this.responseCount = responseCount;
    }

    public void setMessage(SafeDepositMessage message) {
      this.message = message;
      this.retrieved = false;
    }

    public boolean retrieved() {
      return retrieved;
    }

    public String getNodeName() {
      return nodeName;
    }

    public List<SafeDepositReceipt> call() throws Exception {
      List<SafeDepositReceipt> result = new ArrayList<SafeDepositReceipt>();

      if (!retrieved) {
//System.out.println("*Sending query to " + nodeName.toUpperCase());

        final long starttime = System.currentTimeMillis();
        final Response[] responses = console.sendMessageToNodes(message, nodeName, (int)responseTimeout, false);

        if (responses != null) {
          boolean gotResponse = false;

//           if (responses.length == 0) {
//             // a node is currently non-responsive. we have to "count it out".
//             gotResponse = true;
//             this.retrieved = true;
//           }

          for (Response response : responses) {
            if (response != null) {
              final SafeDepositReceipt sdReceipt = (SafeDepositReceipt)response;
              synchronized (message) {
                message.updateClaims(sdReceipt);
              }
              result.add(sdReceipt);

              if (sdReceipt.hasContents()) {
                final SafeDepositBox.Withdrawal withdrawal = sdReceipt.getWithdrawal();

                if (withdrawal != null) {
//System.out.println("*Received results from " + nodeName.toUpperCase());

                  this.retrieved = true;
                  gotResponse = true;

                  synchronized (withdrawals) {
                    withdrawals.add(withdrawal);
                  }
                  synchronized (queryTimes) {
                    queryTimes.add(withdrawal.getDepositTime() - withdrawal.getOpenedTime());
                  }
                }
              }
            }
          }

          if (gotResponse) responseCount.incrementAndGet();

/*
          if (!retrieved && responses.length > 0) {
            // put a little governor on resubmitting the message
            final long deltatime = responseTimeout - (System.currentTimeMillis() - starttime);
            if (deltatime > 100) {
              try {
                Thread.sleep(100);
              }
              catch (InterruptedException ignore) {}
            }
          }
*/
        }
      }
//else System.out.println("*Reusing query results from " + nodeName.toUpperCase());

      return result;
    }

    public boolean matchesNode(String nodeName) {
      return this.nodeName.equals(nodeName);
    }
  }
}
