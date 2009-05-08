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


import java.util.Properties;

import org.sd.cluster.io.SafeDepositMessage;

/**
 * A service wrapper for communicating with a cluster using the SafeDepositBox
 * abstraction.
 * <p>
 * @author Spence Koehler
 */
public class ClusterService {

	private long requestTimeout;
	private long withdrawalTimeout;
	private boolean verbose;
	private ClusterServiceConnector serviceConnector;
	
	/**
	 * Properties:
	 * <ul>
	 * <li>requestTimeout -- (optional, default=20000) Request/response timeout
	 *                       to use in serial console interactions to each
	 *                       cluster node through SafeDepositAgent.
	 * <li>withdrawalTimeout -- (optional, default=60000) Cumulative timeout
	 *                          across all SafeDepositAgent cluster node requests.
	 *                          Note that under usual circumstances, node requests
	 *                          succeed within milliseconds. These timeouts, when
	 *                          reached, typically indicate node outages or
	 *                          failures.
	 * <li>vebose -- (optional, default=false) true to show more debugging output.
	 * <li>ClusterServiceConnector properties.</li>
	 * </ul>
	 */
	public ClusterService(Properties properties) {
		this.requestTimeout = Long.parseLong(properties.getProperty("requestTimeout", "20000"));
		this.withdrawalTimeout = Long.parseLong(properties.getProperty("withdrawalTimeout", "60000"));
		this.verbose = "true".equalsIgnoreCase(properties.getProperty("verbose", "false"));
		this.serviceConnector = new ClusterServiceConnector(properties);
	}

	/**
	 * Submit the task through the service connector with default timeouts.
	 */
	public ProcessHandle submit(SafeDepositMessage serviceTask) {
		return submit(serviceTask, requestTimeout, withdrawalTimeout, verbose);
	}

	/**
	 * Submit the task through the service connector with the given timeouts.
	 */
	public ProcessHandle submit(SafeDepositMessage serviceTask,
															long requestTimeout, long withdrawalTimeout,
															boolean verbose) {
		return serviceConnector.getProcessHandle(serviceTask, requestTimeout, withdrawalTimeout, verbose);
	}

	/**
	 * Resubmit the processHandle for processing.
	 *
	 * @param processHandle  The processHandle to resubmit.
	 *
	 * @return true if successfully resubmitted; otherwise, false.
	 */
	public boolean resubmit(ProcessHandle processHandle) {
		return serviceConnector.resubmit(processHandle);
	}

	/**
	 * Shutdown the resources associated with this service.
	 */
	public void close() {
		serviceConnector.close();
	}

	//
	// ClusterService usage example:
	//
	// Properties:
	//  serviceID=MyClusterService
	//  requestTimeout=2000
	//  withdrawalTimeout=5000
	//  processCacheSize=<max-simultaneous-tasks>
	//  leaveInCache=false
	//  clusterGateway=localhost
	//  #clusterDefDir=
	//  clusterDef=user 1m1n.1 machine
	//  nodesToContact=all
	//
	//  final ClusterService clusterService = new ClusterService(properties);
	//
	//  final SafeDepositMessage serviceTask = new ...
	//  ProcessHandle processHandle = null;
	//  try {
	//    processHandle = clusterService.submit(serviceTask);
	//
	//    // wait until process is finished, checking periodically for status
	//    while (!processHandle.finished()) {
	//      final long[] completionRatio = processHandle.getCompletionRatio();
	//      if (completionRatio != null) {
	//        // show progress, take into account special cases w/ratio values
	//      }
	//    }
	//
	//    // check/collect results from finished process
	//    if (processHandle.getError() != null) {
	//      // do error handling
	//    }
	//    else if (processHandle.hasResults()) {
	//      final ServiceResults serviceResults = processHandle.getResults();
	//      final List<Publishable> taskResults = serviceResults.getAllResults();
	//      // handle task results
	//    }
	//    else {
	//      // no results available.
	//    }
	//  }
	//  finally {
	//    // release the process handle resources
	//    if (processHandle != null) processHandle.close();
	//  }
	//
	//  clusterService.close();  // release the cluster service resources
	//
}
