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
 * Property definition for bundling or'd properties.
 * <p>
 * @author Spence Koehler
 */
public class OrPropertyDefinition extends AbstractPropertyDefinition {
  
  private static final OrPropertyDefinition INSTANCE = new OrPropertyDefinition();

  public static final OrPropertyDefinition getInstance() {
    return INSTANCE;
  }

  private OrPropertyDefinition() {
    super("OR");
  }

  /**
   * Test whether this definition is an 'or'.
   */
  public boolean isOr() {
    return true;
  }

  /**
   * Get this defintion as an 'or' if it is one.
   *
   * @return this definition as an 'or' or null.
   */
  public OrPropertyDefinition asOr() {
    return this;
  }
}
