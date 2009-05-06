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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.sd.cluster.io.SafeDepositMessage;

/**
 * Base utility for connecting a service to a process controller.
 * <p>
 * @author Spence Koehler
 */
public abstract class ServiceConnector {

  /**
   * Build the process handle for this connector.
   */
  protected abstract ProcessHandle buildProcessHandle(SafeDepositMessage serviceTask,
                                                      long requestTimeout, long withdrawalTimeout,
                                                      boolean verbose);


	public final String serviceID;
	private ProcessCache processCache;
	private boolean leaveInCache;
	private ExecutorService threadPool;

	/**
	 * Properties:
	 * <ul>
	 * <li>serviceID -- (required) Identifier for the service being run.</li>
	 * <li>processCacheSize -- (optional, default=1) Maximum number of
	 *                         simultaneous active processes through this
	 *                         service.</li>
	 * <li>leaveInCache -- (optional, default=false) True to leave process
	 *                     handles in the cache, even after error or done.</li>
	 * </ul>
	 */
	public ServiceConnector(Properties properties) {
		this.serviceID = properties.getProperty("serviceID");
		if (serviceID == null) {
			throw new IllegalArgumentException("Must define 'serviceID'!");
		}

		final int processCacheSize = Integer.parseInt(properties.getProperty("processCacheSize", "1"));
		this.processCache = new ProcessCache(processCacheSize);

		this.leaveInCache = "true".equalsIgnoreCase(properties.getProperty("leaveInCache", "false"));

		final AtomicInteger processThreadId = new AtomicInteger(0);
		this.threadPool = Executors.newCachedThreadPool(new ThreadFactory() {
				public Thread newThread(Runnable r) {
					return new Thread(r, serviceID + "-ServiceProcess-" + processThreadId.getAndIncrement());
				}
			});
	}

	/**
	 * Close this connector.
	 */
	public void close() {
		threadPool.shutdownNow();
	}

	/**
	 * Retrieve or create the process handle for the service task.
	 * 
	 * @return the handle to the running process.
	 */
	public ProcessHandle getProcessHandle(SafeDepositMessage serviceTask,
																				long requestTimeout, long withdrawalTimeout,
																				boolean verbose) {

		ProcessHandle result = null;
		final String serviceKey = serviceTask.getKey();

		// retrieve from cache
		synchronized (processCache) {
			result = processCache.get(serviceKey);
			if (result == null) {  // need to create one
        result = buildProcessHandle(serviceTask, requestTimeout, withdrawalTimeout, verbose);
				threadPool.execute(result);            // submit to thread pool
				processCache.put(serviceKey, result);  // add to cache
			}
		}

		// remove from cache if errored or finished
		if (!leaveInCache && result != null &&
				(result.getError() != null || result.finished())) {
			processCache.remove(serviceKey);
		}

		return result;
	}
}
