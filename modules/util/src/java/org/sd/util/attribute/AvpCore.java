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
 * Container for the core data of an AttValPair intended to be carried within
 * all AttValPair copies.
 * <p>
 * NOTE: This is nNot intended for exposure outside the AttValPair.
 *
 * @author Spence Koehler
 */
class AvpCore <E extends Canonical, V, M> {

  private MultipleValuesContainer<V> values;
  private M metaData;
  private AvpContainer<E, V, M> container;

  AvpCore() {
    this.values = null;
    this.metaData = null;
    this.container = null;
  }

  AvpCore(V value) {
    this();
    this.values = new MultipleValuesContainer<V>(value);
  }

  AvpCore(AvpCore<E, V, M> other) {
    this();
    this.values = new MultipleValuesContainer<V>(other.values);
    this.metaData = other.metaData;
    this.container = other.container;
  }

  /** Get the metaData. */
  M getMetaData() {
    return metaData;
  }

  /** Set the metaData. */
  M setMetaData(M metaData) {
    M result = this.metaData;
    this.metaData = metaData;
    return result;
  }

  /** Determine the presence of metaData. */
  boolean hasMetaData() {
    return metaData != null;
  }

  /** Get the container. */
  AvpContainer<E, V, M> getContainer() {
    return container;
  }

  /** Set the container. */
  void setContainer(AvpContainer<E, V, M> container) {
    this.container = container;
  }

  /** Get the value. */
  V getValue() {
    return values == null ? null : values.getValue();
  }

  /** Get all of this instance's values. */
  List<V> getValues() {
    return values == null ? null : values.getValues();
  }

  /** Set the value, overwriting any existing value or values. */
  void setValue(V value) {
    if (this.values == null) {
      this.values = new MultipleValuesContainer<V>(value);
    }
    else {
      this.values.setValue(value);
    }
  }

  /** Determine whether there are multiple values. */
  boolean hasMultipleValues() {
    return values != null && values.hasMultipleValues();
  }

  /** Get the number of values. */
  int getValuesCount() {
    return values == null ? 0 : values.getValuesCount();
  }

  /** Add another value, returning true if added (unique). */
  boolean addValue(V value) {
    boolean result = false;

    if (value != null) {
      if (values == null) {
        values = new MultipleValuesContainer<V>(value);
        result = true;
      }
      else {
        result = values.addValue(value);
      }
    }

    return result;
  }

  /** Remove the given value, returning true if removed (existed). */
  boolean removeValue(V value) {
    return (values == null) ? false : values.removeValue(value);
  }

  /** Determine whether values match. */
  boolean matches(AvpCore<E, V, M> other) {
    boolean result = (this == other);

    if (!result && this.values != null) {
      result = this.values.equals(other.values);
      //NOTE: ignore metaData, container
    }

    return result;
  }

  /** Auxiliary for AttValPair.toString */
  void toString(StringBuilder result) {
    if (values != null) {
      result.append('=').append(values.toString());
    }
    if (metaData != null) {
      // add a marker indicating the presence of metadata
      result.append('+');
    }
  }
}
