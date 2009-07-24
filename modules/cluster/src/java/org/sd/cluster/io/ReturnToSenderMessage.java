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
package org.sd.cluster.io;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A message that is sent to a node to perform an asynchronous operation and
 * then send a response back to the sender in the form of another message.
 * <p>
 * @author Spence Koehler
 */
public abstract class ReturnToSenderMessage implements Message {
  
  /**
   * Empty constructor for publishable reconstruction.
   */
  public ReturnToSenderMessage() {
  }

  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
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
  }

  /**
   * Get this message's response to be returned by the server to the client.
   * <p>
   * NOTE: this response is returned synchronously from a server to the client
   *       after receiving a message. The message as received on the server
   *       is handled in its own thread later.
   *
   * @param serverContext  The context of the server responding to this message.
   */
  public Message getResponse(Context serverContext) {
//todo: send as the response a key that will allow the result sent as a
//  message to be recognized as the result of this message.
//
// need to send in the response an (tracking) identifier that will be used to track this message
// and that will identify it in the message sent in return.
//
    return null;
  }

  /**
   * Handle this message on the server.
   * <p>
   * NOTE: The message received on a server is handled asynchronously through this
   *       method after its response has been sent back to the client.
   */
  public void handle(Context context) {
//todo: perform the work and send the result in a message back to the sender.
//  include the tracking identifier.
//note: allow "sender" to be at a (or multiple) forwarding address(es)
  }
}
