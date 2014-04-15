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
  /// A generic wrapper for OrderedDictionary
  /// </summary>
  /// <author>Abe Sanderson</author>
  public class OrderedDictionary<TKey,TValue> : IOrderedDictionary<TKey, TValue>
  //ISerializable, IDeserializationCallback 
  {
    private readonly OrderedDictionary m_dict;

    public int Count { get { return m_dict.Count; } }
    public bool IsReadOnly { get { return m_dict.IsReadOnly; } }

    public TValue this[TKey key] { 
      get { 
        if(key == null)
          throw new ArgumentNullException("key cannot be null");
        
        if(m_dict.Contains(key))
          return (TValue)m_dict[(object)key]; 
        return default(TValue);
      } 
      set { 
        if(key == null)
          throw new ArgumentNullException("key cannot be null");
        
        m_dict[key] = value; 
      } 
    }

    public TValue this[int index] { 
      get { 
        if(index < 0 || index >= m_dict.Count)
          throw new ArgumentOutOfRangeException("index is out of range");
        
        return (TValue)m_dict[index]; 
      } 
      set { 
        if(index < 0 || index >= m_dict.Count)
          throw new ArgumentOutOfRangeException("index is out of range");
        
        m_dict[index] = value; 
      } 
    }

    public ICollection<TKey> Keys { 
      get { 
        // todo: should be read-only collection
        List<TKey> keys = new List<TKey>(m_dict.Count);
        foreach(TKey key in m_dict.Keys)
          keys.Add(key);
        return keys; 
      } 
    }
    
    public ICollection<TValue> Values { 
      get { 
        // todo: should be read-only collection
        List<TValue> values = new List<TValue>(m_dict.Count);
        foreach(TValue value in m_dict.Values)
          values.Add(value);
        return values; 
      } 
    }
    
    public OrderedDictionary() {
      m_dict = new OrderedDictionary();
    }

    
    public void Add(KeyValuePair<TKey,TValue> entry) {
      Add(entry.Key, entry.Value);
    }
    public void Add(TKey key, TValue value) {
      if(key == null)
        throw new ArgumentNullException("key cannot be null");

      m_dict.Add(key,value);
    }

    public void Clear() {
      m_dict.Clear();
    }

    public bool Contains(KeyValuePair<TKey,TValue> entry) {
      if(entry.Key == null)
        return false;
      
      bool result = false;
      if(m_dict.Contains(entry.Key))
        result = m_dict[(object)entry.Key].Equals(entry.Value);
      return result;
    }

    public bool ContainsKey(TKey key) {
      return m_dict.Contains(key);
    }

    public bool ContainsValue(TValue value) {
      bool result = false;

      if(value == null)
      {
        foreach(DictionaryEntry entry in m_dict)
        {
          if((TValue)entry.Value == null)
            return true;
        }
      }
      else
      {
        EqualityComparer<TValue> comparer = EqualityComparer<TValue>.Default;
        foreach(DictionaryEntry entry in m_dict)
        {
          if(comparer.Equals((TValue)entry.Value,value))
            return true;
        }
      }
        
      return result;
    }

    public void CopyTo(KeyValuePair<TKey,TValue>[] array, 
                       int index) 
    {
      // null array
      if (array == null)
        throw new ArgumentNullException("array cannot be null");
      
      // offset index out of range
      if (index < 0 || index > array.Length)
        throw new ArgumentOutOfRangeException("offset is out of range");

      // copying from dict will overflow the length of the array
      if(array.Length - index < m_dict.Count)
        throw new ArgumentException("dictionary will not fit in array");
 
      int idx = index;
      foreach(DictionaryEntry entry in m_dict)
      {
        TKey key = (TKey)entry.Key;
        TValue value = (TValue)entry.Value;
        array[idx] = new KeyValuePair<TKey,TValue>(key, value);
        idx++;
      }
    }

    public IEnumerator<KeyValuePair<TKey,TValue>> GetEnumerator()
    {
      foreach(DictionaryEntry entry in m_dict)
      {
        TKey key = (TKey)entry.Key;
        TValue value = (TValue)entry.Value;
        yield return new KeyValuePair<TKey,TValue>(key, value);
      }
    }
 
    IEnumerator IEnumerable.GetEnumerator() {
      return GetEnumerator();
    }
    
    public bool Remove(KeyValuePair<TKey,TValue> entry) 
    {
      if(!Contains(entry))
        return false;

      m_dict.Remove(entry.Key);
      return true;
    }
    public bool Remove(TKey key) 
    {
      if(key == null)
        throw new ArgumentNullException("key cannot be null");
      
      if(!m_dict.Contains(key))
        return false;

      m_dict.Remove(key);
      return true;
    }

    public void RemoveAt(int index) 
    {
      if(index < 0 || index >= m_dict.Count)
        throw new ArgumentOutOfRangeException("index is out of range");
      
      m_dict.RemoveAt(index);
    }

    public void Insert(int index, TKey key, TValue value) 
    {
      if(index < 0 || index >= m_dict.Count)
        throw new ArgumentOutOfRangeException("index is out of range");
      
      if(key == null)
        throw new ArgumentNullException("key cannot be null");

      m_dict.Insert(index,key,value);
    }

    public bool TryGetValue(TKey key, out TValue value)
    {
      if(key == null)
        throw new ArgumentNullException("key cannot be null");
      
      bool result = false;
      value = default(TValue);
      if(m_dict.Contains(key))
      {
        value = (TValue)m_dict[(object)key];
        result = true;
      }
      return result;
    }

    // todo: what to do for serialization
  }
}
