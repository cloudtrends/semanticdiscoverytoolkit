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


import org.sd.util.PropertiesParser;
import org.sd.util.tree.Tree;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Class to manage correlated properties.
 * <p>
 * Correlated properties are those for which the value of one property becomes
 * part of the name for another. This allows for various instances of properties
 * to be specified within a single properties file.
 *
 * @author Spence Koehler
 */
public class CorrelatedProperties {

  private PropertySchema propertySchema;
  private PropertiesParser propertiesParser;
  private Map<String, Info> isaMap;

  public CorrelatedProperties(PropertySchema propertySchema, PropertiesParser propertiesParser) {
    this.propertySchema = propertySchema;
    this.propertiesParser = propertiesParser;
    this.isaMap = new HashMap<String, Info>();
  }

  /**
   * Extract properties according to the schema from the parser into the result
   * applicable to the given property name.
   */
  public final boolean extractProperties(Properties result, String propertyName) {
    boolean rv = false;

    // run through properties correlated through propertyName's value
    final Info info = getInfo(propertyName);
    if (info != null) {
      rv = true;
      for (String p : info.properties.stringPropertyNames()) {
        result.setProperty(p, info.properties.getProperty(p));
      }
    }

    return rv;
  }

  private final Info getInfo(String propertyName) {
    Info result = isaMap.get(propertyName);

    if (result == null) {
      result = correlateProperties(propertyName);
    }

    return result;
  }

  private final Info correlateProperties(String propertyName) {
    Info result = null;

    final String propertyValue = propertiesParser.getProperty(propertyName);

    if (propertyValue != null) {
      result = isaMap.get(propertyValue);

      if (result == null) {
        final Tree<PropertyDefinition> def = propertySchema.getPropertyDefinitionTree(propertyName);

        if (def != null) {
          result = new Info(propertyName, null, propertyValue, def);

          // map value back to info
          isaMap.put(propertyValue, result);

          // now recursively find schema-defined "value.*" properties
          extractGroupProperties(result);
        }
      }
    }

    return result;
  }

  private final void correlateProperties(PropertiesParser.Pair pair) {

    final Tree<PropertyDefinition> def = propertySchema.getPropertyDefinitionTree(pair.propertyName);

    if (def != null) {
      final Info info = new Info(pair.key, pair.context, pair.value, def);

      // map value back to info: value, name, type, def
      isaMap.put(pair.value, info);

      // now recursively find schema-defined "name.*" properties
      extractGroupProperties(info);
    }
  }

  private final void extractGroupProperties(Info info) {
    boolean complete = extractGroupProperties(info.schemaNode, info.properties, info.instanceName);
    info.setComplete(complete);
  }

  private final boolean extractGroupProperties(Tree<PropertyDefinition> schemaNode, Properties properties, String context) {
    boolean complete = true;

    if (schemaNode.hasChildren()) {
      final boolean needAll = !schemaNode.getData().isOr();

      for (Tree<PropertyDefinition> child : schemaNode.getChildren()) {
        final PropertyDefinition propertyDefinition = child.getData();
        if (propertyDefinition.isNamed()) {
          complete = extractNamedProperties(properties, propertyDefinition, context, needAll);
        }
        else if (propertyDefinition.isOr()) {
          complete = extractGroupProperties(child, properties, context);
        }
        else if (propertyDefinition.isAnd()) {
          complete = extractGroupProperties(child, properties, context);
        }
      }
    }

    return complete;
  }

  private final boolean extractNamedProperties(Properties properties, PropertyDefinition propertyDefinition, String context, boolean needAll) {
    boolean complete = true;

    final String type = propertyDefinition.getLabel();
    final List<PropertiesParser.Pair> pairs = propertiesParser.findProperties(type, context);

    if (pairs == null && needAll && !propertyDefinition.isOptional()) {
      complete = false;  // missing required value!
    }
    else if (pairs != null && pairs.size() > 1 && !propertyDefinition.repeats()) {
      complete = false;  // too many values!
    }

    if (pairs != null) {
      setProperty(properties, type, pairs);

      // Add new Info instances for each pair and recurse through correlateProperties
      for (PropertiesParser.Pair pair : pairs) {
        correlateProperties(pair);
      }
    }
    
    return complete;
  }

  private final void setProperty(Properties properties, String propertyName, List<PropertiesParser.Pair> pairs) {
    properties.setProperty(propertyName, toString(pairs));
  }

  private final String toString(List<PropertiesParser.Pair> pairs) {
    final StringBuilder result = new StringBuilder();

    for (PropertiesParser.Pair pair : pairs) {
      if (result.length() > 0) result.append(',');
      result.append(pair.value);
    }

    return result.toString();
  }


  /**
   * Structure to hold info about a value from the properties.
   */
  private final class Info {

    public final String propertyName;    // original property name (including context and ordinal)
    public final String context;         // context portion of property name
    public final String instanceName;    // name for an instance = value for propertyName
    public final Tree<PropertyDefinition> schemaNode;  // definition for instance type
    public final Properties properties;  // properties for the instance

    private boolean complete;

    public Info(String propertyName, String context, String instanceName, Tree<PropertyDefinition> schemaNode) {
      this.propertyName = propertyName;
      this.context = context == null ? "" : context;
      this.instanceName = instanceName;
      this.schemaNode = schemaNode;
      this.properties = new Properties();
      this.complete = false;  //(schemaNode != null) && schemaNode.hasChildren();
    }

    public final String getInstanceType() {
      return schemaNode.getData().getLabel();
    }

    public final void setComplete(boolean complete) {
      this.complete = complete;
    }

    public final boolean isComplete() {
      return complete;
    }
  }
}
