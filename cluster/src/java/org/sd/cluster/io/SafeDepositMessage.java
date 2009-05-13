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


import org.sd.cio.MessageHelper;
import org.sd.cluster.config.ClusterContext;
import org.sd.io.Publishable;
import org.sd.util.thread.UnitCounter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A message that is sent to a node to perform an asynchronous operation and
 * then to send a response back to the sender upon request through receipt of
 * another message.
 * <p>
 * @author Spence Koehler
 */
public abstract class SafeDepositMessage implements Message {

  /**
   * Do the work of generating contents for the box.
   *
   * @param context  The currently active context.
   * @param uc  A unit counter through which to record progress. Note that the
   *            implementation of this method is responsible to set uc.toBeDone
   *            if possible.
   *
   * @return the generated contents.
   */

  protected abstract Publishable generateContents(ClusterContext context, UnitCounter uc);

  /**
   * Generate a key that uniquely identifies this instance to the
   * SafeDepositBox. Take care to distinguish it from instances of other
   * classes that might share the key.
   *
   * @return a unique key.
   */
  protected abstract String generateSafeDepositKey();


  private Map<String, Long> claimTickets;  // nodeName -> claimTicket
  private boolean closeBox;
  private boolean forceRehandle;
  private long fillTime;

  private transient SafeDepositReceipt _receipt;
  private transient final Object CLAIM_MUTEX = new Object();

	private String _key;

  /**
   * Empty constructor for publishable reconstruction.
   */
  public SafeDepositMessage() {
  }

  /**
   * Construct with the given claim ticket.
   *
   * @param closeBox       flag to close the box after successfully retrieving contents.
   * @param forceRehandle  force the message to be re-handled if the ticket's
   *                       referent has been incinerated; otherwise, status
   *                       will be reported without action.
   * @param fillTime       the amount of time to wait to fill a box before expiring.
   *                       a positive value is the number of millis after reservation
   *                       before expiring. a negative value is the magnitude of millis
   *                       to cache a result after it is available. 0 means to cache
   *                       results indefinitely (until more memory is needed).
   */
  public SafeDepositMessage(boolean closeBox, boolean forceRehandle, long fillTime) {
    this.closeBox = closeBox;
    this.forceRehandle = forceRehandle;
    this.fillTime = fillTime;
  }

	/**
	 * Get a key that uniquely identifies this message.
	 */
	public String getKey() {
		if (_key == null) {
			_key = generateSafeDepositKey();
		}
		return _key;
	}

  /**
   * Set the claim ticket identifying the contents to retrieve.
   */
  public final void setClaimTicket(String nodeName, long claimTicket) {
    synchronized (CLAIM_MUTEX) {
      if (claimTickets == null) {
        claimTickets = new HashMap<String, Long>();
      }
      claimTickets.put(nodeName, claimTicket);
    }
  }

  /**
   * Reset the claim ticket on this instance (usually for re-use on a single thread).
   */
  public final void resetClaimTickets() {
    synchronized (CLAIM_MUTEX) {
      claimTickets = null;
    }
  }

  /**
   * Reset the claim ticket on this instance (usually for re-use on a single thread).
   */
  public final void resetClaimTickets(Map<String, Long> claimTickets) {
    synchronized (CLAIM_MUTEX) {
      this.claimTickets = claimTickets;
    }
  }

  /**
   * Get the current claim tickets.
   */
  public final Map<String, Long> getClaimTickets() {
    return claimTickets;
  }

  /**
   * Update claim tickets on this instance according to the receipt.
   */
  public final void updateClaims(SafeDepositReceipt sdReceipt) {
    final long claimTicket = sdReceipt.getClaimTicket();
    setClaimTicket(sdReceipt.getNodeName(), claimTicket);
  }

  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    if (claimTickets == null) {
      dataOutput.writeInt(-1);
    }
    else {
      synchronized (CLAIM_MUTEX) {
        dataOutput.writeInt(claimTickets.size());
        for (Map.Entry<String, Long> claim : claimTickets.entrySet()) {
          MessageHelper.writeString(dataOutput, claim.getKey());
          dataOutput.writeLong(claim.getValue());
        }
      }
    }
    dataOutput.writeBoolean(closeBox);
    dataOutput.writeBoolean(forceRehandle);
    dataOutput.writeLong(fillTime);
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
    final int numClaims = dataInput.readInt();
    if (numClaims >= 0) {
      synchronized (CLAIM_MUTEX) {
        this.claimTickets = new HashMap<String, Long>();
        for (int i = 0; i < numClaims; ++i) {
          claimTickets.put(MessageHelper.readString(dataInput), dataInput.readLong());
        }
      }
    }
    this.closeBox = dataInput.readBoolean();
    this.forceRehandle = dataInput.readBoolean();
    this.fillTime = dataInput.readLong();
  }

  public void setCloseBox(boolean closeBox) {
    this.closeBox = closeBox;
  }

  public void setForceRehandle(boolean forceRehandle) {
    this.forceRehandle = forceRehandle;
  }

	public void setFillTime(long fillTime) {
		this.fillTime = fillTime;
	}

  /**
   * Hook to override by extending classes to tweak params before building a
   * receipt for the response.
   */
  protected void preReceiptHook(Context serverContext) {
    //nothing to do.
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
  public final Message getResponse(Context serverContext) {

    preReceiptHook(serverContext);

    this._receipt = new SafeDepositReceipt(serverContext, claimTickets, closeBox,
																					 forceRehandle, fillTime, getKey());

    // if the claimTicket is valid and filled, the contents will be retrieved now.
    // if the claimTicket is valid but not yet filled, it will be returned and can be resubmitted later.
    // if the claimTicket is invalid, if forceRehandle a new claim ticket will be created and returned;
    //   else the (invalid) status will be returned.

    // recipient of this response should resend the message through
    // console.sendMessageToNode(message, nodeName, timeout).

    return _receipt;
  }

  /**
   * Handle this message on the server.
   * <p>
   * NOTE: The message received on a server is handled asynchronously through this
   *       method after its response has been sent back to the client.
   */
  public final void handle(Context context) {

    //
    //NOTE: allow NullPointerException to be thrown here -- it means things
    //      aren't properly set-up and consumer needs to be notified.
    //
    // handle the content generation for this instance. Note that this will
    // call generateContents if necessary.
    //

    _receipt.handleContentGeneration(this);
  }
}
