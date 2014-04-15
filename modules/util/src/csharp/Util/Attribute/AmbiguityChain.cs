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
using System.Linq;
using System.Collections.Generic;
using SemanticDiscovery.Util;

namespace SemanticDiscovery.Util.Attribute
{
  /// <summary>
  /// Helper class for implementation of the AmbiguousEntity interface.
  /// <p>
  /// </summary>
  /// <author>Spence Koehler</author>
  public class AmbiguityChain<T> 
  {
    private LinkedList<AmbiguousEntity<T>> ambiguities;
    private IDictionary<AmbiguousEntity<T>, LinkedListNode<AmbiguousEntity<T>>> entity2link;

    /// The size of (number of entities in) this ambiguity chain.
    public int Count { get { return ambiguities.Count; } }

    /// Construct an empty instance.
    public AmbiguityChain() {
      this.ambiguities = new LinkedList<AmbiguousEntity<T>>();
      this.entity2link = new Dictionary<AmbiguousEntity<T>, LinkedListNode<AmbiguousEntity<T>>>();
    }

    /// Get the entity at the given precedence, or null.
    public AmbiguousEntity<T> Get(int precedence) {
      return (precedence >= 0 && precedence < ambiguities.Count) ? 
              ambiguities.ElementAt(precedence) : null;
    }

    /// Determine whether there is a next (less likely than this) ambiguous entity.
    public bool HasNext(AmbiguousEntity<T> entity) {
      return (entity2link.Get(entity).Next != null);
    }

    /// Get the next (less likely than this) ambiguous entity.
    public AmbiguousEntity<T> GetNext(AmbiguousEntity<T> entity) {
      AmbiguousEntity<T> result = null;

      LinkedListNode<AmbiguousEntity<T>> link = entity2link.Get(entity).Next;
      if (link != null) {
        result = link.Value;
      }

      return result;
    }

    /// Get the ambiguity precedence of this instance, where precedence values
    /// span from low (most likely) to high (least likely).
    public int GetPrecedence(AmbiguousEntity<T> entity) {
      return ambiguities.IndexOf(entity2link.Get(entity));
    }

    /// Remove the given entity from this chain.
    ///
    /// If entity is not in this chain, an ArgumentException is thrown.
    public void Remove(AmbiguousEntity<T> entity) {
      if (entity != null) 
      {
        LinkedListNode<AmbiguousEntity<T>> link = entity2link.Pop(entity);
        if (link != null) {
          ambiguities.Remove(link);
        }
        else if (entity.IsAmbiguous) {
          throw new ArgumentException("Can't remove entity that's not in this chain! "+
                                      "Remove from its own chain.");
        }
      }
    }

    /// Add the given entry to the end of this chain.
    ///
    /// If the entity is already ambiguous, an ArgumentException is thrown.
    /// @return the precedence of the added entity.
    public int Add(AmbiguousEntity<T> entity) {
      int result = -1;

      if (entity != null) {
        if (entity.IsAmbiguous) {
          throw new ArgumentException("Can't add already ambiguous entity! "+
                                      "Must disambiguate before adding here.");
        }
        else {
          LinkedListNode<AmbiguousEntity<T>> link = ambiguities.AddLast(entity);
          entity2link.Put(entity, link);
          result = ambiguities.Count;
        }
      }

      return result;
    }

    /// Insert the new entity immediately after the existing entity.
    ///
    /// If existingEntity is null, then insert newEntity at the beginning of this chain.
    ///
    /// If the existingEntity is in a different chain, an ArgumentException is thrown.
    ///
    /// If the existing entity isn't in this chain, then it will be added to the
    /// end followed by the newEntity.
    ///
    /// If the newEntity is already ambiguous, an ArgumentException is thrown.
    public void InsertAfter(AmbiguousEntity<T> existingEntity, AmbiguousEntity<T> newEntity) {
      if (newEntity == null || existingEntity == newEntity) return;

      LinkedListNode<AmbiguousEntity<T>> existingLink = 
        existingEntity == null ? null : entity2link.Get(existingEntity);

      // find current index of newEntity in this chain (removeIdx)
      if (newEntity.IsAmbiguous) {
        LinkedListNode<AmbiguousEntity<T>> newLink = entity2link.Get(newEntity);
        if (newLink != null) {
          if (newLink.Previous == existingLink) {
            // newEntity is already after existingEntity!
            return;
          }
          else {
            // remove newLink to add at the correct position later
            ambiguities.Remove(newLink);
          }
        }
        else {
          throw new ArgumentException("Can't insertAfter because newEntity is in a different"+
                                      " chain! Must disambiguate before adding here.");
        }
      }

      if (existingLink == null) {
        if (existingEntity == null || !existingEntity.IsAmbiguous) {
          LinkedListNode<AmbiguousEntity<T>> newLink = ambiguities.AddFirst(newEntity);
          entity2link.Put(newEntity, newLink);
        }
        else {
          throw new ArgumentException("Can't insertAfter because existingEntity is in"+
                                      " a different chain!");
        }
      }
      else {
        LinkedListNode<AmbiguousEntity<T>> newLink = ambiguities.AddAfter(existingLink,newEntity);
        entity2link.Put(newEntity, newLink);
      }
    }
  }
}
