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
package org.sd.util.thread;


import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract implementation of the file processor interface.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractFileProcessor implements FileProcessor {

  /** Open an output stream if needed; otherwise, return null. */
  protected abstract OutputStream openOutputStream(File output);

  /** Close a non-null output stream. */
  protected abstract void closeOutputStream(OutputStream outputStream) throws IOException;

  /** Process the input. */
  protected abstract void doProcessFile(File input, OutputStream outputStream, AtomicBoolean die) throws IOException;

  /**
   * Process the input file.
   * <p>
   * This implementation takes care to always call the closeOutputStream
   * method if openOutputStream returns a non-null stream. IOExceptions are
   * caught and wrapped in an IllegalStateException.
   */
  public final void processFile(File input, File output, AtomicBoolean die) {
    OutputStream outputStream = null;

    try {
      outputStream = openOutputStream(output);
      doProcessFile(input, outputStream, die);
      outputStream = ensureClosed(outputStream);
    }
    catch (IOException e) {
      outputStream = ensureClosed(outputStream);
      throw new IllegalStateException(e);
    }
    finally {
      outputStream = ensureClosed(outputStream);
    }
  }

  private final OutputStream ensureClosed(OutputStream outputStream) {
    if (outputStream != null) {
      try {
        closeOutputStream(outputStream);
      }
      catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
    return null;
  }
}
