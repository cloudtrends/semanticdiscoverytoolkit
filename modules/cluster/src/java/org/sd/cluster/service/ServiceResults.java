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
package org.sd.cluster.service;


import java.util.ArrayList;
import java.util.List;

import org.sd.cluster.io.SafeDepositAgent;
import org.sd.cluster.io.SafeDepositBox;
import org.sd.cluster.io.SafeDepositMessage;
import org.sd.io.Publishable;

/**
 * Container for multiple service results.
 * <p>
 * @author Spence Koehler
 */
public class ServiceResults {
  
  private SafeDepositMessage serviceTask;
  private List<SafeDepositAgent.TransactionResult> results;

  public ServiceResults(SafeDepositMessage serviceTask) {
    this.serviceTask = serviceTask;
    this.results = new ArrayList<SafeDepositAgent.TransactionResult>();
  }

  /**
   * Get the task from which these results were generated.
   */
  public SafeDepositMessage getServiceTask() {
    return serviceTask;
  }

  /**
   * Clear this container's results.
   */
  public void clear() {
    synchronized (results) {
      this.results.clear();
    }
  }

  /**
   * Add the result to this container.
   */
  public void add(SafeDepositAgent.TransactionResult result) {
    synchronized (results) {
      results.add(result);
    }
  }

  /**
   * Get the transaction (high-level) results.
   */
  public List<SafeDepositAgent.TransactionResult> getTxnResults() {
    return results;
  }

  /**
   * Get the number of low-level results in this container.
   */
  public int getNumResults() {
    int size = 0;
    synchronized (results) {
      for (SafeDepositAgent.TransactionResult result : results) {
        size += result.withdrawals.size();
      }
    }
    return size;
  }

  /**
   * Get all of the available computed (low-level) results.
   */
  public List<Publishable> getAllResults() {
    final List<Publishable> result = new ArrayList<Publishable>();

    synchronized (results) {
      for (SafeDepositAgent.TransactionResult txnResult : results) {
        if (txnResult.withdrawals == null) continue;
        for (SafeDepositBox.Withdrawal withdrawal : txnResult.withdrawals) {
          final Publishable contents = withdrawal.getContents();
          if (contents != null) {
            result.add(contents);
          }
        }
      }
    }

    return result;
  }

  /**
   * Convenience method to get all available computed results as a single
   * merged result using the given merger.
   */
  public Publishable getAllResults(ResultsMerger merger) {
    final List<Publishable> publishables = getAllResults();
    return doMerge(publishables, merger);
  }

  /**
   * Merge the publishables with the merger, ignoring those that won't merge.
   */
  private Publishable doMerge(List<Publishable> publishables, ResultsMerger merger) {
    Publishable result = null;
    for (Publishable publishable : publishables) {
      if (result == null) {
        result = publishable;
      }
      else {
        final Publishable merged = merger.merge(result, publishable);
        if (merged != null) result = merged;
      }
    }
    return result;
  }
}
