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
package org.sd.io;


import java.io.DataInput;
import java.io.DataOutput;

import java.io.IOException;

/**
 * Base class for a publishable that will be persisted and, consequently,
 * requires versioning.
 * <p>
 * @author Spence Koehler
 */
public abstract class PersistablePublishable implements Publishable {

  /**
   * Get the current revision version.
   */
  protected abstract int getCurrentVersion();

  /**
   * Serialize this instance using the current revision.
   * <p>
   * NOTE: There is no need to write the current revision number.
   */
  protected abstract void writeCurrentVersion(DataOutput dataOutput) throws IOException;

  /**
   * Deserialize this instance from the given revision number.
   */
  protected abstract void readVersion(int version, DataInput dataInput) throws IOException;


  protected PersistablePublishable() {
  }

  /**
   * Utility method to report an unsupported version.
   */
  protected void badVersion(int version) {
    throw new IllegalStateException("Encountered unsupported version during deserialization! (" + version + ")!");
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public final void write(DataOutput dataOutput) throws IOException {
    dataOutput.writeInt(getCurrentVersion());
    writeCurrentVersion(dataOutput);
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
  public final void read(DataInput dataInput) throws IOException {
    final int version = dataInput.readInt();
    readVersion(version, dataInput);
  }

}
