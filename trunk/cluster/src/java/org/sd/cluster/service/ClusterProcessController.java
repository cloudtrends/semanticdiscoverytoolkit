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


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.sd.cluster.config.Console;
import org.sd.cluster.io.SafeDepositAgent;
import org.sd.cluster.io.SafeDepositMessage;
import org.sd.util.thread.TimeLimitedThreadPool;

/**
 * Utility for controlling execution of a cluster service task process.
 * <p>
 * @author Spence Koehler
 */
public class ClusterProcessController extends ProcessController {

	final List<ClusterServiceConnector.ConsoleInfo> consoles;
  private transient Integer numNodes;

  private TimeLimitedThreadPool<SafeDepositAgent.TransactionResult> consolePool;
  private SafeDepositAgent[] safeDepositAgents;
  private List<Callable<SafeDepositAgent.TransactionResult>> transactionCallables;

	public ClusterProcessController(String serviceID, List<ClusterServiceConnector.ConsoleInfo> consoles) {
		super(serviceID);
		this.consoles = consoles;
		this.numNodes = null;

    this.consolePool = new TimeLimitedThreadPool<SafeDepositAgent.TransactionResult>(serviceID, consoles.size());
    this.safeDepositAgents = null;
    this.transactionCallables = null;
	}

	public List<SafeDepositAgent.TransactionResult> doProcessing(SafeDepositMessage serviceTask, long responseTimeout, long withdrawalTimeout, boolean verbose) {

		// Create or update safe deposit agents.
		if (safeDepositAgents == null) {
			initialize(serviceTask, responseTimeout, withdrawalTimeout, verbose);
		}
		else {
			for (SafeDepositAgent sdAgent : safeDepositAgents) {
				sdAgent.reset(serviceTask);
			}
		}

		return getClusterResponses(withdrawalTimeout);
	}

	public int getNumResponses() {
		int result = 0;

		if (safeDepositAgents != null) {
			for (SafeDepositAgent sdAgent : safeDepositAgents) {
				result += sdAgent.numResponded();
			}
		}

		return result;
	}

	public int getNumResponders() {
		if (this.numNodes == null && safeDepositAgents != null) {
			this.numNodes = 0;

			for (SafeDepositAgent sdAgent : safeDepositAgents) {
				this.numNodes += sdAgent.getNumNodes();
			}
		}

		return (numNodes == null) ? 0 : numNodes;
	}

	public void close() {
		// close console pool
		consolePool.shutdown();

		// close agents
		if (safeDepositAgents != null) {
			for (SafeDepositAgent safeDepositAgent : safeDepositAgents) {
				safeDepositAgent.close();
			}
		}
	}

  private synchronized void initialize(SafeDepositMessage serviceTask, long responseTimeout, long withdrawalTimeout, boolean verbose) {
    if (safeDepositAgents == null) {
      safeDepositAgents = new SafeDepositAgent[consoles.size()];
      transactionCallables = new ArrayList<Callable<SafeDepositAgent.TransactionResult>>();

      //NOTE: We're assuming the same nodeNames in all consoles
      //      (usually the same group in cluster definitions like "querier")
      int index = 0;
      for (ClusterServiceConnector.ConsoleInfo consoleInfo : consoles) {
        final SafeDepositAgent sdAgent = new SafeDepositAgent(consoleInfo.console, serviceTask, consoleInfo.nodesToContact, 3, responseTimeout, withdrawalTimeout, verbose);
        safeDepositAgents[index++] = sdAgent;
//System.out.println("ServletProcessorController.initilalize(" + (index - 1) + ")");
        transactionCallables.add(new TransactionCallable(sdAgent));
      }
    }
  }

  private final List<SafeDepositAgent.TransactionResult> getClusterResponses(long timeOut) {

    final TimeLimitedThreadPool.ExecutionInfo<SafeDepositAgent.TransactionResult> executionInfo =
      consolePool.execute(transactionCallables, timeOut, true);

		return executionInfo.getComputedResults();
  }

  private static final class TransactionCallable implements Callable<SafeDepositAgent.TransactionResult> {

    private SafeDepositAgent sdAgent;

    TransactionCallable(SafeDepositAgent sdAgent) {
      this.sdAgent = sdAgent;
    }

    public SafeDepositAgent.TransactionResult call() throws Exception {
			return sdAgent.collectWithdrawals();
    }
  }
}
