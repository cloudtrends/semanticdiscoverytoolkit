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

namespace SemanticDiscovery.Util
{
  /// <summary>
  /// A hash set implementation which preserves insertion order
  /// </summary>
  /// <author>Abe Sanderson</author>
  public class OrderedHashSet<T> : ICollection<T>, ISet<T>
    //, ISerializable, IDeserializationCallback
  {
    private readonly IDictionary<T,LinkedListNode<T>> m_dict;
    private readonly LinkedList<T> m_list;

    public IEqualityComparer<T> Comparer { get; private set; }
    public int Count { get { return m_dict.Count; } }

    public OrderedHashSet() : this(EqualityComparer<T>.Default) { }
    public OrderedHashSet(IEnumerable<T> coll) : this(EqualityComparer<T>.Default) { }

    public OrderedHashSet(IEqualityComparer<T> comparer)
    {
      if(comparer == null)
        comparer = EqualityComparer<T>.Default;
      this.Comparer = comparer;
      m_dict = new Dictionary<T, LinkedListNode<T>>(comparer);
      m_list = new LinkedList<T>();
    }
    public OrderedHashSet(IEnumerable<T> coll, IEqualityComparer<T> comparer) 
      : this(comparer)
    {
      foreach(T item in coll)
        Add(item);
    }

    public bool Add(T item)
    {
      if (m_dict.ContainsKey(item)) 
        return false;

      LinkedListNode<T> node = m_list.AddLast(item);
      m_dict.Add(item, node);
      return true;
    }

    public void Clear()
    {
      m_dict.Clear();
      m_list.Clear();
    }

    public bool Contains(T item) {
      return m_dict.ContainsKey(item);
    }

    public void CopyTo(T[] array) {
      m_list.CopyTo(array, 0);
    }

    public void CopyTo(T[] array, int arrayIndex) {
      m_list.CopyTo(array, arrayIndex);
    }

    // todo:
    //public void CopyTo(T[] array, int arrayIndex, int count) {
    //  m_list.CopyTo(array, arrayIndex, count);
    //}

    // todo: 
    // createsetcomparator
    // exceptwith
    public void ExceptWith(IEnumerable<T> other) {
      foreach(T item in other)
        Remove(item);
    }
    // getenumerator
    // getobjectdata
    // intersectwith
    public void IntersectWith(IEnumerable<T> other) {
      throw new NotSupportedException();
    }
    // ispropersubsetof
    public bool IsProperSubsetOf(IEnumerable<T> other) {
      throw new NotSupportedException();
    }
    // ispropersupersetof
    public bool IsProperSupersetOf(IEnumerable<T> other) {
      throw new NotSupportedException();
    }
    // issubsetof
    public bool IsSubsetOf(IEnumerable<T> other) {
      throw new NotSupportedException();
    }
    // issupersetof
    public bool IsSupersetOf(IEnumerable<T> other) {
      throw new NotSupportedException();
    }
    // ondeserialization
    // overlaps
    public bool Overlaps(IEnumerable<T> other) {
      throw new NotSupportedException();
    }

    public bool Remove(T item)
    {
      LinkedListNode<T> node;
      bool found = m_dict.TryGetValue(item, out node);
      if (!found) 
        return false;

      m_dict.Remove(item);
      m_list.Remove(node);
      return true;
    }

    // todo:
    // removewhere
    // setequals
    public bool SetEquals(IEnumerable<T> other) {
      throw new NotSupportedException();
    }
    // symmetricexceptwith
    public void SymmetricExceptWith(IEnumerable<T> other) {
      throw new NotSupportedException();
    }
    // trimexcess

    public void UnionWith(IEnumerable<T> other) {
      foreach(T item in other)
        Add(item);
    }


    // from ICollection<T>
    void ICollection<T>.Add(T item) {
      Add(item);
    }

    bool ICollection<T>.IsReadOnly {
      get { return m_dict.IsReadOnly; }
    }
    
    // from IEnumerable<T>
    IEnumerator<T> IEnumerable<T>.GetEnumerator() {
      return m_list.GetEnumerator();
    }
    
    // from IEnumerable
    IEnumerator IEnumerable.GetEnumerator() {
      return m_list.GetEnumerator();
    }

    // todo: what to do for serialization
  }
}
