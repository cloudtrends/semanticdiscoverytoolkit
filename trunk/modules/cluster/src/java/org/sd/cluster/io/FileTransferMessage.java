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


import org.sd.cluster.config.ConfigUtil;
import org.sd.io.DataHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A direct response message to request and receive a file from a cluster node.
 * <p>
 * @author Spence Koehler
 */
public class FileTransferMessage extends DirectResponseMessage {
  
  private static final AtomicLong COMMITTED = new AtomicLong(0L);


  private String filePath;

  /**
   * Default constructor for publishable reconstruction.
   */
  public FileTransferMessage() {
  }

  /**
   * Construct a message requesting the file at the given path.
   * <p>
   * This requests the file at filePath on the target node's machine and
   * should typically be an absolute path. A relative path will be interpreted
   * as relative to the "cluster" directory. If filePath contains a colon, ':',
   * then the filePath is taken to be only the string after the colon.
   */
  public FileTransferMessage(String filePath) {
    this.filePath = filePath;
  }

  /**
   * Get this message's response to be returned by the server to the client.
   * <p>
   * NOTE: this response is returned synchronously from a server to the client
   *       after receiving a message. The message as received on the server
   *       is handled in its own thread later.
   *
   * @param serverContext  The context of the server responding to this message. (ignored)
   *
   * @return a FileResponse instance.
   */
  public Message getResponse(Context serverContext) {
    Message result = null;

    String filename = filePath == null ? "" : filePath;
    final int cPos = filename.indexOf(':');
    if (cPos >= 0) filename = filename.substring(cPos + 1);
    if (filename.length() > 0) {
      final char firstChar = filename.charAt(0);
      if (firstChar != '/') {
        filename = ConfigUtil.getClusterRootDir() + filename;
      }
    }

    if (filename.length() > 0) {
      final File file = new File(filename);
      if (file.exists()) {
        final long filelen = file.length();
        final long freemem = Runtime.getRuntime().freeMemory() + 131072;  // 128K
        if (filelen + COMMITTED.get() > freemem) {
          result = new FileResponse(filename, "Memory too low! (fileSize=" + filelen + ", freeMem=" + freemem + ", committed=" + COMMITTED.get() + ")");
        }
        else {
          COMMITTED.addAndGet(filelen);
          result = new FileResponse(file);
          COMMITTED.addAndGet(-filelen);
        }
      }
    }
    else {
      result = new FileResponse(filePath, "No file specified! (" + filePath + ")");
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
    DataHelper.writeString(dataOutput, filePath);
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
    this.filePath = DataHelper.readString(dataInput);
  }
}
