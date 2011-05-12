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
import org.sd.cluster.config.BooleanResponse;
import org.sd.cluster.io.ConnectionContext;
import org.sd.cluster.io.Context;
import org.sd.cluster.io.Message;

import java.io.DataInput;
import java.io.DataOutput;

import java.io.IOException;

/**
 * A message containing e-mail information that is sent to a post office
 * for collection and distribution by e-mail.
 * <p>
 * @author Spence Koehler
 */
public class PostOfficeMessage implements Message {
  
  private static PostOffice _postOffice;
  private static final Object _POST_OFFICE_MUTEX = new Object();

  private static final PostOffice getPostOffice(Context context) {
    if (_postOffice == null) {
      synchronized (_POST_OFFICE_MUTEX) {
        if (_postOffice == null) {
          _postOffice = new PostOffice(context);
        }
      }
    }
    return _postOffice;
  }

  private String batchID;             // for pulling messages into a batch (if null, send right away)
  private String destEmailAddresses;  // comma-delimited list of user@address
  private String sender;              // of form user@machine-jvmNum
  private String subject;             // a subject for this message
  private String message;             // this message's content
  private boolean html;               // true if the content is html-formatted

  /**
   * Default constructor for publishable reconstruction.
   */
  public PostOfficeMessage() {
  }

  public PostOfficeMessage(String batchID, String destEmailAddresses, String sender, String subject, String message, boolean html) {
    this.batchID = batchID;
    this.destEmailAddresses = destEmailAddresses;
    this.sender = sender;
    this.subject = subject;
    this.message = message;
    this.html = html;
  }

  /**
   * Get this message's batch ID.
   */
  public String getBatchID() {
    return batchID;
  }

  /**
   * Get this message's dest email addresses.
   */
  public String getDestEmailAddresses() {
    return destEmailAddresses;
  }

  /**
   * Get this message's sender.
   */
  public String getSender() {
    return sender;
  }

  /**
   * Get this message's subject.
   */
  public String getSubject() {
    return subject;
  }

  /**
   * Get this message's content.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Determine whether message content is formatted in html.
   */
  public boolean isHtml() {
    return html;
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
  public Message getResponse(Context serverContext, ConnectionContext connectionContext) {
    return new BooleanResponse(serverContext, !getPostOffice(serverContext).isDisabled());
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
  public void handle(Context serverContext, ConnectionContext connectionContext) {
    final PostOffice postOffice = getPostOffice(serverContext);
    if (!postOffice.isDisabled()) {
      postOffice.submit(this);
    }
  }

  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    MessageHelper.writeString(dataOutput, batchID);
    MessageHelper.writeString(dataOutput, destEmailAddresses);
    MessageHelper.writeString(dataOutput, sender);
    MessageHelper.writeString(dataOutput, subject);
    MessageHelper.writeString(dataOutput, message);
    dataOutput.writeBoolean(html);
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
    this.batchID = MessageHelper.readString(dataInput);
    this.destEmailAddresses = MessageHelper.readString(dataInput);
    this.sender = MessageHelper.readString(dataInput);
    this.subject = MessageHelper.readString(dataInput);
    this.message = MessageHelper.readString(dataInput);
    this.html = dataInput.readBoolean();
  }
}
