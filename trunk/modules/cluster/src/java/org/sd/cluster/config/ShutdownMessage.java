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
package org.sd.cluster.config;


import org.sd.cluster.io.Context;
import org.sd.cluster.io.DirectResponseMessage;
import org.sd.cluster.io.Message;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A message to tell a ClusterNode to shutdown.
 * <p>
 * @author Spence Koehler
 */
public class ShutdownMessage extends DirectResponseMessage {

  private int countdown;

  public ShutdownMessage() {
    this.countdown = 250;  // default
  }

  public ShutdownMessage(int countdown) {
    this.countdown = countdown;
  }

  public int getCountdown() {
    return countdown;
  }

  /**
   * Get this message's response to be returned by the server to the client.
   * <p>
   * NOTE: this response is returned synchronously from a server to the client
   *       after receiving a message. The message as received on the server
   *       is handled in its own thread later.
   */
  public Message getResponse(Context context) {
    new ClusterNodeStopper(context, countdown).start();
    return new StringResponse(context, "shutting down.");
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    dataOutput.writeInt(countdown);
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
    this.countdown = dataInput.readInt();
  }

  private final class ClusterNodeStopper extends Thread {

    private Context context;
    private int countdown;

    public ClusterNodeStopper(Context context, int countdown) {
      super("ShutdownMessage.ClusterNodeStopper");
      this.context = context;
      this.countdown = countdown;
    }

    public void run() {
      final ClusterNode clusterNode = (ClusterNode)context;
  
      if (clusterNode != null) {  // if null, node wasn't started properly.
        if (countdown > 0) {
          try {
            Thread.sleep(countdown);
          }
          catch (InterruptedException e) {
          }
        }

        clusterNode.shutdown(false);
      }
    }
  }
}
