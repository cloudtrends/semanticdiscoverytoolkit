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
package org.sd.util.range;


/**
 * Interface for defining ranges of numbers.
 * <p>
 * @author Spence Koehler
 */
public interface NumericRange {
  
  /**
   * Determine whether the integer is in this numeric range.
   */
  public boolean includes(int value);

  /**
   * Determine whether the long is in this numeric range.
   */
  public boolean includes(long value);

  /**
   * Determine whether the double is in this numeric range.
   */
  public boolean includes(double value);

  /**
   * Determine whether the value is in this numeric range.
   */
  public boolean includes(String value);

  /**
   * Incorporate the value into this range.
   * <p>
   * See AbstractNumericRange.parseValue's javadoc for the format of value.
   */
  public void include(String value);

  /**
   * Get the number of integers included in this range.
   *
   * @return null if the range is infinite; -1 if the number of integers 
   * exceeds the maximum integer value, otherwise the size.
   */
  public Integer size();

  /**
   * Get this range fully represented as a string.
   */
  public String asString();

}
