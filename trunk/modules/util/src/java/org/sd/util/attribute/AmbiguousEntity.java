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
 * Interface for representing ambiguous entities.
 * <p>
 * Templated for the type of entity, T.
 * <p>
 * An entity that is ambiguous reports itself as so and provides
 * accessors to its alternate (ambiguous) manifestations.
 * <p>
 * Note that ambiguous alternatives should be reported in precedence
 * order, from most (lowest precedence value) to least (highest precedence
 * value) likely.
 *
 * @author Spence Koehler
 */
public interface AmbiguousEntity <T> {

  /**
   * Safely and efficiently typecast this to an AmbiguousEntity.
   */
  public AmbiguousEntity<T> asAmbiguousEntity();

  /**
   * Get the entity wrapped by this instance (safe and efficient typecast).
   */
  public T getEntity();

  /** Determine whether the current instance is ambiguous. */
  public boolean isAmbiguous();

  /** Determine whether there is a next (less likely than this) ambiguous entity. */
  public boolean hasNextAmbiguity();

  /**
   * Get the next (less likely than this) ambiguous entity, or null if there
   * are no others.
   */
  public AmbiguousEntity<T> getNextAmbiguity();

  /** Get the first (most likely) ambiguous entity. */
  public AmbiguousEntity<T> getFirstAmbiguity();

  /**
   * Get the ambiguity precedence of this instance, where precedence values
   * span from low (most likely) to high (least likely).
   */
  public int getPrecedence();

  /** Get the number of ambiguities. */
  public int getAmbiguityCount();

  /** Add another ambiguous entity to the end of the chain. */
  public void addAmbiguity(AmbiguousEntity<T> leastLikelyEntity);

  /** Insert a less-likley ambiguous entity immediately after this. */
  public void insertAmbiguity(AmbiguousEntity<T> lessLikelyEntity);

  /** Rule out this as a valid entity in its ambiguity chain. */
  public void discard();

  /** Resolve this instance as an unambiguous entity. */
  public void resolve();

  /** Remove this instance from its ambiguity chain. */
  public void remove();

  /** Determine whether this ambiguous entity matches (is a duplicate of) the other */
  public boolean matches(AmbiguousEntity<T> other);
}
