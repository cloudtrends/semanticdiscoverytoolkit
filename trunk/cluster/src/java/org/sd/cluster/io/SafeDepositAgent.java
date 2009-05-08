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
import org.sd.io.Publishable;
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
	private boolean verbose;

  private TimeLimitedThreadPool<List<SafeDepositReceipt>> nodePool;
  private List<Callable<List<SafeDepositReceipt>>> nodeCallables;

  // return parameters.
  private List<SafeDepositBox.Withdrawal> withdrawals;   // withdrawal from each node contacted.
	private Map<String, SafeDepositReceipt> receipts;      // node signature to receipt
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
                          int groupSize, long responseTimeout, long withdrawalTimeout,
													boolean verbose) {
    this.console = console;
    this.message = message;
    this.nodesToContact = console.explode(nodesToContact);
    this.groupSize = groupSize;
    this.name2pos = buildName2Pos(this.nodesToContact);
    this.responseTimeout = responseTimeout;
    this.withdrawalTimeout = withdrawalTimeout;
		this.verbose = verbose;

    this.withdrawals = new ArrayList<SafeDepositBox.Withdrawal>();
		this.receipts = new HashMap<String, SafeDepositReceipt>();

    this.nodePool =
      new TimeLimitedThreadPool<List<SafeDepositReceipt>>(
        console.getClusterDefinition().getDefinitionName(),
        groupSize);

    this.responseTimes = new StatsAccumulator("roundTripMillis");
    this.queryTimes = new StatsAccumulator("queryMillis");
    this.responseCount = new AtomicInteger(0);

		if (verbose) {
			System.out.println(new Date() + ": SafeDepositAgent.responseTimeout=" +
												 responseTimeout + " .withdrawalTimeout=" +
												 withdrawalTimeout + " .numNodesToContact=" +
												 nodesToContact.length);
		}

    this.nodeCallables = buildNodeCallables(console, message, responseTimeout, withdrawalTimeout,
																						this.nodesToContact, withdrawals, receipts,
																						queryTimes, responseCount, verbose);

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

  private final List<Callable<List<SafeDepositReceipt>>> buildNodeCallables(
		Console console, SafeDepositMessage message, long responseTimeout,
		long withdrawalTimeout, String[] nodesToContact,
		List<SafeDepositBox.Withdrawal> withdrawals,
		Map<String, SafeDepositReceipt> receipts,	StatsAccumulator queryTimes,
		AtomicInteger responseCount, boolean verbose) {

    final List<Callable<List<SafeDepositReceipt>>> result = new ArrayList<Callable<List<SafeDepositReceipt>>>();

    for (String nodeToContact : nodesToContact) {
      result.add(new NodeCallable(console, message, responseTimeout, withdrawalTimeout,
																	nodeToContact, withdrawals, receipts, queryTimes,
																	responseCount, verbose));
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
			this.receipts.clear();
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
   * @return a TransactionResult instance.
   */
  public TransactionResult collectWithdrawals() {

		// NOTE: this is called from ServletProcessorController.WithdrawalCallable.call() as sdAgent.collectWithdrawals()

    // aggregate results
		final TimeLimitedThreadPool.ExecutionInfo<List<SafeDepositReceipt>> executionInfo =
			nodePool.execute(nodeCallables, withdrawalTimeout);

		responseTimes.combineWith(executionInfo.getOperationTimes());

    cumulativeQueryTimes.combineWith(queryTimes);
    cumulativeResponseTimes.combineWith(responseTimes);

		if (false && verbose) {
			System.out.println(responseTimes);
			System.out.println(cumulativeResponseTimes);
			System.out.println(queryTimes);
			System.out.println(cumulativeQueryTimes);
			System.out.println("responseRatio=" + getResponseRatio() + "  withdrawalCount=" + withdrawals.size());
		}

		List<String> missingResponses = null;
    if (responseCount.get() < nodesToContact.length) {
			missingResponses = new ArrayList<String>();
			if (verbose) System.out.print("\tmissing responses from:");
      for (Callable<List<SafeDepositReceipt>> callable : nodeCallables) {
        final NodeCallable nodeCallable = (NodeCallable)callable;
        if (!nodeCallable.retrieved()) {
					final String nodeName = nodeCallable.getNodeName().toUpperCase();
					missingResponses.add(nodeName);
					if (verbose) System.out.print(" " + nodeName);
				}
      }
      if (verbose) System.out.println();
    }

		return new TransactionResult(message, withdrawals, queryTimes,
																 responseCount, receipts, missingResponses);
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
	 * Get the receipts.
	 */
	public Map<String, SafeDepositReceipt> getReceipts() {
		return receipts;
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

	private final void setAllBusy() {
		for (Callable<List<SafeDepositReceipt>> callable : nodeCallables) {
			final NodeCallable nodeCallable = (NodeCallable)callable;
			nodeCallable.setBusy();
		}
	}

	private final boolean noneAreBusy() {
		boolean result = true;
		for (Callable<List<SafeDepositReceipt>> callable : nodeCallables) {
			final NodeCallable nodeCallable = (NodeCallable)callable;
			if (nodeCallable.isBusy()) {
				result = false;
				break;
			}
		}
		return result;
	}

  private static final class NodeCallable implements Callable<List<SafeDepositReceipt>> {
    private Console console;
    private SafeDepositMessage message;
    private long responseTimeout;
		private long withdrawalTimeout;
    private String nodeName;
    private boolean retrieved;
		private boolean verbose;

    private List<SafeDepositBox.Withdrawal> withdrawals;
		private Map<String, SafeDepositReceipt> receipts;
    private StatsAccumulator queryTimes;
    private AtomicInteger responseCount;

		private AtomicBoolean busy = new AtomicBoolean(false);


    NodeCallable(Console console,
								 SafeDepositMessage message, long responseTimeout,
								 long withdrawalTimeout, String nodeName,
								 List<SafeDepositBox.Withdrawal> withdrawals,
								 Map<String, SafeDepositReceipt> receipts,
								 StatsAccumulator queryTimes,
                 AtomicInteger responseCount, boolean verbose) {
      this.console = console;
      this.message = message;
      this.responseTimeout = responseTimeout;
			this.withdrawalTimeout = withdrawalTimeout;
      this.nodeName = nodeName;
      this.retrieved = false;

      this.withdrawals = withdrawals;
			this.receipts = receipts;
      this.queryTimes = queryTimes;
      this.responseCount = responseCount;
			this.verbose = verbose;
    }

		public void setBusy() {
			this.busy.set(true);
		}

		public void clearBusy() {
			this.busy.set(false);
		}

		public boolean isBusy() {
			return this.busy.get();
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
			if (verbose) System.out.println("*NodeCallable.call " + nodeName.toUpperCase());
      List<SafeDepositReceipt> result = new ArrayList<SafeDepositReceipt>();
			final long starttime = System.currentTimeMillis();
			final long expirationTime = starttime + withdrawalTimeout;
			long curtime = 0L;
			boolean timeToQuit = false;

      while (!retrieved && !timeToQuit && (curtime = System.currentTimeMillis()) < expirationTime) {

        final long remainingTime = expirationTime - curtime;
        final int timeout = (int)Math.min(responseTimeout, remainingTime);
				if (verbose) System.out.println("*Sending query to " + nodeName.toUpperCase() + " timeout=" + timeout + " result.size()=" + result.size());
        final Response[] responses = console.sendMessageToNodes(message, nodeName, timeout, false);

        if (responses != null) {
          boolean gotResponse = false;
					result.clear();

          for (Response response : responses) {
            if (response != null) {
              final SafeDepositReceipt sdReceipt = (SafeDepositReceipt)response;
							final String signature = sdReceipt.getSignature();

							receipts.put(signature, sdReceipt);     // keep latest receipt

              synchronized (message) {
                message.updateClaims(sdReceipt);
              }
              result.add(sdReceipt);

              if (sdReceipt.hasContents()) {
                final SafeDepositBox.Withdrawal withdrawal = sdReceipt.getWithdrawal();

                if (withdrawal != null) {
									if (verbose) System.out.println("*Received results from " + nodeName.toUpperCase());

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

							if (!retrieved) {
								final long[] completionRatio = sdReceipt.getCompletionRatio();
								final double atpu = sdReceipt.getAverageTimePerUnit();
								if (completionRatio != null && completionRatio[1] > 0 && atpu > 0) {
									// given how long we've waited and how much has been accomplished,
									final double etr = (completionRatio[1] - completionRatio[0]) * atpu; // estimated time remaining
									if (verbose) System.out.println("\t*EstimatedTimeRemaining=" + etr + ", cr=" + completionRatio[0] + "/" + completionRatio[1] + ", atpu=" + atpu);
									if (System.currentTimeMillis() + etr < expirationTime) {
										// wait for about the right time to pick up the results.
										try {
											if (verbose) System.out.println("\t*Sleeping=" + etr);
											Thread.sleep((long)Math.floor(etr));
										}
										catch (InterruptedException e) {
											break;
										}
									}
									else {
										// if there isn't time to wait for completion, then skip out now.
										timeToQuit = true;
										break;
									}
								}
							}
            }
          }

          if (gotResponse) responseCount.incrementAndGet();
        }
      }

			clearBusy();
				
      return result;
    }

    public boolean matchesNode(String nodeName) {
      return this.nodeName.equals(nodeName);
    }
  }


	/**
	 * Container for the results of a transaction.
	 */
	public static final class TransactionResult {

		/** The message sent for this transaction */
		public final SafeDepositMessage message;

		/** Successful withdrawals for this transaction */
		public final List<SafeDepositBox.Withdrawal> withdrawals;

		/** The query times for this transaction */
		public final StatsAccumulator queryTimes;

		/** The number of nodes that responded with a non-null withdrawal */
		public final int responseCount;

		/** SafeDepositReceipts for the transaction for each responding node. */
		public final Map<String, SafeDepositReceipt> receipts;

		/** Names of nodes that didn't respond. */
		public final List<String> missingResponses;


		public TransactionResult(SafeDepositMessage message,
														 List<SafeDepositBox.Withdrawal> withdrawals,
														 StatsAccumulator queryTimes,
														 AtomicInteger responseCount,
														 Map<String, SafeDepositReceipt> receipts,
														 List<String> missingResponses) {
			this.message = message;
			this.withdrawals = new ArrayList<SafeDepositBox.Withdrawal>(withdrawals);
			this.queryTimes = new StatsAccumulator(queryTimes);
			this.responseCount = responseCount.get();
			this.receipts = new HashMap<String, SafeDepositReceipt>(receipts);
			this.missingResponses = missingResponses;
		}

		/**
		 * Determine whether this result has any withdrawals.
		 */
		public boolean hasWithdrawals() {
			return withdrawals.size() > 0;
		}
	}
}
