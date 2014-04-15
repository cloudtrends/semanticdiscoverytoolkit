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
using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.Specialized;

namespace SemanticDiscovery.Util
{
  /// <summary>
  /// A generic wrapper for IOrderedDictionary
  /// </summary>
  /// <author>Abe Sanderson</author>
  public interface IOrderedDictionary<TKey,TValue> : IDictionary<TKey, TValue>
  {
    TValue this[TKey key] { get; set; }
    TValue this[int index] { get; set; }

    new IEnumerator<KeyValuePair<TKey,TValue>> GetEnumerator();

    void Insert(int index, TKey key, TValue value);
    void RemoveAt(int index);
  }
}
