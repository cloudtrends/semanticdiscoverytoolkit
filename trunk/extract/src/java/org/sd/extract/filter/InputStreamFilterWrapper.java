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
package org.sd.extract.filter;


import java.io.IOException;
import java.io.InputStream;

/**
 * Abstract class for wrapping an input stream for filtering.
 * <p>
 * @author Spence Koehler
 */
public abstract class InputStreamFilterWrapper implements Filterable {
  
  /**
   * Determine whether the input stream should be accepted.
   *
   * @return true if the input stream should NOT be filtered (i.e. accepted);
   *         otherwise, false. 
   */
  protected abstract boolean accept(InputStream inputStream) throws IOException;


  private InputStream inputStream;

  /**
   * Construct with the given url string.
   */
  protected InputStreamFilterWrapper(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  /**
   * Accessor for this instance's input stream.
   */
  public InputStream getInputStream() {
    return inputStream;
  }

  /**
   * Determine whether this instance should be accepted.
   *
   * @return true if the instance should NOT be filtered (i.e. accepted);
   *         otherwise, false. 
   */
  public final boolean accept() {
    boolean result = false;

    try {
      result = accept(inputStream);
    }
    catch (IOException e) {
      // translate into a runtime exception.
      throw new IllegalStateException(e);
    }

    return result;
  }

//todo: add protected utility methods that can be used by extenders.
}
