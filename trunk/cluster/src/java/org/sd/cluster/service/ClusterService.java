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

	private ClusterServiceConnector serviceConnector;
	
	/**
	 * Properties:
	 * <ul>
	 * <li>ClusterServiceConnector properties.</li>
	 * </ul>
	 */
	public ClusterService(Properties properties) {
		this.serviceConnector = new ClusterServiceConnector(properties);
	}

	public ProcessHandle submit(SafeDepositMessage serviceTask) {
		return submit(serviceTask, 20000, 60000, false);
	}

	public ProcessHandle submit(SafeDepositMessage serviceTask,
															long requestTimeout, long withdrawalTimeout,
															boolean verbose) {
		return serviceConnector.getProcessHandle(serviceTask, requestTimeout, withdrawalTimeout, verbose);
	}

	/**
	 * Shutdown the resources associated with this service.
	 */
	public void close() {
		serviceConnector.close();
	}
}
