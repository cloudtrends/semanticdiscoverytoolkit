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
using System.Collections.Generic;

namespace SemanticDiscovery.Util.Attribute
{
  /// <summary>
  /// Interface for representing multiple non-null values.
  /// 
  /// Templated for the type of value, T.
  /// 
  /// An object that is multi-values reports itself as so and provides
  /// accessors to its additional values.
  /// </summary>
  /// <author>Spence Koehler</author>
  public interface MultipleValues<T> 
  {
    /// A single representative value for this instance.
    T Value { get; set; }

    /// All of this instance's values.
    List<T> Values { get; }

    /// Determine whether the current instance has multiple values.
    bool HasMultipleValues { get; }

    /// The number of values.
    int ValuesCount { get; }


    /// Safely and efficiently typecast this to an MultipleValues.
    MultipleValues<T> AsMultipleValues();

    /// Add another value, returning true if added (unique).
    bool AddValue(T value);

    /// Remove the given value, returning true if removed (existed).
    bool RemoveValue(T value);
  }
}
