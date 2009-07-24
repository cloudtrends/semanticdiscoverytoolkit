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


import org.sd.util.tree.SimpleTreeBuilderStrategy;

/**
 * Implementation of TreeBuilderStrategy for PropertyDefinition data.
 * <p>
 * @author Spence Koehler
 */
class PropertyDefinitionTreeBuilderStrategy extends SimpleTreeBuilderStrategy<PropertyDefinition> {
  
  private static final PropertyDefinitionTreeBuilderStrategy INSTANCE = new PropertyDefinitionTreeBuilderStrategy();

  static final PropertyDefinitionTreeBuilderStrategy getInstance() {
    return INSTANCE;
  }
  
  private PropertyDefinitionTreeBuilderStrategy() {
  }

  /**
   * Construct core node data from its string representation.
   * <p>
   * @param coreDataString The string form of the core node data.
   * <p>
   * @return the core node data.
   */
  public PropertyDefinition constructCoreNodeData(String coreDataString) {
    PropertyDefinition result = null;

    final String nstring = coreDataString.toUpperCase();

    if ("AND".equals(nstring)) {
      result = AndPropertyDefinition.getInstance();
    }
    else if ("OR".equals(nstring)) {
      result = OrPropertyDefinition.getInstance();
    }
    else {
      result = new NamedPropertyDefinition(coreDataString);
    }

    return result;
  }
}
