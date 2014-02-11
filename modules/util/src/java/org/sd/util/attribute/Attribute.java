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
 * Container for potentially ambiguous canonical attributes as generated
 * through an AttributeClassifier.
 * <p>
 * Templated for the canonical attribute enumeration, E.
 *
 * @author Spence Koehler
 */
public class Attribute <E> extends AbstractAmbiguousEntity<Attribute<E>> {

  private E attType;

  public Attribute(E attType) {
    this.attType = attType;
  }

  /** Get the attribute type wrapped by this instance. */
  public E getAttType() {
    return attType;
  }

  ////////
  ///
  /// Implement AmbiguousEntity interface
  ///

  /** Simple typecasting helper auxiliary for getting the next ambiguity. */
  public Attribute<E> nextAmbiguity() {
    return (Attribute<E>)getNextAmbiguity();
  }

  /** Simple typecasting helper auxiliary for getting the first ambiguity. */
  public Attribute<E> firstAmbiguity() {
    return (Attribute<E>)getFirstAmbiguity();
  }

  /**
   * Safely and efficiently typecast this to an Attribute.
   */
  public Attribute<E> getEntity() {
    return this;
  }

  /** Determine whether this ambiguous entity matches (is a duplicate of) the other */
  public boolean matches(AmbiguousEntity<Attribute<E>> other) {
    boolean result = (this == other);
    if (!result && other != null) {
      result = (this.attType == other.getEntity().attType);
    }
    return result;
  }

  ///
  /// end of AmbiguousEntity interface implementation
  ///
  ////////
}
