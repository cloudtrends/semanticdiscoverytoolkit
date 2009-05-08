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
import org.sd.util.thread.UnitCounter;

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
  private transient UnitCounter _uc;          // NOTE: only meaningful while generating contents
  private transient long[] _completionRatio;  // NOTE: only instantiated when reconstructed
	private transient double _atpu;             // NOTE: only instantiated when reconstructed

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
  public SafeDepositReceipt(Context context, Map<String, Long> claimTickets,
														boolean closeBox, boolean forceRehandle, long fillTime,
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
      this._uc = _safeDepositBox.getUnitCounter(claimTicket);

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
        this._uc = new UnitCounter();
        this.claimTicket = _safeDepositBox.reserveDrawer(fillTime, safeDepositKey, _uc);
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

    // write out the current progress (completionRatio) if available
    final long[] completionRatio = getCompletionRatio();
    if (completionRatio == null) {
      dataOutput.writeInt(-1);
    }
    else {
      dataOutput.writeInt(completionRatio.length);
      for (long value : completionRatio) {
        dataOutput.writeLong(value);
      }
    }

		dataOutput.writeDouble(getAverageTimePerUnit());
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

    // read in the completion ratio (if any)
    final int numValues = dataInput.readInt();
    if (numValues < 0) {
      this._completionRatio = null;
    }
    else {
      this._completionRatio = new long[numValues];
      for (int i = 0; i < numValues; ++i) {
        _completionRatio[i] = dataInput.readLong();
      }
    }

		this._atpu = dataInput.readDouble();
  }

	/**
	 * Get the ratio of completion.
   * <ul>
   * <li>doneSoFar -- the total number of units of work done so far or -1 if
   *                  counting has not been started.</li>
   * <li>toBeDone -- the total number of units of work to be done or -1 if
   *                 unknown.</li>
   * </ul>
	 *
	 * @return {doneSoFar, toBeDone}
	 */
  public long[] getCompletionRatio() {
    long[] result = null;

    if (_uc != null) {
      result = _uc.getCompletionRatio();
    }
    else {
      result = this._completionRatio;
    }

    return result;
  }

	/**
	 * Get an estimate on the average time per unit if available.
	 *
	 * @return the average time per unit or 0.0 if unknown.
	 */
	public double getAverageTimePerUnit() {
		double result = -1.0;

		if (_uc != null) {
			result = _uc.getAverageTimePerUnit();
		}
		else {
			result = this._atpu;
		}

		return result;
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
      Publishable contents = safeDepositMessage.generateContents(getClusterContext(), _uc);
      _safeDepositBox.deposit(claimTicket, contents);
      _handled = true;
    }
  }
}
