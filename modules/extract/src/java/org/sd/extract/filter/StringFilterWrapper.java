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


/**
 * Abstract class for wrapping a string for filtering.
 * <p>
 * @author Spence Koehler
 */
public abstract class StringFilterWrapper implements Filterable {
  
  /**
   * Determine whether the string should be accepted.
   *
   * @return true if the string should NOT be filtered (i.e. accepted);
   *         otherwise, false. 
   */
  protected abstract boolean accept(String string);


  private String string;

  /**
   * Construct with the given string.
   */
  protected StringFilterWrapper(String string) {
    this.string = string;
  }

  /**
   * Accessor for this instance's input stream.
   */
  public String getString() {
    return string;
  }

  /**
   * Determine whether this instance should be accepted.
   *
   * @return true if the instance should NOT be filtered (i.e. accepted);
   *         otherwise, false. 
   */
  public final boolean accept() {
    return accept(string);
  }

//todo: add protected utility methods that can be used by extenders.
}
