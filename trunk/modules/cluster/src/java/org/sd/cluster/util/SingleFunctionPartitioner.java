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
package org.sd.cluster.util;


/**
 * A Partitioner for which the partition function never changes.
 * <p>
 * @author Spence Koehler
 */
public class SingleFunctionPartitioner extends Partitioner {

  public SingleFunctionPartitioner() {
    super();
  }

  public SingleFunctionPartitioner(PartitionFunction partitionFunction) {
    super(partitionFunction);
  }

  /**
   * Set the current partition function.
   * <p>
   * If a partition function has already been set, this will throw an
   * IllegalStateException as the contract of this class is to limit
   * the partitionFunction to a single instance.
   */
  public void setPartitionFunction(PartitionFunction partitionFunction) {
    if (super.partitionFunctions.size() > 0) {
      throw new IllegalStateException("Can't add another partitionFunction!");
    }
    super.setPartitionFunction(partitionFunction);
  }

  /**
   * Retrieve the memorized partition for the key if it exists.
   *
   * @return the memorized partition or null if none has seen the key.
   */
  protected Integer retrievePartition(String key) {
    return null;  // force function to be used.
  }

  /**
   * Memorize the partition for the key.
   */
  protected void memorizePartition(String key, int partition) {
    // nothing to do.
  }

  /**
   * Forget any partition memorized for the key.
   */
  protected void forgetKey(String key) {
    // nothing to do.
  }
}
