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
package org.sd.cluster.job;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.sd.cluster.config.IntegerResponse;
import org.sd.cluster.io.Response;
import org.sd.io.Publishable;
import org.sd.util.Timer;
import org.sd.util.thread.UnitCounter;

/**
 * A job that counts, intended to be used as a reference implementation and
 * for demonstrating and/or testing job management.
 * <p>
 * @author Spence Koehler
 */
public class CountingJob extends AbstractCountingJob {
	
	private long countTo;        // how high to count to
	private long countWait;      // how long to wait before incrementing the count

	private Timer _timer;     // timer to keep us on schedule for sleeping between counts

	/**
	 * Default constructor for Publishable reconstruction.
	 */
	public CountingJob() {
		super();
	}

	/**
	 * Construct with the given properties.
	 * <p>
	 * Properties:
	 * <ul>
	 * <li>countTo -- (optional, default=1 billion) How high to count before
	 *                finished.</li>
	 * <li>countWait -- (optional, default=1000) How many millis to wait before
	 *                  incrementing the count.</li>
	 * <li>checkInterval -- (optional, default=50) How many millis to wait
	 *                      between checking for events while sleeping.
	 * <li>verbose -- (optional, default=true) Verbosity flag.
	 * </ul>
	 */
	public CountingJob(Properties properties) {
		super(properties);

		this.countTo = Long.parseLong(properties.getProperty("countTo", "1000000000"));
		this.countWait = Long.parseLong(properties.getProperty("countWait", "1000"));
	}

  public String getDescription() {
		final StringBuilder result = new StringBuilder();
    result.
			append(this.getClass().getName()).
			append(" (to=").append(countTo).
			append(",wait=").append(countWait).
			append(",chk=").append(checkInterval).
			append(")");
		return result.toString();
  }

	/**
	 * Get a string representation of this instance.
	 * <p>
	 * Note that this is returned as a response to this job's status by the
	 * JobManager.
	 */
	public String toString() {
		final StringBuilder result = new StringBuilder();

		result.append(getDescription());

		if (_uc != null) {
			result.append("\n  ").append(_uc.toString());
		}
		else {
			result.append("  INIT");
		}

		return result.toString();
	}

	/**
	 * Operate on the given request.
	 * <p>
	 * For this job, we will return the current count.
	 */
  public Response operate(Publishable request) {
//todo: change 'countTo', 'countWait', 'checkInterval', or 'verbose'
    return new IntegerResponse(getClusterContext(), (int)(_uc.doneSoFar()));
  }

	/**
	 * Do pre-start initializations including building the unit counter instance
	 * (called from startHandlingHook).
	 *
	 * @return the UnitCounter to monitor or 'null' to abort starting.
	 */
	protected UnitCounter initializeForStart() {
		this._timer = new Timer(countWait, new Date());
		return new UnitCounter(countTo);
  }

	/**
	 * Execute the next processing operation for the UnitCounter, _uc.
	 * <p>
	 * NOTE: This is called from within the loop that monitors and increments
	 * the UnitCounter, _uc.
	 * <p>
	 * If the processing is length, the implementation should monitor interruptions:
	 * <ul>
	 * <li>_uc.interrupted()</li>
	 * <li>getStatus() == JobStatus.INTERRUPTED</li>
	 * </ul>
	 *
	 * @return true to continue the counter loop (essentially ending the job's
	 *         thread) or false to break out. Note that if false, the counter
	 *         will not be incremented and the job's status will be set to
	 *         FINISHED (if the counter has ended) or to PAUSED.
	 */
	protected boolean doProcessing() {
		// do unit processing (which is to wait for time to inc count)
		while (!_timer.reachedTimerMillis() &&
					 !_uc.interrupted() &&
					 getStatus() != JobStatus.INTERRUPTED) {
			try {
				Thread.sleep(checkInterval);
			}
			catch (InterruptedException e) {
				break;
			}
		}
		return true;
	}

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
		super.write(dataOutput);

		dataOutput.writeLong(countTo);
		dataOutput.writeLong(countWait);
  }

  /**
   * Read this message's contents from the dataInput stream that was written by
   * this.write(dataOutput).
   * <p>
   * NOTE: this requires all implementing classes to have a default constructor
   *       with no args.
   *
   * @param dataInput  the data output to write to.
   */
  public void read(DataInput dataInput) throws IOException {
		super.read(dataInput);

		this.countTo = dataInput.readLong();
		this.countWait = dataInput.readLong();
  }

	//
	//sample commands to run (from 'bin' directory adjacent to a project's 'build' directory):
	//
	// # start a single local cluster node for testing
	// ./startClusterNode -1
	// # check that the node is alive
	// ./admin -a
	// # start the counting job
	// ./run org.sd.cluster.job.JobManager job=org.sd.cluster.job.CountingJob jobId=CountingJob groupName=all dataDirName=counts countTo="300" countWait="1000" checkInterval="100"
	// # periodically 'ping' to watch its progress
	// ./admin -j
	//
	// # optionally enter jobadmin interactive shell to affect the job
  // ./jobadmin
	// # get the list of possible commands
	// help
	// # get the list of running jobs
	// jobs
	// # use the infor from 'jobs' output to target the counting job ("node" and "lid")
	// pause -n <node-0> -j <lid>
	// jobs
	// jobs
	// resume -n <node-0> -j <lid>
	// jobs
	// jobs
	// quit
	//
	// # stop/kill the job/cluster
	// ./admin -k
	//
	// # NOTE: When code is (re)deployed over a running cluster instance "admin -k" fails as the underlying class environment has changed. In these cases, the cluster must be killed explicitly:
	// killall -9 java
	//
}
