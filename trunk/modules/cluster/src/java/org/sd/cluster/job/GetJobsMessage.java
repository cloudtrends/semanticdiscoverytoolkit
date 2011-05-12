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


import org.sd.cluster.io.ConnectionContext;
import org.sd.cluster.io.Context;
import org.sd.cluster.io.Message;
import org.sd.cluster.config.StringResponse;
import org.sd.cluster.config.ClusterContext;

import org.sd.cluster.io.DirectResponseMessage;

/**
 * A message to get information about all jobs on a cluster node.
 * <p>
 * @author Spence Koehler
 */
public class GetJobsMessage extends DirectResponseMessage {
  
  public GetJobsMessage() {
  }

  /**
   * Get this message's response to be returned by the server to the client.
   * <p>
   * NOTE: this response is returned synchronously from a server to the client
   *       after receiving a message. The message as received on the server
   *       is handled in its own thread later.
   */
  public Message getResponse(Context context, ConnectionContext connectionContext) {
    final ClusterContext clusterContext = (ClusterContext)context;
    return new StringResponse(context, clusterContext.getJobManager().getAllJobStrings());
  }
}
