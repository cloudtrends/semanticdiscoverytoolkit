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
using System.Linq;
using System.Text;
using System.Collections.Generic;
using SemanticDiscovery.Util;

namespace SemanticDiscovery.Util.Attribute
{
  /// <summary>
  /// Container for multiple non-null values.
  /// <p>
  /// </summary>
  /// <author>Spence Koehler</author>
  public class MultipleValuesContainer<T> : MultipleValues<T> 
  {
    private OrderedHashSet<T> values;
    private T value;

    /// Get a single representative value for this instance.
    public T Value {
      get { return value; }
      set {
        this.value = value;
        this.values = null;
      }
    }

    /// Get all of this instance's values.
    public List<T> Values {
      get {
        List<T> result = new List<T>();
        if (value != null) {
          result.Add(value);
        }
        if (values != null) {
          result.AddRange(values);
        }
        return result;
      }
    }

    /// Determine whether the current instance has multiple values.
    public bool HasMultipleValues { get { return ValuesCount > 1; } }

    /// Get the number of values.
    public int ValuesCount {
      get {
        int result = 0;
        
        if (value != null) {
          ++result;
        }
        
        if (values != null) {
          result += values.Count;
        }
        
        return result;
      }
    }

    /// Empty constructor.
    public MultipleValuesContainer() {
      this.values = null;
      this.value = default(T);
    }

    /// Construct with a single value.
    public MultipleValuesContainer(T value) {
      this.values = null;
      this.value = value;
    }

    /// Copy constructor.
    public MultipleValuesContainer(MultipleValuesContainer<T> other) {
      this.values = (other.values == null) ? null : new OrderedHashSet<T>(other.values);
      this.value = other.value;
    }

    /// Safely and efficiently typecast this to an MultipleValues.
    public MultipleValues<T> AsMultipleValues() {
      return this;
    }

    /// Add another value, returning true if added (unique).
    public bool AddValue(T value) {
      bool result = false;

      if (value != null) {
        if (this.value == null) {
          this.value = value;
          result = true;
        }
        else {
          if (!value.Equals(this.value)) {
            if (values == null) values = new OrderedHashSet<T>();
            values.Add(value);
            result = true;
          }
        }
      }

      return result;
    }

    /// Remove the given value, returning true if removed (existed).
    public bool RemoveValue(T value) {
      bool result = false;

      if (value != null) {
        if (value.Equals(this.value)) {
          // remove the "primary" value
          this.value = default(T);
          result = true;

          // pop the first extra value to be the new primary
          if (values != null && values.Count > 0) {
            this.value = values.First();
            values.Remove(this.value);
          }
        }
        else if (values != null) {
          result = values.Remove(value);
        }
      }

      return result;
    }

    public override string ToString() {
      StringBuilder result = new StringBuilder();

      if (HasMultipleValues) {
        result.Append(values.ToString());
      }
      else if (value != null) {
        result.Append(value);
      }

      return result.ToString();
    }

    public override bool Equals(object other) {
      bool result = (this == other);

      if (!result && (other is MultipleValuesContainer<T>)) {
        MultipleValuesContainer<T> otherMVC = (MultipleValuesContainer<T>)other;
        if (this.ValuesCount == otherMVC.ValuesCount) {
          result = this.Values.SequenceEqual(otherMVC.Values);
        }
      }

      return result;
    }

    public override int GetHashCode() {
      int result = 11;
      if (this.ValuesCount != 0) {
        result = result * 11 + this.ValuesCount;
        result = result * 11 + values.GetHashCode();
      }
      return result;
    }
  }
}
