/*
    Copyright 2011 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.cluster.protolog;


import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import org.sd.cluster.protolog.codegen.ProtoLogProtos.*;
import org.sd.io.FileUtil;

/**
 * Utility to log events to a file.
 * <p>
 * @author Spence Koehler
 */
public class FileEventLogger extends EventLogger {
  
  private DataOutputStream dataOut;
  private ProtoLogStreamer streamer;
  private int maxMessageBytes;

  /**
   * Construct an event logger, appending output to the given output file
   * and using the default ProtoLogStreamer.MAX_MESSAGE_BYTES.
   */
  public FileEventLogger(File outputFile) {
    this(outputFile, true, ProtoLogStreamer.DEFAULT_INSTANCE, ProtoLogStreamer.MAX_MESSAGE_BYTES);
  }

  /**
   * Construct an event logger, writing output to the given output file.
   */
  public FileEventLogger(File outputFile, boolean append, ProtoLogStreamer streamer, int maxMessageBytes) {
    super();

    try {
      this.dataOut = new DataOutputStream(FileUtil.getOutputStream(outputFile, append));
    }
    catch (IOException e) {
      throw new IllegalStateException(e);
    }

    this.streamer = streamer;
    this.maxMessageBytes = maxMessageBytes;
  }

  /**
   * Write the event entry to the log file.
   * <p>
   * If the message is too long (more than maxMessageBytes), then the attributes
   * are cleared. If the message is still too long, then an IllegalStateException
   * is thrown.
   * <p>
   * Any IOException is wrapped in an IllegalStateException.
   */
  public byte[] logEventEntry(EventEntry.Builder eventEntry) {
    byte[] result = eventEntry.build().toByteArray();

    if (result.length > maxMessageBytes) {
      eventEntry.clearAttribute();
      eventEntry.addMessage("Removed attributes, length was (" + result.length + " > " + maxMessageBytes + ")");
      result = eventEntry.build().toByteArray();

      if (result.length > maxMessageBytes) {
        throw new IllegalStateException("EventEntry too long! (" + result.length + " > " + maxMessageBytes + ")");
      }
    }

    try {
      streamer.writeTo(result, this.dataOut);
    }
    catch (IOException ioe) {
      throw new IllegalStateException(ioe);
    }

    return result;
  }

  /**
   * Close this event logger.
   * <p>
   * Any IOException is wrapped in an IllegalStateException.
   */
  public void close() {
    if (this.dataOut != null) {
      try {
        this.dataOut.close();
        this.dataOut = null;
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

}
