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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Container for multiple non-null values.
 * <p>
 * @author Spence Koehler
 */
public class MultipleValuesContainer <T> implements MultipleValues<T> {

  private LinkedHashSet<T> values;
  private T value;

  /** Empty constructor. */
  public MultipleValuesContainer() {
    this.values = null;
    this.value = null;
  }

  /** Construct with a single value. */
  public MultipleValuesContainer(T value) {
    this.values = null;
    this.value = value;
  }

  /** Copy constructor. */
  public MultipleValuesContainer(MultipleValuesContainer<T> other) {
    this.values = (other.values == null) ? null : new LinkedHashSet<T>(other.values);
    this.value = other.value;
  }

  /**
   * Safely and efficiently typecast this to an MultipleValues.
   */
  public MultipleValues<T> asMultipleValues() {
    return this;
  }

  /** Determine whether the current instance has multiple values. */
  public boolean hasMultipleValues() {
    return getValuesCount() > 1;
  }

  /** Get all of this instance's values. */
  public List<T> getValues() {
    final List<T> result = new ArrayList<T>();
    if (value != null) {
      result.add(value);
    }
    if (values != null) {
      result.addAll(values);
    }
    return result;
  }

  /** Get a single representative value for this instance. */
  public T getValue() {
    return value;
  }

  /** Get the number of values. */
  public int getValuesCount() {
    int result = 0;

    if (value != null) {
      ++result;
    }

    if (values != null) {
      result += values.size();
    }

    return result;
  }

  /** Set the value, overwriting any existing value or values. */
  public void setValue(T value) {
    this.value = value;
    this.values = null;
  }

  /** Add another value, returning true if added (unique). */
  public boolean addValue(T value) {
    boolean result = false;

    if (value != null) {
      if (this.value == null) {
        this.value = value;
        result = true;
      }
      else {
        if (!value.equals(this.value)) {
          if (values == null) values = new LinkedHashSet<T>();
          values.add(value);
          result = true;
        }
      }
    }

    return result;
  }

  /** Remove the given value, returning true if removed (existed). */
  public boolean removeValue(T value) {
    boolean result = false;

    if (value != null) {
      if (value.equals(this.value)) {
        // remove the "primary" value
        this.value = null;
        result = true;

        // pop the first extra value to be the new primary
        if (values != null && values.size() > 0) {
          final Iterator<T> valuesIter = values.iterator();
          this.value = valuesIter.next();
          valuesIter.remove();
        }
      }
      else if (values != null) {
        result = values.remove(value);
      }
    }

    return result;
  }

  public String toString() {
    final StringBuilder result = new StringBuilder();

    if (hasMultipleValues()) {
      result.append(getValues().toString());
    }
    else if (value != null) {
      result.append(value);
    }

    return result.toString();
  }

  public boolean equals(Object other) {
    boolean result = (this == other);

    if (!result && (other instanceof MultipleValuesContainer)) {
      final MultipleValuesContainer otherMVC = (MultipleValuesContainer)other;
      if (this.getValuesCount() == otherMVC.getValuesCount()) {
        result = this.getValues().equals(otherMVC.getValues());
      }
    }

    return result;
  }

  public int hashCode() {
    int result = 11;

    final int count = this.getValuesCount();
    if (count != 0) {
      result = result * 11 + count;
      result = result * 11 + getValues().hashCode();
    }

    return result;
  }
}
