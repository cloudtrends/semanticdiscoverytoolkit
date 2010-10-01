/*
    Copyright 2010 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
import org.sd.cio.MessageHelper;

/**
 * Wrapper around a message whose publishable data bytes are known.
 * <p>
 * This can be used on the "client" side of a transaction to send a message
 * to a server given that message's serialized bytes and not necessarily
 * having an instance of or access to the message's class on the client.
 *
 * @author Spence Koehler
 */
public class MessageWrapper implements Message {
  
  private String messageClassName;
  private byte[] messageBytes;

  private transient Message wrappedMessage;

  /**
   * Empty constructor for publishable reconstruction.
   */
  public MessageWrapper() {
  }

  /**
   * Construct with the given wrapped class data.
   * 
   * @param messageClassName is the wrapped message's class.getName()
   * @param messageBytes is the wrapped message's publishable bytes
   *                     (see MessageHelper.serialize)
   */
  public MessageWrapper(String messageClassName, byte[] messageBytes) {
    this.messageClassName = messageClassName;
    this.messageBytes = messageBytes;
  }

  /**
   * Set the wrapped message on this instance (expert).
   */
  public void setWrappedMessage(Message wrappedMessage) {
    this.wrappedMessage = wrappedMessage;
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
    return wrappedMessage == null ? null : wrappedMessage.getResponse(serverContext);
  }

  /**
   * Handle this message on the server.
   * <p>
   * NOTE: The message received on a server is handled asynchronously through this
   *       method after its response has been sent back to the client.
   *
   * @param serverContext  The context of the server on which this message is
   *                       being handled.
   */
  public void handle(Context serverContext) {
    if (wrappedMessage != null) wrappedMessage.handle(serverContext);
  }

  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    if (this.wrappedMessage != null) {
      MessageHelper.writePublishable(dataOutput, wrappedMessage);
    }
    else if (messageClassName != null && messageBytes != null) {
      dataOutput.writeBoolean(true);
      MessageHelper.writeString(dataOutput, messageClassName);
      dataOutput.write(messageBytes);
    }
    else {
      dataOutput.writeBoolean(false);
    }
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
    this.wrappedMessage = (Message)MessageHelper.readPublishable(dataInput);
  }
}
