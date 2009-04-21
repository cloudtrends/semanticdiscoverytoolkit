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


import org.sd.cluster.config.SignedResponse;
import org.sd.cluster.io.Context;
import org.sd.cio.MessageHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A response containing data from probing a job.
 * <p>
 * @author Spence Koehler
 */
public class JobProbeResponse extends SignedResponse {
  
  private JobProbeData jobProbeData;
  private transient JobProbeData _lastJobProbeData;

  public JobProbeResponse() {
    super();

    this.jobProbeData = null;
    this._lastJobProbeData = null;
  }

  public JobProbeResponse(Context context) {
    super(context);

    this.jobProbeData = null;
    this._lastJobProbeData = null;
  }

  public void add(JobProbeData data) {
    if (jobProbeData == null) {
      jobProbeData = data;
    }
    else {
      _lastJobProbeData.setNext(data);
    }
    _lastJobProbeData = data;
  }

  public JobProbeData getJobProbeData() {
    return jobProbeData;
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);
    MessageHelper.writePublishable(dataOutput, jobProbeData);
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
    this.jobProbeData = (JobProbeData)MessageHelper.readPublishable(dataInput);

    // push the nodeName from the signedResponse into the jobProbeData
    jobProbeData.setNodeName(getNodeName());

    for (JobProbeData jpd = jobProbeData; jpd != null; jpd = jpd.getNext()) {
      this._lastJobProbeData = jpd;
    }
  }

  public String toString() {
    return getSignature() + ":\n" + jobProbeData.toString() + '\n';
  }
}
