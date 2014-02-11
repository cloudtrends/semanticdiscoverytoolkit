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


import java.util.HashMap;
import java.util.Map;
import org.sd.util.DoublyLinkedList;

/**
 * Helper class for implementation of the AmbiguousEntity interface.
 * <p>
 * @author Spence Koehler
 */
public class AmbiguityChain <T> {

  private DoublyLinkedList<AmbiguousEntity<T>> ambiguities;
  private Map<AmbiguousEntity<T>, DoublyLinkedList.LinkNode<AmbiguousEntity<T>>> entity2link;

  /**
   * Construct an empty instance.
   */
  public AmbiguityChain() {
    this.ambiguities = new DoublyLinkedList<AmbiguousEntity<T>>();
    this.entity2link = new HashMap<AmbiguousEntity<T>, DoublyLinkedList.LinkNode<AmbiguousEntity<T>>>();
  }

  /**
   * Get the size of (number of entities in) this ambiguity chain.
   */
  public int size() {
    return ambiguities.size();
  }

  /**
   * Get the entity at the given precedence, or null.
   */
  public AmbiguousEntity<T> get(int precedence) {
    return (precedence >= 0 && precedence < ambiguities.size()) ? ambiguities.get(precedence) : null;
  }

  /** Determine whether there is a next (less likely than this) ambiguous entity. */
  public boolean hasNext(AmbiguousEntity<T> entity) {
    return entity2link.get(entity).hasNext();
  }

  /** Get the next (less likely than this) ambiguous entity. */
  public AmbiguousEntity<T> getNext(AmbiguousEntity<T> entity) {
    AmbiguousEntity<T> result = null;

    final DoublyLinkedList.LinkNode<AmbiguousEntity<T>> link = entity2link.get(entity).getNext();
    if (link != null) {
      result = link.getElt();
    }

    return result;
  }

  /**
   * Get the ambiguity precedence of this instance, where precedence values
   * span from low (most likely) to high (least likely).
   */
  public int getPrecedence(AmbiguousEntity<T> entity) {
    return entity2link.get(entity).getIndex();
  }

  /**
   * Remove the given entity from this chain.
   * <p>
   * If entity is not in this chain, an IllegalArgumentException is thrown.
   */
  public void remove(AmbiguousEntity<T> entity) {
    if (entity != null) {
      final DoublyLinkedList.LinkNode<AmbiguousEntity<T>> link = entity2link.remove(entity);
      if (link != null) {
        link.remove();
      }
      else if (entity.isAmbiguous()) {
        throw new IllegalArgumentException("Can't remove entity that's not in this chain! Remove from its own chain.");
      }
    }
  }

  /**
   * Add the given entry to the end of this chain.
   * <p>
   * If the entity is already ambiguous, an IllegalArgumentException is thrown.
   *
   * @return the precedence of the added entity.
   */
  public int add(AmbiguousEntity<T> entity) {
    int result = -1;

    if (entity != null) {
      if (entity.isAmbiguous()) {
        throw new IllegalArgumentException("Can't add already ambiguous entity! Must disambiguate before adding here.");
      }
      else {
        final DoublyLinkedList.LinkNode<AmbiguousEntity<T>> link = ambiguities.add(entity);
        entity2link.put(entity, link);
        result = link.getIndex();
      }
    }

    return result;
  }

  /**
   * Insert the new entity immediately after the existing entity.
   * <p>
   * If existingEntity is null, then insert newEntity at the beginning of this chain.
   * <p>
   * If the existingEntity is in a different chain, an IllegalArgumentException is thrown.
   * <p>
   * If the existing entity isn't in this chain, then it will be added to the
   * end followed by the newEntity.
   * <p>
   * If the newEntity is already ambiguous, an IllegalArgumentException is thrown.
   */
  public void insertAfter(AmbiguousEntity<T> existingEntity, AmbiguousEntity<T> newEntity) {
    if (newEntity == null || existingEntity == newEntity) return;

    int addIdx = -1;
    int removeIdx = -1;

    final DoublyLinkedList.LinkNode<AmbiguousEntity<T>> existingLink = existingEntity == null ? null : entity2link.get(existingEntity);

    // find current index of newEntity in this chain (removeIdx)
    if (newEntity.isAmbiguous()) {
      final DoublyLinkedList.LinkNode<AmbiguousEntity<T>> newLink = entity2link.get(newEntity);
      if (newLink != null) {
        if (newLink.getPrev() == existingLink) {
          // newEntity is already after existingEntity!
          return;
        }
        else {
          // remove newLink to add at the correct position later
          newLink.remove();
        }
      }
      else {
        throw new IllegalArgumentException("Can't insertAfter because newEntity is in a different chain! Must disambiguate before adding here.");
      }
    }

    if (existingLink == null) {
      if (existingEntity == null || !existingEntity.isAmbiguous()) {
        insertAt(0, newEntity);
      }
      else {
        throw new IllegalArgumentException("Can't insertAfter because existingEntity is in a different chain!");
      }
    }
    else {
      final DoublyLinkedList.LinkNode<AmbiguousEntity<T>> newLink = existingLink.add(newEntity);
      entity2link.put(newEntity, newLink);
    }
  }

  private final void insertAt(int idx, AmbiguousEntity<T> newEntity) {
    final DoublyLinkedList.LinkNode<AmbiguousEntity<T>> newLink = ambiguities.add(idx, newEntity);
    entity2link.put(newEntity, newLink);
  }
}
