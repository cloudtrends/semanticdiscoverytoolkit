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
import java.util.Properties;

import org.sd.cluster.io.Response;
import org.sd.io.DataHelper;
import org.sd.io.Publishable;
import org.sd.util.ReflectUtil;
import org.sd.util.thread.Governable;
import org.sd.util.thread.UnitCounter;

/**
 * A job that wraps a properties-based governable instance.
 * <p>
 * Running a 'Governable' (FOO) as a 'GovernableJob' would look something like:
 * <pre>
 * ./run org.sd.cluster.config.JobAdmin job=org.sd.cluster.job.GovernableJob governable=FOO ...other-property-defs...
 * </pre>
 * Note that required properties are:
 * <ul>
 * <li>governable -- Classpath for the Governable to run.</li>
 * <li>jobId -- An ID for the job.</li>
 * <li>groupName -- Identify target cluster nodes to run the job.</li>
 * <li>dataDirName -- Identifier for the job output data location.</li>
 * <li>...all properties required by the 'Governable' implementation...</li>
 * </ul>
 * Optional properties are:
 * <ul>
 * numThreads -- Default of 1 is generally appropriate for Governables. Override with care!
 * verbose -- On by default, turn off with "false"
 * checkInterval -- 50 (ms) by default gives the UnitCounter sleep time,
 *                  between which state is monitored
 * </ul>
 *
 * @author Spence Koehler
 */
public class GovernableJob extends AbstractCountingJob {
  
  private Properties properties;
  private Governable _governable;    // constructed instance.

  /**
   * Default constructor for Publishable reconstruction.
   */
  public GovernableJob() {
    super();
    this.properties = null;
  }

  /**
   * Construct with the given properties (passed through to the Governable's
   * construction as well as up the line to Job superclass initializations).
   * <p>
   * Properties:
   * <ul>
   * <li>governable -- (required) The classpath for the governable instance to
   *                   create and run.</li>
   * </ul>
   */
  public GovernableJob(Properties properties) {
    super(properties);
    this.properties = properties;

    if (properties == null || properties.getProperty("governable") == null) {
      throw new IllegalArgumentException("Must define 'governable' property!");
    }
  }

  public String getDescription() {
    return _governable != null ? _governable.getClass().getName() : this.getClass().getName();
  }

  /**
   * Do pre-start initializations including building the unit counter instance
   * (called from startHandlingHook).
   *
   * @return the UnitCounter to monitor or 'null' to abort starting.
   */
  protected UnitCounter initializeForStart() {
    final String governable = properties.getProperty("governable");
    if (governable == null) {
      throw new IllegalStateException("'governable' property not defined!");
    }
    this._governable = (Governable)ReflectUtil.buildInstance(governable, properties);

    if (_governable == null) {
      throw new IllegalStateException("'governable' (" + governable + ") couldn't be constructed!");
    }

    return _governable.getUnitCounter();
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
    // Note: this monitors the unit counter, but not JobStatus so job level
    //       'interruption' will NOT work, while 'pause' and 'stop' will.
    this._governable.run();

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
    DataHelper.writeProperties(dataOutput, properties);
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
    this.properties = DataHelper.readProperties(dataInput);
  }
}
