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


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.sd.cio.MessageHelper;

/**
 * A wrapper to hold multiple file responses.
 * <p>
 * @author Spence Koehler
 */
public class MultiFileResponse extends Response {

  private FileResponse[] fileResponses;

  /**
   * Default constructor for publishable reconstruction.
   */
  public MultiFileResponse() {
  }

  /**
   * Construct and load with the given file's contents.
   */
  public MultiFileResponse(FileResponse[] fileResponses) {
    this.fileResponses = fileResponses;
  }

  /**
   * Get the file responses.
   */
  public FileResponse[] getFileResponses() {
    return fileResponses;
  }

  /**
   * Determine whether this response has file data.
   */
  public boolean hasFiles() {
    return fileResponses != null && getTotalBytes() > 0;
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    if (fileResponses == null) {
      dataOutput.writeInt(-1);
    }
    else {
      dataOutput.writeInt(fileResponses.length);

      for (FileResponse fileResponse : fileResponses) {
        MessageHelper.writePublishable(dataOutput, fileResponse);
      }
    }
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
    final int numFileResponses = dataInput.readInt();
    if (numFileResponses >= 0) {
      this.fileResponses = new FileResponse[numFileResponses];
      for (int i = 0; i < numFileResponses; ++i) {
        this.fileResponses[i] = (FileResponse)MessageHelper.readPublishable(dataInput);
      }
    }
  }

  public long getTotalBytes() {
    long result = 0L;

    if (fileResponses != null) {
      for (FileResponse fileResponse : fileResponses) {
        result += fileResponse.getTotalBytes();
      }
    }

    return result;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append("MultiFileResponse[").
      append(fileResponses == null ? 0 : fileResponses.length).append(" files,").
      append(getTotalBytes()).append(" total bytes]");

    return result.toString();
  }
}
