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
 * Property definition for bundling and'd properties.
 * <p>
 * @author Spence Koehler
 */
public class AndPropertyDefinition extends AbstractPropertyDefinition {
  
  private static final AndPropertyDefinition INSTANCE = new AndPropertyDefinition();

  public static final AndPropertyDefinition getInstance() {
    return INSTANCE;
  }

  private AndPropertyDefinition() {
    super("AND");
  }

  /**
   * Test whether this definition is an 'and'.
   */
  public boolean isAnd() {
    return true;
  }

  /**
   * Get this defintion as an 'and' if it is one.
   *
   * @return this definition as an 'and' or null.
   */
  public AndPropertyDefinition asAnd() {
    return this;
  }
}
