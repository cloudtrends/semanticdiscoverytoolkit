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


import org.sd.cio.MessageHelper;
import org.sd.io.FileUtil;
import org.sd.io.GZIPInputStreamWorkaround;
import org.sd.io.Publishable;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A response that contains a file (or error).
 * <p>
 * @author Spence Koehler
 */
public class FileResponse extends Response {

  private String filename;
  private byte[] bytes;
  private String stackTrace;
  private Publishable metaData;  // metadata to follow the file

  /**
   * Default constructor for publishable reconstruction.
   */
  public FileResponse() {
  }

  /**
   * Construct and load with the given file's contents.
   */
  public FileResponse(File file) {
    this.filename = file.getAbsolutePath();
    try {
      this.bytes = FileUtil.readRawBytes(file);
    }
    catch (Throwable t) {
      this.stackTrace = FileUtil.getStackTrace(t);
    }
  }

  /**
   * Construct with the given error message as stack trace.
   */
  public FileResponse(String filename, String errorMessage) {
    this.filename = filename;
    this.bytes = null;
    this.stackTrace = errorMessage;
  }

  /**
   * Set metaData for this instance.
   */
  public void setMetaData(Publishable metaData) {
    this.metaData = metaData;
  }

  /**
   * Get this instance's (optional) metaData.
   */
  public Publishable getMetaData() {
    return metaData;
  }

  /**
   * Get this instance's original (source) filename.
   *
   * @return the (source) filename.
   */
  public String getFilename() {
    return filename;
  }

  /**
   * Get this instance's file bytes.
   *
   * @return the bytes or null.
   */
  public byte[] getBytes() {
    return bytes;
  }

  /**
   * Get this instance's stack trace.
   *
   * @return the stack trace from any throwable or null if no error.
   */
  public String getStackTrace() {
    return stackTrace;
  }

  /**
   * Determine whether this response has file data.
   */
  public boolean hasFile() {
    return bytes != null && bytes.length > 0;
  }

  /**
   * Get the total number of bytes loaded.
   */
  public int getTotalBytes() {
    return bytes == null ? 0 : bytes.length;
  }

  /**
   * Get an input stream for the bytes.
   * <p>
   * NOTE: If the bytes were from a file ending in ".gz", then a gzip
   *       input stream will be returned.
   *
   * @return the InputStream or null.
   */
  public InputStream getInputStream() throws IOException {
    InputStream result = null;

    if (bytes != null) {
      result = new ByteArrayInputStream(bytes);
      if (filename.toLowerCase().endsWith(".gz")) {
        result = new GZIPInputStreamWorkaround(result);
      }
    }

    return result;
  }

  /**
   * Get a reader for the bytes.
   * <p>
   * NOTE: If the bytes were from a file ending in ".gz", then an underlying
   *       gzip input stream will back the reader.
   *
   * @return the reader or null.
   */
  public BufferedReader getReader() throws IOException {
    return FileUtil.getReader(getInputStream());
  }

  /**
   * Write this instance's data to the local disk with its original file's
   * name, but in the given directory.
   */
  public void writeToDir(File dir) throws IOException {
    final File original = new File(filename);
    final File file = new File(dir, original.getName());
    saveAsFile(file);
  }

  /**
   * Write this instance's data to the local disk as the given file.
   */
  public void saveAsFile(File file) throws IOException {
    FileOutputStream out = null;

    try {
      out = new FileOutputStream(file);
      out.write(bytes);
    }
    finally {
      if (out != null) out.close();
    }
  }

  /**
   * Write thie message to the dataOutput stream such that this message
   * can be completely reconstructed through this.read(dataInput).
   *
   * @param dataOutput  the data output to write to.
   */
  public void write(DataOutput dataOutput) throws IOException {
    MessageHelper.writeString(dataOutput, filename);
    MessageHelper.writeBytes(dataOutput, bytes);
    MessageHelper.writeString(dataOutput, stackTrace);
    MessageHelper.writePublishable(dataOutput, metaData);
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
    this.filename = MessageHelper.readString(dataInput);
    this.bytes = MessageHelper.readBytes(dataInput);
    this.stackTrace = MessageHelper.readString(dataInput);
    this.metaData = MessageHelper.readPublishable(dataInput);
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    result.
      append("FileResponse[").
      append(filename).append(',').
      append(bytes == null ? 0 : bytes.length).append(" bytes,").
      append(metaData == null ? "" : metaData.toString()).
      append(']');

    return result.toString();
  }
}
