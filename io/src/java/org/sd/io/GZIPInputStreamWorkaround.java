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


import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * GZIPInputStream has a bug in which it erroneously throws a "Corrupt GZip Trailer"
 * IOException on files > 4GB.  This class catches and ignores that exception.
 * <p>
 * @author Spence Koehler
 */
public class GZIPInputStreamWorkaround extends GZIPInputStream {

  /**
   * Creates a new input stream with a default buffer size.
   *
   * @param in the input stream
   * @throws java.io.IOException if an I/O error has occurred
   */
  public GZIPInputStreamWorkaround(InputStream in) throws IOException {
    super(in);
  }

  /**
   * Creates a new input stream with the specified buffer size.
   *
   * @param in   the input stream
   * @param size the input buffer size
   * @throws java.io.IOException      if an I/O error has occurred
   * @throws IllegalArgumentException if size is <= 0
   */
  public GZIPInputStreamWorkaround(InputStream in, int size) throws IOException {
    super(in, size);
  }

  public int read(byte[] buf, int off, int len) throws IOException {
    int numBytes;
    try {
      numBytes = super.read(buf, off, len);
    }
    catch (IOException e) {
      if (!e.getMessage().equals("Corrupt GZIP trailer")) {
        throw e;
      }
      numBytes = -1; // Can't throw above exception unless we're already at the end of the file.
    }
    return numBytes;
  }

}
