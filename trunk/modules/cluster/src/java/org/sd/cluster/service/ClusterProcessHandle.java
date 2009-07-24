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
package org.sd.cluster.service;


import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sd.cluster.io.SafeDepositAgent;
import org.sd.cluster.io.SafeDepositMessage;
import org.sd.cluster.io.SafeDepositReceipt;
import org.sd.io.Publishable;
import org.sd.util.StatsAccumulator;

/**
 * Runnable container for sending, monitoring, and collecting the service
 * task's results.
 * <p>
 * @author Spence Koehler
 */
public class ClusterProcessHandle implements ProcessHandle {

  private String serviceKey;
  private ProcessController controller;
  private SafeDepositMessage serviceTask;
  private ServiceResults results;
  private long requestTimeout;
  private long withdrawalTimeout;
  private boolean verbose;
  private long resultTime;

  private AtomicBoolean finished = new AtomicBoolean(false);
  private Throwable error;
  private AtomicBoolean running = new AtomicBoolean(false);
  private AtomicBoolean die = new AtomicBoolean (false);

  public ClusterProcessHandle(String serviceKey,
                              ProcessController controller,
                              SafeDepositMessage serviceTask,
                              long requestTimeout, long withdrawalTimeout,
                              boolean verbose) {
    this.serviceKey = serviceKey;
    this.controller = controller;
    this.serviceTask = serviceTask;
    this.results = new ServiceResults(serviceTask);
    this.requestTimeout = requestTimeout;
    this.withdrawalTimeout = withdrawalTimeout;
    this.verbose = verbose;
    this.resultTime = 0L;
  }

  /**
   * Send the service task to the server.
   */
  public void run() {
    if (running.compareAndSet(false, true)) {
      try {
        final long t1 = System.currentTimeMillis();

        if (verbose) {
          System.out.println(new Date() + ": ClusterProcessHandle sending '" + controller.serviceID +"'");
        }

        // send service task message through controller
        final List<SafeDepositAgent.TransactionResult> responses =
          controller.doProcessing(serviceTask, requestTimeout, withdrawalTimeout, verbose);

        if (verbose) {
          System.out.println(new Date() + ": ClusterProcessHandle received " +
                             (responses == null ? 0 : responses.size()) +
                             " responses for '" + controller.serviceID + "'");
        }

        this.results.clear();

        if (responses != null) {
          for (SafeDepositAgent.TransactionResult response : responses) {
            results.add(response);
          }
        }

        final long t2 = System.currentTimeMillis();
        this.resultTime = t2 - t1;
      }
      catch (Throwable t) {
        this.error = t;

        System.err.println(new Date() + ": ClusterProcessHandle '" +
                           controller.serviceID + "' encountered unexpected Exception: " +
                           t.toString());
        t.printStackTrace(System.err);
      }
      finally {
        running.set(false);
        finished.set(true);
      }
    }
  }

  /**
   * Get a unique key that identifies this handle and distinguishes it from
   * others.
   */
  public String getServiceKey() {
    return serviceKey;
  }

  /**
   * Get an error from the process if present.
   */
  public Throwable getError() {
    return error;
  }
  
  /**
   * Determine whether the underlying process has finished.
   */
  public boolean finished() {
    return finished.get();
  }

  /**
   * Send this handle's process a kill signal.
   */
  public void kill() {
    die.set(true);
  }

  /**
   * Determine whether this handle has retrievable results.
   */
  public boolean hasResults() {
    return finished() && results.getNumResults() > 0;
  }

  /**
   * Get the results from this handle if processing is finished and results
   * are available.
   *
   * @return the results or null.
   */
  public ServiceResults getResults() {
    return finished() ? results : null;
  }

  /**
   * Reset finished from "true" to "false".
   *
   * @return true if successfully reset; otherwise, false.
   */
  public boolean resetFinished() {
    return finished.compareAndSet(true, false);
  }

  /**
   * Get the time this handle used for processing.
   */
  public long getProcessingTime() {
    return resultTime;
  }

  /**
   * Get the ratio of completion.
   * <ul>
   * <li>doneSoFar -- the total number of units of work done so far or -1 if
   *                  counting has not been started.</li>
   * <li>toBeDone -- the total number of units of work to be done or -1 if
   *                 unknown.</li>
   * </ul>
   *
   * @return {doneSoFar, toBeDone}
   */
  public long[] getCompletionRatio() {
    final long[] result = new long[]{0L, 0L};

//todo: check/test the logic of this method...
//todo: add in getting/returning sdReceipt.getAverageTimePerUnit()

    int numUnknownTxns = 0;
    int numUnknownNodes = 0;
    final StatsAccumulator txnToBeDoneStats = new StatsAccumulator("txnToBeDone");
    final StatsAccumulator nodeToBeDoneStats = new StatsAccumulator("nodeToBeDone");

    for (SafeDepositAgent.TransactionResult txnResult : results.getTxnResults()) {
      if (txnResult.receipts == null) {
        // count unknown toBeDone
        ++numUnknownTxns;
      }
      else {
        long txnToBeDone = 0L;   // total to be done for this transaction

        for (SafeDepositReceipt sdReceipt : txnResult.receipts.values()) {
          final long[] completionRatio = sdReceipt.getCompletionRatio();
          if (completionRatio == null) {
            ++numUnknownNodes;
          }
          else {
            if (completionRatio[0] > 0) {
              result[0] += completionRatio[0];
            }
            // else unknown doneSoFar doesn't count

            if (completionRatio[1] > 0) {
              nodeToBeDoneStats.add(completionRatio[1]);
              txnToBeDone += completionRatio[1];
            }
            else {  // unknown node toBeDone will use average
              ++numUnknownNodes;
            }
          }
        }

        // add current totals to txnToBeDoneStats
        txnToBeDoneStats.add(txnToBeDone);
      }
    }

    result[1] =
      (long)nodeToBeDoneStats.getSum() +
      (long)Math.ceil(numUnknownTxns * Math.max(1.0, txnToBeDoneStats.getMean())) +
      (long)Math.ceil(numUnknownNodes * Math.max(1.0, nodeToBeDoneStats.getMean()));

    return result;
  }

  /**
   * Close this process handle.
   */
  public void close() {
    if (controller != null) {
      controller.close();
    }
  }

  /**
   * Wait until the process has finished or a signal to die has been issued,
   * returning the current results.
   *
   * @param checkInterval  Amount of time to wait between checking for termination.
   * @param die  Flag to monitor for signal to end (ok if null).
   *
   * @return non-null ServiceResults, possibly incomplete.
   */
  public ServiceResults runUntilDone(long checkInterval, AtomicBoolean die) {

    while (!finished() && (die == null || !die.get())) {
      try {
        Thread.sleep(checkInterval);
      }
      catch (InterruptedException e) {
        // time to go.
        break;
      }
    }

    return results;
  }
}
