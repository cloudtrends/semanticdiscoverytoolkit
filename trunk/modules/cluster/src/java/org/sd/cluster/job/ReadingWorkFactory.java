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
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A work factory implementation that reads input work units from a file.
 * <p>
 * @author Spence Koehler
 */
public class ReadingWorkFactory extends AbstractWorkFactory {

  private DataInputStream inputStream;
  private final AtomicBoolean finished = new AtomicBoolean(false);
  private long totalLength;
  private long readLength;
  private long numRead;
  
  /**
   * Initialize with the given stream.
   *
   * @param inputStream        The stream from which units will be read.
   */
  public ReadingWorkFactory(DataInputStream inputStream, long totalLength) {
    this.inputStream = inputStream;
    this.totalLength = totalLength;
    this.readLength = 0L;
    this.numRead = 0;
  }

  /**
   * Get the next unit of work in a thread-safe way.
   *
   * @return the next unit of work or null if there are no more.
   */
  protected UnitOfWork doGetNext() throws IOException {
    UnitOfWork result = null;

    try {
      synchronized (inputStream) {
        result = (UnitOfWork)MessageHelper.readPublishable(inputStream);

        ++numRead;
        readLength += result.getSerializedSize() + MessageHelper.numOverheadBytes(result);
      }
    }
    catch (EOFException e) {
      // reached end of input.
      result = null;
      finished.set(true);
    }

    return result;
  }

  /**
   * Release the unit of work in a thread-safe way.
   * <p>
   * Behavior is undefined if a unit of work is re-released.
   */
  public void release(UnitOfWork unitOfWork) throws IOException {
    // nothing to do.
  }

  /**
   * Explicitly close the resources used by the work factory.
   */
  public void close() throws IOException {
    synchronized (inputStream) {
      this.inputStream.close();
    }
  }

  /**
   * Report whether all units retrieved have been released or
   * accounted for.
   */
  public boolean isComplete() {
    return finished.get() && super.isComplete();
  }

  /**
   * Get an estimate for the remaining work to do.
   */
  public long getRemainingEstimate() {
    // readLength / totalLength = numRead / totalRead
    long result = super.getRemainingEstimate();

    if (readLength > 0) {
//      result = (((double)(numRead * totalLength)) / (double)readLength);

      final double a = numRead;
      final double b = totalLength;
      final double c = readLength;
      final double d = a * b / c + 0.5;

      result += (long)d;
    }

    return result;
  }


  //java -Xmx640m org.sd.cluster.job.ReadingWorkFactory ~/tmp/icis/batches-080706/workbatch-0.dat 
  public static void main(String[] args) throws IOException {
    final File file = new File(args[0]);
    final WorkFactory workFactory = new ReadingWorkFactory(new DataInputStream(new FileInputStream(file)), file.length());

    UnitOfWork unitOfWork = null;
    int count = 0;
    while ((unitOfWork = workFactory.getNext()) != null) {
      System.out.println(unitOfWork);
      ++count;
      workFactory.release(unitOfWork);
    }
    workFactory.close();
    System.out.println("count=" + count);
  }
}
