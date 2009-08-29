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


import java.util.LinkedList;

/**
 * Abstract utility to partition data based on a key in light of a changing
 * partition function over time.
 * <p>
 * Extenders must implement the strategy for memorizing and retrieving
 * partitions for keys.
 * <p>
 * Consumers supply the partitioning strategies as partitionFunction instance
 * that are set over time.
 *
 * @author Spence Koehler
 */
public abstract class Partitioner {

  public interface PartitionFunction {
    /** Get the partition for the given key. */
    public int getPartition(String key);
  }
  

  /**
   * History of partition functions with the last being the current.
   */
  protected final LinkedList<PartitionFunction> partitionFunctions;

  /**
   * Default constructor with no partition function.
   */
  public Partitioner() {
    this.partitionFunctions = new LinkedList<PartitionFunction>();
  }

  /**
   * Construct with the given partitionFunction.
   */
  public Partitioner(PartitionFunction partitionFunction) {
    this();
    doSetPartitionFunction(partitionFunction);
  }

  /**
   * (Re)set the current partition function.
   * <p>
   * Extenders may override to, for example, limit the number of partition
   * functions that may be set or to add logic around changing the function.
   * <p>
   * Default implementation adds the partitionFunction to the end of the
   * partitionFunctions list.
   */
  public void setPartitionFunction(PartitionFunction partitionFunction) {
    doSetPartitionFunction(partitionFunction);
  }

  /**
   * (Re)set the current partition function.
   */
  protected final void doSetPartitionFunction(PartitionFunction partitionFunction) {
    partitionFunctions.add(partitionFunction);
  }

  /**
   * Get the partition for the given key.
   * <p>
   * If the key has been "seen" before, return the partition according to the
   * partition function that originally "saw" it; otherwise, return the
   * partition according to the current partition function.
   *
   * @param key  The key for which to get the partition.
   *
   * @return the partition for the key.
   */
  public final int getPartition(String key) {
    Integer result = retrievePartition(key);
    if (result == null) {
      if (partitionFunctions.size() == 0) {
        throw new IllegalStateException("Can't get partition without a partitionFunction!");
      }
      result = partitionFunctions.getLast().getPartition(key);
      memorizePartition(key, result);
    }
    return result;
  }

  /**
   * Retrieve the memorized partition for the key if it exists.
   *
   * @return the memorized partition or null if none has seen the key.
   */
  protected abstract Integer retrievePartition(String key);

  /**
   * Memorize the partition for the key.
   */
  protected abstract void memorizePartition(String key, int partition);

  /**
   * Forget any partition memorized for the key.
   */
  protected abstract void forgetKey(String key);
}
