/*
    Copyright 2009 Semantic Discovery, Inc. (www.semanticdiscovery.com)

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
package org.sd.nlp;


/**
 * An enumeration of types to be used to specify Categories.
 * <p>
 * Instances are to be defined and retrieved through a CategoryFactory instance.
 *
 * @author Spence Koehler
 */
public class Category {
  
  private int id;
  private String label;
  private boolean canGuess;

  /** Always constructs a unique instance. */
  Category(int id, String label, boolean canGuess) {
    this.id = id;
    this.label = label;
    this.canGuess = canGuess;
  }

  public int getId() {
    return id;
  }

  public boolean canGuess() {
    return canGuess;
  }

  public String name() {
    return label;
  }

  /** Compute a hashCode for this compareType. */
  public int hashCode() {
    return getId();
  }

  /** Determine equality. */
  public boolean equals(Object other) {
    return this.hashCode() == other.hashCode();
  }

  /**
   * Get a string describing this compareType.
   */
  public String toString() {
    return label;
  }
}
