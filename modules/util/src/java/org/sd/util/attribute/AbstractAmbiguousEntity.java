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


/**
 * Abstract implementation of the AmbiguousEntity interface.
 * <p>
 * Extenders (e.g., "MyClass") need to implement getEntity, addAmbiguity, and
 * insertAmbiguity similarly to:
 * <pre>
 *  public MyClass getEntity() {
 *    return this;
 *  }
 * </pre>
 * <p>
 * See AttValPair and Attribute for implementation examples.
 *
 * @author Spence Koehler
 */
public abstract class AbstractAmbiguousEntity <T> implements AmbiguousEntity<T> {

  private AmbiguityChain<T> ambiguityChain;
  
  protected AbstractAmbiguousEntity() {
    this.ambiguityChain = null;
  }

  /**
   * Safely and efficiently typecast this to an AmbiguousEntity.
   */
  public AmbiguousEntity<T> asAmbiguousEntity() {
    return this;
  }

  /** Determine whether the current instance is ambiguous. */
  public boolean isAmbiguous() {
    boolean result = false;

    if (ambiguityChain != null) {
      int count = 0;
      for (AmbiguousEntity<T> entity = getFirstAmbiguity(); entity != null; entity = entity.getNextAmbiguity()) {
        ++count;
        if (count > 1) {
          result = true;
          break;
        }
      }
    }

    return result;
  }

  /** Determine whether there is a next (less likely than this) ambiguous entity. */
  public boolean hasNextAmbiguity() {
    return ambiguityChain != null && ambiguityChain.hasNext(this);
  }

  /**
   * Get the ambiguity precedence of this instance, where precedence values
   * span from low (most likely) to high (least likely).
   */
  public int getPrecedence() {
    return ambiguityChain.getPrecedence(this);
  }

  /** Get the number of ambiguities. */
  public int getAmbiguityCount() {
    int result = 0;

    if (ambiguityChain != null) {
      for (AmbiguousEntity<T> entity = getFirstAmbiguity(); entity != null; entity = entity.getNextAmbiguity()) {
        ++result;
      }
    }

    return result;
  }

  /** Rule out this as a valid entity in its ambiguity chain. */
  public void discard() {
    remove();
  }

  /** Resolve this instance as an unambiguous entity. */
  public void resolve() {
    remove();
  }

  /** Remove this instance from its ambiguity chain. */
  public void remove() {
    if (ambiguityChain != null) {
      ambiguityChain.remove(this);
      this.ambiguityChain = null;
    }
  }

  /** Get the next (less likely than this) ambiguous entity. */
  public AmbiguousEntity<T> getNextAmbiguity() {
    AmbiguousEntity<T> result = null;

    if (ambiguityChain != null) {
      // get the next ambiguity that doesn't match this
      for (result = ambiguityChain.getNext(this); result != null && result.matches(this); result = ambiguityChain.getNext(result));
    }

    return result;
  }

  /** Get the first (most likely) ambiguous entity. */
  public AmbiguousEntity<T> getFirstAmbiguity() {
    AmbiguousEntity<T> result = null;

    if (ambiguityChain != null) {
      result = ambiguityChain.get(0);
    }

    return result;
  }


  /** Add another ambiguous entity to the end of the chain. */
  public void addAmbiguity(AmbiguousEntity<T> leastLikelyEntity) {
    if (leastLikelyEntity == null) return;
    if (ambiguityChain == null) {
      this.ambiguityChain = new AmbiguityChain<T>();
      this.ambiguityChain.add(this);
    }
    ambiguityChain.add(leastLikelyEntity);
    ((AbstractAmbiguousEntity<T>)leastLikelyEntity).ambiguityChain = this.ambiguityChain;
  }

  /** Insert a less-likley ambiguous entity immediately after this. */
  public void insertAmbiguity(AmbiguousEntity<T> lessLikelyEntity) {
    if (lessLikelyEntity == null) return;
    if (ambiguityChain == null) {
      if (lessLikelyEntity.isAmbiguous()) {
        final AbstractAmbiguousEntity<T> other = (AbstractAmbiguousEntity<T>)lessLikelyEntity;
        other.ambiguityChain.insertAfter(null, this);
        this.ambiguityChain = other.ambiguityChain;
      }
      else {
        this.ambiguityChain = new AmbiguityChain<T>();
        this.ambiguityChain.add(this);
        this.ambiguityChain.add(lessLikelyEntity);
      }
    }
    else {
      ambiguityChain.insertAfter(this, lessLikelyEntity);
    }
    ((AbstractAmbiguousEntity<T>)lessLikelyEntity).ambiguityChain = this.ambiguityChain;
  }
}
