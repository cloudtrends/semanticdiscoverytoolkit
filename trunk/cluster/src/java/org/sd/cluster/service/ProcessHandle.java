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


import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.sd.io.Publishable;

/**
 * Runnable container for sending, monitoring, and collecting the service
 * task's results.
 * <p>
 * @author Spence Koehler
 */
public interface ProcessHandle extends Runnable {

	/**
	 * Get a unique key that identifies this handle and distinguishes it from
	 * others.
	 */
	public String getServiceKey();

	/**
	 * Get an error from the process if present.
	 */
	public Throwable getError();
	
	/**
	 * Determine whether the underlying process has finished.
	 */
	public boolean finished();

	/**
	 * Reset finished from "true" to "false".
	 *
	 * @return true if successfully reset; otherwise, false.
	 */
	public boolean resetFinished();

  /**
   * Send this handle's process a kill signal.
   */
	public void kill();

	/**
	 * Determine whether this handle has retrievable results.
	 */
	public boolean hasResults();

	/**
	 * Get the results from this handle if processing is finished and results
	 * are available.
	 *
	 * @return the results or null.
	 */
	public ServiceResults getResults();

	/**
	 * Get the time this handle used for processing.
	 */
	public long getProcessingTime();

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
  public long[] getCompletionRatio();

	/**
	 * Close this process handle (when finished).
	 */
	public void close();

	/**
	 * Wait until the process has finished or a signal to die has been issued,
	 * returning the current results.
	 *
	 * @param checkInterval  Amount of time to wait between checking for termination.
	 * @param die  Flag to monitor for signal to end (ok if null).
	 *
	 * @return non-null ServiceResults, possibly incomplete.
	 */
	public ServiceResults runUntilDone(long checkInterval, AtomicBoolean die);
}
