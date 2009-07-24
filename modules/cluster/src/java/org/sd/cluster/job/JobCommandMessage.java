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
import org.sd.cluster.io.DirectResponseMessage;
import org.sd.cluster.io.Message;
import org.sd.cluster.config.ClusterContext;
import org.sd.io.Publishable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A message for sending a command to a job.
 * <p>
 * @author Spence Koehler
 */
public class JobCommandMessage extends DirectResponseMessage {
  
  private JobCommand jobCommand;
  private LocalJobId localJobId;
  private Publishable payload;  // payload to accompany the command

  public JobCommandMessage() {
  }

  /**
   * Construct with the given job command and local job id.
   *
   * @param jobCommand  the job command to send
   * @param localJobId  the target job for receiving the command. (all jobs if null).
   */
  public JobCommandMessage(JobCommand jobCommand, LocalJobId localJobId) {
    this.jobCommand = jobCommand;
    this.localJobId = localJobId;
    this.payload = null;
  }

  /**
   * Construct with the given job command, local job id, and payload.
   *
   * @param jobCommand  the job command to send
   * @param localJobId  the target job for receiving the command. (all jobs if null).
   * @param payload the payload accompanying the command (usually null).
   */
  public JobCommandMessage(JobCommand jobCommand, LocalJobId localJobId, Publishable payload) {
    this(jobCommand, localJobId);
    this.payload = payload;
  }

  /**
   * Get this message's job command.
   */
  public JobCommand getJobCommand() {
    return jobCommand;
  }

  /**
   * Get this message's local job id.
   */
  public LocalJobId getLocalJobId() {
    return localJobId;
  }

  /**
   * Get this message's payload.
   */
  public Publishable getPayload() {
    return payload;
  }

  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);

    final String jobCommandName = jobCommand == null ? null : jobCommand.name();
    MessageHelper.writeString(dataOutput, jobCommandName);
    MessageHelper.writePublishable(dataOutput, localJobId);
    MessageHelper.writePublishable(dataOutput, payload);
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

    final String jobCommandName = MessageHelper.readString(dataInput);
    if (jobCommandName != null) {
      this.jobCommand = Enum.valueOf(JobCommand.class, jobCommandName);
    }
    this.localJobId = (LocalJobId)MessageHelper.readPublishable(dataInput);
    this.payload = MessageHelper.readPublishable(dataInput);
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
    return clusterContext.getJobManager().handleJobCommand(context, jobCommand, localJobId, payload, null);
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.append("JobCommandMessage: ").append(jobCommand).append(" to ").append(localJobId).
      append(" <").append(payload).append('>');

    return result.toString();
  }
}
