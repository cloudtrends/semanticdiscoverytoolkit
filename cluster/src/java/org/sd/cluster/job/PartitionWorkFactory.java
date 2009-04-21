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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A work factory suitable for direct use in jobs that manages multiple
 * partitioned files.
 * <p>
 * @author Spence Koehler
 */
public abstract class PartitionWorkFactory extends AbstractWorkFactory implements PublishableWorkFactory {

  private String partitionPattern;
  private String dataDirPath;

  private WorkFactory[] workFactories;
  protected final AtomicInteger curFileIndex = new AtomicInteger(0);
  private Map<UnitOfWork, Integer> unit2index;
  private transient long totalLength;
  private transient File[] files;

  protected PartitionWorkFactory() {
    this.partitionPattern = null;
    this.dataDirPath = null;
    this.totalLength = 0L;
    this.files = null;
  }

  public PartitionWorkFactory(String partitionPattern, String dataDirPath) throws IOException {
    this.partitionPattern = partitionPattern;
    this.dataDirPath = dataDirPath;
    this.unit2index = new HashMap<UnitOfWork, Integer>();
    this.totalLength = 0L;
    this.files = null;
    init();
  }

  private final void init() throws IOException {
    final Pattern pattern = Pattern.compile(partitionPattern);
    final File dataDir = new File(dataDirPath);
    this.files = dataDir.listFiles(new FileFilter() {
        public boolean accept(File file) {
          final String name = file.getName();
          final Matcher m = pattern.matcher(name);
          return m.matches();
        }
      });
    if (files == null) {
      System.err.println("no work files found at '" + dataDirPath + "'! Nothing to do! (partitionPattern=" + partitionPattern + ")");
      this.workFactories = new WorkFactory[0];
      return;
    }
    this.workFactories = new WorkFactory[files.length];
    for (int i = 0; i < files.length; ++i) {
      workFactories[i] = createWorkFactory(files[i]);
      totalLength += files[i].length();

      // log creation of work factory
      System.out.println("PartitionWorkFactory created workFactory '" + files[i] + "'");
    }
  }

  public abstract WorkFactory createWorkFactory(File file) throws IOException;

  /**
   * Get the next unit of work in a thread-safe way.
   *
   * @return the next unit of work or null if there are no more.
   */
  protected UnitOfWork doGetNext() throws IOException {
    int fileIndex = curFileIndex.get();
    UnitOfWork result = null;
    while (result == null && fileIndex < workFactories.length) {
      result = workFactories[fileIndex].getNext();
      if (result == null) {
        fileIndex = curFileIndex.incrementAndGet();
      }
    }
    if (result != null) {
      unit2index.put(result, fileIndex);
    }
    return result;
  }

  /**
   * Release the unit of work in a thread-safe way.
   * <p>
   * Behavior is undefined if a unit of work is re-released.
   */
  public void release(UnitOfWork unitOfWork) throws IOException {
    final Integer fileIndex = unit2index.get(unitOfWork);
    if (fileIndex != null) {
      unit2index.remove(unitOfWork);
      workFactories[fileIndex].release(unitOfWork);
    }
  }

  /**
   * Report whether all units retrieved have been released or
   * accounted for.
   */
  public boolean isComplete() {
    if (!super.isComplete()) return false;

    boolean isComplete = true;

    for (WorkFactory workFactory : workFactories) {
      if (!workFactory.isComplete()) {
        isComplete = false;
        break;
      }
    }

    return (unit2index.size() == 0) && isComplete;
  }

  /**
   * Explicitly close the resources used by the work factory.
   */
  public void close() throws IOException {
    for (WorkFactory workFactory : workFactories) {
      workFactory.close();
    }
  }

  /**
   * Get an estimate for the remaining work to do. -1 if unknown.
   */
  public long getRemainingEstimate() {
    long result = super.getRemainingEstimate();

    final int cfi = curFileIndex.get();
    if (cfi < workFactories.length) {
      final WorkFactory cwf = workFactories[cfi];
      final File cf = files[cfi];

      final long curRemainder = cwf.getRemainingEstimate();
      result += curRemainder / cf.length() * totalLength;
    }

    return result;
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    MessageHelper.writeString(dataOutput, partitionPattern);
    MessageHelper.writeString(dataOutput, dataDirPath);
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
    this.partitionPattern = MessageHelper.readString(dataInput);
    this.dataDirPath = MessageHelper.readString(dataInput);
    init();
  }

  protected final int getNumWorkFactories() {
    return workFactories.length;
  }

  protected final int getCurrentFactoryIndex() {
    return curFileIndex.get();
  }
}
