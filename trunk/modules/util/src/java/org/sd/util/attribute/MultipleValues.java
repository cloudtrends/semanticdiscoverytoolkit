/*
    Copyright 2013 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.util.attribute;


import java.util.List;

/**
 * Interface for representing multiple non-null values.
 * <p>
 * Templated for the type of value, T.
 * <p>
 * An object that is multi-values reports itself as so and provides
 * accessors to its additional values.
 *
 * @author Spence Koehler
 */
public interface MultipleValues <T> {

  /**
   * Safely and efficiently typecast this to an MultipleValues.
   */
  public MultipleValues<T> asMultipleValues();

  /** Determine whether the current instance has multiple values. */
  public boolean hasMultipleValues();

  /** Get all of this instance's values. */
  public List<T> getValues();

  /** Get a single representative value for this instance. */
  public T getValue();

  /** Get the number of values. */
  public int getValuesCount();

  /** Set the value, overwriting any existing value or values. */
  public void setValue(T value);

  /** Add another value, returning true if added (unique). */
  public boolean addValue(T value);

  /** Remove the given value, returning true if removed (existed). */
  public boolean removeValue(T value);

}
