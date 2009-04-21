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


import org.sd.io.Publishable;

/**
 * Interface for messages sent over streams (i.e. sockets).
 * <p>
 * @author Spence Koehler
 */
public interface Message extends Publishable {

  /**
   * Get this message's response to be returned by the server to the client.
   * <p>
   * NOTE: this response is returned synchronously from a server to the client
   *       after receiving a message. The message as received on the server
   *       is handled in its own thread later.
   *
   * @param serverContext  The context of the server responding to this message.
   */
  public Message getResponse(Context serverContext);

  /**
   * Handle this message on the server.
   * <p>
   * NOTE: The message received on a server is handled asynchronously through this
   *       method after its response has been sent back to the client.
   *
   * @param serverContext  The context of the server on which this message is
   *                       being handled.
   */
  public void handle(Context serverContext);
}
