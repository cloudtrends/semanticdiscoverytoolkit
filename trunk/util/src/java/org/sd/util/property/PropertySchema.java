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


import org.sd.io.FileUtil;
import org.sd.util.tree.SimpleTreeBuilder;
import org.sd.util.tree.Tree;
import org.sd.util.tree.TreeBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 * Definition of possible property tree structures.
 * <p>
 * @author Spence Koehler
 */
public class PropertySchema {

  private static final TreeBuilder<PropertyDefinition> PROPERTY_TREE_BUILDER = new SimpleTreeBuilder<PropertyDefinition>(PropertyDefinitionTreeBuilderStrategy.getInstance());

  private Map<String, Tree<PropertyDefinition>> property2def;
  
  public PropertySchema() {
    this.property2def = new HashMap<String, Tree<PropertyDefinition>>();
  }

  public void load(File schemaFile) throws IOException {
    final BufferedReader reader = FileUtil.getReader(schemaFile);
    String line = null;
    while ((line = reader.readLine()) != null) {
      if (!"".equals(line) && line.charAt(0) != '#') {
        addDefinition(line);
      }
    }
    reader.close();
  }

  /**
   * Add a property definition mapping to this schema.
   * <p>
   * The root's label will be mapped to the tree.
   * <p>
   * Note that the root must hold a named property definition or an
   * IllegalArgumentException will be thrown.
   * <p>
   * If a property definition tree already exists with the label, an
   * IllegalStateException will be thrown.
   *
   * @param definition  The string form of a simple property definition tree 
   *                    for a named property. Note that a simple property
   *                    definition tree defines a property and its possible
   *                    parameter sets.
   */
  public void addDefinition(String definition) {
    final Tree<PropertyDefinition> def = PROPERTY_TREE_BUILDER.buildTree(definition);

    if (def == null) System.err.println("*** WARNING: null definition tree from '" + definition + "'!");

    addDefinition(def);
  }

  /**
   * Add a property definition mapping to this schema.
   * <p>
   * The root's label will be mapped to the tree.
   * <p>
   * Note that the root must hold a named property definition or an
   * IllegalArgumentException will be thrown.
   * <p>
   * If a property definition tree already exists with the label, an
   * IllegalStateException will be thrown.
   *
   * @param def  A simple property definition tree for a named property.
   *             Note that a simple property definition tree defines a property
   *             and its possible parameter sets.
   */
  void addDefinition(Tree<PropertyDefinition> def) {
    if (def == null) return;

    if (!def.getData().isNamed()) {
      throw new IllegalArgumentException("Top-level schema definition must be a named property (i.e. cannot be 'and' or 'or')!");
    }

    final String property = def.getData().getLabel();

    if (property2def.containsKey(property)) {
      throw new IllegalStateException("Duplicate property '" + property + "' cannot be defined!");
    }

    property2def.put(property, def);
  }

  /**
   * Retrieve the property definition tree having the given label at its root.
   * <p>
   * Note that the label must be the name of a property without a repeat marker.
   *
   * @return the property definition tree or null if not found.
   */
  Tree<PropertyDefinition> getPropertyDefinitionTree(String property) {
    return property2def.get(property);
  }

//todo: add public accessors to validate property parameters.
}
