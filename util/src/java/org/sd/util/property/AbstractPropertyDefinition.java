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
package org.sd.util.property;


/**
 * Abstract implementation of the property definition interface.
 * <p>
 * @author Spence Koehler
 */
public abstract class AbstractPropertyDefinition implements PropertyDefinition {

  private String label;

  protected AbstractPropertyDefinition(String label) {
    this.label = label;
  }

  /**
   * Get this definition's label.
   * <p>
   * The label will be "and", "or", or the name.
   *
   * @return this definition's label.
   */
  public String getLabel() {
    return label;
  }

  /**
   * Test whether this definition is an 'and'.
   */
  public boolean isAnd() {
    return false;
  }

  /**
   * Test whether this definition is an 'or'.
   */
  public boolean isOr() {
    return false;
  }

  /**
   * Test whether this definition is an 'named'.
   */
  public boolean isNamed() {
    return false;
  }

  /**
   * Test whether this named property definition repeats.
   *
   * @return true if this repeats; otherwise, false.
   */
  public boolean repeats() {
    return false;
  }

  /**
   * Test whether this named property definition is optional.
   *
   * @return true if this is optional; otherwise, false.
   */
  public boolean isOptional() {
    return false;
  }

  /**
   * Get this defintion as an 'and' if it is one.
   *
   * @return this definition as an 'and' or null.
   */
  public AndPropertyDefinition asAnd() {
    return null;
  }

  /**
   * Get this defintion as an 'or' if it is one.
   *
   * @return this definition as an 'or' or null.
   */
  public OrPropertyDefinition asOr() {
    return null;
  }

  /**
   * Get this defintion as a 'named' if it is one.
   *
   * @return this definition as a 'named' or null.
   */
  public NamedPropertyDefinition asNamed() {
    return null;
  }

  /**
   * The default string form is the label.
   */
  public String toString() {
    return label;
  }
}
