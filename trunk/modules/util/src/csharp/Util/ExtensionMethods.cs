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

namespace SemanticDiscovery.Util
{
  public static class ExtensionMethods
  {
    // IDictionary<K,V>
    public static V Put<K,V>(this IDictionary<K,V> dict, 
                             K key, V value)
    {
      V result = default(V);
      if(!dict.ContainsKey(key))
        dict.Add(key,value);
      else
      {
        result = dict[key];
        dict[key] = value;
      }
      return result;
    }

    public static V Get<K,V>(this IDictionary<K,V> dict, 
                             K key)
    {
      V result = default(V);
      if(dict.ContainsKey(key))
        result = dict[key];
      return result;
    }

    public static V Pop<K,V>(this IDictionary<K,V> dict, 
                             K key)
    {
      V result = default(V);
      if(dict.ContainsKey(key))
      {
        result = dict[key];
        dict.Remove(key);
      }
      return result;
    }

    // LinkedList<T>
    public static int IndexOf<T>(this LinkedList<T> list, 
                                 T item)
    {
      int idx = -1;
      foreach(T linkItem in list)
      {
        idx++;
        if(item.Equals(linkItem))
          return idx;
      }
      return idx;
    }

    public static int IndexOf<T>(this LinkedList<T> list, 
                                 LinkedListNode<T> linkNode)
    {
      int idx = -1;
      for(LinkedListNode<T> node = list.First; node != null; node = node.Next)
      {
        idx++;
        if(linkNode.Equals(node))
          return idx;
      }
      return idx;
    }

  }
}
