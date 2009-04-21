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
 * A job whose processing is monitored/controlled through a UnitCounter instance.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractCountingJob extends AbstractJob {

	/**
	 * Do pre-start initializations including building the unit counter instance
	 * (called from startHandlingHook).
	 *
	 * @return the UnitCounter to monitor or 'null' to abort starting.
	 */
	protected abstract UnitCounter initializeForStart();

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
	protected abstract boolean doProcessing();


	//
	// Extending classes need:
	// - an empty constructor that calls super()
	// - a constructor with a properties argument that calls super()
	// - superclass implementations:
  //     public String getDescription()
	//
	// If the implementation has state, then override and call super of:
	//   void write(DataOutput dataOutput) throws IOException
	//   void read(DataInput dataInput) throws IOException
	//
	// Extending classes may want to override:
	//     public Response operate(Publishable request)
	//     public String toString()
	//

	// persisted state variables (read/write)
	protected boolean verbose;
	protected long checkInterval;  // how long to wait between checking for events

	// transient variables (computed at processing time)
	protected transient UnitCounter _uc;     // a UnitCounter to monitor/control counting

	/**
	 * Default constructor for Publishable reconstruction.
	 */
	protected AbstractCountingJob() {
		super();
	}

	/**
	 * Construct with the given properties.
	 * <p>
	 * Properties:
	 * <ul>
	 * <li>verbose -- (optional, default=true) Verbosity flag.
	 * <li>checkInterval -- (optional, default=50) How many millis to wait
	 *                      between checking for events while paused.
	 * </ul>
	 */
	protected AbstractCountingJob(Properties properties) {
		super(properties);

		this.verbose = "true".equalsIgnoreCase(properties.getProperty("verbose", "true"));
		this.checkInterval = Long.parseLong(properties.getProperty("checkInterval", "50"));
	}

	/**
	 * Get the unit counter, possibly null if requested before running.
	 */
	public UnitCounter getUnitCounter() {
		return _uc;
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
    return new IntegerResponse(getClusterContext(), (int)(_uc.doneSoFar()));
  }

  /**
   * Hook called when we start handling the job.
   * <p>
   * A common operation is to initialize the totalOps.
   *
   * @return true to start handling right away; false to delay handling
   *         until manually started.
   */
  protected boolean startHandlingHook() {
		this._uc = initializeForStart();
    return (this._uc != null);
  }

  public void start() {  // start processing job's work.
		setStatus(JobStatus.RUNNING);

		// start counting!
		switch (_uc.getStatus()) {
			case IDLE :
				_uc.markStartNow(); break;
			case PAUSED :
				_uc.resume(); break;

			// else, _uc.hasEnded() will be true and loop won't execute.
		}
		
		while (!_uc.hasEnded() && getStatus() == JobStatus.RUNNING) {
			if (_uc.isTimeToQuit()) {  // checks for die and waits for pause to resume
				break;
			}

			// do unit processing
			if (!doProcessing()) {
				break;  // time to quit!
			}

			if (!_uc.inc(true)) {
				break;
			}
		}

		if (getStatus() == JobStatus.INTERRUPTED) {
			// treat interruptions like a pause
			pause();
		}

		setStatus(_uc.hasEnded() ? JobStatus.FINISHED : JobStatus.PAUSED);
  }

  public void stop() {  // end processing job in this jvm; could still suspend? can't resume.
		if (_uc != null) {
			this._uc.kill();
			if (verbose) System.out.println(new Date() + ": CountingJob killed at " + _uc.doneSoFar());
		}
  }

  public void pause() {  // pause a job in this jvm to be resumed.
		if (_uc != null) {
			this._uc.pause(checkInterval);
			if (verbose) System.out.println(new Date() + ": CountingJob paused at " + _uc.doneSoFar());
		}
  }

  public void resume() {  // resume paused job.
		if (_uc != null) {
			if (verbose) System.out.println(new Date() + ": CountingJob resuming from " + _uc.doneSoFar());
			this._uc.resume();
			if (getStatus() != JobStatus.RUNNING) start();
		}
  }

  public void suspend() {  // suspend to disk for restoration through JobManager in another jvm
		if (_uc != null) {
			System.err.println("Suspend not available in CountingJob! ... pausing instead.");
			pause();
		}
  }

  public void shutdown(boolean now) {  // called when cluster is being shutdown
		if (_uc != null) {
			stop();
		}
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
		super.write(dataOutput);
		dataOutput.writeBoolean(verbose);
		dataOutput.writeLong(checkInterval);
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
		this.verbose = dataInput.readBoolean();
		this.checkInterval = dataInput.readLong();
  }
}
