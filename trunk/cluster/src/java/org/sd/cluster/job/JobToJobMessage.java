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


import org.sd.cio.MessageHelper;
import org.sd.cluster.io.Context;
import org.sd.cluster.io.Message;
import org.sd.cluster.config.ClusterContext;
import org.sd.io.Publishable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A job command message between two nodes' jobs.
 * <p>
 * @author Spence Koehler
 */
public class JobToJobMessage extends JobCommandMessage {

  private LocalJobId senderID;

  public JobToJobMessage() {
  }

  public JobToJobMessage(LocalJobId senderID, LocalJobId recipientID, JobCommand jobCommand, Publishable payload) {
    super(jobCommand, recipientID, payload);
    this.senderID = senderID;
  }

  /**
   * Get the sender of this message.
   * <p>
   * NOTE: To get the recipient, use getLocalJobId().
   */
  public LocalJobId getSenderID() {
    return senderID;
  }

  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);
    MessageHelper.writePublishable(dataOutput, senderID);
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
    this.senderID = (LocalJobId)MessageHelper.readPublishable(dataInput);
  }

  /**
   * Get this message's response to be returned by the server to the client.
   * <p>
   * NOTE: this response is returned synchronously from a server to the client
   *       after receiving a message. The message as received on the server
   *       is handled in its own thread later.
   */
  public Message getResponse(Context context) {
    final ClusterContext clusterContext = (ClusterContext)context;
    return clusterContext.getJobManager().handleJobCommand(context, getJobCommand(), getLocalJobId(), getPayload(), senderID);
  }
}
