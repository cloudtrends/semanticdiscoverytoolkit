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
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

import org.sd.cio.MessageHelper;
import org.sd.io.Publishable;

/**
 * Utility class for (low-level) sending and receiving messages.
 * <p>
 * @author Spence Koehler
 */
public class Messenger {

  private DataOutputStream dataOutput;
  private DataInputStream dataInput;

	private long receiveTime;
	private long responseGenTime;
	private long sendTime;

  Messenger(DataOutputStream dataOutput, DataInputStream dataInput) {
    this.dataOutput = dataOutput;
    this.dataInput = dataInput;

		this.receiveTime = -1L;
		this.responseGenTime = -1L;
		this.sendTime = -1L;
  }

  /**
   * Send the given message over the socket and collect the response.
   *
   * @param message  the message to send.
   *
   * @return the handled response (if it was successfully handled) or null if the message
   *         could not be sent or the response could not be received in the given time limit.
   */
  public synchronized Message sendMessage(Message message) throws IOException {
    Message response = null;

    if (message == null) return response;

    // send message through dataOutput
    sendMessage(message, dataOutput);
    dataOutput.flush();

    // wait for response on dataInput
    response = receiveMessage(dataInput);

    return response;
  }

  public synchronized Message receiveMessage(Context serverContext) throws IOException {
    Message message = null;
		final long starttime = System.currentTimeMillis();

    // listen for a message to receive on dataInput
    message = receiveMessage(dataInput);
		final long postReceiveTime = System.currentTimeMillis();
		this.receiveTime = postReceiveTime - starttime;

    // send a response through dataOutput
    Message response = message.getResponse(serverContext);
    if (response == null) response = new NullMessage();
		final long postResponseGenTime = System.currentTimeMillis();
		this.responseGenTime = postResponseGenTime - postReceiveTime;

    try {
      sendMessage(response, dataOutput);
    }
    catch (IOException e) {
      System.err.println(new Date() + ": Messenger.receiveMessage() unable to send response! received=" + message + " response=" + response);
      throw e;
    }
    dataOutput.flush();

		this.sendTime = System.currentTimeMillis() - postResponseGenTime;

    return message;
  }

  /**
   * Send the given message through the given data output.
   */
  public static void sendMessage(Message message, DataOutput dataOutput) throws IOException {
    // write the class and the message
    MessageHelper.writePublishable(dataOutput, message);
  }

  /**
   * Receive the next message on the data input.
   */
  public static Message receiveMessage(DataInput dataInput) throws IOException {
    // read the class and the message
    return (Message)MessageHelper.readPublishable(dataInput);
  }
  
	/**
	 * Get the amount of time (in millis) taken to receive the message.
	 *
	 * @return the time to receive, or -1 if nothing has been received.
	 */
	public long getReceiveTime() {
		return receiveTime;
	}

  
	/**
	 * Get the amount of time (in millis) taken to generate the response.
	 *
	 * @return the time to generate, or -1 if no response has been generated.
	 */
	public long getResponseGenTime() {
		return responseGenTime;
	}

  
	/**
	 * Get the amount of time (in millis) taken to send the response.
	 *
	 * @return the time to send, or -1 if nothing has been sent.
	 */
	public long getSendTime() {
		return sendTime;
	}
}
