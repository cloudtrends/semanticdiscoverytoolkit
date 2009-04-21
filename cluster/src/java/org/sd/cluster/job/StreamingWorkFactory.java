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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A work factory implementation that streams input work units through to
 * output work units when released.
 * <p>
 * @author Spence Koehler
 */
public class StreamingWorkFactory extends AbstractWorkFactory {

  private DataInputStream inputStream;
  private DataOutputStream outputStream;
  private Set<UnitOfWork> previouslyProcessed;
  private final AtomicInteger checkedOut = new AtomicInteger(0);
  
  /**
   * Initialize with the given streams.
   *
   * @param inputStream        The stream from which units will be read.
   * @param outputStream       The stream to which released units will be written (usually new).
   */
  public StreamingWorkFactory(DataInputStream inputStream, DataOutputStream outputStream) {
    this.inputStream = inputStream;
    this.outputStream = outputStream;
    this.previouslyProcessed = new HashSet<UnitOfWork>();
  }

  /**
   * Initialize with the given streams.
   *
   * @param inputStream             The stream from which units will be read.
   * @param outputStream            The stream to which released units will be written (usually append).
   * @param priorOutputStream       An input stream to read a prior output stream; Okay if null.
   * @param priorStatusToReprocess  Status values for work units to reprocess. If null no units will
   *                                be reprocessed. Note that reprocessed units will be replicated,
   *                                NOT replaced, in the output stream.
   */
  public StreamingWorkFactory(DataInputStream inputStream, DataOutputStream outputStream,
                              DataInputStream priorOutputStream, Set<WorkStatus> priorStatusToReprocess)
    throws IOException {

    this(inputStream, outputStream);

    if (priorOutputStream != null) {
      // pull prior work units, keep those that should NOT be reprocessed.
      while (true) {
        try {
          final UnitOfWork workUnit = (UnitOfWork)MessageHelper.readPublishable(priorOutputStream);
          if (priorStatusToReprocess == null) {
            previouslyProcessed.add(workUnit);
          }
          else {
            // NOTE: equals and hashCode for a workUnit cannot depend on status!!!
            if (!priorStatusToReprocess.contains(workUnit.getWorkStatus())) {
              previouslyProcessed.add(workUnit);
            }
            else if (previouslyProcessed.contains(workUnit)) {
              // prefer the later status of a work unit.
              previouslyProcessed.remove(workUnit);
            }
          }
        }
        catch (EOFException e) {
          // okay, reached end.
          break;
        }
      }
    }
  }

  /**
   * Get the next unit of work in a thread-safe way.
   *
   * @return the next unit of work or null if there are no more.
   */
  protected UnitOfWork doGetNext() throws IOException {
    UnitOfWork result = null;

    try {
      // skip if previously processed.
      do {
        synchronized (inputStream) {
          result = (UnitOfWork)MessageHelper.readPublishable(inputStream);
        }
      } while (result != null && previouslyProcessed.contains(result));
    }
    catch (EOFException e) {
      // reached end of input.
      result = null;
    }

    if (result != null) checkedOut.incrementAndGet();

    return result;
  }

  /**
   * Release the unit of work in a thread-safe way.
   * <p>
   * Behavior is undefined if a unit of work is re-released.
   */
  public void release(UnitOfWork unitOfWork) throws IOException {
    synchronized (outputStream) {
      MessageHelper.writePublishable(outputStream, unitOfWork);
    }
    checkedOut.decrementAndGet();
  }

  /**
   * Explicitly close the resources used by the work factory.
   */
  public void close() throws IOException {
    synchronized (inputStream) {
      this.inputStream.close();
    }
    synchronized (outputStream) {
      this.outputStream.close();
    }
  }

  /**
   * Report whether all units retrieved have been released or
   * accounted for.
   */
  public boolean isComplete() {
    return (checkedOut.get() <= 0) && super.isComplete();
  }

  /**
   * Get an estimate for the remaining work to do. -1 if unknown.
   */
  public long getRemainingEstimate() {
    long result = super.getRemainingEstimate();

    if (checkedOut.get() <= 0 && result == 0) {
      result = -1L;
    }

    return result;
  }

  public static void main(String[] args) throws IOException {
    final File file = new File(args[0]);
    final WorkFactory workFactory = new TrackingWorkFactory().createWorkFactory(file);

    UnitOfWork unitOfWork = null;
    int count = 0;
    while ((unitOfWork = workFactory.getNext()) != null) {
      ++count;
      workFactory.release(unitOfWork);
    }
    workFactory.close();
    System.out.println("count=" + count);
  }
}
