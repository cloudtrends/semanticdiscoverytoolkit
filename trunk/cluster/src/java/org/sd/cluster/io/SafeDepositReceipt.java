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
import org.sd.cluster.config.SignedResponse;
import org.sd.io.Publishable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

/**
 * A signed response from a safe deposit message.
 * <p>
 * Note that a recipient will use the identifying information
 * in the response to re-contact the "bank" (handling node)
 * for status or withdrawal.
 * <p>
 * NOTE: The receipt is responsible for interacting with the
 *       SafeDepositBox.
 *
 * @author Spence Koehler
 */
public class SafeDepositReceipt extends SignedResponse {

  private long claimTicket;
  private SafeDepositBox.Withdrawal withdrawal;

  private transient SafeDepositBox _safeDepositBox;
  private transient boolean _handled;

  /**
   * Empty constructor for publishable reconstruction.
   */
  public SafeDepositReceipt() {
  }

  /**
   * A receipt from a SafeDepositMessage.
   * <p>
   * Recipient should check the withdrawal for status. If null, the claimTicket
   * should be resubmitted later to retrieve the contents.
   */
  public SafeDepositReceipt(Context context, Map<String, Long> claimTickets, boolean closeBox, boolean forceRehandle, long fillTime,
                            String safeDepositKey) {
    super(context);

    final ClusterContext clusterContext = getClusterContext(); // from super
    this._safeDepositBox = clusterContext.getSafeDepositBox();

    // get my claim ticket.
    this.claimTicket = -1;
    Long myTicket = null;

    if (claimTickets != null) {
      myTicket = claimTickets.get(getNodeName());
    }
    if (myTicket == null && safeDepositKey != null) {
      myTicket = _safeDepositBox.lookupKey(safeDepositKey);
    }
    if (myTicket != null) {
      this.claimTicket = myTicket;
    }


    // if the claimTicket is valid and filled, the contents will be retrieved now.
    // if the claimTicket is valid but not yet filled, it will be returned and can be resubmitted later.
    // if the claimTicket is invalid, if forceRehandle a new claim ticket will be created and returned;
    //   else the (invalid) status will be returned.

    boolean haveTicket = false;
    this._handled = true;

    if (_safeDepositBox.wasReserved(claimTicket)) {
      haveTicket = true;

      // see if we have results
      this.withdrawal = _safeDepositBox.withdraw(claimTicket, closeBox);
      final SafeDepositBox.WithdrawalCode withdrawalCode = withdrawal.getWithdrawalCode();

      switch(withdrawalCode) {
        case RETRIEVED :
          break;  // signifies the content exists

        case NO_DEPOSIT :
          break;  // signifies valid claim ticket, but no deposit yet.

        case EXPIRED :
          if (forceRehandle) {
            this.withdrawal = null;
            haveTicket = false;
          }
          break;

        case UNRESERVED :
          this.withdrawal = null;
          haveTicket = false;
          break;
      }
    }

    if (withdrawal == null) {
      if (!haveTicket) {
        this.claimTicket = _safeDepositBox.reserveDrawer(fillTime, safeDepositKey);
        _handled = false;
      }
      // else, just waiting for already running work to finish.
    }
  }

  /**
   * Write this message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    super.write(dataOutput);

    dataOutput.writeLong(claimTicket);
    MessageHelper.writePublishable(dataOutput, withdrawal);
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

    this.claimTicket = dataInput.readLong();
    this.withdrawal = (SafeDepositBox.Withdrawal)MessageHelper.readPublishable(dataInput);
  }

  /**
   * Get the claim ticket.
   */
  public long getClaimTicket() {
    return claimTicket;
  }

  /**
   * Get the withdrawal.
   * <p>
   * If null, the claim ticket should be resubmitted later.
   */
  public SafeDepositBox.Withdrawal getWithdrawal() {
    return withdrawal;
  }

  /**
   * Determine whether we need to come back for the real results.
   */
  public boolean rainCheck() {
    boolean result = true;

    // invite back unless the contents have been retrieved or expunged.
    if (withdrawal != null) {
      final SafeDepositBox.WithdrawalCode wc = withdrawal.getWithdrawalCode();
      if (wc == SafeDepositBox.WithdrawalCode.RETRIEVED ||
          wc == SafeDepositBox.WithdrawalCode.EXPIRED) {
        result = false;
      }
    }

    return result;
  }

  /**
   * Conveniende method to determine whether contents are contained.
   */
  public boolean hasContents() {
    boolean result = false;

    if (withdrawal != null) {
      result = (withdrawal.getContents() != null);
    }

    return result;
  }

  /**
   * Convenience method to retrieve contents.
   */
  public Publishable getContents() {
    Publishable result = null;

    if (withdrawal != null) {
      result = withdrawal.getContents();
    }

    return result;
  }

  void handleContentGeneration(SafeDepositMessage safeDepositMessage) {
    if (!_handled) {
      Publishable contents = safeDepositMessage.generateContents(getClusterContext());
      _safeDepositBox.deposit(claimTicket, contents);
      _handled = true;
    }
  }
}
